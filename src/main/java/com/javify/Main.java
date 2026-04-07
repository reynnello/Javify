package com.javify;

import javax.swing.*;
import java.sql.*;

public class Main {
    public static void main(String[] args) {
        DatabaseManager.initDatabase();
        SwingUtilities.invokeLater(() -> new LoginWindow(DatabaseManager.URL));
    }
}
