package com.javify.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

final class IconLoader {

    private IconLoader() {
    }

    static Icon svg(String name, int size, Color color) {
        return svg(name, size, size, color);
    }

    static Icon svg(String name, int width, int height, Color color) {
        String resource = name.startsWith("icons/") ? name : "icons/" + name;
        try {
            FlatSVGIcon icon = new FlatSVGIcon(resource, width, height);
            if (color != null) {
                icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> isNearBlack(c) ? color : c));
            }
            return icon;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean isNearBlack(Color color) {
        return color.getRed() < 40 && color.getGreen() < 40 && color.getBlue() < 40;
    }
}

