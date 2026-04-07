package com.javify.ui;

import com.javify.services.AuthService;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class Register extends JDialog {
    // UI components
    private final AuthService authService;
    private final Consumer<String> onRegistered; // Callback when registration is successful
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final JButton registerButton;
    private final JButton cancelButton;

    // Constructor
    public Register(JFrame owner, AuthService authService, Consumer<String> onRegistered) {
        super(owner, "Javify - Register", true);
        this.authService = authService;
        this.onRegistered = onRegistered;

        usernameField = new JTextField(18);
        passwordField = new JPasswordField(18);
        confirmPasswordField = new JPasswordField(18);
        registerButton = new JButton("Create Account");
        cancelButton = new JButton("Cancel");

        initUi();
        bindActions();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }

    // UI setup
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

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Confirm password"), gbc);

        gbc.gridx = 1;
        panel.add(confirmPasswordField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancelButton);
        buttons.add(registerButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(buttons, gbc);

        setContentPane(panel);
    }

    // Event handling
    private void bindActions() {
        registerButton.addActionListener(e -> onRegister());
        cancelButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(registerButton);
    }

    // Registration logic
    private void onRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (username.isEmpty() || password.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Username, password and password confirmation are required.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Validate password match
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

        // Notify the parent frame if registration was successful
        if (onRegistered != null) {
            onRegistered.accept(username);
        }
        dispose();
    }
}
