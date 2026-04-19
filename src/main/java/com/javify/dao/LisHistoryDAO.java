package com.javify.dao;

import com.javify.db.DatabaseManager;
import com.javify.objects.Track;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LisHistoryDAO {

    // add track to history
    public void addToHistory(int userId, int trackId) {
        String selectSql = "SELECT COALESCE(play_count, 1) FROM listening_history WHERE user_id = ? AND track_id = ? ORDER BY played_at DESC LIMIT 1";
        String deleteSql = "DELETE FROM listening_history WHERE user_id = ? AND track_id = ?";
        String insertSql = "INSERT INTO listening_history (user_id, track_id, played_at, play_count) VALUES (?, ?, CURRENT_TIMESTAMP, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            int nextCount = 1;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, userId);
                selectStmt.setInt(2, trackId);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    nextCount = rs.getInt(1) + 1;
                }
            }

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, userId);
                deleteStmt.setInt(2, trackId);
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, trackId);
                insertStmt.setInt(3, nextCount);
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // track history of the last 7 tracks
    public List<Track> getHistory(int userId) {
        List<Track> tracks = new ArrayList<>();
        String sql = """
                SELECT t.*, h.played_at FROM tracks t
                JOIN listening_history h ON t.id = h.track_id
                WHERE h.user_id = ?
                ORDER BY h.played_at DESC
                LIMIT 6
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

    // total number of tracks played by a user
    public int getTotalPlayedTracks(int userId) {
        String sql = "SELECT COALESCE(SUM(play_count), 0) FROM listening_history WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
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
