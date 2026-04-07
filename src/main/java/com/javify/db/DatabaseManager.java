package com.javify;

import java.sql.*;

public class DatabaseManager {
    // path to the database file
    private static final String DB_PATH = "db/javify.db";
    public static final String URL = "jdbc:sqlite:" + DB_PATH;

    // initialize the database
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // create tables if they don't exist'
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
            System.out.println("Database initialized at: " + DB_PATH);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}