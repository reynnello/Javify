package com.javify.ui;

import com.javify.dao.LisHistoryDAO;
import com.javify.dao.UserDAO;
import com.javify.objects.Track;
import com.javify.objects.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class UserPanel extends JPanel {

    private static final int AVATAR_SIZE = 80;
    private static final int PROFILE_FORM_WIDTH = 390;
    private static final int PROFILE_PRIMARY_BUTTON_WIDTH = 210;
    private static final int PROFILE_SECONDARY_BUTTON_WIDTH = 150;

    private final User currentUser;
    private final UserDAO userDAO;
    private final LisHistoryDAO historyDAO;
    private JPanel historyPanel;
    private JLabel totalPlayedLabel;
    private final Runnable onBack;
    private final Runnable onAvatarChanged;

    private JLabel avatarLabel;
    private JPasswordField oldPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    public UserPanel(User currentUser, String dbUrl, Runnable onBack, Runnable onAvatarChanged) {
        this.currentUser = currentUser;
        this.userDAO = new UserDAO(dbUrl);
        this.historyDAO = new LisHistoryDAO();
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

        // two columns layout for profile and history
        JPanel columns = new JPanel(new GridLayout(1, 2, 24, 0));
        columns.setBackground(new Color(28, 28, 28));
        columns.setBorder(new EmptyBorder(32, 32, 32, 32));
        columns.setMaximumSize(new Dimension(980, Integer.MAX_VALUE));
        columns.setPreferredSize(new Dimension(980, 560));

        columns.add(createProfileColumn());
        columns.add(createHistoryColumn());

        outer.add(columns);
        return outer;
    }

    private JPanel createProfileColumn() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(18, 18, 18));
        panel.setBorder(new EmptyBorder(28, 32, 28, 32));

        avatarLabel = new JLabel(loadUserAvatar());
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(avatarLabel);

        panel.add(Box.createVerticalStrut(12));

        JLabel usernameLabel = new JLabel(currentUser.getUsername());
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setFont(new Font("Sans-Serif", Font.BOLD, 20));
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(usernameLabel);

        panel.add(Box.createVerticalStrut(12));

        JButton changeAvatarBtn = new RoundedButton("Change avatar");
        changeAvatarBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        changeAvatarBtn.setMaximumSize(new Dimension(PROFILE_SECONDARY_BUTTON_WIDTH, 34));
        changeAvatarBtn.setPreferredSize(new Dimension(PROFILE_SECONDARY_BUTTON_WIDTH, 34));
        changeAvatarBtn.setBackground(new Color(185, 99, 6));
        changeAvatarBtn.setForeground(Color.WHITE);
        changeAvatarBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        changeAvatarBtn.setFocusPainted(false);
        changeAvatarBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (changeAvatarBtn instanceof RoundedButton rounded) {
            rounded.setCornerRadius(14);
            rounded.setHoverBackground(new Color(205, 114, 16));
        }
        installButtonHover(changeAvatarBtn, new Color(185, 99, 6), new Color(205, 114, 16));
        changeAvatarBtn.addActionListener(e -> handleChangeAvatar());
        panel.add(changeAvatarBtn);

        panel.add(Box.createVerticalStrut(24));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 60, 60));
        sep.setMaximumSize(new Dimension(PROFILE_FORM_WIDTH, 1));
        sep.setPreferredSize(new Dimension(PROFILE_FORM_WIDTH, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(sep);

        panel.add(Box.createVerticalStrut(20));

        // section: change password
        JLabel pwTitle = new JLabel("Change password");
        pwTitle.setForeground(new Color(160, 160, 160));
        pwTitle.setFont(new Font("Sans-Serif", Font.BOLD, 13));
        pwTitle.setHorizontalAlignment(SwingConstants.CENTER);
        pwTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        pwTitle.setMaximumSize(new Dimension(PROFILE_FORM_WIDTH, pwTitle.getPreferredSize().height));
        pwTitle.setPreferredSize(new Dimension(PROFILE_FORM_WIDTH, pwTitle.getPreferredSize().height));
        panel.add(pwTitle);

        panel.add(Box.createVerticalStrut(14));

        oldPasswordField = createPasswordField();
        newPasswordField = createPasswordField();
        confirmPasswordField = createPasswordField();

        panel.add(labeledField("Current password", oldPasswordField));
        panel.add(Box.createVerticalStrut(10));
        panel.add(labeledField("New password", newPasswordField));
        panel.add(Box.createVerticalStrut(10));
        panel.add(labeledField("Confirm new password", confirmPasswordField));

        panel.add(Box.createVerticalStrut(20));

        // change password button
        JButton changePasswordBtn = new RoundedButton("Change password");
        changePasswordBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        changePasswordBtn.setMaximumSize(new Dimension(PROFILE_PRIMARY_BUTTON_WIDTH, 38));
        changePasswordBtn.setPreferredSize(new Dimension(PROFILE_PRIMARY_BUTTON_WIDTH, 38));
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

        return panel;
    }

    // history column
    private JPanel createHistoryColumn() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(18, 18, 18));
        panel.setBorder(new EmptyBorder(28, 32, 28, 32));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(18, 18, 18));
        header.setBorder(new EmptyBorder(0, 0, 14, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("Listening history");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Sans-Serif", Font.BOLD, 16));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        totalPlayedLabel = new JLabel();
        totalPlayedLabel.setForeground(new Color(170, 170, 170));
        totalPlayedLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
        totalPlayedLabel.setBorder(new EmptyBorder(2, 0, 0, 0));
        totalPlayedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(title);
        titleBlock.add(totalPlayedLabel);
        header.add(titleBlock, BorderLayout.WEST);

        RoundedButton clearBtn = new RoundedButton("Clear");
        clearBtn.setBackground(new Color(40, 40, 40));
        clearBtn.setHoverBackground(new Color(56, 56, 56));
        clearBtn.setForeground(new Color(200, 200, 200));
        clearBtn.setBorder(new EmptyBorder(5, 12, 5, 12));
        clearBtn.setCornerRadius(10);
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Clear listening history?",
                    "Clear history",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                historyDAO.clearHistory(currentUser.getId());
                refreshHistory();
            }
        });
        header.add(clearBtn, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        historyPanel.setBackground(new Color(18, 18, 18));

        JScrollPane scroll = new JScrollPane(historyPanel);
        scroll.setBorder(null);
        scroll.setBackground(new Color(18, 18, 18));
        scroll.getViewport().setBackground(new Color(18, 18, 18));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(185, 99, 6);
                trackColor = new Color(30, 30, 30);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return emptyBtn();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return emptyBtn();
            }

            private JButton emptyBtn() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
        });

        panel.add(scroll, BorderLayout.CENTER);
        refreshHistory();
        return panel;
    }

    public void refreshHistory() {
        if (historyPanel == null) {
            return;
        }

        historyPanel.removeAll();
        List<Track> history = historyDAO.getHistory(currentUser.getId());
        if (totalPlayedLabel != null) {
            totalPlayedLabel.setText("Total played tracks: " + historyDAO.getTotalPlayedTracks(currentUser.getId()));
        }

        if (history.isEmpty()) {
            JLabel empty = new JLabel("No tracks played yet");
            empty.setForeground(new Color(100, 100, 100));
            empty.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
            empty.setBorder(new EmptyBorder(8, 0, 0, 0));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            historyPanel.add(empty);
        } else {
            for (Track track : history) {
                historyPanel.add(createHistoryItem(track));
                historyPanel.add(Box.createVerticalStrut(4));
            }
        }

        historyPanel.revalidate();
        historyPanel.repaint();
    }

    private JPanel createHistoryItem(Track track) {
        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setBackground(new Color(28, 28, 28));
        item.setBorder(new EmptyBorder(8, 10, 8, 10));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel cover = new JLabel(getTrackCover(track, 36));
        item.add(cover, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(new Color(28, 28, 28));
        info.setBorder(new EmptyBorder(0, 8, 0, 0));

        String titleText = (track.getTitle() == null || track.getTitle().isBlank()) ? "Unknown title" : track.getTitle();
        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 13));

        String artistText = (track.getArtist() == null || track.getArtist().isBlank()) ? "Unknown artist" : track.getArtist();
        JLabel artistLabel = new JLabel(artistText);
        artistLabel.setForeground(new Color(140, 140, 140));
        artistLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 11));

        info.add(titleLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(artistLabel);
        item.add(info, BorderLayout.CENTER);

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                item.setBackground(new Color(38, 38, 38));
                info.setBackground(new Color(38, 38, 38));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                item.setBackground(new Color(28, 28, 28));
                info.setBackground(new Color(28, 28, 28));
            }
        });

        return item;
    }

    private ImageIcon getTrackCover(Track track, int size) {
        if (track.getCoverData() != null && track.getCoverData().length > 0) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(track.getCoverData()));
                if (img != null) {
                    return new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_SMOOTH));
                }
            } catch (IOException ignored) {
            }
        }

        BufferedImage placeholder = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = placeholder.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(44, 44, 44));
        graphics.fillRoundRect(0, 0, size, size, 10, 10);
        Icon noteIcon = IconLoader.svg("music-note.svg", Math.max(12, size - 14), new Color(170, 170, 170));
        if (noteIcon != null) {
            int x = (size - noteIcon.getIconWidth()) / 2;
            int y = (size - noteIcon.getIconHeight()) / 2;
            noteIcon.paintIcon(null, graphics, x, y);
        }
        graphics.dispose();
        return new ImageIcon(placeholder);
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
        wrapper.setMaximumSize(new Dimension(PROFILE_FORM_WIDTH, 64));
        wrapper.setPreferredSize(new Dimension(PROFILE_FORM_WIDTH, 64));
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
        field.setMaximumSize(new Dimension(PROFILE_FORM_WIDTH, 36));
        field.setPreferredSize(new Dimension(PROFILE_FORM_WIDTH, 36));
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
        return FileChooserUtils.chooseAvatarFile(this);
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
