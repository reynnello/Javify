package com.javify;

import com.javify.dao.TrackDAO;
import com.javify.db.DatabaseManager;
import com.javify.objects.Track;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TrackDAOTest {

    private static TrackDAO trackDAO;
    private static Track testTrack;
    private static Path tempTrackFile;

    @BeforeAll
    static void setup() {
        DatabaseManager.initDatabase();
        trackDAO = new TrackDAO();
        try {
            tempTrackFile = Files.createTempFile("javify-trackdao-test-", ".mp3");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        testTrack = new Track(0, "Test Track", "Test Artist", "Test Album",
                "Rock", 180, tempTrackFile.toAbsolutePath().toString(), null);
    }

    @AfterAll
    static void cleanup() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM tracks WHERE file_path = ?")) {
            ps.setString(1, tempTrackFile.toAbsolutePath().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            Files.deleteIfExists(tempTrackFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(1)
    void testAddTrack() {
        assertDoesNotThrow(() -> trackDAO.addTrack(testTrack));
    }

    @Test
    @Order(2)
    void testAddDuplicateTrackIgnored() {
        assertDoesNotNotThrow(() -> trackDAO.addTrack(testTrack));
    }

    @Test
    @Order(3)
    void testSearchByTitle() {
        List<Track> results = trackDAO.searchTracks("Test Track");
        assertFalse(results.isEmpty(), "Search by title should return results");
        assertTrue(results.stream().anyMatch(t -> t.getTitle().equals("Test Track")));
    }

    @Test
    @Order(4)
    void testSearchByArtist() {
        List<Track> results = trackDAO.searchTracks("Test Artist");
        assertFalse(results.isEmpty(), "Search by artist should return results");
    }

    @Test
    @Order(5)
    void testSearchEmptyQuery() {
        List<Track> results = trackDAO.searchTracks("");
        assertNotNull(results, "Empty search should return list, not null");
    }

    @Test
    @Order(6)
    void testSearchNoResults() {
        List<Track> results = trackDAO.searchTracks("xyzxyzxyz_nonexistent");
        assertTrue(results.isEmpty(), "Search for nonexistent track should return empty list");
    }

    private static void assertDoesNotNotThrow(Runnable runnable) {
        assertDoesNotThrow(runnable::run);
    }
}