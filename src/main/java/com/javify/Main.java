package com.javify;

import javax.swing.*;
import java.sql.*;

public class Main {
    private static final String DB_PATH = "db/users.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginWindow(URL));
    }
}
