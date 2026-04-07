package com.javify;

import com.javify.ui.Login;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        DatabaseManager.initDatabase();
        SwingUtilities.invokeLater(() -> new Login(DatabaseManager.URL));
    }
}
