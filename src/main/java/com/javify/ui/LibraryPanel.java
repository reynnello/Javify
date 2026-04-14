package com.javify.ui;

import com.javify.objects.Track;
import com.javify.objects.User;
import com.javify.services.LibraryService;
import com.javify.services.PlayerService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.List;
import javax.swing.table.TableRowSorter;

public class LibraryPanel extends JPanel {

    private final User currentUser;
    private final LibraryService libraryService;
    private final PlayerService playerService;
    private final CardLayout cardLayout;
    private final JPanel cards;

    private DefaultTableModel tableModel;
    private List<Track> currentTracks;

    private static final String EMPTY_CARD = "empty";
    private static final String LIBRARY_CARD = "library";

    public LibraryPanel(User currentUser, String dbUrl) {
        this.currentUser = currentUser;
        this.libraryService = new LibraryService();
        this.playerService = new PlayerService();
        this.cardLayout = new CardLayout();
        this.cards = new JPanel(cardLayout);

        setLayout(new BorderLayout());
        setBackground(new Color(18, 18, 18));

        cards.add(createEmptyState(), EMPTY_CARD);
        cards.add(createLibraryView(), LIBRARY_CARD);
        add(cards, BorderLayout.CENTER);

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

        JLabel icon = new JLabel("🎵");
        icon.setFont(new Font("Sans-Serif", Font.PLAIN, 48));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(icon);

        inner.add(Box.createVerticalStrut(16));

        JLabel title = new JLabel("No music folder selected");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Sans-Serif", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(title);

        inner.add(Box.createVerticalStrut(8));

        JLabel subtitle = new JLabel("Choose a folder with your music files");
        subtitle.setForeground(new Color(160, 160, 160));
        subtitle.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(subtitle);

        inner.add(Box.createVerticalStrut(24));

        JButton chooseBtn = new JButton("Choose folder");
        chooseBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        chooseBtn.setBackground(new Color(185, 99, 6));
        chooseBtn.setForeground(Color.WHITE);
        chooseBtn.setFont(new Font("Sans-Serif", Font.BOLD, 13));
        chooseBtn.setBorder(new EmptyBorder(12, 32, 12, 32));
        chooseBtn.setFocusPainted(false);
        chooseBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chooseBtn.addActionListener(e -> openFolderChooser());
        inner.add(chooseBtn);

        panel.add(inner);
        return panel;
    }

    // основной вид с таблицей треков
    private JPanel createLibraryView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(18, 18, 18));

        // шапка с количеством треков и кнопкой сменить папку
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(18, 18, 18));
        header.setBorder(new EmptyBorder(16, 24, 8, 24));

        JLabel countLabel = new JLabel();
        countLabel.setForeground(new Color(160, 160, 160));
        countLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        header.add(countLabel, BorderLayout.WEST);

        // button to change music folder
        JButton changeFolderBtn = new JButton("Change folder");
        changeFolderBtn.setBackground(new Color(40, 40, 40));
        changeFolderBtn.setForeground(new Color(200, 200, 200));
        changeFolderBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        changeFolderBtn.setFocusPainted(false);
        changeFolderBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        changeFolderBtn.addActionListener(e -> openFolderChooser());
        header.add(changeFolderBtn, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);

        // table with tracks
        String[] columns = {"", "#", "Title", "Artist", "Album", "Duration"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        styleTable(table);

        // double-click on table row to play track
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.convertRowIndexToModel(table.getSelectedRow());
                    if (row >= 0 && currentTracks != null) {
                        playerService.setQueue(currentTracks, row);
                    }
                }
            }
        });

        // scrollable table
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 40, 40)));
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

    // system folder chooser
    private void openFolderChooser() {

        // os specific L&F for test
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select music folder");

        int result = chooser.showOpenDialog(this);

        // return to default L&F
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        if (result == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            List<Track> tracks = libraryService.scanFolder(folder);
            if (tracks.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No MP3, WAV or FLAC files found in this folder.",
                        "Empty folder", JOptionPane.WARNING_MESSAGE);
            } else {
                loadTracks(libraryService.getAllTracks());
            }
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
                    cover,
                    i + 1,
                    t.getTitle(),
                    t.getArtist() != null ? t.getArtist() : "—",
                    t.getAlbum() != null ? t.getAlbum() : "—",
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
        table.setRowHeight(44);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(true);
        table.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(18, 18, 18));
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setFocusable(false); // focus disabled

        // header
        table.getTableHeader().setBackground(new Color(18, 18, 18));
        table.getTableHeader().setForeground(new Color(160, 160, 160));
        table.getTableHeader().setFont(new Font("Sans-Serif", Font.PLAIN, 12));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 50, 50)));
        table.getTableHeader().setReorderingAllowed(false);

        // column widths
        table.getColumnModel().getColumn(0).setMaxWidth(44);
        table.getColumnModel().getColumn(0).setMinWidth(44);
        table.getColumnModel().getColumn(1).setMaxWidth(40);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setMaxWidth(70);

        // renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(new Color(18, 18, 18));
                setForeground(new Color(160, 160, 160));
                setBorder(noFocusBorder);
                return this;
            }
        };
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        // cover icon renderer
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                JLabel label = new JLabel();
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBackground(new Color(18, 18, 18));
                label.setOpaque(true);
                label.setBorder(null);
                if (value instanceof ImageIcon icon) label.setIcon(icon);
                return label;
            }
        });

        // render plain text in other columns
        DefaultTableCellRenderer plainRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                setBackground(new Color(18, 18, 18));
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
        sorter.setComparator(1, (a, b) -> Integer.compare((Integer) a, (Integer) b));

        // no sort for cover icon column
        sorter.setSortable(0, false);

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
                panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 50, 50)));

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

    // get cover icon for a track
    private ImageIcon getCoverIcon(Track track) {
        if (track.getCoverData() != null) {
            try {
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(track.getCoverData());
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(bis);
                if (img != null) {
                    Image scaled = img.getScaledInstance(36, 36, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }
            } catch (Exception ignored) {}
        }
        // placeholder icon if cover is not available
        java.awt.image.BufferedImage placeholder = new java.awt.image.BufferedImage(36, 36, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = placeholder.createGraphics();
        graphics.setColor(new Color(50, 50, 50));
        graphics.fillRect(0, 0, 36, 36);
        graphics.setColor(new Color(100, 100, 100));
        graphics.setFont(new Font("Sans-Serif", Font.PLAIN, 18));
        FontMetrics metrics = graphics.getFontMetrics();
        int x = (36 - metrics.stringWidth("♪")) / 2;
        int y = (36 - metrics.getHeight()) / 2 + metrics.getAscent();
        graphics.drawString("♪", x, y);
        graphics.dispose();
        return new ImageIcon(placeholder);
    }

    // format duration in mm:ss format
    private String formatDuration(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}