package com.javify.dao;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class UserDAO {
    // Constructor
    private final String dbUrl;
    public UserDAO(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    // Login method
    public int login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return -1;
        }

        String sql = "SELECT id, password FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && BCrypt.checkpw(password, rs.getString("password"))) { // Check if the password matches the hashed password
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Register method
    public boolean register(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return false;
        }

        if (isUsernameTaken(username)) {
            return false;
        }

        // Hash the password
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hash);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) return false;
            e.printStackTrace();
            return false;
        }
    }

    // Helper methods
    private boolean isUsernameTaken(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
