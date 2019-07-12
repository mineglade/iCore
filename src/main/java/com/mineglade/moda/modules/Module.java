package com.mineglade.moda.modules;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.UnknownDependencyException;

import com.mineglade.moda.Moda;
import com.mineglade.moda.modules.chat.ChatModule;
import com.mineglade.moda.modules.mute.MuteModule;
import com.mineglade.moda.modules.votes.Votes;
import com.mineglade.moda.utils.InvalidModuleException;
import com.mineglade.moda.utils.JarLoader;
import com.mineglade.moda.utils.storage.DatabaseStorageHandler;
import com.mineglade.moda.utils.storage.FileStorageHandler;
import com.mineglade.moda.utils.storage.ModuleStorageHandler;
import com.mineglade.moda.utils.storage.StorageType;

import xyz.derkades.derkutils.FileUtils;

public abstract class Module<T extends ModuleStorageHandler> implements Listener {

	public static final Module<? extends ModuleStorageHandler>[] INTERNAL_MODULES = new Module<?>[]{
			new Votes(),
			new ChatModule(),
			new MuteModule(),
	};

	public static final List<Module<? extends ModuleStorageHandler>> LOADED = new ArrayList<>();

	public static final List<Module<? extends ModuleStorageHandler>> ENABLED = new ArrayList<>();

	protected Moda plugin;
	protected ModuleLogger logger;
	protected FileConfiguration config;
	protected LangFile lang;
	protected Scheduler scheduler;
	protected T storage;

	private boolean external;

	public Module() {
		this.plugin = Moda.instance;
	}

	public abstract String getName();

	public void onLoad() {}

	public void onEnable() {}

	public void onDisable() {}

	public abstract IMessage[] getMessages();

	public abstract FileStorageHandler getFileStorageHandler();

	public abstract DatabaseStorageHandler getDatabaseStorageHandler();

	public String[] getPluginDependencies() {
		return new String[] {};
	}

	public final File getDataFolder() {
		return new File("modules", this.getName());
	}

	public boolean isExternal() {
		return this.external;
	}

	@SuppressWarnings("unchecked")
	public static void loadExternal(final File jarFile) throws Exception {
		final JarLoader jarLoader = new JarLoader(Moda.instance);
		try (final ZipFile zip = new ZipFile(jarFile)){
			final ZipEntry moduleYamlEntry = zip.getEntry("module.yaml");

			if (moduleYamlEntry == null) {
				throw new InvalidModuleException("Module jar does not contain 'module.yaml' file.");
			}

			final InputStream inputStream = zip.getInputStream(moduleYamlEntry);
			final Reader reader = new InputStreamReader(inputStream);
			final FileConfiguration yaml = YamlConfiguration.loadConfiguration(reader);

			String mainClassName;
			if (yaml.contains("main")){
				mainClassName = yaml.getString("main");
			} else {
				throw new InvalidModuleException("No main class specified");
			}

			jarLoader.loadJar(jarFile);

			Class<?> mainClass;

			try {
				mainClass = Class.forName(mainClassName);
			} catch (final ClassNotFoundException e) {
				throw new InvalidModuleException("Main class not found: " + mainClassName);
			}

			final Object mainClassInstance = mainClass.newInstance();

			Module<? extends ModuleStorageHandler> module;

			if (mainClassInstance instanceof Module) {
				module = (Module<? extends ModuleStorageHandler>) mainClass.newInstance();
			} else {
				throw new InvalidModuleException("Main class is not a subclass of Module");
			}

			module.external = true;
			final String moduleName = module.getName();

			for (final Module<? extends ModuleStorageHandler> loadedModule : LOADED) {
				if (loadedModule.getName().equalsIgnoreCase(moduleName)) {
					throw new IllegalStateException("A module with the name " + moduleName + " is already loaded");
				}
			}

			LOADED.add(module);
			module.initLogger();
			module.loadLang();

			final ZipEntry configYamlEntry = zip.getEntry("config.yaml");

			if (configYamlEntry == null) {
				module.logger.debug("Module jar does not contain 'config.yaml' file.");
			} else {
				final File output = new File(module.getDataFolder(), "config.yaml");
				if (!output.exists()) {
					module.logger.debug("Config yaml file does not exist, copying from jar file..");
					final InputStream inputStream2 = zip.getInputStream(configYamlEntry);
					final Path outputPath = Paths.get(output.toURI());
					Files.copy(inputStream2, outputPath);
				} else {
					module.logger.debug("Config yaml file already exists");
				}
			}

			module.onLoad();
		} catch (final Exception e) {
			throw new Exception(e);
		}
	}

	public static void loadInternal(final Module<? extends ModuleStorageHandler> module) throws Exception {
		module.external = false;

		final String moduleName = module.getName();

		for (final Module<? extends ModuleStorageHandler> loadedModule : LOADED) {
			if (loadedModule.getName().equalsIgnoreCase(moduleName)) {
				throw new IllegalStateException("A module with the name " + moduleName + " is already loaded");
			}
		}

		LOADED.add(module);

		module.initLogger();
		module.loadLang();
		try {
			final File configOutputFile = new File(module.getDataFolder(), "config.yaml");
			module.logger.debug("Internal module, copying config from %s",  "/modules/" + module.getName() + "/config.yaml");
			FileUtils.copyOutOfJar(module.getClass(), "/modules/" + module.getName() + "/config.yaml", configOutputFile);
			module.config = YamlConfiguration.loadConfiguration(configOutputFile);
		} catch (final NullPointerException e) {
			module.logger.debug("Module does not have a config file");
		}

		module.onLoad();
	}

	protected final void initLogger() {
		// Initialize logger
		this.logger = new ModuleLogger(Moda.instance.getLogger(), this);
	}

	protected final void loadLang() throws IOException {
		// Load language file
		if (this.getMessages() != null) {
			final File langFile = new File(this.getDataFolder(), "lang.yaml");
			this.lang = new LangFile(langFile, this.getMessages());
		}
	}

	@SuppressWarnings("unchecked")
	public void enable() throws Exception {
		if (ENABLED.contains(this)) {
			throw new IllegalStateException("This module is already enabled");
		}

		if (!LOADED.contains(this)) {
			throw new IllegalStateException("This module has not been loaded");
		}

		// Check dependencies
		for (final String dependencyString : this.getPluginDependencies()) {
			final Plugin dependency = Bukkit.getPluginManager().getPlugin(dependencyString);
			if ((dependency == null) || !dependency.isEnabled()) {
				//this.logger.severe("This module could not be enabled, because it requires the plugin " + dependencyString);
				throw new UnknownDependencyException("This module could not be enabled, because it requires the plugin " + dependencyString);
			}
		}

		// Initialize scheduler
		this.scheduler = new Scheduler(this);

		// Initialize data storage
		final StorageType storageType = Moda.instance.getStorageType();
		if (storageType == StorageType.MYSQL) {
			if (this.getDatabaseStorageHandler() != null) {
				this.logger.debug("Mysql enabled, using database");
				final DatabaseStorageHandler handler = this.getDatabaseStorageHandler();
				handler.setDatabaseHandler(Moda.db);
				handler.setup();
				this.storage = (T) handler;
			} else {
				this.logger.debug("No mysql storage handler provided, using file storage instead.");
				final FileStorageHandler handler = this.getFileStorageHandler();
				this.storage = (T) handler;

				// Save config periodically
				this.scheduler.interval(5*60*20, 5*60*20, () -> {
					this.scheduler.async(handler::save);
				});
			}
		} else if (storageType == StorageType.FILE) {
			this.logger.debug("Using file storage");
			final FileStorageHandler handler = this.getFileStorageHandler();
			this.storage = (T) handler;

			// Save config periodically
			this.scheduler.interval(5*60*20, 5*60*20, () -> {
				this.logger.debug("Saving config");
				this.scheduler.async(handler::save);
			});
		} else {
			throw new AssertionError();
		}

		// Register listeners
		Bukkit.getPluginManager().registerEvents(this, this.plugin);

		// Initialize scheduler
		this.scheduler = new Scheduler(this);

		this.logger.debug("Enabled");

		ENABLED.add(this);
	}

	public final void disable() throws Exception {
		if (!ENABLED.contains(this)) {
			throw new IllegalStateException("This module is not enabled");
		}

		HandlerList.unregisterAll(this);
		Scheduler.cancelAllTasks(this);

		if (this.storage instanceof FileStorageHandler) {
			((FileStorageHandler) this.storage).saveBlocking();
		}

		this.logger.debug("Disabled");

		ENABLED.remove(this);
	}

	protected void registerCommand(final Command command) {
		this.logger.debug("Registering command: [name=%s, description=%s, usage=%s, aliases=%s",
				command.getName(),
				command.getDescription(),
				command.getUsage(),
				String.join(".", command.getAliases().toArray(new String[] {})));
		try {
			final Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			field.setAccessible(true);
			final CommandMap map = (CommandMap) field.get(Bukkit.getServer());
			map.register(Moda.instance.getName(), command);
		} catch (final IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	public static Module<? extends ModuleStorageHandler> getLoadedModuleByName(final String name){
		for (final Module<? extends ModuleStorageHandler> module : Module.LOADED) {
			if (module.getName().equals(name)) {
				return module;
			}
		}
		return null;
	}

}
