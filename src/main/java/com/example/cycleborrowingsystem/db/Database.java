package com.example.cycleborrowingsystem.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * MySQL Database helper. Reads credentials from classpath resource db.properties
 * or environment variables JDBC_URL, DB_USERNAME, DB_PASSWORD.
 */
public class Database {
    private static final Database INSTANCE = new Database();
    private final String jdbcUrl;
    private final String username;
    private final String password;

    private Database() {
        Properties props = new Properties();
        try (InputStream in = Database.class.getResourceAsStream("/db.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignore) {}

        this.jdbcUrl = firstNonEmpty(
            props.getProperty("jdbcUrl"),
            System.getenv("JDBC_URL")
        );
        this.username = firstNonEmpty(
            props.getProperty("username"),
            System.getenv("DB_USERNAME")
        );
        this.password = firstNonEmpty(
            props.getProperty("password"),
            System.getenv("DB_PASSWORD")
        );

        if (jdbcUrl == null || username == null) {
            throw new IllegalStateException("Database not configured. Provide db.properties or env vars JDBC_URL, DB_USERNAME, DB_PASSWORD.");
        }
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    public static Database getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, Objects.toString(password, ""));
    }
}
