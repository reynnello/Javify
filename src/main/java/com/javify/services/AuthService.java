package com.javify.services;

import com.javify.dao.UserDAO;

public class AuthService {
    private final UserDAO userDAO;

    public AuthService(String dbUrl) {
        this.userDAO = new UserDAO(dbUrl);
    }

    public int login(String username, String password) {
        return userDAO.login(username, password);
    }

    public boolean register(String username, String password) {
        return userDAO.register(username, password);
    }

}
