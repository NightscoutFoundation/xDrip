package com.eveningoutpost.dexdrip.UtilityModels.music;

public class MusicSpec {
    public static final int MUSIC_UNDEFINED = 0;
    public static final int MUSIC_PLAY = 1;
    public static final int MUSIC_PAUSE = 2;
    public static final int MUSIC_PLAYPAUSE = 3;
    public static final int MUSIC_NEXT = 4;
    public static final int MUSIC_PREVIOUS = 5;

    public String artist;
    public String album;
    public String track;
    public int duration;
    public int trackCount;
    public int trackNr;

    public MusicSpec() {

    }

    public MusicSpec(MusicSpec old) {
        this.duration = old.duration;
        this.trackCount = old.trackCount;
        this.trackNr = old.trackNr;
        this.track = old.track;
        this.album = old.album;
        this.artist = old.artist;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MusicSpec)) {
            return false;
        }
        MusicSpec musicSpec = (MusicSpec) obj;

        return this.hashCode() == musicSpec.hashCode();
    }

    @Override
    public int hashCode() {
        int result = artist != null ? artist.hashCode() : 0;
        result = 31 * result + (album != null ? album.hashCode() : 0);
        result = 31 * result + (track != null ? track.hashCode() : 0);
        result = 31 * result + duration;
        result = 31 * result + trackCount;
        result = 31 * result + trackNr;
        return result;
    }

    @Override
    public String toString() {
        return "MusicSpec{" +
                "artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", track='" + track + '\'' +
                ", duration=" + duration +
                ", trackCount=" + trackCount +
                ", trackNr=" + trackNr +
                '}';
    }
}
