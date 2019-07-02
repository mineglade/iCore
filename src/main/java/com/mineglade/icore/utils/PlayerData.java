package com.mineglade.icore.utils;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.mineglade.icore.ICore;

import net.md_5.bungee.api.ChatColor;
import xyz.derkades.derkutils.bukkit.Colors;

/**
 * handles all player data in either mysql or a yaml file.
 * 
 * @author MineGlade
 */
public class PlayerData {

	private OfflinePlayer player;

	private boolean mysql = ICore.instance.getConfig().getBoolean("mysql.enabled");
	private FileConfiguration dataFile;
	private File dataFileFile;

	/**
	 * establishes player, writes and establishes datafilefile.
	 * 
	 * @param player
	 */
	public PlayerData(OfflinePlayer player) {
		this.player = player;
		if (!mysql) {
			File dataFileFileFolder = new File(ICore.instance.getDataFolder(), "playerdata");
			dataFileFileFolder.mkdirs();
			dataFileFile = new File(dataFileFileFolder, player.getUniqueId() + ".yaml");
			this.dataFile = YamlConfiguration.loadConfiguration(dataFileFile);
		}
	}

	public PlayerData(File file) {
		this.player = null;
		dataFileFile = file;
		this.dataFile = YamlConfiguration.loadConfiguration(dataFileFile);
	}

	/**
	 * saves playerdata file.
	 */
	public void save() {
		try {
			dataFile.save(dataFileFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * gets chat-color character from mysql or playerdata file.
	 * 
	 * @return ChatColor object
	 */
	public ChatColor getChatColor() {

		if (mysql) {
			try {
				PreparedStatement statement = ICore.db
						.prepareStatement("SELECT `color` FROM `playerChatColor` WHERE uuid=?", player.getUniqueId());
				ResultSet result = statement.executeQuery();

				if (result.next()) {
					return ChatColor.getByChar(result.getString("color").charAt(0));
				} else {
					return ChatColor
							.getByChar(ICore.instance.getConfig().getString("chat.default-chat-color").charAt(0));
				}

			} catch (SQLException e) {
				e.printStackTrace();
				return ChatColor.getByChar(ICore.instance.getConfig().getString("chat.default-chat-color").charAt(0));
			}
		} else {
			return ChatColor.getByChar(dataFile
					.getString("color.chat-color", ICore.instance.getConfig().getString("chat.default-chat-color"))
					.charAt(0));
		}
	}

	/**
	 * sets a chat-color character in the mysql or playerdata file.
	 * 
	 * @param color <br>
	 *              &nbsp;&nbsp;example: <code>c</code>
	 */
	public void setChatColor(char color) {
		if (mysql) {
			Bukkit.getScheduler().runTaskAsynchronously(ICore.instance, () -> {
				try {
					PreparedStatement statement = ICore.db.prepareStatement(
							"INSERT INTO playerChatColor (uuid, color) VALUES (?, ?) ON DUPLICATE KEY UPDATE color=?",
							player.getUniqueId(), color, color);
					statement.execute();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
		} else {
			dataFile.set("color.chat-color", color);
			this.save();
		}
	}

	/**
	 * removes chat-color entry from mysql or playerdata file.
	 */
	public void resetChatColor() {
		if (mysql) {
			Bukkit.getScheduler().runTaskAsynchronously(ICore.instance, () -> {
				try {
					PreparedStatement statement = ICore.db.prepareStatement("DELETE FROM playerChatColor WHERE uuid=?",
							player.getUniqueId());
					statement.execute();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
		} else {
			dataFile.set("color.chat-color", null);
			this.save();
		}
	}

	/**
	 * gets chat-format character from mysql or playerdata file.
	 * 
	 * @return
	 */
	public ChatColor getChatFormat() {
		if (mysql) {
			try {
				PreparedStatement statement = ICore.db
						.prepareStatement("SELECT `color` FROM `playerChatFormat` WHERE uuid=?", player.getUniqueId());
				ResultSet result = statement.executeQuery();

				if (result.next()) {
					return ChatColor.getByChar(result.getString("color").charAt(0));
				} else {
					return ChatColor
							.getByChar(ICore.instance.getConfig().getString("chat.default-chat-format").charAt(0));
				}

			} catch (SQLException e) {
				e.printStackTrace();
				return ChatColor.getByChar(ICore.instance.getConfig().getString("chat.default-chat-format").charAt(0));
			}
		} else {
			return ChatColor.getByChar(dataFile
					.getString("color.chat-format", ICore.instance.getConfig().getString("chat.default-chat-format"))
					.charAt(0));
		}
	}

	/**
	 * sets a chat-format character in the mysql or playerdata file.
	 * 
	 * @param color <br>
	 *              &nbsp;&nbsp;example: <code>c</code>
	 */
	public void setChatFormat(String color) {
		if (mysql) {
			Bukkit.getScheduler().runTaskAsynchronously(ICore.instance, () -> {
				try {
					PreparedStatement statement = ICore.db.prepareStatement(
							"INSERT INTO playerChatFormat (uuid, color) VALUES (?, ?) ON DUPLICATE KEY UPDATE color=?",
							player.getUniqueId(), color, color);
					statement.execute();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
		} else {
			dataFile.set("color.chat-format", color);
			this.save();
		}
	}

	/**
	 * removes the chatFormat entry from mysql or playerdata file.
	 */
	public void resetChatFormat() {
		if (mysql) {
			Bukkit.getScheduler().runTaskAsynchronously(ICore.instance, () -> {
				try {
					PreparedStatement statement = ICore.db.prepareStatement("DELETE FROM playerChatFormat WHERE uuid=?",
							player.getUniqueId());
					statement.execute();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
		} else {
			dataFile.set("color.chat-format", null);
			this.save();
		}
	}

	/**
	 * gets name-color from mysql or playerdata file.
	 * 
	 * @return ChatColor object
	 */
	public ChatColor getNameColor() {
		if (ICore.instance.getConfig().getBoolean("mysql.enabled")) {
			try {
				PreparedStatement statement = ICore.db
						.prepareStatement("SELECT `color` FROM `playerNameColor` WHERE uuid=?", player.getUniqueId());
				ResultSet result = statement.executeQuery();

				if (result.next()) {
					return ChatColor.getByChar(result.getString("color").charAt(0));
				} else {
					return ChatColor
							.getByChar(ICore.instance.getConfig().getString("chat.default-name-color").charAt(0));
				}

			} catch (SQLException e) {
				e.printStackTrace();
				return ChatColor.getByChar(ICore.instance.getConfig().getString("chat.default-name-color").charAt(0));
			}
		} else {
			return ChatColor.getByChar(dataFile
					.getString("color.name-color", ICore.instance.getConfig().getString("chat.default-name-color"))
					.charAt(0));
		}
	}

	/**
	 * sets a name-color character in the mysql or playerdata file.
	 * 
	 * @param color <br>
	 *              &nbsp;&nbsp;example: <code>c</code>
	 */
	public void setNameColor(char color) {
		if (mysql) {
			Bukkit.getScheduler().runTaskAsynchronously(ICore.instance, () -> {
				try {
					PreparedStatement statement = ICore.db.prepareStatement(
							"INSERT INTO playerNameColor (uuid, color) VALUES (?, ?) ON DUPLICATE KEY UPDATE color=?",
							player.getUniqueId(), color, color);
					statement.execute();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
		} else {
			dataFile.set("color.name-color", color);
			this.save();
		}
	}

	/**
	 * removes the name-color entry from mysql or playerdata file.
	 */
	public void resetNameColor() {
		if (mysql) {
			Bukkit.getScheduler().runTaskAsynchronously(ICore.instance, () -> {
				try {
					PreparedStatement statement = ICore.db.prepareStatement("DELETE FROM playerNameColor WHERE uuid=?",
							player.getUniqueId());
					statement.execute();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
		} else {
			dataFile.set("color.name-color", null);
			this.save();
		}
	}

	/**
	 * gets chat-format from mysql or playerdata file.
	 * 
	 * @return ChatColor object
	 */
	public ChatColor getNameFormat() {
		if (ICore.instance.getConfig().getBoolean("mysql.enabled")) {
			try {
				PreparedStatement statement = ICore.db
						.prepareStatement("SELECT `color` FROM `playerNameFormat` WHERE uuid=?", player.getUniqueId());
				ResultSet result = statement.executeQuery();

				if (result.next()) {
					return ChatColor.getByChar(result.getString("color").charAt(0));
				} else {
					return ChatColor
							.getByChar(ICore.instance.getConfig().getString("chat.default-name-format").charAt(0));
				}

			} catch (SQLException e) {
				e.printStackTrace();
				return ChatColor.getByChar(ICore.instance.getConfig().getString("chat.default-name-format").charAt(0));
			}
		} else {
			return ChatColor.getByChar(dataFile
					.getString("color.name-format", ICore.instance.getConfig().getString("chat.default-name-format"))
					.charAt(0));
		}

	}

	/**
	 * sets a name-format character in the mysql or playerdata file.
	 * 
	 * @param color <br>
	 *              &nbsp;&nbsp;example: <code>c</code>
	 */
	public void setNameFormat(String color) {
		if (mysql) {
			Bukkit.getScheduler().runTaskAsynchronously(ICore.instance, () -> {
				try {
					PreparedStatement statement = ICore.db.prepareStatement(
							"INSERT INTO playerNameFormat (uuid, color) VALUES (?, ?) ON DUPLICATE KEY UPDATE color=?",
							player.getUniqueId(), color, color);
					statement.execute();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
		} else {
			dataFile.set("color.name-format", color);
			this.save();
		}
	}

	/**
	 * removes the name-format entry from mysql or playerdata file.
	 */
	public void resetNameFormat() {
		if (mysql) {
			Bukkit.getScheduler().runTaskAsynchronously(ICore.instance, () -> {
				try {
					PreparedStatement statement = ICore.db.prepareStatement("DELETE FROM playerNameFormat WHERE uuid=?",
							player.getUniqueId());
					statement.execute();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
		} else {
			dataFile.set("color.name-format", null);
			this.save();
		}
	}

	/**
	 * sets a nickname in the mysql or playerdata file. (maximum of 16 characters,
	 * ignoring color codes).
	 * 
	 * @param nickname <br>
	 *                 &nbsp;&nbsp;example: <code>&aThis&cIs&bMy&eNickname</code>
	 * @return
	 */
	public void setNickName(CommandSender sender, String nickname, Consumer<Boolean> callback) {

		Bukkit.getScheduler().runTaskAsynchronously(ICore.instance, () -> {
			
			boolean nickNameExists = false;
			// Checking if the nickname already exists

			// Don't even start checking if the sender has the permission to set an existing nickname.
			if (!sender.hasPermission("icore.command.nickname.existing")) {
				// MySQL check
				if (mysql) {
					try {
						PreparedStatement nameStatement = ICore.db.prepareStatement(
								"SELECT `nickname`,`username` FROM `playerNickName`,`playerUserName`");
						ResultSet nameResults = nameStatement.executeQuery();
						while (nameResults.next()) {
							if (Colors.stripColors(nameResults.getString("nickname")).equalsIgnoreCase(Colors.stripColors(nickname))
									|| nameResults.getString("username").equalsIgnoreCase(Colors.stripColors(nickname))) {
								nickNameExists = true;
								break;
							}
						}
					} catch (SQLException e) {
						e.printStackTrace();
						return;
					}
				} 
				
				// flatfile check
				else { 
					if (!Colors.stripColors(nickname).equalsIgnoreCase(Colors.stripColors(getNickName()))
							&& !Colors.stripColors(nickname).equalsIgnoreCase(getLastUsername())) {
						for (PlayerData file : getAllDataFiles()) {
							if ((player.getUniqueId() + ".yaml").equals(file.getFileName())) {
								break;
							}
							String existingNickName = ChatColor.stripColor(Colors.parseColors(file.getNickName()));
							String existingName = file.getLastUsername();
							if (existingNickName.equalsIgnoreCase(ChatColor.stripColor(Colors.parseColors(nickname)))
									|| existingName.equalsIgnoreCase(ChatColor.stripColor(Colors.parseColors(nickname)))) {
								nickNameExists = true;
								break;
							}
						}
					}
				}
			}
			
			// Setting the nickname
			
			// Only if the nickname doesn't exist.
			if (!nickNameExists) {
				if (mysql) {
					try {
						PreparedStatement setStatement = ICore.db.prepareStatement(
								"INSERT INTO playerNickName (uuid, nickname) VALUES (?, ?) ON DUPLICATE KEY UPDATE nickname=?",
								player.getUniqueId(), nickname, nickname);
						setStatement.execute();
					} catch (SQLException e) {
						
					}
				} else {
					dataFile.set("nickname", nickname);
					save();
				}
				if (player.isOnline()) {
					Player onlinePlayer = (Player) player;
					onlinePlayer.setDisplayName(ChatColor.stripColor(nickname));
				}
				callback.accept(!nickNameExists);
			} 
			
			// If the nickname does exist:
			else {
				callback.accept(!nickNameExists);
			}

		});
	}

	/**
	 * gets a nickname from the mysql or playerdata file. (maximum of 16 characters,
	 * ignoring color codes).
	 * 
	 * @return iCore nickname <br>
	 *         &nbsp;&nbsp;example: <code>&aThis&cIs&bMy&eNickname</code>
	 */
	public String getNickName() {

		if (mysql) {
			try {
				PreparedStatement statement = ICore.db
						.prepareStatement("SELECT `nickname` FROM `playerNickName` WHERE uuid=?", player.getUniqueId());
				ResultSet result = statement.executeQuery();

				if (result.next()) {
					return Colors.parseColors(result.getString("nickname"));
				} else {
					if (player.isOnline()) {
						Player onlinePlayer = (Player) player;
						return onlinePlayer.getDisplayName();
					} else {
						return "";
					}
				}

			} catch (SQLException e) {
				e.printStackTrace();
				if (player.isOnline()) {
					Player onlinePlayer = (Player) player;
					return onlinePlayer.getDisplayName();
				} else {
					return "";
				}
			}
		} else {
			return dataFile.getString("nickname", getLastUsername());

		}
	}

	/**
	 * removes the nickname entry from the mysql or playerdata file.
	 */
	public void resetNickName() {

		if (mysql) {
			Bukkit.getScheduler().runTaskAsynchronously(ICore.instance, () -> {
				try {
					PreparedStatement statement = ICore.db.prepareStatement("DELETE FROM playerNickName WHERE uuid=?",
							player.getUniqueId());
					statement.execute();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
		} else {
			dataFile.set("nickname", null);
			save();
		}
		if (player.isOnline()) {
			Player onlinePlayer = (Player) player;
			onlinePlayer.setDisplayName(onlinePlayer.getName());
		}
	}

	/**
	 * gets the last username a player connected to the server with from the
	 * playerdatabase.
	 * 
	 * @return Minecraft username
	 */
	public String getLastUsername() {
		if (mysql) {
			try {
				PreparedStatement statement = ICore.db
						.prepareStatement("SELECT `username` FROM `playerUserName` WHERE uuid=?", player.getUniqueId());
				ResultSet result = statement.executeQuery();

				if (result.next()) {
					return result.getString("username");
				} else {
					return player.getName();
				}

			} catch (SQLException e) {
				e.printStackTrace();
				return player.getName();
			}
		} else {
			return dataFile.getString("last-login.username");

		}
	}

	/**
	 * sets the username a player is currently connected to the server with to the
	 * playerdatabase.
	 * 
	 * @param username (player.getName())
	 */
	public void setLastUsername() {
		if (mysql) {
			Bukkit.getScheduler().runTaskAsynchronously(ICore.instance, () -> {
				try {
					PreparedStatement statement = ICore.db.prepareStatement(
							"INSERT INTO playerUserName (uuid, username) VALUES (?, ?) ON DUPLICATE KEY UPDATE username=?",
							player.getUniqueId(), player.getName(), player.getName());
					statement.execute();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			});
		} else {
			dataFile.set("last-login.username", player.getName());
			save();
		}
	}

	/**
	 * gets the IP-address the player last connected to the server with from the
	 * playerdatabase.
	 * 
	 * @return IP-address
	 */
	public String getLastConnectionAddress() {
		return "temp";
	}

	/**
	 * Sets the IP-address the player is currently connected to the server with in
	 * the playerdatabase.
	 */
	public void setLastConnectionAddress() {

	}

	public String getFileName() {
		return dataFileFile.getName();
	}

	public static List<PlayerData> getAllDataFiles() {
		List<PlayerData> dataFileList = new ArrayList<>();
		File dataFileFileFolder = new File(ICore.instance.getDataFolder(), "playerdata");
		dataFileFileFolder.mkdirs();
		for (File file : dataFileFileFolder.listFiles()) {
			PlayerData data = new PlayerData(file);
			dataFileList.add(data);
		}
		return dataFileList;
	}
}