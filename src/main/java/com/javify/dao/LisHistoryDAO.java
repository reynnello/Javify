package com.javify.dao;

import com.javify.DatabaseManager;
import com.javify.objects.Track;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LisHistoryDAO {

    // add track to history
    public void addToHistory(int userId, int trackId) {
        String sql = "INSERT INTO listening_history (user_id, track_id) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, trackId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // track history of the last 5 tracks
    public List<Track> getHistory(int userId) {
        List<Track> tracks = new ArrayList<>();
        String sql = """
                SELECT t.*, h.played_at FROM tracks t
                JOIN listening_history h ON t.id = h.track_id
                WHERE h.user_id = ?
                ORDER BY h.played_at DESC
                LIMIT 5
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) tracks.add(mapTrack(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tracks;
    }

    // clear history for a user
    public void clearHistory(int userId) {
        String sql = "DELETE FROM listening_history WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // mapping resultset into an object
    private Track mapTrack(ResultSet rs) throws SQLException {
        return new Track(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("artist"),
                rs.getString("album"),
                rs.getString("genre"),
                rs.getInt("duration"),
                rs.getString("file_path"),
                rs.getBytes("cover_data")
        );
    }
}