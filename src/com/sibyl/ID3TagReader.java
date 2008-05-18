/* 
 *
 * Copyright (C) 2007-2008 sibyl project
 * http://code.google.com/p/sibyl/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sibyl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

//string replace optimiser
public class ID3TagReader extends TagReader{

    private static final String[] tags = { "TALB", "TCON", "TIT2", "TRCK", "TPE1", "TPE2", "TPE3", "TOPE", "TCOM"};

    private long fileSize;
    // could be static
    public ID3TagReader(String filename) throws FileNotFoundException, IOException {

        cv = new HashMap<String,String>();

        File fi = new File(filename);
        fileSize = fi.length();
        FileInputStream f = new FileInputStream(fi);
        byte[] buff = new byte[3];
        f.read(buff);

        if ( buff[0] == 'I' && buff[1] == 'D' && buff[2] == '3' ){
            int size = f.read()<<21;
            size += f.read()<<14;
            size += f.read()<<7;
            size += f.read();
            readID3v2Tags(f, size);
        }else{
            long count = fileSize-131;
            skipSure(f, count);
            if( f.read() == 'T' && f.read() == 'A' && f.read() == 'G' ){
                readID3v1Tags(f);
            }
        }
        f.close();
    }

    private void readID3v2Tags(FileInputStream f, int taille) throws IOException{
        byte[] buff = new byte[4];
        int pos = 0;
        TagReader.skipSure(f, 3); // skip tags header
        f.read(buff, 0, 4); // read frame header
        pos+=7;
        while(pos<taille && buff[0] >= 'A' && buff[0] <='Z'){
            int size;
            size = f.read()<<21;
            size += f.read()<<14;
            size += f.read()<<7;
            size += f.read();
            skipSure(f,2);
            // search for corresponding tag
            int i;
            for(i=0; i<tags.length; i++)
            {
                if(tags[i].charAt(0) == buff[0] && tags[i].charAt(1) == buff[1] 
                                                                             && tags[i].charAt(2) == buff[2] && tags[i].charAt(3) == buff[3])
                {
                    // read frame size
                    byte[] buff2 = new byte[size-1];
                    // read frame content
                    skipSure(f, 1);
                    f.read(buff2, 0, size-1);
                    pos+=size;
                    // trick to read multiple tags for the artist name
                    int insert = i;
                    if(i>=cols.length){
                        insert = cols.length-1;
                    }
                    String val;
                    // UTF-16 ?
                    if(size >= 2 && 
                            ( (buff2[0] == 0xFFFFFFFF && buff2[1] == 0xFFFFFFFE) 
                                    || (buff2[0] == 0xFFFFFFFE && buff2[1]==0xFFFFFFFF))){
                        val = new String(buff2, "UTF-16");
                    }else{
                        val = new String(buff2);
                    }

                    cv.put(cols[insert], val);
                    break;
                }
            }
            if(i==tags.length){
                skipSure(f, size);
                pos+=size;
            }
            f.read(buff, 0, 4); // read next frame header
            pos += 10;
        }

        // if we didn't get any info we try ID3V1
        if(cv.size() == 0){
            long count = fileSize-131-4-pos; // so we remember what we have already read
            skipSure(f, count);
            if( f.read() == 'T' && f.read() == 'A' && f.read() == 'G' ){
                readID3v1Tags(f);
            }
        }
    }

    private void readID3v1Tags(FileInputStream f) throws IOException{
        byte[] buff = new byte[30];
        f.read(buff, 0, 30);
        cv.put(Music.SONG.TITLE, new String(buff).trim());

        f.read(buff, 0, 30);
        cv.put(Music.ARTIST.NAME, new String(buff).trim());

        f.read(buff, 0, 30);
        cv.put(Music.ALBUM.NAME, new String(buff).trim());

        skipSure(f, 32);
        if(f.read() == 0){
            cv.put(Music.SONG.TRACK, Integer.toString(f.read()));
        }else{
            f.read();
        }
        int t = f.read();
        cv.put(Music.GENRE.ID, Integer.toString(t>0 && t<147 ? t : 1));
    }

    public HashMap<String, String> getValues(){
        return cv;
    }

}
