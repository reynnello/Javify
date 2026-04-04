package com.javify;

import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginWindow extends JFrame {
    private final String dbUrl;
    private JTextField userTextField;
    private JPasswordField pwBox;

    public LoginWindow(String dbUrl) {
        this.dbUrl = dbUrl;
        initWindow();
    }

    private void initWindow() {
        setTitle("Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(350, 200);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(10, 10, 10, 10);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        JLabel userNameLabel = new JLabel("Username:");
        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(userNameLabel, constraints);

        userTextField = new JTextField(15);
        constraints.gridx = 1;
        constraints.gridy = 0;
        panel.add(userTextField, constraints);

        JLabel pwLabel = new JLabel("Password:");
        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(pwLabel, constraints);

        pwBox = new JPasswordField(15);
        constraints.gridx = 1;
        constraints.gridy = 1;
        panel.add(pwBox, constraints);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        panel.add(buttonPanel, constraints);

        loginBtn.addActionListener(e -> handleLogin(userTextField.getText(), new String(pwBox.getPassword())));
        registerBtn.addActionListener(e -> {
            new RegisterWindow(dbUrl);
            dispose();
        });

        add(panel);
        setVisible(true);
    }

    private void handleLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill all fields!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT password FROM users WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String hashed = rs.getString("password");
                        if (BCrypt.checkpw(password, hashed)) {
                            showAlert("Success", "Login Successful!", JOptionPane.INFORMATION_MESSAGE);
                            System.exit(0);
                        } else {
                            showAlert("Error", "Invalid password", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        showAlert("Error", "User not found!", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            showAlert("Error", e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAlert(String title, String content, int messageType) {
        JOptionPane.showMessageDialog(this, content, title, messageType);
    }
}
