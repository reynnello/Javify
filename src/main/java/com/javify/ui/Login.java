package com.javify.ui;

import com.javify.services.AuthService;

import javax.swing.*;
import java.awt.*;

public class Login extends JFrame {
    // Switches between login and register cards
    private static final String LOGIN_CARD = "login";
    private static final String REGISTER_CARD = "register";

    // UI components
    private final AuthService authService;
    private final CardLayout cardLayout;
    private final JPanel cardsPanel;

    private final JTextField loginUsernameField;
    private final JPasswordField loginPasswordField;
    private final JButton loginButton;
    private final JButton showRegisterButton;

    private final JTextField registerUsernameField;
    private final JPasswordField registerPasswordField;
    private final JPasswordField registerConfirmPasswordField;
    private final JButton registerSubmitButton;
    private final JButton backToLoginButton;

    // Constructor
    public Login(String dbUrl) {
        super("Javify - Login");
        this.authService = new AuthService(dbUrl);
        this.cardLayout = new CardLayout();
        this.cardsPanel = new JPanel(cardLayout);

        loginUsernameField = new JTextField(18);
        loginPasswordField = new JPasswordField(18);
        loginButton = new JButton("Login");
        showRegisterButton = new JButton("Register");

        registerUsernameField = new JTextField(18);
        registerPasswordField = new JPasswordField(18);
        registerConfirmPasswordField = new JPasswordField(18);
        registerSubmitButton = new JButton("Create Account");
        backToLoginButton = new JButton("Back");

        initUi();
        bindActions();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        showLoginCard(null);
        setVisible(true);
    }

    // UI setup
    private void initUi() {
        cardsPanel.add(createLoginPanel(), LOGIN_CARD);
        cardsPanel.add(createRegisterPanel(), REGISTER_CARD);
        setContentPane(cardsPanel);
    }

    // Login ui
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Username"), gbc);

        gbc.gridx = 1;
        panel.add(loginUsernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Password"), gbc);

        gbc.gridx = 1;
        panel.add(loginPasswordField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(loginButton);
        buttons.add(showRegisterButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(buttons, gbc);

        return panel;
    }

    // Register ui
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Username"), gbc);

        gbc.gridx = 1;
        panel.add(registerUsernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Password"), gbc);

        gbc.gridx = 1;
        panel.add(registerPasswordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Confirm password"), gbc);

        gbc.gridx = 1;
        panel.add(registerConfirmPasswordField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backToLoginButton);
        buttons.add(registerSubmitButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(buttons, gbc);

        return panel;
    }

    // Event handling
    private void bindActions() {
        loginButton.addActionListener(e -> onLogin());
        showRegisterButton.addActionListener(e -> showRegisterCard());
        registerSubmitButton.addActionListener(e -> onRegisterSubmit());
        backToLoginButton.addActionListener(e -> showLoginCard(null));
    }

    // Login logic
    private void onLogin() {
        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword());
        if (username.isEmpty() || password.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Username and password are required.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int userId;
        try {
            userId = authService.login(username, password);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Database error: " + ex.getMessage(),
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (userId <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Invalid username or password.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "Login successful. User ID: " + userId,
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // Register logic
    private void onRegisterSubmit() {
        String username = registerUsernameField.getText().trim();
        String password = new String(registerPasswordField.getPassword());
        String confirmPassword = new String(registerConfirmPasswordField.getPassword());

        if (username.isEmpty() || password.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Username, password and password confirmation are required.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Passwords do not match.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        boolean success;
        try {
            success = authService.register(username, password);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Database error: " + ex.getMessage(),
                    "Registration Failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (!success) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to register. Username might already exist.",
                    "Registration Failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "Registration successful. You can now log in.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );

        showLoginCard(username);
    }

    // Card switching to register
    private void showRegisterCard() {
        registerUsernameField.setText(loginUsernameField.getText().trim());
        registerPasswordField.setText("");
        registerConfirmPasswordField.setText("");

        cardLayout.show(cardsPanel, REGISTER_CARD);
        getRootPane().setDefaultButton(registerSubmitButton);
        registerUsernameField.requestFocusInWindow();
    }

    // Card switching to login
    private void showLoginCard(String username) {
        if (username != null && !username.isBlank()) {
            loginUsernameField.setText(username);
        }
        loginPasswordField.setText("");

        cardLayout.show(cardsPanel, LOGIN_CARD);
        getRootPane().setDefaultButton(loginButton);
        loginPasswordField.requestFocusInWindow();
    }
}
