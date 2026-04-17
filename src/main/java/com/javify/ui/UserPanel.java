package com.javify.ui;

import com.javify.dao.UserDAO;
import com.javify.objects.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class UserPanel extends JPanel {

    private static final int AVATAR_SIZE = 80;

    private final User currentUser;
    private final UserDAO userDAO;
    private final Runnable onBack;
    private final Runnable onAvatarChanged;

    private JLabel avatarLabel;
    private JPasswordField oldPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    public UserPanel(User currentUser, String dbUrl, Runnable onBack, Runnable onAvatarChanged) {
        this.currentUser = currentUser;
        this.userDAO = new UserDAO(dbUrl);
        this.onBack = onBack;
        this.onAvatarChanged = onAvatarChanged;
        initUi();
    }

    private void initUi() {
        setLayout(new BorderLayout());
        setBackground(new Color(28, 28, 28));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(185, 99, 6));
        topBar.setPreferredSize(new Dimension(getWidth(), 56));
        topBar.setBorder(new EmptyBorder(8, 16, 8, 16));

        JButton backBtn = new RoundedButton("Back");
        backBtn.setForeground(Color.WHITE);
        backBtn.setBackground(new Color(28, 28, 28));
        backBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Icon backIcon = IconLoader.svg("arrow-left.svg", 14, Color.WHITE);
        backBtn.setIcon(backIcon);
        backBtn.setIconTextGap(6);
        if (backIcon == null) {
            backBtn.setText("← Back");
        }
        if (backBtn instanceof RoundedButton rounded) {
            rounded.setCornerRadius(14);
            rounded.setHoverBackground(new Color(48, 48, 48));
        }
        installButtonHover(backBtn, new Color(28, 28, 28), new Color(48, 48, 48));
        backBtn.addActionListener(e -> onBack.run());
        topBar.add(backBtn, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        JPanel content = createContentPanel();
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setBackground(new Color(18, 18, 18));
        scrollPane.getViewport().setBackground(new Color(18, 18, 18));
        add(scrollPane, BorderLayout.CENTER);
    }

    // content panel
    private JPanel createContentPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(new Color(28, 28, 28));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(18, 18, 18));
        panel.setBorder(new EmptyBorder(32, 48, 32, 48));
        panel.setMaximumSize(new Dimension(460, Integer.MAX_VALUE));
        panel.setPreferredSize(new Dimension(460, 520));

        // avatar
        avatarLabel = new JLabel(loadUserAvatar());
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(avatarLabel);

        panel.add(Box.createVerticalStrut(12));

        // username
        JLabel usernameLabel = new JLabel(currentUser.getUsername());
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setFont(new Font("Sans-Serif", Font.BOLD, 20));
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(usernameLabel);

        panel.add(Box.createVerticalStrut(16));

        // change avatar button
        JButton changeAvatarBtn = new RoundedButton("Change avatar");
        changeAvatarBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        changeAvatarBtn.setBackground(new Color(185, 99, 6));
        changeAvatarBtn.setForeground(Color.WHITE);
        changeAvatarBtn.setBorder(new EmptyBorder(5, 10, 5, 10));
        changeAvatarBtn.setFocusPainted(false);
        changeAvatarBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (changeAvatarBtn instanceof RoundedButton rounded) {
            rounded.setCornerRadius(14);
            rounded.setHoverBackground(new Color(205, 114, 16));
        }
        installButtonHover(changeAvatarBtn, new Color(185, 99, 6), new Color(205, 114, 16));
        changeAvatarBtn.addActionListener(e -> handleChangeAvatar());
        panel.add(changeAvatarBtn);

        panel.add(Box.createVerticalStrut(16));


        // separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 60, 60));
        sep.setMaximumSize(new Dimension(360, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(sep);

        panel.add(Box.createVerticalStrut(24));

        // section: change password


        panel.add(Box.createVerticalStrut(16));

        // password fields
        oldPasswordField = createPasswordField();
        newPasswordField = createPasswordField();
        confirmPasswordField = createPasswordField();

        panel.add(labeledField("Current password", oldPasswordField));
        panel.add(Box.createVerticalStrut(10));
        panel.add(labeledField("New password", newPasswordField));
        panel.add(Box.createVerticalStrut(10));
        panel.add(labeledField("Confirm new password", confirmPasswordField));

        panel.add(Box.createVerticalStrut(24));

        // change password button
        JButton changePasswordBtn = new RoundedButton("Change password");
        changePasswordBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        changePasswordBtn.setBackground(new Color(185, 99, 6));
        changePasswordBtn.setForeground(Color.WHITE);
        changePasswordBtn.setFont(new Font("Sans-Serif", Font.BOLD, 13));
        changePasswordBtn.setBorder(new EmptyBorder(10, 32, 10, 32));
        changePasswordBtn.setFocusPainted(false);
        changePasswordBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (changePasswordBtn instanceof RoundedButton rounded) {
            rounded.setCornerRadius(16);
            rounded.setHoverBackground(new Color(205, 114, 16));
        }
        installButtonHover(changePasswordBtn, new Color(185, 99, 6), new Color(205, 114, 16));
        changePasswordBtn.addActionListener(e -> handleChangePassword());
        panel.add(changePasswordBtn);

        outer.add(panel);
        return outer;
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
        wrapper.setBackground(new Color(18, 18, 18));
        wrapper.setMaximumSize(new Dimension(360, 64));
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

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

    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setMaximumSize(new Dimension(360, 36));
        field.setPreferredSize(new Dimension(360, 36));
        field.setBackground(new Color(50, 50, 50));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                new EmptyBorder(4, 10, 4, 10)
        ));
        return field;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void handleChangeAvatar() {
        File selectedImage = chooseAvatarFile();
        if (selectedImage == null) {
            return;
        }

        try {
            BufferedImage source = ImageIO.read(selectedImage);
            if (source == null) {
                showError("Selected file is not a supported image.");
                return;
            }

            BufferedImage avatarImage = createCircularAvatarImage(source, AVATAR_SIZE);
            byte[] avatarBytes = toPngBytes(avatarImage);
            boolean saved = userDAO.updateAvatar(currentUser.getId(), avatarBytes);
            if (!saved) {
                showError("Unable to save avatar.");
                return;
            }

            avatarLabel.setIcon(new ImageIcon(avatarImage));
            avatarLabel.revalidate();
            avatarLabel.repaint();
            if (onAvatarChanged != null) {
                onAvatarChanged.run();
            }
        } catch (IllegalStateException ex) {
            showError("Unable to save avatar.");
        } catch (IOException ex) {
            showError("Unable to open selected image.");
        }
    }

    private File chooseAvatarFile() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner instanceof Frame frameOwner) {
            FileDialog dialog = new FileDialog(frameOwner, "Choose avatar", FileDialog.LOAD);
            dialog.setFilenameFilter((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
            });
            dialog.setVisible(true);

            if (dialog.getFile() == null) {
                return null;
            }
            return new File(dialog.getDirectory(), dialog.getFile());
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose avatar");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Image files (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"
        ));
        chooser.setApproveButtonText("Choose");
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
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

    // create circular avatar from a square image
    private BufferedImage createCircularAvatarImage(BufferedImage source, int size) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;

        BufferedImage square = source.getSubimage(x, y, side, side);
        Image scaled = square.getScaledInstance(size, size, Image.SCALE_SMOOTH);

        BufferedImage circle = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D avatarGraphics = circle.createGraphics();
        avatarGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        avatarGraphics.setClip(new Ellipse2D.Float(0, 0, size, size));
        avatarGraphics.drawImage(scaled, 0, 0, null);
        avatarGraphics.dispose();
        return circle;
    }

    // convert BufferedImage to byte array
    private byte[] toPngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    // load user avatar
    private ImageIcon loadUserAvatar() {
        try {
            byte[] avatarData = userDAO.getAvatarData(currentUser.getId());
            if (avatarData == null || avatarData.length == 0) {
                return generateInitialsAvatar(AVATAR_SIZE);
            }

            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(avatarData));
            return image != null ? new ImageIcon(image) : generateInitialsAvatar(AVATAR_SIZE);
        } catch (Exception ignored) {
            return generateInitialsAvatar(AVATAR_SIZE);
        }
    }

    // generate initials avatar
    private ImageIcon generateInitialsAvatar(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D avatarGraphics = img.createGraphics();
        avatarGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        avatarGraphics.setBackground(new Color(185, 99, 6));
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
