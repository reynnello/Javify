package com.javify.dao;

import com.javify.db.DatabaseManager;
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
            throw new IllegalStateException("Database error while logging in.", e);
        }
        return -1;
    }

    // Register method
    public boolean register(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return false;
        }

        // Hash the password
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            pstmt.setString(2, hash);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                return false;
            }
            throw new IllegalStateException("Database error while registering user.", e);
        }
    }

    // Helper methods
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isUniqueViolation(SQLException e) {
        String message = e.getMessage();
        return message != null && message.contains("UNIQUE constraint failed");
    }

    // change password method
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        String sql = "SELECT password FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && BCrypt.checkpw(oldPassword, rs.getString("password"))) {
                String updateSql = "UPDATE users SET password = ? WHERE id = ?";
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                    update.setInt(2, userId);
                    update.executeUpdate();
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
