package haruna.storage;

import haruna.HarunaServer;
import haruna.structure.HarunaUser;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.*;
import java.time.Instant;

public class HarunaStore {
    private final JdbcConnectionPool pool;

    public HarunaStore(HarunaServer harunaServer, String location) throws SQLException {
        pool = JdbcConnectionPool.create(
                "jdbc:h2:file:" + location + "db\\HarunaStore;MODE=MYSQL;MULTI_THREADED=1",
                "",
                ""
        );
        pool.getConnection()
                .prepareStatement("CREATE TABLE IF NOT EXISTS HarunaStore(" +
                        "user VARCHAR(128) NOT NULL," +
                        "timestamp BIGINT NOT NULL," +
                        "weekend BOOLEAN NOT NULL," +
                        "UNIQUE(user)" +
                        ")" )
                .execute();
        harunaServer.harunaLog.info("Connected to the database! Location: " + location + "db\\HarunaStore");
    }

    public void save(String user, long timestamp, boolean weekend) throws Exception {
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO HarunaStore(user, timestamp, weekend) " +
                    "VALUES(?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "timestamp = ?, weekend = ?"
            );
            statement.setString(1, user);
            statement.setLong(2, timestamp);
            statement.setBoolean(3, weekend);
            statement.setLong(4, timestamp);
            statement.setBoolean(5, weekend);
            statement.execute();
        }
    }

    public HarunaUser get(String user) throws Exception {
        HarunaUser data = null;
        try (Connection connection = pool.getConnection()) {
            try (PreparedStatement cmd = connection.prepareStatement("SELECT DISTINCT * FROM HarunaStore WHERE user = ?")) {
                cmd.setString(1, user);
                try (ResultSet results = cmd.executeQuery()) {
                    while (results.next()) {
                        data = new HarunaUser();
                        data.user = results.getString("user");
                        data.timestamp = results.getLong("timestamp");
                        data.weekend = results.getBoolean("weekend");
                    }
                }
            }
        }
        return data;
    }

    public int savedCount() throws Exception {
        int data = 0;
        try (Connection connection = pool.getConnection()) {
            try (PreparedStatement cmd = connection.prepareStatement("SELECT COUNT(*) AS count FROM HarunaStore")) {
                try (ResultSet results = cmd.executeQuery()) {
                    while (results.next()) {
                        data = results.getInt("count");
                    }
                }
            }
        }
        return data;
    }

    public int clean() throws Exception {
        int cleaned;
        try (Connection connection = pool.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM HarunaStore WHERE timestamp <= ?");
            statement.setLong(1, Instant.now().toEpochMilli());
            cleaned = statement.executeUpdate();
        }
        return cleaned;
    }
}