package com.javify;

import javafx.application.Application;
import javafx.stage.Stage;
import java.sql.*;

public class Main extends Application {
    private static final String DB_PATH = "db/users.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    @Override
    public void start(Stage primaryStage) {
        initDatabase();
        new LoginWindow(primaryStage, URL);
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL" +
                ");";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database initialized successfully at: " + DB_PATH);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
