package com.javify.ui;

import com.javify.objects.Track;
import com.javify.services.PlayerService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

public class PlayerBar extends JPanel {

    private final PlayerService playerService;

    private JLabel coverLabel;
    private JLabel titleLabel;
    private JLabel artistLabel;
    private JButton playPauseBtn;
    private JButton prevBtn;
    private JButton nextBtn;
    private JButton shuffleBtn;
    private JSlider progressSlider;
    private JLabel currentTimeLabel;
    private JLabel totalTimeLabel;
    private JSlider volumeSlider;
    private JLabel volumeIconLabel;

    private static final Color ICON_MUTED = new Color(160, 160, 160);
    private static final Color ICON_ACTIVE = Color.WHITE;
    private static final Color ACCENT = new Color(185, 99, 6);

    private boolean seeking = false;

    public PlayerBar(PlayerService playerService) {
        this.playerService = playerService;
        initUi();
        bindPlayerCallbacks();
    }

    private void initUi() {
        setLayout(new BorderLayout());
        setBackground(new Color(10, 10, 10));
        setPreferredSize(new Dimension(0, 90));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 40, 40)));

        add(createLeftSection(), BorderLayout.WEST);
        add(createCenterSection(), BorderLayout.CENTER);
        add(createRightSection(), BorderLayout.EAST);
    }

    // left side: covers, title, artist
    private JPanel createLeftSection() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(new Color(10, 10, 10));
        panel.setPreferredSize(new Dimension(280, 90));
        panel.setBorder(new EmptyBorder(0, 16, 0, 0));

        // cover image
        JPanel coverWrapper = new JPanel(new GridBagLayout());
        coverWrapper.setBackground(new Color(10, 10, 10));
        coverLabel = new JLabel(emptycover());
        coverWrapper.add(coverLabel);
        panel.add(coverWrapper, BorderLayout.WEST);

        // centered title and artist
        JPanel info = new JPanel(new GridBagLayout());
        info.setBackground(new Color(10, 10, 10));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setBackground(new Color(10, 10, 10));

        titleLabel = new JLabel("—");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Sans-Serif", Font.BOLD, 13));

        artistLabel = new JLabel("—") {
            @Override
            // tooltip with full artist name
            public JToolTip createToolTip() {
                JToolTip tooltip = super.createToolTip();
                tooltip.setBackground(new Color(18, 18, 18));
                tooltip.setForeground(Color.WHITE);
                tooltip.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
                tooltip.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
                return tooltip;
            }
        };
        artistLabel.setForeground(new Color(160, 160, 160));
        artistLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 12));

        textStack.add(titleLabel);
        textStack.add(Box.createVerticalStrut(4));
        textStack.add(artistLabel);

        info.add(textStack);
        panel.add(info, BorderLayout.CENTER);

        return panel;
    }

    // center: controls + progress bar
    private JPanel createCenterSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(10, 10, 10));

        // controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        controls.setBackground(new Color(10, 10, 10));

        shuffleBtn = controlBtn("shuffle.svg", "⇄", 18, ICON_MUTED);
        prevBtn = controlBtn("rewind.svg", "⏮", 18, ICON_MUTED);
        playPauseBtn = controlBtn("play.svg", "▶", 20, ICON_ACTIVE);
        nextBtn = controlBtn("fast-forward.svg", "⏭", 18, ICON_MUTED);

        shuffleBtn.addActionListener(e -> {
            playerService.toggleShuffle();
            updateShuffleIcon();
        });
        prevBtn.addActionListener(e -> playerService.previous());
        playPauseBtn.addActionListener(e -> playerService.togglePlayPause());
        nextBtn.addActionListener(e -> playerService.next());

        controls.add(shuffleBtn);
        controls.add(prevBtn);
        controls.add(playPauseBtn);
        controls.add(nextBtn);

        updateShuffleIcon();

        panel.add(controls, BorderLayout.NORTH);

        // progress bar
        JPanel progressRow = new JPanel(new BorderLayout(8, 0));
        progressRow.setBackground(new Color(10, 10, 10));
        progressRow.setBorder(new EmptyBorder(0, 16, 12, 16));

        currentTimeLabel = new JLabel("0:00");
        currentTimeLabel.setForeground(new Color(160, 160, 160));
        currentTimeLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 11));

        totalTimeLabel = new JLabel("0:00");
        totalTimeLabel.setForeground(new Color(160, 160, 160));
        totalTimeLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 11));

        progressSlider = createSlider(0, 1000, 0);
        progressSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                seeking = true;
            }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) {
                if (playerService.getDuration() > 0) {
                    long pos = (long) (progressSlider.getValue() / 1000.0 * playerService.getDuration());
                    playerService.seekTo(pos);
                }
                seeking = false;
            }
        });

        progressRow.add(currentTimeLabel, BorderLayout.WEST);
        progressRow.add(progressSlider, BorderLayout.CENTER);
        progressRow.add(totalTimeLabel, BorderLayout.EAST);

        panel.add(progressRow, BorderLayout.SOUTH);
        return panel;
    }

    // volume slider
    private JPanel createRightSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(10, 10, 10));
        panel.setPreferredSize(new Dimension(200, 90));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        row.setOpaque(false);

        volumeIconLabel = new JLabel();
        volumeIconLabel.setPreferredSize(new Dimension(18, 18));

        volumeSlider = createSlider(0, 100, Math.round(playerService.getVolume() * 100));
        volumeSlider.setPreferredSize(new Dimension(100, 20));
        volumeSlider.addChangeListener(e -> {
            playerService.setVolume(volumeSlider.getValue() / 100f);
            updateVolumeIcon();
        });

        row.add(volumeIconLabel);
        row.add(volumeSlider);
        panel.add(row);

        updateVolumeIcon();

        return panel;
    }

    // callbacks from player service to update UI
    private void bindPlayerCallbacks() {
        playerService.setOnTrackChanged(track -> SwingUtilities.invokeLater(() -> {
            titleLabel.setText(track.getTitle());
            artistLabel.setText(track.getArtist() != null ? track.getArtist() : "Unknown artist");
            artistLabel.setToolTipText(artistLabel.getText());

            if (track.getCoverData() != null) {
                try {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(track.getCoverData()));
                    if (img != null) {
                        Image scaled = img.getScaledInstance(56, 56, Image.SCALE_SMOOTH);
                        coverLabel.setIcon(new ImageIcon(scaled));
                    } else {
                        coverLabel.setIcon(emptycover());
                    }
                } catch (Exception e) {
                    coverLabel.setIcon(emptycover());
                }
            } else {
                coverLabel.setIcon(emptycover());
            }

            titleLabel.revalidate();
            titleLabel.repaint();
            artistLabel.revalidate();
            artistLabel.repaint();
            coverLabel.revalidate();
            coverLabel.repaint();
        }));

        playerService.setOnStateChanged(state -> SwingUtilities.invokeLater(() -> {
            if (state == PlayerService.State.PLAYING) {
                setButtonIcon(playPauseBtn, "pause.svg", "⏸", 20, ICON_ACTIVE);
            } else {
                setButtonIcon(playPauseBtn, "play.svg", "▶", 20, ICON_ACTIVE);
            }
            updateShuffleIcon();
            playPauseBtn.revalidate();
            playPauseBtn.repaint();
        }));

        playerService.setOnProgress((pos, dur) -> SwingUtilities.invokeLater(() -> {
            if (!seeking && dur > 0) {
                progressSlider.setValue((int) (pos * 1000 / dur));
                currentTimeLabel.setText(formatTime(pos / 1_000_000));
                totalTimeLabel.setText(formatTime(dur / 1_000_000));
            }
        }));
    }

    private void updateTrackInfo(Track track) {
        titleLabel.setText(track.getTitle());
        artistLabel.setText(track.getArtist() != null ? track.getArtist() : "Unknown artist");
        artistLabel.setToolTipText(artistLabel.getText());

        // cover image
        if (track.getCoverData() != null) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(track.getCoverData()));
                if (img != null) {
                    Image scaled = img.getScaledInstance(56, 56, Image.SCALE_SMOOTH);
                    coverLabel.setIcon(new ImageIcon(scaled));
                    return;
                }
            } catch (Exception ignored) {}
        }
        coverLabel.setIcon(emptycover());
    }

    // helper methods
    private JButton controlBtn(String iconName, String fallbackText, int iconSize, Color iconColor) {
        JButton btn = new RoundedButton();
        Dimension fixedControlSize = new Dimension(40, 32);
        btn.setFont(new Font("Sans-Serif", Font.PLAIN, 16));
        btn.setBackground(new Color(10, 10, 10));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setPreferredSize(fixedControlSize);
        btn.setMinimumSize(fixedControlSize);
        btn.setMaximumSize(fixedControlSize);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (btn instanceof RoundedButton rounded) {
            rounded.setCornerRadius(12);
            rounded.setHoverBackground(new Color(28, 28, 28));
            rounded.setPressedBackground(new Color(36, 36, 36));
        }
        setButtonIcon(btn, iconName, fallbackText, iconSize, iconColor);
        return btn;
    }

    private void updateShuffleIcon() {
        Color color = playerService.isShuffle() ? ACCENT : ICON_MUTED;
        setButtonIcon(shuffleBtn, "shuffle.svg", "⇄", 18, color);
    }

    private void updateVolumeIcon() {
        int value = volumeSlider != null ? volumeSlider.getValue() : Math.round(playerService.getVolume() * 100);
        String iconName;
        if (value <= 0) {
            iconName = "speaker-none.svg";
        } else if (value < 50) {
            iconName = "speaker-low.svg";
        } else {
            iconName = "speaker-high.svg";
        }
        Icon icon = IconLoader.svg(iconName, 18, ICON_MUTED);
        volumeIconLabel.setIcon(icon);
        volumeIconLabel.setText(icon == null ? "🔊" : "");
        volumeIconLabel.setForeground(ICON_MUTED);
    }

    private void setButtonIcon(JButton button, String iconName, String fallbackText, int size, Color color) {
        Icon icon = IconLoader.svg(iconName, size, color);
        button.setIcon(icon);
        button.setText(icon == null ? fallbackText : "");
        button.setForeground(color);
    }

    // slider with custom UI
    private JSlider createSlider(int min, int max, int val) {
        JSlider slider = new JSlider(min, max, val);
        slider.setBackground(new Color(10, 10, 10));
        slider.setFocusable(false);
        slider.setUI(new javax.swing.plaf.basic.BasicSliderUI(slider) {
            @Override
            public void setThumbLocation(int x, int y) {
                super.setThumbLocation(x, y);
                // Repaint entire track to avoid stale painted fragments while dragging.
                slider.repaint(trackRect.x, trackRect.y, trackRect.width, trackRect.height);
            }

            @Override public void paintTrack(Graphics g) {
                Graphics2D graphics2D = (Graphics2D) g;
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Rectangle t = trackRect;
                int cy = t.y + t.height / 2;
                // background
                graphics2D.setColor(new Color(60, 60, 60));
                graphics2D.fillRoundRect(t.x, cy - 2, t.width, 4, 4, 4);
                // filled part
                int filled = thumbRect.x + thumbRect.width / 2 - t.x;
                graphics2D.setColor(new Color(185, 99, 6));
                graphics2D.fillRoundRect(t.x, cy - 2, filled, 4, 4, 4);
            }
            @Override public void paintThumb(Graphics g) {
                Graphics2D graphics = (Graphics2D) g;
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = thumbRect.x + thumbRect.width / 2;
                int cy = thumbRect.y + thumbRect.height / 2;
                graphics.setColor(Color.WHITE);
                graphics.fillOval(cx - 6, cy - 6, 12, 12);
            }
        });
        return slider;
    }

    // empty cover icon
    private ImageIcon emptycover() {
        BufferedImage img = new BufferedImage(56, 56, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = img.createGraphics();
        graphics.setColor(new Color(40, 40, 40));
        graphics.fillRect(0, 0, 56, 56);
        graphics.setColor(new Color(100, 100, 100));
        graphics.setFont(new Font("Sans-Serif", Font.PLAIN, 24));
        FontMetrics fm = graphics.getFontMetrics();
        graphics.drawString("♪", (56 - fm.stringWidth("♪")) / 2, 56/2 + fm.getAscent()/2 - 2);
        graphics.dispose();
        return new ImageIcon(img);
    }

    // format time in mm:ss format
    private String formatTime(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}