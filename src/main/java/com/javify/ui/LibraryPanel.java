package com.javify.ui;

import com.javify.objects.Track;
import com.javify.objects.User;
import com.javify.services.LibraryService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.List;

public class LibraryPanel extends JPanel {

    private final User currentUser;
    private final LibraryService libraryService;
    private final CardLayout cardLayout;
    private final JPanel cards;

    private DefaultTableModel tableModel;
    private List<Track> currentTracks;

    private static final String EMPTY_CARD = "empty";
    private static final String LIBRARY_CARD = "library";

    public LibraryPanel(User currentUser, String dbUrl) {
        this.currentUser = currentUser;
        this.libraryService = new LibraryService();
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
        String[] columns = {"#", "Title", "Artist", "Album", "Duration"};
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
                    int row = table.getSelectedRow();
                    if (row >= 0 && currentTracks != null) {
                        Track track = currentTracks.get(row);
                        System.out.println("Play: " + track.getTitle()); // todo: fix when player is ready
                    }
                }
            }
        });

        // scrollable table
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.setBackground(new Color(18, 18, 18));
        scrollPane.getViewport().setBackground(new Color(18, 18, 18));
        panel.add(scrollPane, BorderLayout.CENTER);

        // counter label in the header
        panel.putClientProperty("countLabel", countLabel);

        return panel;
    }

    // system folder chooser
    private void openFolderChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select music folder");

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            List<Track> tracks = libraryService.scanFolder(folder);
            // if no tracks found, show a message
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
            tableModel.addRow(new Object[]{
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
        table.setShowVerticalLines(false);
        table.setFont(new Font("Sans-Serif", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(40, 40, 40));
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);

        // header
        table.getTableHeader().setBackground(new Color(18, 18, 18));
        table.getTableHeader().setForeground(new Color(160, 160, 160));
        table.getTableHeader().setFont(new Font("Sans-Serif", Font.PLAIN, 12));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 50, 50)));

        // wide columns
        table.getColumnModel().getColumn(0).setMaxWidth(40);   // #
        table.getColumnModel().getColumn(4).setMaxWidth(70);   // duration

        // center align columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centerRenderer.setBackground(new Color(18, 18, 18));
        centerRenderer.setForeground(new Color(160, 160, 160));
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
    }

    // format duration in mm:ss format
    private String formatDuration(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}