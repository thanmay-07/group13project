package com.example.cycleborrowingsystem.dao;

import com.example.cycleborrowingsystem.db.Database;
import com.example.cycleborrowingsystem.models.User;
import com.example.cycleborrowingsystem.security.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public void createTableIfNotExists() throws SQLException {
        try (Connection c = Database.getInstance().getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(255), " +
                    "email VARCHAR(255) UNIQUE, " +
                    "password_hash VARCHAR(255), " +
                    "salt VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        }
    }

    public User createUser(String name, String email, char[] password) throws Exception {
        createTableIfNotExists();
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);
        String sql = "INSERT INTO users(name,email,password_hash,salt) VALUES(?,?,?,?)";
        try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email.toLowerCase());
            ps.setString(3, hash);
            ps.setString(4, salt);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new User(id, name, email.toLowerCase());
                }
            }
        }
        return null;
    }

    public User findByEmail(String email) throws SQLException {
        createTableIfNotExists();
        String sql = "SELECT id, name, email FROM users WHERE email=? LIMIT 1";
        try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new User(rs.getLong("id"), rs.getString("name"), rs.getString("email"));
            }
        }
        return null;
    }

    public boolean verifyCredentials(String email, char[] password) throws SQLException {
        createTableIfNotExists();
        String sql = "SELECT password_hash, salt FROM users WHERE email=? LIMIT 1";
        try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    return PasswordUtil.verifyPassword(password, salt, hash);
                }
            }
        }
        return false;
    }

    public List<User> listAll() throws SQLException {
        createTableIfNotExists();
        List<User> out = new ArrayList<>();
        String sql = "SELECT id,name,email FROM users ORDER BY created_at DESC";
        try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new User(rs.getLong("id"), rs.getString("name"), rs.getString("email")));
            }
        }
        return out;
    }
}
