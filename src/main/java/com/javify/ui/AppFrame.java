package com.javify.ui;

import com.javify.dao.UserDAO;
import com.javify.objects.User;
import com.javify.services.PlayerService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class AppFrame extends JFrame {

    private final User currentUser;
    private final String dbUrl;

    private static final String MAIN_CARD = "main";
    private static final String PROFILE_CARD = "profile";
    private static final Color MENU_BG = new Color(24, 24, 24);
    private static final Color MENU_HOVER = new Color(48, 48, 48);

    private CardLayout cardLayout;
    private JPanel cardsPanel;
    private LibraryPanel libraryPanel;
    private final PlayerService playerService;
    private final UserDAO userDAO;
    private JLabel topAvatarLabel;

    public AppFrame(User currentUser, String dbUrl) {
        this.currentUser = currentUser;
        this.dbUrl = dbUrl;
        this.playerService = new PlayerService();
        this.userDAO = new UserDAO(dbUrl);
        initWindow();
    }

    private void initWindow() {
        setTitle("Javify");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 900);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);

        // main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(28, 28, 28));

        // top bar
        JPanel topBar = createTopBar();
        mainPanel.add(topBar, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        // main window
        JPanel mainWindow = new JPanel(new BorderLayout());
        mainWindow.setBackground(new Color(18, 18, 18));
        mainWindow.add(createTopBar(), BorderLayout.NORTH);
        mainWindow.add(createPlayerBar(), BorderLayout.SOUTH);  // todo: player bar
        mainWindow.add(createContentArea(), BorderLayout.CENTER);
        cardsPanel.add(mainWindow, MAIN_CARD);

        UserPanel userPanel = new UserPanel(
                currentUser,
                dbUrl,
                () -> cardLayout.show(cardsPanel, MAIN_CARD),
                this::refreshTopAvatar
        );
        cardsPanel.add(userPanel, PROFILE_CARD);

        add(cardsPanel);
        setVisible(true);
    }

    // player bar
    private JPanel createPlayerBar() {
        return new PlayerBar(playerService);
    }

    // center panel
    private JPanel createContentArea() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(new Color(18, 18, 18));

        // todo: playlist columnt
        JPanel sidebar = createSidebar();
        area.add(sidebar, BorderLayout.WEST);

        // library panel
        libraryPanel = new LibraryPanel(currentUser, playerService);
        area.add(libraryPanel, BorderLayout.CENTER);

        return area;
    }

    // todo: sidebar panel
    // for now it's just a placeholder with create playlist button
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(12, 12, 12));
        sidebar.setPreferredSize(new Dimension(220, getHeight()));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        header.setBackground(new Color(12, 12, 12));

        JLabel title = new JLabel("Your Library");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Sans-Serif", Font.BOLD, 14));
        header.add(title);

        JButton newPlaylist = new RoundedButton("+");
        newPlaylist.setForeground(Color.WHITE);
        newPlaylist.setBackground(new Color(40, 40, 40));
        newPlaylist.setBorder(new EmptyBorder(4, 10, 4, 10));
        newPlaylist.setFocusPainted(false);
        newPlaylist.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (newPlaylist instanceof RoundedButton rounded) {
            rounded.setCornerRadius(12);
            rounded.setHoverBackground(new Color(58, 58, 58));
        }
        newPlaylist.setToolTipText("New playlist");
        header.add(newPlaylist);

        sidebar.add(header, BorderLayout.NORTH);

        // todo: playlist list
        JPanel playlistList = new JPanel();
        playlistList.setBackground(new Color(12, 12, 12));
        sidebar.add(new JScrollPane(playlistList) {{
            setBorder(null);
            getViewport().setBackground(new Color(12, 12, 12));
        }}, BorderLayout.CENTER);

        return sidebar;
    }

    // top bar
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(185, 99, 6));
        topBar.setPreferredSize(new Dimension(getWidth(), 64));
        topBar.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel appName = new JLabel("Javify");
        appName.setFont(new Font("Sans-Serif", Font.BOLD, 18));
        appName.setForeground(Color.WHITE);
        topBar.add(appName, BorderLayout.WEST);

        // search field
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        searchPanel.setBackground(new Color(185, 99, 6));

        JPanel searchWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D graphics = (Graphics2D) g.create();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setColor(new Color(20, 20, 20));
                graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                graphics.setColor(new Color(255, 255, 255, 70));
                graphics.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                graphics.dispose();
            }
        };
        searchWrapper.setOpaque(false);
        searchWrapper.setPreferredSize(new Dimension(460, 40));
        searchWrapper.setMinimumSize(new Dimension(320, 40));

        JTextField searchField = new JTextField(34);
        searchField.setOpaque(false);
        searchField.setBackground(new Color(0, 0, 0, 0));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(new EmptyBorder(10, 14, 10, 14));
        searchField.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        // placeholder
        searchField.setText("Search...");
        searchField.setForeground(new Color(200, 200, 200));
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("Search...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.WHITE);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search...");
                    searchField.setForeground(new Color(200, 200, 200));
                }
            }
        });
        // live search
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) {
                doSearch();
            }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) {
                doSearch();
            }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) {}
            private void doSearch() {
                String q = searchField.getText().trim();
                if (!q.equals("Search...") && libraryPanel != null) {
                    libraryPanel.search(q);
                }
            }
        });

        searchWrapper.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchWrapper);
        topBar.add(searchPanel, BorderLayout.CENTER);

        topBar.add(createUserButton(), BorderLayout.EAST);
        return topBar;
    }

    private JButton createUserButton() {
        JButton btn = new RoundedButton();
        btn.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btn.setBackground(new Color(28, 28, 28));
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (btn instanceof RoundedButton rounded) {
            rounded.setCornerRadius(14);
            rounded.setHoverBackground(new Color(45, 45, 45));
        }
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(45, 45, 45));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(28, 28, 28));
            }
        });

        // user avatar
        ImageIcon avatar = loadAvatar();
        topAvatarLabel = new JLabel(avatar);
        btn.add(topAvatarLabel);

        // username
        JLabel nameLabel = new JLabel(currentUser.getUsername());
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        btn.add(nameLabel);

        // dropdown arrow
        JLabel arrow = new JLabel("↓");
        arrow.setForeground(Color.WHITE);
        btn.add(arrow);

        btn.addActionListener(e -> showDropdown(btn));

        return btn;
    }

    // dropdown menu
    private void showDropdown(JButton btn) {
        JPopupMenu menu = new JPopupMenu() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D graphics = (Graphics2D) g.create();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setColor(MENU_BG);
                graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                graphics.setColor(new Color(60, 60, 60));
                graphics.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                graphics.dispose();
            }
        };
        menu.setOpaque(false);
        menu.setBorder(new EmptyBorder(6, 6, 6, 6));

        JMenuItem profileItem = new JMenuItem("Profile");
        JMenuItem settingsItem = new JMenuItem("Settings");
        JMenuItem logoutItem = new JMenuItem("Log out");

        styleMenuItem(profileItem);
        styleMenuItem(settingsItem);
        styleMenuItem(logoutItem);
        logoutItem.setForeground(new Color(220, 80, 80));

        profileItem.addActionListener(e -> cardLayout.show(cardsPanel, PROFILE_CARD));
        settingsItem.addActionListener(e -> {});
        logoutItem.addActionListener(e -> handleLogout());

        menu.add(profileItem);
        menu.add(settingsItem);
        menu.add(Box.createVerticalStrut(4));

        JPanel divider = new JPanel();
        divider.setOpaque(true);
        divider.setBackground(new Color(50, 50, 50));
        divider.setPreferredSize(new Dimension(1, 1));
        menu.add(divider);

        menu.add(Box.createVerticalStrut(4));
        menu.add(logoutItem);

        // set popup size to match button size
        menu.setPopupSize(btn.getWidth(), menu.getPreferredSize().height);
        menu.show(btn, 0, btn.getHeight());
    }

    // logout action
    private void handleLogout() {
        // confirmation dialog
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Log out",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new Login(dbUrl);
        }
    }

    // style menu items
    private void styleMenuItem(JMenuItem item) {
        item.setOpaque(true);
        item.setBackground(MENU_BG);
        item.setForeground(Color.WHITE);
        item.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        item.setBorder(new EmptyBorder(9, 14, 9, 14));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setFocusPainted(false);
        item.setContentAreaFilled(false);
        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                item.setBackground(MENU_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                item.setBackground(MENU_BG);
            }
        });
    }

    // load avatar from file todo: fix, not working
    private ImageIcon loadAvatar() {
        try {
            byte[] avatarData = userDAO.getAvatarData(currentUser.getId());
            if (avatarData != null && avatarData.length > 0) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(avatarData));
                if (img != null) {
                    // circle avatar
                    BufferedImage circle = new BufferedImage(28, 28, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D avatarGraphics = circle.createGraphics();
                    avatarGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    avatarGraphics.setClip(new Ellipse2D.Float(0, 0, 28, 28));
                    avatarGraphics.drawImage(img.getScaledInstance(28, 28, Image.SCALE_SMOOTH), 0, 0, null);
                    avatarGraphics.dispose();
                    return new ImageIcon(circle);
                }
            }
        } catch (IllegalStateException | IOException ignored) {
        }
        // if there is no avatar image, generate a default one
        return generateInitialsAvatar();
    }

    // refresh top avatar in the user button
    private void refreshTopAvatar() {
        if (topAvatarLabel != null) {
            topAvatarLabel.setIcon(loadAvatar());
            topAvatarLabel.revalidate();
            topAvatarLabel.repaint();
        }
    }

    // generate a default avatar image with initials
    private ImageIcon generateInitialsAvatar() {
        BufferedImage img = new BufferedImage(28, 28, BufferedImage.TYPE_INT_ARGB);
        Graphics2D avatarGraphics = img.createGraphics();
        avatarGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        avatarGraphics.setColor(new Color(255, 255, 255));
        avatarGraphics.fillOval(0, 0, 28, 28);
        avatarGraphics.setColor(Color.BLACK);
        avatarGraphics.setFont(new Font("Sans-Serif", Font.BOLD, 13));
        String initial = currentUser.getUsername().substring(0, 1).toUpperCase(); // get first letter of username
        FontMetrics fm = avatarGraphics.getFontMetrics(); // get font metrics
        int x = (28 - fm.stringWidth(initial)) / 2; // center the initial
        int y = (28 - fm.getHeight()) / 2 + fm.getAscent(); // align the initial to the bottom
        avatarGraphics.drawString(initial, x, y);
        avatarGraphics.dispose();
        return new ImageIcon(img);
    }
}
