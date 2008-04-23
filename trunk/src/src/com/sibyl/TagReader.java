package com.sibyl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public abstract class TagReader {

    protected HashMap<String, String> cv;
    
    // columns you'll find in the hashmap
    protected static final String[] cols = { Music.ALBUM.NAME, Music.GENRE.NAME, Music.SONG.TITLE, Music.SONG.TRACK, Music.ARTIST.NAME};
    
    protected static void skipSure(InputStream f, long n) throws IOException{
        while(n > 0){
            n -= f.skip(n);
        }
    }
    
    protected static void skipSure(InputStream f, int n) throws IOException{
        while(n > 0){
            n -= f.skip(n);
        }
    }

    public HashMap<String, String> getValues(){
        return cv;
    }


}
