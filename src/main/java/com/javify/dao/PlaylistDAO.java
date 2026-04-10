package com.javify.dao;

import com.javify.db.DatabaseManager;
import com.javify.objects.Playlist;
import com.javify.objects.Track;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDAO {

    // create playlist
    public void createPlaylist(String name, int userId) {
        String sql = "INSERT INTO playlists (name, user_id) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // list of all playlists
    public List<Playlist> getPlaylistsByUser(int userId) {
        List<Playlist> playlists = new ArrayList<>();
        String sql = "SELECT * FROM playlists WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                playlists.add(new Playlist(id, rs.getString("name"), userId, getTracksForPlaylist(id)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return playlists;
    }

    // add track to playlist
    public void addTrackToPlaylist(int playlistId, int trackId) {
        String sql = "INSERT OR IGNORE INTO playlist_tracks (playlist_id, track_id, position) " +
                "VALUES (?, ?, (SELECT COALESCE(MAX(position), 0) + 1 FROM playlist_tracks WHERE playlist_id = ?))"; // position is the index of the track in the playlist
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playlistId);
            pstmt.setInt(2, trackId);
            pstmt.setInt(3, playlistId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // delete track from playlist
    public void removeTrackFromPlaylist(int playlistId, int trackId) {
        String sql = "DELETE FROM playlist_tracks WHERE playlist_id = ? AND track_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playlistId);
            pstmt.setInt(2, trackId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // tracks in playlist
    public List<Track> getTracksForPlaylist(int playlistId) {
        List<Track> tracks = new ArrayList<>();
        // SQL query to get tracks in the playlist with their positions by joining playlist_tracks and tracks tables
        String sql = """
                SELECT t.* FROM tracks t
                JOIN playlist_tracks pt ON t.id = pt.track_id
                WHERE pt.playlist_id = ?
                ORDER BY pt.position
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playlistId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) tracks.add(mapTrack(rs)); // map each row to a Track object
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tracks;
    }

    // rename playlist
    public void renamePlaylist(int playlistId, String newName) {
        String sql = "UPDATE playlists SET name = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setInt(2, playlistId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // delete playlist
    public void deletePlaylist(int playlistId) {
        String sql1 = "DELETE FROM playlist_tracks WHERE playlist_id = ?"; // delete all tracks in the playlist
        String sql2 = "DELETE FROM playlists WHERE id = ?"; // delete the playlist itself
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement pstmt1 = conn.prepareStatement(sql1)) {
                pstmt1.setInt(1, playlistId);
                pstmt1.executeUpdate();
            }
            try (PreparedStatement pstmt2 = conn.prepareStatement(sql2)) {
                pstmt2.setInt(1, playlistId);
                pstmt2.executeUpdate();
            }
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
