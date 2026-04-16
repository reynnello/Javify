package com.javify;

import com.javify.dao.AppStateDAO;
import com.javify.dao.UserDAO;
import com.javify.db.DatabaseManager;
import com.javify.objects.User;
import com.javify.ui.AppFrame;
import com.javify.ui.Login;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        DatabaseManager.initDatabase();
        SwingUtilities.invokeLater(() -> {
            AppStateDAO appStateDAO = new AppStateDAO();
            Integer lastUserId = appStateDAO.getLastUserId();

            if (lastUserId != null) {
                User user = new UserDAO(DatabaseManager.URL).getUserById(lastUserId);
                if (user != null) {
                    new AppFrame(user, DatabaseManager.URL);
                    return;
                }
                appStateDAO.clearLastUserId();
            }

            new Login(DatabaseManager.URL);
        });
    }
}
