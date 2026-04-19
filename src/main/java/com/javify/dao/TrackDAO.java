package com.javify.dao;

import com.javify.db.DatabaseManager;
import com.javify.objects.Track;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        List<Integer> staleTrackIds = new ArrayList<>();
        String sql = "SELECT * FROM tracks";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Track track = mapTrack(rs);
                if (track.getFilePath() != null && new File(track.getFilePath()).exists()) {
                    tracks.add(track);
                } else {
                    staleTrackIds.add(track.getId());
                }
            }

            // remove stale rows so deleted files do not come back on next app launch.
            for (Integer staleTrackId : staleTrackIds) {
                deleteTrackWithDependencies(conn, staleTrackId);
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
                Track track = mapTrack(rs);
                if (track.getFilePath() != null && new File(track.getFilePath()).exists()) {
                    return track;
                }
                deleteTrackWithDependencies(conn, track.getId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // search by title and/or artist
    public List<Track> searchTracks(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            return getAllTracks();
        }

        // search by using case-insensitive matching
        List<Track> result = new ArrayList<>();
        for (Track track : getAllTracks()) {
            String title = track.getTitle() != null ? track.getTitle().toLowerCase(Locale.ROOT) : "";
            String artist = track.getArtist() != null ? track.getArtist().toLowerCase(Locale.ROOT) : "";
            if (title.contains(normalizedQuery) || artist.contains(normalizedQuery)) {
                result.add(track);
            }
        }
        return result;
    }

    // delete track by id
    public boolean deleteTrackById(int trackId) {
        String existsSql = "SELECT 1 FROM tracks WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement existsStmt = conn.prepareStatement(existsSql)) {
                existsStmt.setInt(1, trackId);
                ResultSet rs = existsStmt.executeQuery();
                if (!rs.next()) {
                    return false;
                }
            }

            deleteTrackWithDependencies(conn, trackId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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

    // delete track and its dependencies
    private void deleteTrackWithDependencies(Connection conn, int trackId) throws SQLException {
        try (PreparedStatement deletePlaylistLinks = conn.prepareStatement("DELETE FROM playlist_tracks WHERE track_id = ?");
             PreparedStatement deleteHistory = conn.prepareStatement("DELETE FROM listening_history WHERE track_id = ?");
             PreparedStatement deleteTrack = conn.prepareStatement("DELETE FROM tracks WHERE id = ?")) {
            deletePlaylistLinks.setInt(1, trackId);
            deletePlaylistLinks.executeUpdate();

            deleteHistory.setInt(1, trackId);
            deleteHistory.executeUpdate();

            deleteTrack.setInt(1, trackId);
            deleteTrack.executeUpdate();
        }
    }
}
