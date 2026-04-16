package com.javify.ui;

import javax.swing.*;
import java.awt.*;

// class for rounded buttons
public class RoundedButton extends JButton {

    private int cornerRadius = 14;
    private Color hoverBackground;
    private Color pressedBackground;

    public RoundedButton() {
        this("");
    }

    public RoundedButton(String text) {
        super(text);
        initUi();
    }

    private void initUi() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = Math.max(0, cornerRadius);
        repaint();
    }

    public void setHoverBackground(Color hoverBackground) {
        this.hoverBackground = hoverBackground;
        repaint();
    }

    public void setPressedBackground(Color pressedBackground) {
        this.pressedBackground = pressedBackground;
        repaint();
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (bg != null) {
            if (hoverBackground == null) {
                hoverBackground = shift(bg, 16);
            }
            if (pressedBackground == null) {
                pressedBackground = shift(bg, -10);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color fill = getBackground();
        ButtonModel model = getModel();
        if (model.isPressed() && pressedBackground != null) {
            fill = pressedBackground;
        } else if (model.isRollover() && hoverBackground != null) {
            fill = hoverBackground;
        }

        if (fill != null) {
            graphics.setColor(fill);
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        }

        graphics.dispose();
        super.paintComponent(g);
    }

    private Color shift(Color color, int delta) {
        int r = Math.max(0, Math.min(255, color.getRed() + delta));
        int g = Math.max(0, Math.min(255, color.getGreen() + delta));
        int b = Math.max(0, Math.min(255, color.getBlue() + delta));
        return new Color(r, g, b, color.getAlpha());
    }
}

