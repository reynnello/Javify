package com.javify;

import java.io.File;
import java.sql.*;

public class Main {
    private static final String DB_PATH = "db/users.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    public static void main(String[] args) {
        initDatabase();
        javax.swing.SwingUtilities.invokeLater(() -> {
            new LoginWindow(URL).setVisible(true);
        });
    }

    public static void initDatabase() {
        // Ensure db directory exists
        File dbFile = new File(DB_PATH);
        File dbDir = dbFile.getParentFile();
        if (dbDir != null && !dbDir.exists()) {
            if (dbDir.mkdirs()) {
                System.out.println("Created database directory: " + dbDir.getAbsolutePath());
            }
        }

        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL" +
                ");";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database initialized successfully at: " + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            System.err.println("Error of DB Init: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
