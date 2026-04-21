package com.javify;

import com.javify.dao.PlaylistDAO;
import com.javify.dao.UserDAO;
import com.javify.db.DatabaseManager;
import com.javify.objects.Playlist;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PlaylistDAOTest {

    private static PlaylistDAO playlistDAO;
    private static int testUserId;
    private static int testPlaylistId;
    private static final String TEST_USER = "playlist_test_" + System.currentTimeMillis();

    @BeforeAll
    static void setup() {
        DatabaseManager.initDatabase();
        playlistDAO = new PlaylistDAO();
        UserDAO userDAO = new UserDAO(DatabaseManager.URL);
        userDAO.register(TEST_USER, "testpass123");
        testUserId = userDAO.login(TEST_USER, "testpass123");
    }

    @Test
    @Order(1)
    void testCreatePlaylist() {
        assertDoesNotThrow(() ->
                playlistDAO.createPlaylist("My Test Playlist", testUserId, null));
    }

    @Test
    @Order(2)
    void testGetPlaylistsByUser() {
        List<Playlist> playlists = playlistDAO.getPlaylistsByUser(testUserId);
        assertFalse(playlists.isEmpty(), "User should have at least one playlist");
        testPlaylistId = playlists.get(0).getId();
    }

    @Test
    @Order(3)
    void testRenamePlaylist() {
        assertDoesNotThrow(() ->
                playlistDAO.renamePlaylist(testPlaylistId, "Renamed Playlist"));

        List<Playlist> playlists = playlistDAO.getPlaylistsByUser(testUserId);
        assertTrue(playlists.stream().anyMatch(p -> p.getName().equals("Renamed Playlist")));
    }

    @Test
    @Order(4)
    void testDeletePlaylist() {
        playlistDAO.deletePlaylist(testPlaylistId);
        List<Playlist> playlists = playlistDAO.getPlaylistsByUser(testUserId);
        assertTrue(playlists.stream().noneMatch(p -> p.getId() == testPlaylistId));
    }

    @AfterAll
    static void cleanup() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
            ps.setString(1, TEST_USER);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}