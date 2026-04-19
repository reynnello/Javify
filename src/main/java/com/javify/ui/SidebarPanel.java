package com.javify.ui;

import com.javify.dao.PlaylistDAO;
import com.javify.objects.Playlist;
import com.javify.objects.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

public class SidebarPanel extends JPanel {

    private final User currentUser;
    private final String dbUrl;
    private final PlaylistDAO playlistDAO;
    private final JFrame parentFrame;
    private final Consumer<Playlist> onPlaylistSelected;
    private final Runnable onLibrarySelected;

    private JPanel playlistListPanel;
    private Integer selectedPlaylistId;

    public SidebarPanel(JFrame parentFrame, User currentUser, String dbUrl, Consumer<Playlist> onPlaylistSelected, Runnable onLibrarySelected) {
        this.parentFrame = parentFrame;
        this.currentUser = currentUser;
        this.dbUrl = dbUrl;
        this.playlistDAO = new PlaylistDAO();
        this.onPlaylistSelected = onPlaylistSelected;
        this.onLibrarySelected = onLibrarySelected;
        initUi();
        refreshPlaylists();
    }

    private void initUi() {
        setLayout(new BorderLayout());
        setBackground(new Color(12, 12, 12));
        setPreferredSize(new Dimension(220, 0));

        // header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(12, 12, 12));
        header.setBorder(new EmptyBorder(14, 16, 10, 12));

        RoundedButton libraryBtn = new RoundedButton("Your Library");
        libraryBtn.setBackground(new Color(12, 12, 12));
        libraryBtn.setHoverBackground(new Color(26, 26, 26));
        libraryBtn.setPressedBackground(new Color(20, 20, 20));
        libraryBtn.setForeground(Color.WHITE);
        libraryBtn.setFont(new Font("Sans-Serif", Font.BOLD, 14));
        libraryBtn.setBorder(new EmptyBorder(6, 10, 6, 10));
        libraryBtn.setCornerRadius(12);
        libraryBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Icon libraryIcon = IconLoader.svg("playlist.svg", 15, Color.WHITE);
        if (libraryIcon != null) {
            libraryBtn.setIcon(libraryIcon);
            libraryBtn.setIconTextGap(8);
        }
        libraryBtn.addActionListener(e -> selectLibrary());
        header.add(libraryBtn, BorderLayout.WEST);

        Icon addIconDefault = IconLoader.svg("plus-circle.svg", 18, new Color(160, 160, 160));
        Icon addIconHover = IconLoader.svg("plus-circle.svg", 18, Color.WHITE);

        RoundedButton addBtn = new RoundedButton();
        addBtn.setIcon(addIconDefault);
        addBtn.setBackground(new Color(12, 12, 12));
        addBtn.setHoverBackground(new Color(30, 30, 30));
        addBtn.setPressedBackground(new Color(24, 24, 24));
        addBtn.setBorder(new EmptyBorder(4, 8, 4, 8));
        addBtn.setCornerRadius(12);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.setToolTipText("New playlist");
        addBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                addBtn.setIcon(addIconHover);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                addBtn.setIcon(addIconDefault);
            }
        });
        addBtn.addActionListener(e -> {
            CreatePlaylistDialog dialog = new CreatePlaylistDialog(parentFrame, currentUser, dbUrl, this::refreshPlaylists);
            dialog.setVisible(true);
        });
        header.add(addBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // playlist list
        playlistListPanel = new JPanel();
        playlistListPanel.setLayout(new BoxLayout(playlistListPanel, BoxLayout.Y_AXIS));
        playlistListPanel.setBackground(new Color(12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(playlistListPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(new Color(12, 12, 12));
        scrollPane.getViewport().setBackground(new Color(12, 12, 12));
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(60, 60, 60);
                trackColor = new Color(12, 12, 12);
            }
            @Override protected JButton createDecreaseButton(int o) { return emptyBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return emptyBtn(); }
            private JButton emptyBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    public void refreshPlaylists() {
        playlistListPanel.removeAll();

        List<Playlist> playlists = playlistDAO.getPlaylistsByUser(currentUser.getId());

        if (playlists.isEmpty()) {
            JLabel empty = new JLabel("No playlists yet");
            empty.setForeground(new Color(100, 100, 100));
            empty.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
            empty.setBorder(new EmptyBorder(16, 16, 0, 0));
            playlistListPanel.add(empty);
        }

        for (Playlist playlist : playlists) {
            playlistListPanel.add(createPlaylistItem(playlist));
        }

        playlistListPanel.revalidate();
        playlistListPanel.repaint();
    }

    private JPanel createPlaylistItem(Playlist playlist) {
        JPanel item = new JPanel(new BorderLayout(10, 0));
        boolean selected = selectedPlaylistId != null && selectedPlaylistId == playlist.getId();
        Color baseBg = selected ? new Color(28, 28, 28) : new Color(12, 12, 12);
        Color hoverBg = new Color(24, 24, 24);
        item.setBackground(baseBg);
        item.setBorder(new EmptyBorder(6, 12, 6, 12));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setFocusable(true);

        // cover
        JLabel cover = new JLabel(getPlaylistCover(playlist, 40));
        item.add(cover, BorderLayout.WEST);

        // title
        JLabel name = new JLabel(playlist.getName());
        name.setForeground(selected ? Color.WHITE : new Color(200, 200, 200));
        name.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        item.add(name, BorderLayout.CENTER);

        java.awt.event.MouseAdapter clickAndHover = new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                item.setBackground(hoverBg);
                name.setForeground(Color.WHITE);
            }

            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                item.setBackground(baseBg);
                name.setForeground(selected ? Color.WHITE : new Color(200, 200, 200));
            }

            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                selectedPlaylistId = playlist.getId();
                item.requestFocusInWindow();
                refreshPlaylists();
                if (onPlaylistSelected != null) {
                    onPlaylistSelected.accept(playlist);
                }
            }
        };
        item.addMouseListener(clickAndHover);
        cover.addMouseListener(clickAndHover);
        name.addMouseListener(clickAndHover);

        // context menu on right click
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.setBackground(new Color(28, 28, 28));
        contextMenu.setBorder(BorderFactory.createLineBorder(new Color(52, 52, 52)));
        contextMenu.setOpaque(true);

        JMenuItem changeItem = new JMenuItem("Change");
        JMenuItem deleteItem = new JMenuItem("Delete");
        styleContextMenuItem(changeItem);
        styleContextMenuItem(deleteItem);
        deleteItem.setForeground(new Color(220, 80, 80));

        changeItem.addActionListener(ev -> {
            CreatePlaylistDialog dialog = new CreatePlaylistDialog(parentFrame, currentUser, dbUrl, playlist, this::refreshPlaylists);
            dialog.setVisible(true);
        });

        deleteItem.addActionListener(ev -> {
            int confirm = JOptionPane.showConfirmDialog(parentFrame, "Delete \"" + playlist.getName() + "\"?", "Delete playlist", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean wasSelected = selectedPlaylistId != null && selectedPlaylistId == playlist.getId();
                playlistDAO.deletePlaylist(playlist.getId());
                if (wasSelected) {
                    selectLibrary();
                }
                refreshPlaylists();
            }
        });

        contextMenu.add(changeItem);
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(52, 52, 52));
        separator.setBackground(new Color(52, 52, 52));
        contextMenu.add(separator);
        contextMenu.add(deleteItem);

        java.awt.event.MouseAdapter contextListener = new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) contextMenu.show(item, e.getX(), e.getY());
            }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger())contextMenu.show(item, e.getX(), e.getY());
            }
        };
        item.addMouseListener(contextListener);
        cover.addMouseListener(contextListener);
        name.addMouseListener(contextListener);

        return item;
    }

    private ImageIcon getPlaylistCover(Playlist playlist, int size) {
        if (playlist.getCoverData() != null) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(playlist.getCoverData()));
                if (img != null) {
                    return new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_SMOOTH));
                }
            } catch (Exception ignored) {}
        }

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = img.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(50, 50, 50));
        graphics.fillRoundRect(0, 0, size, size, 6, 6);

        Icon noteIcon = IconLoader.svg("music-note.svg", size / 2, new Color(120, 120, 120));
        if (noteIcon != null) {
            int x = (size - noteIcon.getIconWidth()) / 2;
            int y = (size - noteIcon.getIconHeight()) / 2;
            noteIcon.paintIcon(null, graphics, x, y);
        }

        graphics.dispose();
        return new ImageIcon(img);
    }

    private void styleContextMenuItem(JMenuItem item) {
        Color baseBg = new Color(28, 28, 28);
        Color hoverBg = new Color(40, 40, 40);

        item.setBackground(baseBg);
        item.setForeground(Color.WHITE);
        item.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        item.setBorder(new EmptyBorder(8, 14, 8, 14));
        item.setBorderPainted(false);
        item.setFocusPainted(false);
        item.setFocusable(false);
        item.setOpaque(true);

        item.setUI(new javax.swing.plaf.basic.BasicMenuItemUI() {
            @Override
            protected void paintBackground(Graphics graphics, JMenuItem menuItem, Color backgroundColor) {
                ButtonModel model = menuItem.getModel();
                graphics.setColor(model.isArmed() || model.isRollover() ? hoverBg : baseBg);
                graphics.fillRect(0, 0, menuItem.getWidth(), menuItem.getHeight());
            }

            @Override
            protected void paintText(Graphics graphics, JMenuItem menuItem, Rectangle textRect, String text) {
                Graphics2D graphics2d = (Graphics2D) graphics.create();
                graphics2d.setFont(menuItem.getFont());
                // Keep text bright on hover while preserving custom red color for Delete.
                Color baseColor = menuItem.getForeground();
                Color hoverColor = baseColor.equals(new Color(220, 80, 80)) ? new Color(240, 130, 130) : Color.WHITE;
                graphics2d.setColor(menuItem.getModel().isArmed() || menuItem.getModel().isRollover() ? hoverColor : baseColor);
                FontMetrics fm = graphics2d.getFontMetrics();
                graphics2d.drawString(text, textRect.x, textRect.y + fm.getAscent());
                graphics2d.dispose();
            }
        });
    }

    private void selectLibrary() {
        selectedPlaylistId = null;
        refreshPlaylists();
        if (onLibrarySelected != null) {
            onLibrarySelected.run();
        }
    }
}
