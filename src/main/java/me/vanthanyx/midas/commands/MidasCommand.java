package me.vanthanyx.midas.commands;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import me.vanthanyx.midas.Helium;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MidasCommand implements CommandExecutor {

  public final Helium plugin;
  private FileConfiguration config;
  public Connection conn;

  public MidasCommand(Helium plugin) {
    this.plugin = plugin;
    this.config = plugin.getConfig();
  }

  public boolean onCommand(
    CommandSender sender,
    Command command,
    String label,
    String[] args
  ) {
    try {
      if (args.length == 0) {
        sender.sendMessage("Incomplete command");
        return true;
      }
      File dataFolder = plugin.pullDataFolder();

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

      if (!(sender instanceof Player)) {
        sender.sendMessage("Players only.");
        return true;
      }

      Player player = (Player) sender;
      UUID playerUUID = player.getUniqueId();
      String playerName = player.getName();

      try {
        PreparedStatement stmt = conn.prepareStatement(
          "SELECT data FROM players WHERE uuid = ?"
        );
        stmt.setString(1, playerUUID.toString());
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
          String dataValue = rs.getString("data");
          if (dataValue.equals("false")) {
            sender.sendMessage("§cYou are not a MIDAS client.");
            return true;
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
        return false;
      }

      if (args.length == 1 && args[0].equals("--dontSendData")) {
        sender.sendMessage(
          "§eTo confirm data collection opt out, type \"/midas --dontSendData --registryErase\""
        );
      }
      if (
        args.length == 2 &&
        args[0].equals("--dontSendData") &&
        args[1].equals("--registryErase")
      ) {
        try {
          PreparedStatement pstmt = conn.prepareStatement(
            "UPDATE players SET id = NULL WHERE uuid = ?"
          );
          pstmt.setString(1, playerUUID.toString());
          pstmt.executeUpdate();
          try (
            PreparedStatement stmt = conn.prepareStatement(
              "UPDATE players SET data = ? WHERE uuid = ?"
            )
          ) {
            stmt.setString(1, "false");
            stmt.setString(2, playerUUID.toString());
            stmt.executeUpdate();
          } catch (SQLException e) {
            e.printStackTrace();
            return false;
          }
          sender.sendMessage("§c§l[MIDAS]");
          sender.sendMessage("§cEND USER POLICY");
          sender.sendMessage("§c - All Data Collection Has Stopped");
          sender.sendMessage("§c - Existing Data Will Not Be Deleted");
          sender.sendMessage("§c - User's MIDAS ID Has Been Voided");
          sender.sendMessage(
            "[Server] MIDAS has stopped surveillance on " + playerName
          );
          sender.sendMessage("[Server] " + playerUUID);
          return true;
        } catch (SQLException e) {
          e.printStackTrace();
          return false;
        }
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
