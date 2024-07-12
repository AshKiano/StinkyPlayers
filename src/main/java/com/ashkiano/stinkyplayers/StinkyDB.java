package com.ashkiano.stinkyplayers;

import org.bukkit.entity.Player;

import java.sql.*;

public class StinkyDB {
    private final Connection connection;

    public StinkyDB(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS players(" +
                    "uuid TEXT PRIMARY KEY, " +
                    "username TEXT NOT NULL, " +
                    "stinkyTime INTEGER NOT NULL)");
        }
    }

    public void addPlayer(Player player, long time) throws SQLException{
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO players (uuid, username, stinkyTime) VALUES (?, ?, ?)")){
            preparedStatement.setString(1,player.getUniqueId().toString());
            preparedStatement.setString(2,player.getDisplayName());
            preparedStatement.setLong(3, time);
            preparedStatement.executeUpdate();

        }
    }

    public boolean checkPlayer(Player player) throws SQLException{
        try(PreparedStatement preparedStatement = connection.prepareStatement("SELECT uuid FROM players WHERE uuid = ?")){
            preparedStatement.setString(1, player.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }

    public void setStinkyTime(Player player, long time) throws SQLException{
        if(!checkPlayer(player)){
            addPlayer(player, time);
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE players SET stinkyTime = ? WHERE uuid = ?")){
            preparedStatement.setLong(1, time);
            preparedStatement.setString(2, player.getUniqueId().toString());
            preparedStatement.executeUpdate();
        }
    }

    public long getStinkyTime(Player player) throws SQLException{
        try(PreparedStatement preparedStatement = connection.prepareStatement("SELECT stinkyTime FROM players WHERE uuid = ?")){
            preparedStatement.setString(1,player.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return resultSet.getLong("stinkyTime");
            }else{
                return 0L;
            }
        }
    }

    public void closeDBCon() throws SQLException{
        if (connection != null && !connection.isClosed()){
            connection.close();
        }
    }
}
