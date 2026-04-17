package com.javify.objects;

import java.util.List;

public class Playlist {
    // Playlist properties
    private int id;
    private String name;
    private int userId;
    private List<Track> tracks;
    private byte[] coverData;

    // Constructor
    public Playlist(int id, String name, int userId, List<Track> tracks, byte[] coverData) {
            this.id = id;
            this.name = name;
            this.userId = userId;
            this.tracks = tracks;
            this.coverData = coverData;
        }

        // Getters
        public int getId () {
            return id;
        }
        public String getName () {
            return name;
        }
        public int getUserId () {
            return userId;
        }
        public List<Track> getTracks () {
            return tracks;
        }

        public byte[] getCoverData() {
            return coverData;
        }
    }
