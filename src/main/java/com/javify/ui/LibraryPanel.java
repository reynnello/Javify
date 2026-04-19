package com.javify.ui;

import com.javify.objects.Track;
import com.javify.objects.User;
import com.javify.services.LibraryService;
import com.javify.services.PlayerService;
import com.javify.dao.PlaylistDAO;
import com.javify.objects.Playlist;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.TableRowSorter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class LibraryPanel extends JPanel {

    private static final int TRACK_ROW_HEIGHT = 56;
    private static final int COVER_SIZE = 46;
    private static final Color ACCENT = new Color(185, 99, 6);

    private final Icon trackPlayIcon = IconLoader.svg("play.svg", 14, new Color(18, 18, 18));
    private final Icon trackPauseIcon = IconLoader.svg("pause.svg", 14, new Color(18, 18, 18));

    private final User currentUser;
    private final LibraryService libraryService;
    private final PlayerService playerService;
    private final PlaylistDAO playlistDAO;
    private final CardLayout cardLayout;
    private final JPanel cards;

    private DefaultTableModel tableModel;
    private List<Track> currentTracks;
    private Integer activePlaylistId;
    private int hoveredViewRow = -1;
    private JPanel toastContainer;
    private Timer toastTimer;

    private static final String EMPTY_CARD = "empty";
    private static final String LIBRARY_CARD = "library";

    public LibraryPanel(User currentUser, PlayerService playerService) {
        this.currentUser = currentUser;
        this.libraryService = new LibraryService();
        this.playerService = playerService;
        this.playlistDAO = new PlaylistDAO();
        this.cardLayout = new CardLayout();
        this.cards = new JPanel(cardLayout);

        setLayout(new BorderLayout());
        setBackground(new Color(18, 18, 18));

        cards.add(createEmptyState(), EMPTY_CARD);
        cards.add(createLibraryView(), LIBRARY_CARD);
        add(cards, BorderLayout.CENTER);

        toastContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        toastContainer.setOpaque(false);
        toastContainer.setVisible(false);
        add(toastContainer, BorderLayout.SOUTH);

        // if tracks already exist, load them immediately from db
        List<Track> existing = libraryService.getAllTracks();
        if (!existing.isEmpty()) {
            loadTracks(existing);
        }
    }

    // empty state
    private JPanel createEmptyState() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(18, 18, 18));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(new Color(18, 18, 18));

        JLabel icon = new JLabel();
        Icon noteIcon = IconLoader.svg("music-note.svg", 48, Color.WHITE);
        icon.setIcon(noteIcon);
        icon.setText("");
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(icon);

        inner.add(Box.createVerticalStrut(16));

        JLabel title = new JLabel("No music folder selected");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Sans-Serif", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(title);

        inner.add(Box.createVerticalStrut(8));

        JLabel subtitle = new JLabel("Choose music files or a folder");
        subtitle.setForeground(new Color(160, 160, 160));
        subtitle.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(subtitle);

        inner.add(Box.createVerticalStrut(24));

        JButton chooseBtn = new RoundedButton("Add music");
        chooseBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        chooseBtn.setBackground(ACCENT);
        chooseBtn.setForeground(Color.WHITE);
        chooseBtn.setFont(new Font("Sans-Serif", Font.BOLD, 13));
        chooseBtn.setBorder(new EmptyBorder(12, 32, 12, 32));
        chooseBtn.setFocusPainted(false);
        chooseBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chooseBtn.setIcon(IconLoader.svg("plus-circle.svg", 16, new Color(255, 255, 255)));
        chooseBtn.setIconTextGap(8);
        if (chooseBtn instanceof RoundedButton rounded) {
            rounded.setCornerRadius(18);
            rounded.setHoverBackground(new Color(205, 114, 16));
        }
        chooseBtn.addActionListener(e -> openMusicChooser());
        inner.add(chooseBtn);

        panel.add(inner);
        return panel;
    }

    private JPanel createLibraryView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(18, 18, 18));
        header.setBorder(new EmptyBorder(16, 24, 8, 24));

        JLabel countLabel = new JLabel();
        countLabel.setForeground(new Color(160, 160, 160));
        countLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        header.add(countLabel, BorderLayout.WEST);

        // button to add music when tracks are already present, opens the same file chooser as in empty state
        JButton changeFolderBtn = new RoundedButton("Add music");
        changeFolderBtn.setBackground(ACCENT);
        changeFolderBtn.setForeground(new Color(255, 255, 255));
        changeFolderBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        changeFolderBtn.setFocusPainted(false);
        changeFolderBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        changeFolderBtn.setIcon(IconLoader.svg("plus-circle.svg", 20, new Color(255, 255, 255)));
        changeFolderBtn.setIconTextGap(6);
        if (changeFolderBtn instanceof RoundedButton rounded) {
            rounded.setCornerRadius(14);
            rounded.setHoverBackground(new Color(205, 114, 16));
        }
        changeFolderBtn.addActionListener(e -> openMusicChooser());
        header.add(changeFolderBtn, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);

        // table with tracks
        String[] columns = {"", "", "Title", "Artist", "Album", "Duration"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        styleTable(table);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0 || currentTracks == null) {
                    return;
                }
                int modelRow = table.convertRowIndexToModel(viewRow);
                int viewCol = table.columnAtPoint(e.getPoint());

                // single click on cover cell toggles play/pause for that row
                if (e.getClickCount() == 1 && viewCol == 0) {
                    togglePlaybackAtRow(modelRow);
                    table.repaint();
                    return;
                }

            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoveredViewRow = -1;
                table.setCursor(Cursor.getDefaultCursor());
                table.repaint();
            }
        });

        // hover icon on table row
        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (hoveredViewRow != row) {
                    hoveredViewRow = row;
                    table.repaint();
                }
                table.setCursor(col == 0 && row >= 0
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }
        });

        // context menu on right click
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) showTrackContextMenu(table, e);
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) showTrackContextMenu(table, e);
            }
        });

        // repaint hover icon when player state/current track changes
        playerService.setOnStateChanged(state -> SwingUtilities.invokeLater(table::repaint));
        playerService.setOnTrackChanged(track -> SwingUtilities.invokeLater(table::repaint));

        // scrollable table
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(new Color(18, 18, 18));
        scrollPane.getViewport().setBackground(new Color(18, 18, 18));

        // hide scrollbar thumb and track color
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(185, 99, 6);
                trackColor = new Color(30, 30, 30);
            }
            @Override
            protected JButton createDecreaseButton(int o) { return emptyBtn(); }
            @Override
            protected JButton createIncreaseButton(int o) { return emptyBtn(); }
            private JButton emptyBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);

        // counter label in the header
        panel.putClientProperty("countLabel", countLabel);

        return panel;
    }

    // system chooser for music files and folders
    private void openMusicChooser() {
        File[] selected = FileChooserUtils.chooseAudioFiles(this);
        
        if (selected.length == 0) {
            return;
        }

        List<Track> tracks = libraryService.scanSelection(Arrays.asList(selected));
        if (tracks.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No MP3, WAV or FLAC files found in selected items.",
                    "Nothing added", JOptionPane.WARNING_MESSAGE);
        } else {
            loadTracks(libraryService.getAllTracks());
        }
    }

    // load tracks into the table
    private void loadTracks(List<Track> tracks) {
        this.currentTracks = tracks;
        tableModel.setRowCount(0);

        // loop through all tracks and add them to the table
        for (int i = 0; i < tracks.size(); i++) {
            Track t = tracks.get(i);
            ImageIcon cover = getCoverIcon(t);
            tableModel.addRow(new Object[]{
                    i + 1,
                    cover,
                    t.getTitle(),
                    t.getArtist() != null ? t.getArtist() : "Unknown Artist",
                    t.getAlbum() != null ? t.getAlbum() : "Unknown Album",
                    formatDuration(t.getDuration())
            });
        }

        // change count label in the header
        Component libraryView = cards.getComponent(1);
        if (libraryView instanceof JPanel lv) {
            JLabel countLabel = (JLabel) lv.getClientProperty("countLabel");
            if (countLabel != null) {
                countLabel.setText(tracks.size() + " tracks");
            }
        }

        cardLayout.show(cards, LIBRARY_CARD);
    }

    // style table
    private void styleTable(JTable table) {
        table.setBackground(new Color(18, 18, 18));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(40, 40, 40));
        table.setRowHeight(TRACK_ROW_HEIGHT);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        // Remove any LAF-specific gaps that look like column separators.
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getColumnModel().setColumnMargin(0);
        table.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(18, 18, 18));
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setFocusable(false); // focus disabled

        // header
        table.getTableHeader().setBackground(new Color(18, 18, 18));
        table.getTableHeader().setForeground(new Color(160, 160, 160));
        table.getTableHeader().setFont(new Font("Sans-Serif", Font.PLAIN, 12));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        table.getTableHeader().setReorderingAllowed(false);

        // column widths
        table.getColumnModel().getColumn(0).setMaxWidth(54);
        table.getColumnModel().getColumn(0).setMinWidth(54);
        table.getColumnModel().getColumn(1).setMaxWidth(56);
        table.getColumnModel().getColumn(1).setMinWidth(56);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setMaxWidth(70);

        // renderers
        DefaultTableCellRenderer numberRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                int modelRow = t.convertRowIndexToModel(row);
                boolean isCurrent = isCurrentTrackAtModelRow(modelRow);
                boolean showControl = row == hoveredViewRow || isCurrent;

                if (showControl) {
                    boolean showPause = isCurrent && playerService.getState() == PlayerService.State.PLAYING;
                    setIcon(showPause ? trackPauseIcon : trackPlayIcon);
                    setText("");
                    setForeground(new Color(18, 18, 18));
                    setBackground(ACCENT);
                    setFont(new Font("Sans-Serif", Font.BOLD, 14));
                } else {
                    setIcon(null);
                    setText(value != null ? value.toString() : "");
                    setForeground(new Color(160, 160, 160));
                    setBackground(getRowBackground(row));
                    setFont(new Font("Sans-Serif", Font.PLAIN, 13));
                }
                setBorder(noFocusBorder);
                return this;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(numberRenderer);

        // duration column
        DefaultTableCellRenderer durationRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(getRowBackground(row));
                setForeground(new Color(160, 160, 160));
                setBorder(noFocusBorder);
                setFont(new Font("Sans-Serif", Font.PLAIN, 13));
                return this;
            }
        };
        table.getColumnModel().getColumn(5).setCellRenderer(durationRenderer);

        // cover icon renderer
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                JPanel container = new JPanel(new GridBagLayout());
                container.setBackground(getRowBackground(row));

                JLabel label = new JLabel();
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setVerticalAlignment(SwingConstants.CENTER);
                label.setPreferredSize(new Dimension(COVER_SIZE, COVER_SIZE));
                label.setOpaque(true);

                label.setBackground(getRowBackground(row));
                label.setText("");
                if (value instanceof ImageIcon icon) {
                    label.setIcon(icon);
                } else {
                    label.setIcon(null);
                }

                container.add(label);
                return container;
            }
        });

        // render plain text in other columns
        DefaultTableCellRenderer plainRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                setBackground(getRowBackground(row));
                setForeground(Color.WHITE);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        };
        table.getColumnModel().getColumn(2).setCellRenderer(plainRenderer); // title
        table.getColumnModel().getColumn(3).setCellRenderer(plainRenderer); // artist
        table.getColumnModel().getColumn(4).setCellRenderer(plainRenderer); // album

        // sorter
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // track number sort fix
        sorter.setComparator(0, (a, b) -> Integer.compare((Integer) a, (Integer) b));

        // no sort for cover icon column
        sorter.setSortable(1, false);

        // duration sort
        sorter.setComparator(5, (a, b) -> {
            String[] pa = a.toString().split(":");
            String[] pb = b.toString().split(":");
            int sa = Integer.parseInt(pa[0]) * 60 + Integer.parseInt(pa[1]);
            int sb = Integer.parseInt(pb[0]) * 60 + Integer.parseInt(pb[1]);
            return Integer.compare(sa, sb);
        });

        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            // render header with sort arrows
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                panel.setBackground(new Color(18, 18, 18));
                panel.setBorder(new EmptyBorder(0, 8, 0, 8));

                JLabel text = new JLabel(value != null ? value.toString() : "");
                text.setForeground(new Color(160, 160, 160));
                text.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
                panel.add(text);

                // sort arrow styled
                RowSorter<?> rs = t.getRowSorter();
                if (rs != null) {
                    for (RowSorter.SortKey key : rs.getSortKeys()) {
                        if (t.convertColumnIndexToModel(col) == key.getColumn()) {
                            JLabel arrow = new JLabel(key.getSortOrder() == SortOrder.ASCENDING ? "↑" : "↓");
                            arrow.setForeground(Color.WHITE);
                            arrow.setFont(new Font("Sans-Serif", Font.BOLD, 12));
                            panel.add(arrow);
                            break;
                        }
                    }
                }
                return panel;
            }
        });

    }

    private Color getRowBackground(int viewRow) {
        return viewRow == hoveredViewRow ? new Color(34, 34, 34) : new Color(18, 18, 18);
    }

    private void togglePlaybackAtRow(int modelRow) {
        if (currentTracks == null || modelRow < 0 || modelRow >= currentTracks.size()) {
            return;
        }

        Track clickedTrack = currentTracks.get(modelRow);
        Track currentTrack = playerService.getCurrentTrack();
        boolean sameTrack = currentTrack != null && currentTrack.getId() == clickedTrack.getId();

        if (!sameTrack) {
            playerService.setQueue(currentTracks, modelRow);
            return;
        }

        if (playerService.getState() == PlayerService.State.PLAYING) {
            playerService.pause();
        } else if (playerService.getState() == PlayerService.State.PAUSED) {
            playerService.play();
        } else {
            playerService.setQueue(currentTracks, modelRow);
        }
    }

    private boolean isCurrentTrackAtModelRow(int modelRow) {
        if (currentTracks == null || modelRow < 0 || modelRow >= currentTracks.size()) {
            return false;
        }
        Track current = playerService.getCurrentTrack();
        if (current == null) {
            return false;
        }
        return current.getId() == currentTracks.get(modelRow).getId();
    }

    // get cover icon for a track
    private ImageIcon getCoverIcon(Track track) {
        if (track.getCoverData() != null) {
            try {
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(track.getCoverData());
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(bis);
                if (img != null) {
                    Image scaled = img.getScaledInstance(COVER_SIZE, COVER_SIZE, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }
            } catch (Exception ignored) {}
        }

        java.awt.image.BufferedImage placeholder = new java.awt.image.BufferedImage(COVER_SIZE, COVER_SIZE, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = placeholder.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(50, 50, 50));
        graphics.fillRect(0, 0, COVER_SIZE, COVER_SIZE);

        Icon noteIcon = IconLoader.svg("music-note.svg", 20, new Color(100, 100, 100));
        if (noteIcon != null) {
            int x = (COVER_SIZE - noteIcon.getIconWidth()) / 2;
            int y = (COVER_SIZE - noteIcon.getIconHeight()) / 2;
            noteIcon.paintIcon(null, graphics, x, y);
        }

        graphics.dispose();
        return new ImageIcon(placeholder);
    }

    // format duration in mm:ss format
    private String formatDuration(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    // search tracks in the library
    public void search(String query) {
        List<Track> base = activePlaylistId == null
                ? libraryService.getAllTracks()
                : playlistDAO.getTracksForPlaylist(activePlaylistId);

        List<Track> results;
        if (query.isEmpty()) {
            results = base;
        } else {
            String q = query.toLowerCase();
            results = base.stream()
                    .filter(track -> containsIgnoreCase(track.getTitle(), q)
                            || containsIgnoreCase(track.getArtist(), q)
                            || containsIgnoreCase(track.getAlbum(), q))
                    .toList();
        }
        loadTracks(results);
    }

    public void showPlaylistTracks(Playlist playlist) {
        if (playlist == null) {
            return;
        }
        activePlaylistId = playlist.getId();
        loadTracks(playlistDAO.getTracksForPlaylist(playlist.getId()));
    }

    public void showAllTracks() {
        activePlaylistId = null;
        loadTracks(libraryService.getAllTracks());
    }

    private boolean containsIgnoreCase(String value, String queryLowercase) {
        return value != null && value.toLowerCase().contains(queryLowercase);
    }

    // context menu for the library view
    private void showTrackContextMenu(JTable table, java.awt.event.MouseEvent e) {
        int viewRow = table.rowAtPoint(e.getPoint());
        if (viewRow < 0 || currentTracks == null) return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        Track track = currentTracks.get(modelRow);

        JPopupMenu menu = new JPopupMenu();
        stylePopupMenu(menu);

        if (activePlaylistId == null) {
            // in full library view allow adding only to playlists that do not already contain this track
            List<Playlist> playlists = playlistDAO.getPlaylistsByUser(currentUser.getId());
            List<Playlist> availablePlaylists = playlists.stream()
                    .filter(playlist -> !playlistContainsTrack(playlist, track.getId()))
                    .toList();

            if (!availablePlaylists.isEmpty()) {
                JMenu addToMenu = new JMenu("Add to playlist");
                styleContextMenu(addToMenu);
                stylePopupMenu(addToMenu.getPopupMenu());

                for (Playlist playlist : availablePlaylists) {
                    JMenuItem item = new JMenuItem(playlist.getName());
                    styleContextItem(item);
                    item.addActionListener(ev -> {
                        playlistDAO.addTrackToPlaylist(playlist.getId(), track.getId());
                        showToast("Track \"" + track.getTitle() + "\" added to \"" + playlist.getName() + "\"");
                    });
                    addToMenu.add(item);
                }
                menu.add(addToMenu);
            }

            if (menu.getComponentCount() > 0) {
                menu.addSeparator();
            }
            JMenuItem deleteItem = new JMenuItem("Delete track");
            styleContextItem(deleteItem);
            deleteItem.setForeground(new Color(230, 120, 120));
            deleteItem.addActionListener(ev -> deleteTrack(track));
            menu.add(deleteItem);
        } else {
            // inside playlist view: only remove from current playlist
            JMenuItem removeItem = new JMenuItem("Remove from this playlist");
            styleContextItem(removeItem);
            removeItem.setForeground(new Color(230, 120, 120));
            removeItem.addActionListener(ev -> {
                playlistDAO.removeTrackFromPlaylist(activePlaylistId, track.getId());
                refreshCurrentView();
                showToast("Track removed from playlist");
            });
            menu.add(removeItem);
        }

        if (menu.getComponentCount() == 0) {
            return;
        }

        menu.show(table, e.getX(), e.getY());
    }

    private boolean playlistContainsTrack(Playlist playlist, int trackId) {
        if (playlist == null || playlist.getTracks() == null) {
            return false;
        }
        for (Track existing : playlist.getTracks()) {
            if (existing != null && existing.getId() == trackId) {
                return true;
            }
        }
        return false;
    }

    // allows to delete any track from the library
    private void deleteTrack(Track track) {
        if (track == null || track.getId() <= 0) {
            return;
        }

        String title = track.getTitle() == null || track.getTitle().isBlank() ? "Unknown title" : track.getTitle();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete track \"" + title + "\" from library?\nThis will remove it from database and playlists.",
                "Delete track",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        Track current = playerService.getCurrentTrack();
        if (current != null && current.getId() == track.getId()) {
            playerService.stop();
        }

        boolean deleted = libraryService.deleteTrackById(track.getId());
        if (!deleted) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to delete track from database.",
                    "Delete track",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        refreshCurrentView();
        showToast("Track \"" + title + "\" deleted");
    }

    private void refreshCurrentView() {
        if (activePlaylistId == null) {
            List<Track> all = libraryService.getAllTracks();
            if (all.isEmpty()) {
                currentTracks = new ArrayList<>();
                tableModel.setRowCount(0);

                Component libraryView = cards.getComponent(1);
                if (libraryView instanceof JPanel lv) {
                    JLabel countLabel = (JLabel) lv.getClientProperty("countLabel");
                    if (countLabel != null) {
                        countLabel.setText("0 tracks");
                    }
                }

                cardLayout.show(cards, EMPTY_CARD);
            } else {
                loadTracks(all);
            }
        } else {
            loadTracks(playlistDAO.getTracksForPlaylist(activePlaylistId));
        }
    }

    private void styleContextItem(JMenuItem item) {
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

        item.setUI(new BasicMenuItemUI() {
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
                graphics2d.setColor(menuItem.getForeground());
                FontMetrics fm = graphics2d.getFontMetrics();
                graphics2d.drawString(text, textRect.x, textRect.y + fm.getAscent());
                graphics2d.dispose();
            }
        });
    }

    private void stylePopupMenu(JPopupMenu menu) {
        menu.setBackground(new Color(28, 28, 28));
        menu.setOpaque(true);
        menu.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        menu.setBorderPainted(false);
    }

    // message when track is added to playlist
    private void showToast(String message) {
        if (toastTimer != null && toastTimer.isRunning()) {
            toastTimer.stop();
        }

        toastContainer.removeAll();

        JPanel bubble = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        bubble.setBackground(new Color(28, 28, 28, 235));
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                new EmptyBorder(2, 10, 2, 10)
        ));

        JLabel text = new JLabel(message);
        text.setForeground(Color.WHITE);
        text.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
        bubble.add(text);

        toastContainer.add(bubble);
        toastContainer.setVisible(true);
        toastContainer.revalidate();
        toastContainer.repaint();

        toastTimer = new Timer(2200, event -> {
            toastContainer.setVisible(false);
            toastContainer.removeAll();
            toastContainer.revalidate();
            toastContainer.repaint();
        });
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    private void styleContextMenu(JMenu menu) {
        Color baseBg = new Color(28, 28, 28);
        Color hoverBg = new Color(40, 40, 40);

        menu.setOpaque(true);
        menu.setBackground(baseBg);
        menu.setForeground(Color.WHITE);
        menu.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        menu.setBorder(new EmptyBorder(8, 14, 8, 14));
        menu.setBorderPainted(false);
        menu.setContentAreaFilled(false);

        menu.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                menu.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                menu.setBackground(baseBg);
            }
        });
    }
}

