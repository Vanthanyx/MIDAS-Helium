package me.vanthanyx.midas.handlers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import me.vanthanyx.midas.Helium;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinHandler implements Listener {

  public final Helium plugin;
  private FileConfiguration config;
  public Connection conn;

  public JoinHandler(Helium plugin) {
    this.plugin = plugin;
    this.config = plugin.getConfig();
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID playerUUID = player.getUniqueId();
    String playerName = player.getName();

    File dataFolder = plugin.pullDataFolder();
    File databaseFile = new File(dataFolder, "registry.db");
    if (databaseFile.exists()) {
      // Step 1: Add the SQLite JDBC driver to the classpath
      try {
        Class.forName("org.sqlite.JDBC");
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

      // Step 2: Connect to the SQLite database
      try {
        String dbFile = "registry.db";
        String url =
          "jdbc:sqlite:" +
          dataFolder.getAbsolutePath() +
          File.separator +
          dbFile;
        conn = DriverManager.getConnection(url);
      } catch (SQLException e) {
        e.printStackTrace();
      }

      try (
        PreparedStatement pstmt = conn.prepareStatement(
          "CREATE TABLE IF NOT EXISTS players (id INT, uuid TEXT PRIMARY KEY, name TEXT, joins INT, hours INT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, data TEXT)"
        )
      ) {
        pstmt.executeUpdate();
      } catch (SQLException e) {
        e.printStackTrace();
      }

      boolean playerExists = false;
      try (
        PreparedStatement pstmt = conn.prepareStatement(
          "SELECT uuid FROM players WHERE uuid = ?"
        )
      ) {
        pstmt.setString(1, playerUUID.toString());
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            playerExists = true;
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }

      int playerID = 0;
      int playerJoins = 0;
      String playerData = "true";
      try (
        PreparedStatement pstmt = conn.prepareStatement(
          "SELECT MAX(id) FROM players"
        )
      ) {
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            playerID = rs.getInt(1) + 1;
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }

      String getPlayerUUID = player.getUniqueId().toString();
      try {
        PreparedStatement stmt = conn.prepareStatement(
          "UPDATE players SET joins = joins + 1 WHERE uuid = ?"
        );
        stmt.setString(1, getPlayerUUID);
        stmt.executeUpdate();
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        PreparedStatement stmt = conn.prepareStatement(
          "UPDATE players SET data = true WHERE uuid = ?"
        );
        stmt.setString(1, getPlayerUUID);
        stmt.executeUpdate();
      } catch (SQLException e) {
        e.printStackTrace();
      }

      boolean hidePlayerIDs = config.getBoolean("hidePlayerIDs");
      String playerIDValue;

      if (hidePlayerIDs) {
        playerIDValue = "§c§lHIDDEN";
      } else {
        playerIDValue = String.valueOf(playerID);
      }

      if (playerExists) {
        // player already in database
        try (
          PreparedStatement pstmt = conn.prepareStatement(
            "SELECT id FROM players WHERE uuid = ?"
          )
        ) {
          pstmt.setString(1, getPlayerUUID);
          try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
              playerID = rs.getInt(1);
            }
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
        if (conn != null) {
          try {
            conn.close();
          } catch (SQLException e) {
            e.printStackTrace();
          }
        }
      } else {
        saveToDB(playerID, playerUUID, playerName, playerJoins);
        player.sendMessage("§n§lWelcome to the Aether Network");
        player.sendMessage(
          "§7Using our new software line MIDAS, we will be improving your experience through data collection, to opt out of this type \"/midas --dontSendData\". This is highly discouraged as you'll lose access to MIDAS events and programs, thank you."
        );
        player.sendMessage(
          "§o§8You have been registered to the database with ID: §f§l" +
          playerIDValue
        );
        player.sendMessage("§o§8Player Name: §f§l" + playerName);
        if (conn != null) {
          try {
            conn.close();
          } catch (SQLException e) {
            e.printStackTrace();
          }
        }
      }
    } else {
      // Step 1: Add the SQLite JDBC driver to the classpath
      try {
        Class.forName("org.sqlite.JDBC");
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

      // Step 2: Connect to the SQLite database
      try {
        String dbFile = "registry.db";
        String url =
          "jdbc:sqlite:" +
          dataFolder.getAbsolutePath() +
          File.separator +
          dbFile;
        conn = DriverManager.getConnection(url);
      } catch (SQLException e) {
        e.printStackTrace();
      }

      try (
        PreparedStatement pstmt = conn.prepareStatement(
          "CREATE TABLE IF NOT EXISTS players (id INT, uuid TEXT PRIMARY KEY, name TEXT, joins INT, hours INT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, data TEXT)"
        )
      ) {
        pstmt.executeUpdate();
      } catch (SQLException e) {
        e.printStackTrace();
      }
      onPlayerJoin(event);
    }

    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  public void saveToDB(
    int playerID,
    UUID playerUUID,
    String playerName,
    int playerJoins
  ) {
    try (
      PreparedStatement pstmt = conn.prepareStatement(
        "INSERT INTO players (id, uuid, name, joins, data) VALUES (?, ?, ?, ?, ?)"
      )
    ) {
      pstmt.setString(1, String.valueOf(playerID));
      pstmt.setString(2, playerUUID.toString());
      pstmt.setString(3, playerName);
      pstmt.setString(4, String.valueOf(playerJoins));
      pstmt.setString(5, "true");

      pstmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
