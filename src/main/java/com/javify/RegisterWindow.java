package com.javify;

import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RegisterWindow extends JFrame {
    private String dbUrl;
    private JTextField usernameField;
    private JPasswordField passwordField;

    public RegisterWindow(String dbUrl) {
        this.dbUrl = dbUrl;
        setTitle("Register");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        add(passwordField, gbc);

        JPanel buttonPanel = new JPanel();
        JButton registerButton = new JButton("Register");
        JButton backButton = new JButton("Back");
        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        registerButton.addActionListener(e -> handleRegister());
        backButton.addActionListener(e -> {
            new LoginWindow(dbUrl).setVisible(true);
            dispose();
        });
    }

    private void handleRegister() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String hashPsw = BCrypt.hashpw(password, BCrypt.gensalt()); // hashing password with BCrypt

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashPsw);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Registration Successful!");
                new LoginWindow(dbUrl).setVisible(true);
                dispose();
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                JOptionPane.showMessageDialog(this, "Username already exists", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
