package dev.migwel.radioreader.metadata;

import java.util.Objects;

public class SongInfo {
    private final String artist;
    private final String title;

    public SongInfo(String artist, String title) {
        this.artist = artist;
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SongInfo songInfo = (SongInfo) o;
        return Objects.equals(artist, songInfo.artist) && Objects.equals(title, songInfo.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artist, title);
    }

    @Override
    public String toString() {
        return "SongInfo{" +
                "artist='" + artist + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
