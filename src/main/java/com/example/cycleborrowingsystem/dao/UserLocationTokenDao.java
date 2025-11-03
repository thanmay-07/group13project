package com.example.cycleborrowingsystem.dao;

import com.example.cycleborrowingsystem.db.Database;

import java.sql.*;
import java.util.UUID;

public class UserLocationTokenDao {
	public void createTableIfNotExists() throws SQLException {
		try (Connection c = Database.getInstance().getConnection(); Statement st = c.createStatement()) {
			st.executeUpdate("CREATE TABLE IF NOT EXISTS user_location_tokens (" +
				"user_id BIGINT PRIMARY KEY, " +
				"location_token VARCHAR(64) NOT NULL, " +
				"lat DOUBLE NULL, " +
				"lon DOUBLE NULL, " +
				"created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
				"FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
			") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
			// try to add columns if older DB exists without them
			try (Statement st2 = c.createStatement()) {
				st2.executeUpdate("ALTER TABLE user_location_tokens ADD COLUMN lat DOUBLE NULL");
				st2.executeUpdate("ALTER TABLE user_location_tokens ADD COLUMN lon DOUBLE NULL");
			} catch (SQLException ignore) { /* columns may already exist */ }
		}
	}

	public void setLocation(long userId, Double lat, Double lon) throws SQLException {
		// ensure record exists
		getOrCreateToken(userId);
		String sql = "UPDATE user_location_tokens SET lat=?, lon=? WHERE user_id=?";
		try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			if (lat == null) ps.setNull(1, Types.DOUBLE); else ps.setDouble(1, lat);
			if (lon == null) ps.setNull(2, Types.DOUBLE); else ps.setDouble(2, lon);
			ps.setLong(3, userId);
			ps.executeUpdate();
		}
	}

	public Double[] getLocation(long userId) throws SQLException {
		String sql = "SELECT lat, lon FROM user_location_tokens WHERE user_id=?";
		try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setLong(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					Object latObj = rs.getObject("lat");
					Object lonObj = rs.getObject("lon");
					Double lat = latObj == null ? null : rs.getDouble("lat");
					Double lon = lonObj == null ? null : rs.getDouble("lon");
					return new Double[]{lat, lon};
				}
			}
		}
		return null;
	}

	public String getOrCreateToken(long userId) throws SQLException {
		String sql = "SELECT location_token FROM user_location_tokens WHERE user_id=?";
		try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setLong(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) return rs.getString("location_token");
			}
		}
		String token = UUID.randomUUID().toString().replace("-", "");
		String ins = "INSERT INTO user_location_tokens(user_id,location_token) VALUES(?,?) ON DUPLICATE KEY UPDATE location_token=VALUES(location_token)";
		try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(ins)) {
			ps.setLong(1, userId);
			ps.setString(2, token);
			ps.executeUpdate();
		}
		return token;
	}

	public String getToken(long userId) throws SQLException {
		String sql = "SELECT location_token FROM user_location_tokens WHERE user_id=?";
		try (Connection c = Database.getInstance().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setLong(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) return rs.getString("location_token");
			}
		}
		return null;
	}
}
