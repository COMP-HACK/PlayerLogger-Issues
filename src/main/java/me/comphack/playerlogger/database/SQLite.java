package me.comphack.playerlogger.database;

import me.comphack.playerlogger.PlayerLogger;
import me.comphack.playerlogger.data.PlayerChat;
import me.comphack.playerlogger.data.PlayerLog;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SQLite implements Database {

    private PlayerLogger plugin;
    private Connection connection;

    public SQLite(PlayerLogger plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("Using storage method [SQLITE]");
        File dataFolder = plugin.getDataFolder();
        if(!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dataFile = new File(dataFolder, "data.db");
        if(!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String url = "jdbc:sqlite:" + dataFile;

        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(url);
            plugin.getLogger().info("Connected to MySQL database successfully");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            plugin.getLogger().info("Could not connect to SQLite database");
        }
        init();
    }

    @Override
    public void init() {
        String SQL = "CREATE TABLE IF NOT EXISTS player_logs " +
                "(username VARCHAR(16) PRIMARY KEY," +
                " uuid VARCHAR(32)," +
                " ip_address VARCHAR(32)," +
                " last_join_date VARCHAR(32)," +
                " logout_world VARCHAR(32)," +
                " logout_x VARCHAR(32)," +
                " logout_y VARCHAR(32)," +
                " logout_z VARCHAR(32)," +
                " firstjoin_world VARCHAR(32)," +
                " firstjoin_x VARCHAR(32)," +
                " firstjoin_y VARCHAR(32)," +
                " firstjoin_z VARCHAR(32));";
        String SQL2 = " CREATE TABLE IF NOT EXISTS chat_logs" +
                " (username VARCHAR(16), " +
                " message VARCHAR(256), " +
                " date_and_time VARCHAR(32));";
        String SQL3 = " CREATE TABLE IF NOT EXISTS command_logs" +
                " (username VARCHAR(16), " +
                " command VARCHAR(256), " +
                " date_and_time VARCHAR(32));";

        try {
            PreparedStatement ps = connection.prepareStatement(SQL);
            ps.executeUpdate();
            ps = connection.prepareStatement(SQL2);
            ps.executeUpdate();
            ps = connection.prepareStatement(SQL3);
            ps.executeUpdate();
        } catch (SQLException e ) {
            e.printStackTrace();
        }
    }

    public boolean isDebugMode() {
        return plugin.getConfig().getBoolean("general.debug-mode");
    }

    public void sendDebugLog(String s) {
        plugin.getLogger().info(s);
    }

    @Override
    public PlayerLog getLogs(String player) {
        String sql = "SELECT * FROM player_logs WHERE username = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, player);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String firstJoinWorldName = rs.getString("firstjoin_world");
                double firstJoinX = rs.getDouble("firstjoin_x");
                double firstJoinY = rs.getDouble("firstjoin_y");
                double firstJoinZ = rs.getDouble("firstjoin_z");
                Location firstJoinLocation = null;
                if (firstJoinWorldName != null) {
                    firstJoinLocation = new Location(Bukkit.getServer().getWorld(firstJoinWorldName), firstJoinX, firstJoinY, firstJoinZ);
                }
                String logoutWorldName = rs.getString("logout_world");
                double logoutX = rs.getDouble("logout_x");
                double logoutY = rs.getDouble("logout_y");
                double logoutZ = rs.getDouble("logout_z");
                Location logoutLocation;
                if(logoutWorldName != null) {
                    logoutLocation = new Location(Bukkit.getServer().getWorld(logoutWorldName), logoutX, logoutY, logoutZ);
                } else {
                    logoutLocation = null;
                }

                String lastJoinDate = rs.getString("last_join_date");
                String ip = rs.getString("ip_address");

                return new PlayerLog(
                        rs.getString("username"),
                        UUID.fromString(rs.getString("uuid")),
                        firstJoinLocation,
                        logoutLocation,
                        lastJoinDate,
                        ip
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public List<PlayerChat> getChatLogs(String player, int limit, String... sort) {
        String SQL = null;
        List<PlayerChat> chatLogs = new ArrayList<>();
        if (sort[0] == null) {
            sort[0] = "NEW";
        }

        switch (sort[0]) {
            case "OLD":
                SQL = "SELECT * FROM chat_logs WHERE username = ? ORDER BY date_and_time ASC LIMIT ?";
                break;
            case "NEW":
                SQL = "SELECT * FROM chat_logs WHERE username = ? ORDER BY date_and_time DESC LIMIT ?";
                break;
        }

        try {
            PreparedStatement ps = connection.prepareStatement(SQL);
            ps.setString(1, player);
            ps.setInt(2, limit);
            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()) {
                String message = resultSet.getString("message");
                String dateTimeStr = resultSet.getString("date_and_time");

                // Convert the date string to a standardized format
                SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date date = inputFormat.parse(dateTimeStr);
                    String standardizedDateTime = outputFormat.format(date);

                    String username = resultSet.getString("username");
                    PlayerChat chat = new PlayerChat(message, username, standardizedDateTime);
                    chatLogs.add(chat);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chatLogs;
    }

    @Override
    public List<PlayerChat> getCommandLogs(String player, int limit, String... sort) {
        String SQL = null;
        List<PlayerChat> chatLogs = new ArrayList<>();
        if (sort[0] == null) {
            sort[0] = "NEW";
        }

        switch (sort[0]) {
            case "OLD":
                SQL = "SELECT * FROM command_logs WHERE username = ? ORDER BY date_and_time ASC LIMIT ?";
                break;
            case "NEW":
                SQL = "SELECT * FROM command_logs WHERE username = ? ORDER BY date_and_time DESC LIMIT ?";
                break;
        }

        try {
            PreparedStatement ps = connection.prepareStatement(SQL);
            ps.setString(1, player);
            ps.setInt(2, limit);
            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()) {
                String message = resultSet.getString("command");
                String dateTimeStr = resultSet.getString("date_and_time");

                // Convert the date string to a standardized format
                SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                try {
                    Date date = inputFormat.parse(dateTimeStr);
                    String standardizedDateTime = outputFormat.format(date);

                    String username = resultSet.getString("username");
                    PlayerChat chat = new PlayerChat(message, username, standardizedDateTime);
                    chatLogs.add(chat);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chatLogs;
    }

    @Override
    public void setJoinStats(Player player, Location location) {
        String insertSQL = "INSERT INTO player_logs (username, uuid, ip_address, last_join_date) VALUES (?, ?, ?, ?)";
        String updateSQL = "UPDATE player_logs SET ip_address = ?, last_join_date = ? WHERE username = ?";

        try (PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
             PreparedStatement updateStatement = connection.prepareStatement(updateSQL)) {

            insertStatement.setString(1, player.getName());
            insertStatement.setString(2, player.getUniqueId().toString());
            insertStatement.setString(3, player.getAddress().toString());
            insertStatement.setDate(4, java.sql.Date.valueOf(LocalDate.now()));

            updateStatement.setString(1, player.getAddress().toString());
            updateStatement.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            updateStatement.setString(3, player.getName());

            if (isDebugMode()) {
                sendDebugLog("Set Join Stats for " + player);
            }

            // Try to insert. If it fails due to a duplicate key, then update.
            try {
                insertStatement.execute();
            } catch (SQLException e) {
                try {
                    updateStatement.execute();
                }  catch (SQLException ex) {
                    ex.printStackTrace();
                }

            }
        }   catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setFirstJoinStats(Player player, Location location) {
        String SQL = "UPDATE player_logs SET firstjoin_world = ?, firstjoin_x = ?, firstjoin_y = ?, firstjoin_z = ? WHERE username = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL)) {
            preparedStatement.setString(1, location.getWorld().getName());
            preparedStatement.setDouble(2, location.getX());
            preparedStatement.setDouble(3, location.getY());
            preparedStatement.setDouble(4, location.getZ());
            preparedStatement.setString(5, player.getName());

            preparedStatement.executeUpdate();

            if (isDebugMode()) {
                sendDebugLog("Set First Join Information Stats for " + player);
            }
        }   catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setLogoutInfo(String player, Location location) {
        String SQL = "UPDATE player_logs SET logout_world = ?, logout_x = ?, logout_y = ?, logout_z = ? WHERE username = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL)) {
            preparedStatement.setString(1, location.getWorld().getName());
            preparedStatement.setDouble(2, location.getX());
            preparedStatement.setDouble(3, location.getY());
            preparedStatement.setDouble(4, location.getZ());
            preparedStatement.setString(5, player);

            preparedStatement.executeUpdate();

            if (isDebugMode()) {
                sendDebugLog("Set Log out Location info for " + player);
            }

        }  catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addCommandLogs(String player, String command, String dateTime) {
        String SQL = "INSERT INTO command_logs (username, command, date_and_time) VALUES (?, ?, ?);";
        try(PreparedStatement ps = connection.prepareStatement(SQL)) {
            ps.setString(1, player);
            ps.setString(2, command);
            ps.setString(3, dateTime);
            ps.execute();
        }   catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addChatLogs(String player, String message, String dateTime) {
        String SQL = "INSERT INTO chat_logs (username, message, date_and_time) VALUES (?, ?, ?);";
        try(PreparedStatement ps = connection.prepareStatement(SQL)) {
            ps.setString(1, player);
            ps.setString(2, message);
            ps.setString(3, dateTime);
            ps.execute();
        }   catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
