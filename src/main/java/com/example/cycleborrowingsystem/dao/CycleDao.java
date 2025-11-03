package com.example.cycleborrowingsystem.dao;

import com.example.cycleborrowingsystem.db.Database;
import com.example.cycleborrowingsystem.models.Cycle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CycleDao {
    public void createTableIfNotExists() throws SQLException {
        try (Connection c = Database.getInstance().getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS cycles (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "model VARCHAR(255) UNIQUE, " +
                    "lat DOUBLE, " +
                    "lon DOUBLE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        }
    }

    public List<Cycle> listAll() throws SQLException {
        createTableIfNotExists();
        List<Cycle> out = new ArrayList<>();
        String sql = "SELECT id, model, lat, lon FROM cycles ORDER BY created_at DESC, id DESC";
        try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Cycle(rs.getLong("id"), rs.getString("model"), rs.getDouble("lat"), rs.getDouble("lon")));
                }
            }
        }
        return out;
    }

    public Cycle addOrUpdate(String model, double lat, double lon) throws SQLException {
        createTableIfNotExists();
        String upsert = "INSERT INTO cycles(model, lat, lon) VALUES(?,?,?) ON DUPLICATE KEY UPDATE lat=VALUES(lat), lon=VALUES(lon)";
        try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(upsert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, model);
            ps.setDouble(2, lat);
            ps.setDouble(3, lon);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new Cycle(id, model, lat, lon);
                }
            }
        }
        // fetch existing if updated
        String fetch = "SELECT id FROM cycles WHERE model=?";
        try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(fetch)) {
            ps.setString(1, model);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Cycle(rs.getLong(1), model, lat, lon);
            }
        }
        return null;
    }

    public boolean deleteById(long id) throws SQLException {
        createTableIfNotExists();
        String sql = "DELETE FROM cycles WHERE id=?";
        try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}


