package com.sibyl;

public class Music {

    public static String[] SONGS = { SONG.ID, SONG.URL, SONG.TITLE,
	    SONG.LAST_PLAYED, SONG.COUNT_PLAYED, SONG.TRACK, SONG.ARTIST,
	    SONG.ALBUM, SONG.GENRE };
    public static String[] ARTISTS = { ARTIST.ID, ARTIST.NAME };
    public static String[] ALBUMS = { ALBUM.ID, ALBUM.NAME };
    public static String[] GENRES = { GENRE.ID, GENRE.NAME };
    public static String[] CURRENT_PLAYLISTS = { CURRENT_PLAYLIST.POS,
	    CURRENT_PLAYLIST.ID };

    public static class SONG {
	public static String ID = "id";
	public static String URL = "url";
	public static String TITLE = "title";
	public static String LAST_PLAYED = "last_played";
	public static String COUNT_PLAYED = "count_played";
	public static String TRACK = "track";
	public static String ARTIST = "artist";
	public static String ALBUM = "album";
	public static String GENRE = "genre";
    }

    public static class ARTIST {
	public static String ID = "id";
	public static String NAME = "artist_name";
    }

    public static class ALBUM {
	public static String ID = "id";
	public static String NAME = "album_name";
    }

    public static class GENRE {
	public static String ID = "id";
	public static String NAME = "genre_name";
    }

    public static class CURRENT_PLAYLIST {
	public static String POS = "pos";
	public static String ID = "id";
    }

}
