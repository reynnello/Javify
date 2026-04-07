package com.javify.objects;

public class Track {
    // Track properties
    private int id;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private int duration;
    private String filePath;
    private byte[] coverData;

    // Constructor
    public Track(int id, String title, String artist, String album, String genre, int duration, String filePath, byte[] coverData) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.duration = duration;
        this.filePath = filePath;
        this.coverData = coverData;
    }

    // Getters
    public int getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getArtist() {
        return artist;
    }
    public String getAlbum() {
        return album;
    }
    public String getGenre() {
        return genre;
    }
    public int getDuration() {
        return duration;
    }
    public String getFilePath() {
        return filePath;
    }
    public byte[] getCoverData() {
        return coverData;
    }
}
