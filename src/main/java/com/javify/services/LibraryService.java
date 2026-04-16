package com.javify.services;

import com.javify.dao.TrackDAO;
import com.javify.objects.Track;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LibraryService {

    private final TrackDAO trackDAO;

    public LibraryService() {
        this.trackDAO = new TrackDAO();
    }

    // scan folder for mp3 files and add them to the database
    public List<Track> scanFolder(File folder) {
        List<Track> scanned = new ArrayList<>();

        // if the folder doesn't exist or isn't a directory, return an empty list
        if (folder == null || !folder.isDirectory()) {
            return scanned;
        }

        // remove JAudioTagger logging noise
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);

        // list all files in the folder with mp3, wav, or flac extensions by using a lambda expression
        File[] files = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac");
        });

        // if the folder is empty, return an empty list
        if (files == null) {
            return scanned;
        }

        // iterate over the files and parse their metadata using JAudioTagger
        for (File file : files) {
            Track track = parseTrack(file);
            if (track != null) {
                trackDAO.addTrack(track);
                scanned.add(track);
            }
        }

        return scanned;
    }

    // scan selected files/folders and add discovered tracks to the database
    public List<Track> scanSelection(List<File> selections) {
        List<Track> scanned = new ArrayList<>();
        if (selections == null || selections.isEmpty()) {
            return scanned;
        }

        for (File file : selections) {
            if (file == null || !file.exists()) {
                continue;
            }

            if (file.isDirectory()) {
                scanned.addAll(scanFolder(file));
                continue;
            }

            if (!isSupportedAudioFile(file.getName())) {
                continue;
            }

            Track track = parseTrack(file);
            if (track != null) {
                trackDAO.addTrack(track);
                scanned.add(track);
            }
        }

        return scanned;
    }

    // checks metadata of a file and returns a track object
    private Track parseTrack(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            int duration = audioFile.getAudioHeader().getTrackLength();

            String title  = getTag(tag, FieldKey.TITLE,  file.getName().replaceAll("\\.[^.]+$", ""));
            String artist = getTag(tag, FieldKey.ARTIST, "Unknown Artist");
            String album  = getTag(tag, FieldKey.ALBUM,  "Unknown Album");
            String genre  = getTag(tag, FieldKey.GENRE,  "");

            // set cover to null
            // if tag is exist, takes first picture(which is a cover) and puts in cover
            byte[] cover = null;
            if (tag != null) {
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null) cover = artwork.getBinaryData();
            }

            return new Track(0, title, artist, album, genre, duration, file.getAbsolutePath(), cover);

        } catch (Exception e) {
            System.err.println("Failed to parse: " + file.getName() + " — " + e.getMessage());
            return null;
        }
    }

    // get tag from JAudioTagger
    private String getTag(Tag tag, FieldKey key, String fallback) {
        if (tag == null) return fallback;
        try {
            // if tag is exist, return tag value, else return fallback
            String value = tag.getFirst(key);
            return (value != null && !value.isBlank()) ? value : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    // return all tracks
    public List<Track> getAllTracks() {
        return trackDAO.getAllTracks();
    }

    // search for tracks by title, artist, or album
    public List<Track> search(String query) {
        return trackDAO.searchTracks(query);
    }

    private boolean isSupportedAudioFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac");
    }
}