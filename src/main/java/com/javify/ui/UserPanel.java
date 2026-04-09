package com.javify.ui;

import com.javify.dao.UserDAO;
import com.javify.objects.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class UserPanel extends JPanel {

    private final User currentUser;
    private final UserDAO userDAO;
    private final Runnable onBack;

    private JLabel avatarLabel;
    private JPasswordField oldPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    public UserPanel(User currentUser, String dbUrl, Runnable onBack) {
        this.currentUser = currentUser;
        this.userDAO = new UserDAO(dbUrl);
        this.onBack = onBack;
        initUi();
    }

    private void initUi() {
        setLayout(new BorderLayout());
        setBackground(new Color(255, 255, 255));

        // topbar with back button
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topBar.setBackground(new Color(230, 153, 75));

        JButton backBtn = new JButton("← Back");
        backBtn.setForeground(Color.WHITE);
        backBtn.setBackground(new Color(102, 102, 102));
        backBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> onBack.run()); // handle back button click
        topBar.add(backBtn);

        add(topBar, BorderLayout.NORTH);

        // center panel with content
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(new Color(255, 255, 255));
        centerWrapper.add(createContentPanel());

        add(centerWrapper, BorderLayout.CENTER);
    }

    // create content panel
    private JPanel createContentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(28, 28, 28));
        panel.setBorder(new EmptyBorder(32, 48, 32, 48));

        // avatar
        avatarLabel = new JLabel(generateInitialsAvatar(80));
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(avatarLabel);

        panel.add(Box.createVerticalStrut(12));

        // change avatar button
        JButton changeAvatarBtn = new JButton("Change avatar");
        changeAvatarBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        changeAvatarBtn.setForeground(new Color(255, 129, 0));
        changeAvatarBtn.setBackground(new Color(28, 28, 28));
        changeAvatarBtn.setBorder(BorderFactory.createLineBorder(new Color(255, 128, 0)));
        changeAvatarBtn.setFocusPainted(false);
        changeAvatarBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        changeAvatarBtn.addActionListener(e -> {
            // todo: open systeme dialog to choose avatar
        });
        panel.add(changeAvatarBtn);

        panel.add(Box.createVerticalStrut(16));

        // username
        JLabel usernameLabel = new JLabel(currentUser.getUsername());
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setFont(new Font("Sans-Serif", Font.BOLD, 20));
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(usernameLabel);

        panel.add(Box.createVerticalStrut(32));

        // separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0, 0, 0));
        sep.setMaximumSize(new Dimension(320, 1));
        panel.add(sep);

        panel.add(Box.createVerticalStrut(24));

        // section: change password
        JLabel pwTitle = new JLabel("Change password");
        pwTitle.setForeground(new Color(255, 255, 255));
        pwTitle.setFont(new Font("Sans-Serif", Font.BOLD, 13));
        pwTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(pwTitle);

        panel.add(Box.createVerticalStrut(16));

        // password fields
        oldPasswordField = createPasswordField("Current password");
        newPasswordField = createPasswordField("New password");
        confirmPasswordField = createPasswordField("Confirm new password");

        panel.add(labeledField("Current password", oldPasswordField));
        panel.add(Box.createVerticalStrut(10));
        panel.add(labeledField("New password", newPasswordField));
        panel.add(Box.createVerticalStrut(10));
        panel.add(labeledField("Confirm new password", confirmPasswordField));

        panel.add(Box.createVerticalStrut(24));

        // change password button
        JButton changePasswordBtn = new JButton("Change password");
        changePasswordBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        changePasswordBtn.setBackground(new Color(255, 129, 0));
        changePasswordBtn.setForeground(Color.BLACK);
        changePasswordBtn.setFont(new Font("Sans-Serif", Font.BOLD, 13));
        changePasswordBtn.setBorder(new EmptyBorder(10, 32, 10, 32));
        changePasswordBtn.setFocusPainted(false);
        changePasswordBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        changePasswordBtn.addActionListener(e -> handleChangePassword()); // handle change password button click
        panel.add(changePasswordBtn);

        return panel;
    }

    // change password logic
    private void handleChangePassword() {
        String oldPw = new String(oldPasswordField.getPassword());
        String newPw = new String(newPasswordField.getPassword());
        String confirmPw = new String(confirmPasswordField.getPassword());

        // validation
        if (oldPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
            showError("Please fill all password fields.");
            return;
        }
        if (newPw.length() < 6) {
            showError("New password must be at least 6 characters.");
            return;
        }
        if (!newPw.equals(confirmPw)) {
            showError("New passwords do not match.");
            return;
        }
        if (oldPw.equals(newPw)) {
            showError("New password must be different from current.");
            return;
        }

        // confirmation dialog
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to change your password?",
                "Change password",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        // change password
        boolean success = userDAO.changePassword(currentUser.getId(), oldPw, newPw);

        if (success) {
            JOptionPane.showMessageDialog(
                    this,
                    "Password changed successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );
            oldPasswordField.setText("");
            newPasswordField.setText("");
            confirmPasswordField.setText("");
        } else {
            showError("Current password is incorrect.");
        }
    }

    // helper methods
    private JPanel labeledField(String labelText, JComponent field) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(new Color(28, 28, 28));
        wrapper.setMaximumSize(new Dimension(320, 64));

        JLabel label = new JLabel(labelText);
        label.setForeground(new Color(160, 160, 160));
        label.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(label);
        wrapper.add(Box.createVerticalStrut(4));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(field);

        return wrapper;
    }

    private JPasswordField createPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setMaximumSize(new Dimension(320, 36));
        field.setPreferredSize(new Dimension(320, 36));
        field.setBackground(new Color(102, 102, 102));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(132, 132, 102)),
                new EmptyBorder(4, 10, 4, 10)
        ));
        return field;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // generate initials avatar
    private ImageIcon generateInitialsAvatar(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D avatarGraphics = img.createGraphics();
        avatarGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        avatarGraphics.setColor(new Color(215, 129, 0));
        avatarGraphics.fillOval(0, 0, size, size);
        avatarGraphics.setColor(Color.BLACK);
        avatarGraphics.setFont(new Font("Sans-Serif", Font.BOLD, size / 2));
        String initial = currentUser.getUsername().substring(0, 1).toUpperCase();
        FontMetrics fm = avatarGraphics.getFontMetrics();
        int x = (size - fm.stringWidth(initial)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        avatarGraphics.drawString(initial, x, y);
        avatarGraphics.dispose();
        return new ImageIcon(img);
    }
}