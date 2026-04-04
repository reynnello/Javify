package com.javify;

import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RegisterWindow extends JFrame {
    private final String dbUrl;
    private JTextField userTextField;
    private JPasswordField pwBox;

    public RegisterWindow(String dbUrl) {
        this.dbUrl = dbUrl;
        initWindow();
    }

    private void initWindow() {
        setTitle("Register");
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
        JButton registerBtn = new JButton("Register");
        JButton backBtn = new JButton("Back");
        buttonPanel.add(registerBtn);
        buttonPanel.add(backBtn);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        panel.add(buttonPanel, constraints);

        registerBtn.addActionListener(e -> handleRegister(userTextField.getText(), new String(pwBox.getPassword())));
        backBtn.addActionListener(e -> {
            new LoginWindow(dbUrl);
            dispose();
        });

        add(panel);
        setVisible(true);
    }

    private void handleRegister(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill all fields!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String hashPsw = BCrypt.hashpw(password, BCrypt.gensalt());

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashPsw);
                pstmt.executeUpdate();
                showAlert("Success", "Registration Successful!", JOptionPane.INFORMATION_MESSAGE);
                new LoginWindow(dbUrl);
                dispose();
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                showAlert("Error", "Username already exists", JOptionPane.ERROR_MESSAGE);
            } else {
                showAlert("Error", e.getMessage(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAlert(String title, String content, int messageType) {
        JOptionPane.showMessageDialog(this, content, title, messageType);
    }
}
