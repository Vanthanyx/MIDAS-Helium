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
import java.util.Calendar;

public class JoinIncrement implements Listener {

  public final Helium plugin;
  private FileConfiguration config;
  public Connection conn;

  public JoinIncrement(Helium plugin) {
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
                          "CREATE TABLE IF NOT EXISTS joins (monday INT, tuesday INT, wednesday INT, thursday INT, friday INT, saturday INT, sunday INT)"
                  )
      ) {
              pstmt.executeUpdate();
          } catch (SQLException e) {
              e.printStackTrace();
          }

          Calendar calendar = Calendar.getInstance();
          int day = calendar.get(Calendar.DAY_OF_WEEK);
          String dayName = "";
          switch (day) {
              case 1: dayName = "sunday"; break;
              case 2: dayName = "monday"; break;
              case 3: dayName = "tuesday"; break;
              case 4: dayName = "wednesday"; break;
              case 5: dayName = "thursday"; break;
              case 6: dayName = "friday"; break;
              case 7: dayName = "saturday"; break;
          }
          System.out.println(dayName);

          try {
              PreparedStatement stmt = conn.prepareStatement(
                      "UPDATE joins SET ? = ? + 1"
              );
              stmt.setString(1, dayName);
              stmt.setString(2, dayName);
              stmt.executeUpdate();
          } catch (SQLException e) {
              e.printStackTrace();
          }

      }
  }
}
