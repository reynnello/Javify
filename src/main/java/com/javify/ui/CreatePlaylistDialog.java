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
import java.io.ByteArrayOutputStream;
import java.io.File;

// dialog for creating new playlist
public class CreatePlaylistDialog extends JDialog {

    private final PlaylistDAO playlistDAO;
    private final User currentUser;
    private final Runnable onCreated;
    private final Playlist editingPlaylist;

    private JTextField nameField;
    private JLabel coverPreview;
    private byte[] selectedCoverData = null;

    public CreatePlaylistDialog(JFrame parent, User currentUser, String dbUrl, Runnable onCreated) {
        this(parent, currentUser, dbUrl, null, onCreated);
    }

    public CreatePlaylistDialog(JFrame parent, User currentUser, String dbUrl, Playlist editingPlaylist, Runnable onCreated) {
        super(parent, editingPlaylist == null ? "New Playlist" : "Change Playlist", true);
        this.currentUser = currentUser;
        this.playlistDAO = new PlaylistDAO();
        this.onCreated = onCreated;
        this.editingPlaylist = editingPlaylist;
        this.selectedCoverData = editingPlaylist != null ? editingPlaylist.getCoverData() : null;
        initUi();
    }

    private void initUi() {
        setSize(400, 420);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(new Color(28, 28, 28));
        setLayout(new BorderLayout());

        String titleText = editingPlaylist == null ? "New Playlist" : "Change Playlist";
        String actionText = editingPlaylist == null ? "Create" : "Save";

        // title
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 16));
        header.setBackground(new Color(28, 28, 28));
        JLabel title = new JLabel(titleText);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Sans-Serif", Font.BOLD, 18));
        header.add(title);
        add(header, BorderLayout.NORTH);

        // content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(28, 28, 28));
        content.setBorder(new EmptyBorder(12, 24, 20, 24));

        // cover section
        JPanel coverSection = new JPanel();
        coverSection.setLayout(new BoxLayout(coverSection, BoxLayout.Y_AXIS));
        coverSection.setOpaque(false);
        coverSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        coverSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        // cover preview
        coverPreview = new JLabel(generateDefaultCover());
        if (selectedCoverData != null) {
            ImageIcon preloaded = coverIconFromBytes(selectedCoverData, 100);
            if (preloaded != null) {
                coverPreview.setIcon(preloaded);
            }
        }
        coverPreview.setAlignmentX(Component.CENTER_ALIGNMENT);
        coverPreview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        coverPreview.setToolTipText("Click to choose cover image");
        coverPreview.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooseCover();
            }
        });
        coverSection.add(coverPreview);

        coverSection.add(Box.createVerticalStrut(12));

        // cover change button
        RoundedButton changeCoverBtn = new RoundedButton("Choose cover");
        changeCoverBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        changeCoverBtn.setBackground(new Color(40, 40, 40));
        changeCoverBtn.setHoverBackground(new Color(54, 54, 54));
        changeCoverBtn.setPressedBackground(new Color(32, 32, 32));
        changeCoverBtn.setForeground(new Color(220, 220, 220));
        changeCoverBtn.setFont(new Font("Sans-Serif", Font.BOLD, 12));
        changeCoverBtn.setBorder(new EmptyBorder(8, 16, 8, 16));
        changeCoverBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        changeCoverBtn.setCornerRadius(12);
        changeCoverBtn.addActionListener(e -> chooseCover());
        coverSection.add(changeCoverBtn);

        content.add(coverSection);
        content.add(Box.createVerticalStrut(20));

        // form section
        JPanel formSection = new JPanel();
        formSection.setLayout(new BoxLayout(formSection, BoxLayout.Y_AXIS));
        formSection.setOpaque(false);
        formSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        formSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // title field label
        JLabel nameLabel = new JLabel("Playlist name");
        nameLabel.setForeground(new Color(160, 160, 160));
        nameLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formSection.add(nameLabel);

        formSection.add(Box.createVerticalStrut(8));

        // name field
        nameField = new JTextField(editingPlaylist != null ? editingPlaylist.getName() : "");
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        nameField.setPreferredSize(new Dimension(280, 40));
        nameField.setBackground(new Color(50, 50, 50));
        nameField.setForeground(Color.WHITE);
        nameField.setCaretColor(Color.WHITE);
        nameField.setFont(new Font("Sans-Serif", Font.PLAIN, 14));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                new EmptyBorder(8, 12, 8, 12)
        ));
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        formSection.add(nameField);

        content.add(formSection);

        content.add(Box.createVerticalGlue());
        add(content, BorderLayout.CENTER);

        // buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttons.setBackground(new Color(28, 28, 28));
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 50, 50)));

        RoundedButton cancelBtn = new RoundedButton("Cancel");
        cancelBtn.setBackground(new Color(40, 40, 40));
        cancelBtn.setHoverBackground(new Color(56, 56, 56));
        cancelBtn.setPressedBackground(new Color(32, 32, 32));
        cancelBtn.setForeground(new Color(220, 220, 220));
        cancelBtn.setFont(new Font("Sans-Serif", Font.BOLD, 13));
        cancelBtn.setBorder(new EmptyBorder(8, 20, 8, 20));
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.setCornerRadius(12);
        cancelBtn.addActionListener(e -> dispose());

        RoundedButton createBtn = new RoundedButton(actionText);
        createBtn.setBackground(new Color(185, 99, 6));
        createBtn.setHoverBackground(new Color(206, 115, 18));
        createBtn.setPressedBackground(new Color(166, 88, 6));
        createBtn.setForeground(Color.WHITE);
        createBtn.setFont(new Font("Sans-Serif", Font.BOLD, 13));
        createBtn.setBorder(new EmptyBorder(8, 20, 8, 20));
        createBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createBtn.setCornerRadius(12);
        createBtn.addActionListener(e -> handleCreate());

        // Enter = Save/Create
        getRootPane().setDefaultButton(createBtn);

        buttons.add(cancelBtn);
        buttons.add(createBtn);
        add(buttons, BorderLayout.SOUTH);

        nameField.requestFocusInWindow();
    }

    private void handleCreate() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            nameField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 80, 80)),
                    new EmptyBorder(6, 12, 6, 12)
            ));
            nameField.requestFocus();
            return;
        }

        if (editingPlaylist == null) {
            playlistDAO.createPlaylist(name, currentUser.getId(), selectedCoverData);
        } else {
            playlistDAO.updatePlaylist(editingPlaylist.getId(), name, selectedCoverData);
        }

        if (onCreated != null) {
            onCreated.run();
        }
        dispose();
    }

    private void chooseCover() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose cover image");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Images (*.jpg, *.jpeg, *.png)", "jpg", "jpeg", "png"
        ));

        int result = chooser.showOpenDialog(this);
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    // save selected cover data
                    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                    ImageIO.write(img, "png", byteOutput);
                    selectedCoverData = byteOutput.toByteArray();
                    // preview cover
                    Image scaled = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                    coverPreview.setIcon(new ImageIcon(scaled));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ImageIcon coverIconFromBytes(byte[] data, int size) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) {
                return null;
            }
            return new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_SMOOTH));
        } catch (Exception ignored) {
            return null;
        }
    }

    // default cover image
    private ImageIcon generateDefaultCover() {
        int size = 100;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = img.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(50, 50, 50));
        graphics.fillRoundRect(0, 0, size, size, 12, 12);

        Icon noteIcon = IconLoader.svg("music-note.svg", 40, new Color(100, 100, 100));
        if (noteIcon != null) {
            int x = (size - noteIcon.getIconWidth()) / 2;
            int y = (size - noteIcon.getIconHeight()) / 2;
            noteIcon.paintIcon(null, graphics, x, y);
        }

        graphics.dispose();
        return new ImageIcon(img);
    }
}
