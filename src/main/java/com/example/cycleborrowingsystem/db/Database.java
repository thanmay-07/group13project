package com.example.cycleborrowingsystem.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

/**
 * Minimal SQLite-backed Database helper used by DAOs.
 * Creates a file-based SQLite DB in the user's home directory at .cbs/db.sqlite
 */
public class Database {
    private static final Database INSTANCE = new Database();
    private final String url;

    private Database() {
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".cbs");
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path dbFile = dir.resolve("db.sqlite");
            this.url = "jdbc:sqlite:" + dbFile.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database path", e);
        }
    }

    public static Database getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }
}
