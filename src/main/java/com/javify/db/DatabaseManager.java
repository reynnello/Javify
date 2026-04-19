package com.javify.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

public class DatabaseManager {
    // path to the database file
    private static final String DB_PATH = "db/javify.db";
    public static final String URL = "jdbc:sqlite:" + DB_PATH;

    // initialize the database
    public static Connection getConnection() throws SQLException {
        ensureDatabaseDirectory();
        return DriverManager.getConnection(URL);
    }

    // create tables
    public static void initDatabase() {
        String[] tables = {
                """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS tracks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                artist TEXT,
                album TEXT,
                genre TEXT,
                duration INTEGER,
                file_path TEXT UNIQUE NOT NULL,
                cover_data BLOB
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS playlists (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                user_id INTEGER NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS playlist_tracks (
                playlist_id INTEGER,
                track_id INTEGER,
                position INTEGER,
                PRIMARY KEY (playlist_id, track_id),
                FOREIGN KEY (playlist_id) REFERENCES playlists(id),
                FOREIGN KEY (track_id) REFERENCES tracks(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS listening_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                track_id INTEGER NOT NULL,
                played_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (track_id) REFERENCES tracks(id)
            )
            """
        };

        // create tables and indices using SQL statements
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : tables) stmt.execute(sql);
            ensureUsersAvatarColumn(conn);
            ensurePlaylistCoverColumn(conn);
            ensureListeningHistoryPlayCountColumn(conn);
            System.out.println("Database initialized at: " + DB_PATH);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // add avatar_data column to users table if it doesn't exist'
    private static void ensureUsersAvatarColumn(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE users ADD COLUMN avatar_data BLOB");
        } catch (SQLException e) {
            // SQLite throws an error when the column already exists.
            if (e.getMessage() == null || !e.getMessage().contains("duplicate column name")) {
                throw e;
            }
        }
    }

    // ensure the database directory exists if it doesn't exist then create the database
    private static void ensureDatabaseDirectory() throws SQLException {
        Path dbFile = Paths.get(DB_PATH);
        Path parent = dbFile.getParent();
        if (parent == null || Files.exists(parent)) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new SQLException("Failed to create database directory: " + parent, e);
        }
    }
    // add cover_data column to playlists table if it doesn't exist
    private static void ensurePlaylistCoverColumn(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE playlists ADD COLUMN cover_data BLOB");
        } catch (SQLException e) {
            if (e.getMessage() == null || !e.getMessage().contains("duplicate column name")) throw e;
        }
    }

    // add play_count column to listening_history table if it doesn't exist
    private static void ensureListeningHistoryPlayCountColumn(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE listening_history ADD COLUMN play_count INTEGER NOT NULL DEFAULT 1");
        } catch (SQLException e) {
            if (e.getMessage() == null || !e.getMessage().contains("duplicate column name")) {
                throw e;
            }
        }
    }
}
