package com.javify.objects;

public class User {
    // User properties
    private int id;
    private String username;

    // Constructor
    public User(int id, String username) {
        this.id = id;
        this.username = username;
    }

    // Getters
    public int getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
}