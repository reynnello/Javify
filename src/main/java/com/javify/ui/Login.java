package com.javify.ui;

import com.javify.db.DatabaseManager;
import com.javify.objects.User;
import com.javify.services.AuthService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Login extends JFrame {
    // Switches between login and register cards
    private static final String LOGIN_CARD = "login";
    private static final String REGISTER_CARD = "register";

    //region UI Colors and Fonts
    private static final Color BG_COLOR = new Color(18, 18, 18);
    private static final Color CARD_COLOR = new Color(28, 28, 28);
    private static final Color FIELD_COLOR = new Color(36, 36, 36);
    private static final Color BORDER_COLOR = new Color(58, 58, 58);
    private static final Color FIELD_FOCUS_COLOR = new Color(185, 99, 6);
    private static final Color FIELD_FOCUS_BG = new Color(42, 42, 42);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color MUTED_TEXT_COLOR = new Color(170, 170, 170);
    private static final Color ACCENT_COLOR = new Color(185, 99, 6);
    private static final Color ACCENT_HOVER_COLOR = new Color(204, 112, 13);
    private static final Color SECONDARY_COLOR = new Color(42, 42, 42);
    private static final Color SECONDARY_HOVER_COLOR = new Color(60, 60, 60);
    private static final Font TITLE_FONT = new Font("Sans-Serif", Font.BOLD, 24);
    private static final Font SUBTITLE_FONT = new Font("Sans-Serif", Font.PLAIN, 13);
    private static final Font LABEL_FONT = new Font("Sans-Serif", Font.PLAIN, 12);
    private static final Font INPUT_FONT = new Font("Sans-Serif", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Sans-Serif", Font.BOLD, 13);
    private static final int FIELD_WIDTH = 280;
    private static final int FIELD_HEIGHT = 40;
    //endregion

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
        setMinimumSize(new Dimension(520, 440));
        pack();
        setLocationRelativeTo(null);
        showLoginCard(null);
        setVisible(true);
    }

    // UI setup
    private void initUi() {
        cardsPanel.setBackground(BG_COLOR);
        cardsPanel.setBorder(new EmptyBorder(28, 28, 28, 28));
        cardsPanel.add(createLoginPanel(), LOGIN_CARD);
        cardsPanel.add(createRegisterPanel(), REGISTER_CARD);
        setContentPane(cardsPanel);
    }

    // Login ui
    private JPanel createLoginPanel() {
        styleTextField(loginUsernameField);
        stylePasswordField(loginPasswordField);
        stylePrimaryButton(loginButton);
        styleSecondaryButton(showRegisterButton);

        JPanel card = createAuthCard("Welcome back", "Login to continue listening");
        JPanel content = createFormContentPanel();
        content.add(createFieldPanel("Username", loginUsernameField));
        content.add(Box.createVerticalStrut(12));
        content.add(createFieldPanel("Password", loginPasswordField));
        content.add(Box.createVerticalStrut(18));

        JPanel buttons = createButtonsRow();
        buttons.add(showRegisterButton);
        buttons.add(loginButton);
        content.add(buttons);

        card.add(wrapLeftAligned(content), BorderLayout.CENTER);
        return wrapCenteredCard(card);
    }

    // Register ui
    private JPanel createRegisterPanel() {
        styleTextField(registerUsernameField);
        stylePasswordField(registerPasswordField);
        stylePasswordField(registerConfirmPasswordField);
        stylePrimaryButton(registerSubmitButton);
        styleSecondaryButton(backToLoginButton);

        JPanel card = createAuthCard("Create account", "Start your music journey");
        JPanel content = createFormContentPanel();
        content.add(createFieldPanel("Username", registerUsernameField));
        content.add(Box.createVerticalStrut(12));
        content.add(createFieldPanel("Password", registerPasswordField));
        content.add(Box.createVerticalStrut(12));
        content.add(createFieldPanel("Confirm password", registerConfirmPasswordField));
        content.add(Box.createVerticalStrut(18));

        JPanel buttons = createButtonsRow();
        buttons.add(backToLoginButton);
        buttons.add(registerSubmitButton);
        content.add(buttons);

        card.add(wrapLeftAligned(content), BorderLayout.CENTER);
        return wrapCenteredCard(card);
    }

    private JPanel createAuthCard(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(24, 24, 24, 24)
        ));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(titleLabel);

        header.add(Box.createVerticalStrut(6));

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(MUTED_TEXT_COLOR);
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(subtitleLabel);

        card.add(header, BorderLayout.NORTH);
        return card;
    }

    private JPanel createFormContentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(FIELD_WIDTH, Integer.MAX_VALUE));
        return panel;
    }

    private JPanel createFieldPanel(String labelText, JComponent field) {
        JPanel fieldPanel = new JPanel();
        fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));
        fieldPanel.setOpaque(false);
        fieldPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.setMaximumSize(new Dimension(FIELD_WIDTH, 70));
        fieldPanel.setPreferredSize(new Dimension(FIELD_WIDTH, 70));

        JLabel label = new JLabel(labelText);
        label.setForeground(MUTED_TEXT_COLOR);
        label.setFont(LABEL_FONT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(label);
        fieldPanel.add(Box.createVerticalStrut(6));

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldPanel.add(field);
        return fieldPanel;
    }

    private JPanel createButtonsRow() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(FIELD_WIDTH, 44));
        buttons.setPreferredSize(new Dimension(FIELD_WIDTH, 44));
        return buttons;
    }

    private JPanel wrapLeftAligned(JComponent content) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(content);
        return wrapper;
    }

    private JPanel wrapCenteredCard(JPanel card) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_COLOR);
        wrapper.add(card);
        return wrapper;
    }

    private void styleTextField(JTextField field) {
        field.setBackground(FIELD_COLOR);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setSelectedTextColor(TEXT_COLOR);
        field.setSelectionColor(new Color(185, 99, 6, 120));
        field.setFont(INPUT_FONT);
        Border idleBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(10, 12, 10, 12)
        );
        field.setBorder(idleBorder);
        Dimension inputSize = new Dimension(FIELD_WIDTH, FIELD_HEIGHT);
        field.setPreferredSize(inputSize);
        field.setMinimumSize(inputSize);
        field.setMaximumSize(inputSize);

        Border focusBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_FOCUS_COLOR, 2),
                new EmptyBorder(9, 11, 9, 11)
        );

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(focusBorder);
                field.setBackground(FIELD_FOCUS_BG);
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(idleBorder);
                field.setBackground(FIELD_COLOR);
            }
        });
    }

    private void stylePasswordField(JPasswordField field) {
        styleTextField(field);
    }

    private void stylePrimaryButton(JButton button) {
        button.setBackground(ACCENT_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setFont(BUTTON_FONT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        installButtonHover(button, ACCENT_COLOR, ACCENT_HOVER_COLOR);
    }

    private void styleSecondaryButton(JButton button) {
        button.setBackground(SECONDARY_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setFont(BUTTON_FONT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        installButtonHover(button, SECONDARY_COLOR, SECONDARY_HOVER_COLOR);
    }

    private void installButtonHover(JButton button, Color normalColor, Color hoverColor) {
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(normalColor);
            }
        });
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
        String dbUrl = DatabaseManager.URL;
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

        User user = new User(userId, username);
        dispose();
        new AppFrame(user, dbUrl);
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

        if (password.length() < 6) {
            JOptionPane.showMessageDialog(
                    this,
                    "Password must be at least 6 characters long.",
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
        loginUsernameField.requestFocusInWindow();
    }
}
