public class Playlist {
    // Playlist properties
    private int id;
    private String name;
    private int userId;
    private List<Track> tracks;

    // Constructor
    public Playlist(int id, String name, int userId, List<Track> tracks) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.tracks = tracks;
    }

    // Getters
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public int getUserId() {
        return userId;
    }
    public List<Track> getTracks() {
        return tracks;
    }
}