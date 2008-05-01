package com.sibyl;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import android.util.Log;


public class OggTagReader extends TagReader{

    private static final String[] labels = { "ALBUM", "GENRE", "TITLE", "TRACKNUMBER", "ARTIST"};

    public OggTagReader(String filename) throws IOException, FileNotFoundException{

        cv = new HashMap<String, String>();
        
        BufferedInputStream f = new BufferedInputStream(new FileInputStream(filename));

        // skip all beginning
        skipSure(f,102);
        // read header type
        int packtype = f.read();
        // skip "vorbis"
        skipSure(f,6);
        if(packtype == 0x03){
            // read header comments
            // vendor
            int size = f.read();
            size += f.read()<<7;
            size += f.read()<<14;
            size += f.read()<<21;
            // skip vendor string
            skipSure(f,size);
            // read number of tags
            int nbTags = f.read();
            nbTags += f.read()<<7;
            nbTags += f.read()<<14;
            nbTags += f.read()<<21;
            // read tags
            for(int i = 0; i<nbTags; i++){
                // read tag size
                size = f.read();
                size += f.read()<<7;
                size += f.read()<<14;
                size += f.read()<<21;
                // assume that tag is shorter than 255c
                if(size > 0 && size < 255){
                    // read tag
                    byte[] buff = new byte[size];
                    f.read(buff, 0, size);
                    String vector = new String(buff, "UTF-8");
                    int pos = vector.indexOf('=');
                    String label = vector.substring(0, pos);
                    String value = vector.substring(pos+1);
                    // associate label with cols
                    pos = 0;
                    while(pos < labels.length && label.compareTo(labels[pos])!=0 ){
                        pos++;
                    }
                    if(pos < labels.length){
                        cv.put(cols[pos], value);
                        if(cv.size() == cols.length) break;
                    }
                }else{
                    // skip tag
                    skipSure(f,size);
                }
            }
        }

        f.close();

    }
    
    public HashMap<String, String> getValues(){
        return cv;
    }

}