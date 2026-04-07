package com.javify.dao;

import com.javify.DatabaseManager;
import com.javify.objects.Track;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrackDAO {

    // add track when folder is scanned
    public void addTrack(Track track) {
        String sql = "INSERT OR IGNORE INTO tracks (title, artist, album, genre, duration, file_path, cover_data) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, track.getTitle());
            pstmt.setString(2, track.getArtist());
            pstmt.setString(3, track.getAlbum());
            pstmt.setString(4, track.getGenre());
            pstmt.setInt(5, track.getDuration());
            pstmt.setString(6, track.getFilePath());
            pstmt.setBytes(7, track.getCoverData());
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // list of all tracks
    public List<Track> getAllTracks() {
        List<Track> tracks = new ArrayList<>();
        String sql = "SELECT * FROM tracks";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tracks.add(mapTrack(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return tracks;
    }

    // search by id
    public Track getTrackById(int id) {
        String sql = "SELECT * FROM tracks WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapTrack(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // search by title and/or artist
    public List<Track> searchTracks(String query) {
        List<Track> tracks = new ArrayList<>();
        String sql = "SELECT * FROM tracks WHERE title LIKE ? OR artist LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + query + "%");
            pstmt.setString(2, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) tracks.add(mapTrack(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return tracks;
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
