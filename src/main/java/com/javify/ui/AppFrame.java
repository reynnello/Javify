package com.javify;

import com.javify.objects.User;
import com.javify.ui.LibraryPanel;
import com.javify.ui.UserPanel;
import com.javify.ui.Login;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class AppFrame extends JFrame {

    private final User currentUser;
    private final String dbUrl;

    private static final String MAIN_CARD = "main";
    private static final String PROFILE_CARD = "profile";

    private CardLayout cardLayout;
    private JPanel cardsPanel;
    private LibraryPanel libraryPanel;

    public AppFrame(User currentUser, String dbUrl) {
        this.currentUser = currentUser;
        this.dbUrl = dbUrl;
        initWindow();
    }

    private void initWindow() {
        setTitle("Javify");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
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

        UserPanel userPanel = new UserPanel(currentUser, dbUrl, () -> cardLayout.show(cardsPanel, MAIN_CARD));
        cardsPanel.add(userPanel, PROFILE_CARD);

        add(cardsPanel);
        setVisible(true);
    }

    // todo: player
    private JPanel createPlayerBar() {
        JPanel bar = new JPanel();
        bar.setBackground(new Color(10, 10, 10));
        bar.setPreferredSize(new Dimension(getWidth(), 80));
        return bar;
    }

    // center panel
    private JPanel createContentArea() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(new Color(18, 18, 18));

        // todo: playlist columnt
        JPanel sidebar = createSidebar();
        area.add(sidebar, BorderLayout.WEST);

        // library panel
        libraryPanel = new LibraryPanel(currentUser, dbUrl);
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

        JButton newPlaylist = new JButton("+");
        newPlaylist.setForeground(Color.WHITE);
        newPlaylist.setBackground(new Color(40, 40, 40));
        newPlaylist.setBorder(new EmptyBorder(4, 10, 4, 10));
        newPlaylist.setFocusPainted(false);
        newPlaylist.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(185, 99, 6));
        topBar.setPreferredSize(new Dimension(getWidth(), 56));
        topBar.setBorder(new EmptyBorder(8, 16, 8, 16));

        // left sided logo
        JLabel appName = new JLabel("Javify");
        appName.setBackground(new Color(185, 99, 6));
        appName.setFont(new Font("Sans-Serif", Font.BOLD, 18));
        appName.setForeground(Color.WHITE);
        topBar.add(appName, BorderLayout.WEST);

        // user button
        JButton userButton = createUserButton();
        topBar.add(userButton, BorderLayout.EAST);

        return topBar;
    }

    private JButton createUserButton() {
        JButton btn = new JButton();
        btn.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btn.setBackground(new Color(28, 28, 28));
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // user avatar
        ImageIcon avatar = loadAvatar();
        JLabel avatarLabel = new JLabel(avatar);
        btn.add(avatarLabel);

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
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(28, 28, 28));
        menu.setBorder(BorderFactory.createLineBorder(new Color(28, 28, 28)));

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
        menu.addSeparator();
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
        item.setBackground(new Color(28, 28, 28));
        item.setForeground(Color.WHITE);
        item.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        item.setBorder(new EmptyBorder(6, 16, 6, 16));
    }

    // load avatar from file todo: fix, not working
    private ImageIcon loadAvatar() {
        try {
            var stream = getClass().getResourceAsStream("src/main/resources/com/javify/avatars/avatar.png");
            if (stream != null) {
                BufferedImage img = ImageIO.read(stream);
                // circle avatar
                BufferedImage circle = new BufferedImage(28, 28, BufferedImage.TYPE_INT_ARGB);
                Graphics2D avatarGraphics = circle.createGraphics();
                avatarGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                avatarGraphics.setClip(new Ellipse2D.Float(0, 0, 28, 28));
                avatarGraphics.drawImage(img.getScaledInstance(28, 28, Image.SCALE_SMOOTH), 0, 0, null);
                avatarGraphics.dispose();
                return new ImageIcon(circle);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // if there is no avatar image, generate a default one
        return generateInitialsAvatar();
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
