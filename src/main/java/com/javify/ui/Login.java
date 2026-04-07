package com.javify.ui;

import javax.swing.*;
import java.awt.*;

public class Login extends JFrame {
    private final String dbUrl;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JButton registerButton;

    public Login(String dbUrl) {
        super("Javify - Login");
        this.dbUrl = dbUrl;

        usernameField = new JTextField(18);
        passwordField = new JPasswordField(18);
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        initUi();
        bindActions();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initUi() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Username"), gbc);

        gbc.gridx = 1;
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Password"), gbc);

        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(loginButton);
        buttons.add(registerButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(buttons, gbc);

        setContentPane(panel);
    }

    private void bindActions() {
        loginButton.addActionListener(e -> onLogin());
        registerButton.addActionListener(e -> onRegister());
        getRootPane().setDefaultButton(loginButton);
    }

    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Username and password are required.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "Login action is ready. Connect main window launch here.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void onRegister() {
        JOptionPane.showMessageDialog(
                this,
                "Registration window is not implemented yet.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
