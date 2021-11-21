package me.sisko.partygames.util;

import org.bukkit.OfflinePlayer;

import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.zaxxer.hikari.HikariDataSource;

import me.sisko.partygames.Main;

/*
This class handles all database-related operations, specifically for storing
and retriving player statistics. I would want to write the database queries
in an async manner, but sadly placeholderapi forces this to all by sync. Instead,
a cache is used to minimize the number of database queries needed.

CREATE TABLE `partygames_stats` (
	`uuid` CHAR(36) NOT NULL,
	`data` VARCHAR(10000) NOT NULL,
	PRIMARY KEY (`uuid`)
);

*/

public class Database {
    private static HikariDataSource ds;
    private static Map<OfflinePlayer, JSONObject> cache;

    public static synchronized void connect() {
        String host = Main.getPlugin().getConfig().getString("sql.url");
        String user = Main.getPlugin().getConfig().getString("sql.user");
        String pass = Main.getPlugin().getConfig().getString("sql.pass");

        ds = new HikariDataSource();
        ds.setJdbcUrl(host);
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.validate();

        cache = new HashMap<OfflinePlayer, JSONObject>();
    }

    public static void saveToDb(OfflinePlayer op) {
        if(cache.containsKey(op)) {
            final String query = "SELECT data FROM partygames_stats WHERE uuid = ?;";
            try {
                Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query);
                ps.setString(1, op.getUniqueId().toString());
                ResultSet rs = ps.executeQuery();

                String update = null;
                // if player already exists
                if(rs.next()) {
                    update = "UPDATE partygames_stats SET data = ? WHERE uuid = ?;";
                } else {
                    update = "INSERT INTO partygames_stats(data, uuid) VALUES (?, ?);";
                }

                ps = con.prepareStatement(update);
                ps.setString(1, cache.get(op).toString());
                ps.setString(2, op.getUniqueId().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static int getOverallWins(OfflinePlayer op) {
        JSONObject playerData = getPlayer(op);
        if(playerData.has("overall_wins")) {
            return playerData.getInt("overall_wins");
        }
        return 0;
    }

    public static void addOverallWins(OfflinePlayer op, int amount) {
        JSONObject playerData = getPlayer(op);
        if(playerData.has("overall_wins")) {
            playerData.put("overall_wins", playerData.getInt("overall_wins") + amount);
        } else {
            playerData.put("overall_wins", amount);
        }
    }

    public static int getOverallPoints(OfflinePlayer op) {
        JSONObject playerData = getPlayer(op);
        if(playerData.has("overall_points")) {
            return playerData.getInt("overall_points");
        }
        return 0;
    }

    public static void addOverallPoints(OfflinePlayer op, int amount) {
        JSONObject playerData = getPlayer(op);
        if(playerData.has("overall_points")) {
            playerData.put("overall_points", playerData.getInt("overall_points") + amount);
        } else {
            playerData.put("overall_points", amount);
        }
    }

    public static int getOverallGames(OfflinePlayer op) {
        JSONObject playerData = getPlayer(op);
        if(playerData.has("overall_games")) {
            return playerData.getInt("overall_games");
        }
        return 0;
    }

    public static void addOverallGames(OfflinePlayer op, int amount) {
        JSONObject playerData = getPlayer(op);
        if(playerData.has("overall_games")) {
            playerData.put("overall_games", playerData.getInt("overall_games") + amount);
        } else {
            playerData.put("overall_games", amount);
        }
    }

    public static double getOverallWinRate(OfflinePlayer op) {
        return ((double) getOverallWins(op)) / getOverallGames(op);
    }

    public static double getOverallAveragePoints(OfflinePlayer op) {
        return ((double) getOverallPoints(op)) / getOverallGames(op);
    }

    public static int getWins(String game, OfflinePlayer op) {
        JSONObject playerData = getPlayer(op);
        if(playerData.has(game) && playerData.getJSONObject(game).has("wins")) {
            return playerData.getJSONObject(game).getInt("wins");
        }
        return 0;
    }

    public static void addWins(String game, OfflinePlayer op, int amount) {
        JSONObject playerData = getPlayer(op);

        // case 1: game jsonobject and its key are both defined
        if(playerData.has(game) && playerData.getJSONObject(game).has("wins")) {
            playerData.getJSONObject(game).put("wins",
                playerData.getJSONObject(game).getInt("wins") + amount);

        // case 2: game jsonobject defined, key is not
        } else if (playerData.has(game)) {
            playerData.getJSONObject(game).put("wins",
                amount);

        // case 3: neither game jsonobject nor key is defined
        } else {
            playerData.put(game, new JSONObject());
            playerData.getJSONObject(game).put("wins",
                amount);
        }
    }

    public static int getPoints(String game, OfflinePlayer op) {
        JSONObject playerData = getPlayer(op);
        if(playerData.has(game) && playerData.getJSONObject(game).has("points")) {
            return playerData.getJSONObject(game).getInt("points");
        }
        return 0;
    }

    public static void addPoints(String game, OfflinePlayer op, int amount) {
        JSONObject playerData = getPlayer(op);

        // case 1: game jsonobject and its key are both defined
        if(playerData.has(game) && playerData.getJSONObject(game).has("points")) {
            playerData.getJSONObject(game).put("points",
                playerData.getJSONObject(game).getInt("points") + amount);

        // case 2: game jsonobject defined, key is not
        } else if (playerData.has(game)) {
            playerData.getJSONObject(game).put("points",
                amount);

        // case 3: neither game jsonobject nor key is defined
        } else {
            playerData.put(game, new JSONObject());
            playerData.getJSONObject(game).put("points",
                amount);
        }
    }

    public static int getGames(String game, OfflinePlayer op) {
        JSONObject playerData = getPlayer(op);
        if(playerData.has(game) && playerData.getJSONObject(game).has("games")) {
            return playerData.getJSONObject(game).getInt("games");
        }
        return 0;
    }

    public static void addGames(String game, OfflinePlayer op, int amount) {
        JSONObject playerData = getPlayer(op);

        // case 1: game jsonobject and its key are both defined
        if(playerData.has(game) && playerData.getJSONObject(game).has("games")) {
            playerData.getJSONObject(game).put("games",
                playerData.getJSONObject(game).getInt("games") + amount);

        // case 2: game jsonobject defined, key is not
        } else if (playerData.has(game)) {
            playerData.getJSONObject(game).put("games",
                amount);

        // case 3: neither game jsonobject nor key is defined
        } else {
            playerData.put(game, new JSONObject());
            playerData.getJSONObject(game).put("games",
                amount);
        }
    }


    public static double getWinRate(String game, OfflinePlayer op) {
        return ((double) getWins(game, op)) / getGames(game, op);
    }

    public static double getAveragePoints(String game, OfflinePlayer op) {
        return ((double) getPoints(game, op)) / getGames(game, op);
    }

    private static JSONObject getPlayer(OfflinePlayer op) {
        if (cache.containsKey(op)) {
            return cache.get(op);
        } else {
            final String query = "SELECT data FROM partygames_stats WHERE uuid = ?;";
            try {
                Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query);
                ps.setString(1, op.getUniqueId().toString());
                ResultSet rs = ps.executeQuery();
                if(rs.next()) {
                    cache.put(op, new JSONObject(rs.getString("data")));
                } else {
                    cache.put(op, new JSONObject());
                }
                return cache.get(op);

            } catch (SQLException e) {
                e.printStackTrace();
            }

            // in the case of error, return a blank jsonobject
            // do not add to cache, otherwise blank data might
            // overwrite saved data
            return new JSONObject();
        }
    }
}
