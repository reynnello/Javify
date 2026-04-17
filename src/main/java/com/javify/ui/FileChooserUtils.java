package com.javify.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;

 // cross-platform file chooser utility using native system dialogs (thanks AI)
public class FileChooserUtils {

    private FileChooserUtils() {
    }

    // image file chooser
    public static File chooseImageFile(Component parent) {
        Frame frame = getFrameOwner(parent);
        if (frame == null) {
            return null;
        }

        FileDialog dialog = new FileDialog(frame, "Choose image", FileDialog.LOAD);
        dialog.setFile("*.jpg;*.jpeg;*.png");
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
        });
        dialog.setVisible(true);

        String filename = dialog.getFile();
        if (filename != null) {
            return new File(dialog.getDirectory(), filename);
        }
        return null;
    }

    // audio file chooser
    public static File[] chooseAudioFiles(Component parent) {
        Frame frame = getFrameOwner(parent);
        if (frame == null) {
            return new File[0];
        }

        FileDialog dialog = new FileDialog(frame, "Add music files or folder", FileDialog.LOAD);
        dialog.setFile("*.mp3;*.wav;*.flac");
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac") ||
                    new File(dir, name).isDirectory();
        });
        dialog.setVisible(true);

        String filename = dialog.getFile();
        if (filename != null) {
            return new File[] { new File(dialog.getDirectory(), filename) };
        }
        return new File[0];
    }

    // avatar file chooser (same as image)
    public static File chooseAvatarFile(Component parent) {
        return chooseImageFile(parent);
    }

    // get the frame owner of a component
    private static Frame getFrameOwner(Component component) {
        Window owner = SwingUtilities.getWindowAncestor(component);
        if (owner instanceof Frame) {
            return (Frame) owner;
        }
        return null;
    }
}


