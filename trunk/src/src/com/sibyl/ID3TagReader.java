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
import java.util.Map;

//string replace optimiser
public class ID3TagReader extends TagReader{

    private static final String[] tags23 = { "TALB", "TCON", "TIT2", "TRCK", "TPE1", "TPE2", "TPE3", "TOPE"};
    private static final String[] tags22 = {"TAL", "TCO", "TT2", "TRK", "TP1"};
    
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
            int version = f.read();
            skipSure(f, 2);
            switch(version)
            {
            case 0x02:
                readID3v22Tags(f);
                break;
            case 0x03 :
            default :
                readID3v23Tags(f);
                break;
            }
        }else{
            long count = fileSize-131;
            skipSure(f, count);
            if( f.read() == 'T' && f.read() == 'A' && f.read() == 'G' ){
                readID3v1Tags(f);
            }
        }
        f.close();
    }

    private void readID3v22Tags(FileInputStream f) throws IOException{
        
        byte[] buff = new byte[3];
        int pos = 10;
        int size;
        String val;
        
        int max = f.read()<<21;
        max += f.read()<<15;
        max += f.read()<<7;
        max += f.read();
                
        f.read(buff, 0, 3); // read frame header
        
        while(pos<max && buff[0] >= 'A' && buff[0] <='Z')
        {
            size = f.read()<<14;
            size += f.read()<<7;
            size += f.read();
            
            size -= 2;
            
            skipSure(f,1);
            pos+=4;

            // search for corresponding tag
            int i;
            for(i=0; i<tags22.length; i++)
            {
                if(tags22[i].charAt(0) == buff[0] 
                   && tags22[i].charAt(1) == buff[1] 
                   && tags22[i].charAt(2) == buff[2])
                {
                    
                    // read frame size
                    byte[] buff2 = new byte[size];
                    
                    f.read(buff2, 0, size);
                                        
                    // trick to read multiple tags for the artist name
                    int insert = i;
                    if(insert>=cols.length)
                    {
                        insert = cols.length-1;
                    }
                    
                    // UTF-16 ?
                    if(size >= 2 && 
                            ( (buff2[0] == 0xFFFFFFFF && buff2[1] == 0xFFFFFFFE) 
                                    || (buff2[0] == 0xFFFFFFFE && buff2[1]==0xFFFFFFFF)))
                    {
                        val = new String(buff2, "UTF-16");
                    }
                    else
                    {
                        val = new String(buff2);
                    }
                    
                    cv.put(cols[insert], val);
                    
                    skipSure(f, 1);
                    
                    break;
                }
            }
            
            if(i==tags22.length)
            {
                skipSure(f, size+1);
            }

            pos+=size+1;
            
            f.read(buff, 0, 3); // read next frame header
            pos += 3;
        }

        // if we didn't get any info we try ID3V1
        if(cv.size() == 0)
        {
            long count = fileSize-131-pos; // so we remember what we have already read
            skipSure(f, count);
            if( f.read() == 'T' && f.read() == 'A' && f.read() == 'G' ){
                readID3v1Tags(f);
            }
        }
    }

    private void readID3v23Tags(FileInputStream f) throws IOException{
        
        byte[] buff = new byte[4];
        byte[] buff2;
        int pos = 10;
        int size;
        int i;
        String val;
        
        int max = f.read()<<21;
        max += f.read()<<15;
        max += f.read()<<7;
        max += f.read();
                
        f.read(buff, 0, 4); // read frame header
        pos+=4;
        
        while(pos<max && buff[0] >= 'A' && buff[0] <='Z')
        {
            size = f.read()<<21;
            size += f.read()<<14;
            size += f.read()<<7;
            size += f.read();
            size--;
            skipSure(f,3);
            pos += 7;
            
            // search for corresponding tag
            for(i=0; i<tags23.length; i++)
            {
                if(tags23[i].charAt(0) == buff[0] 
                   && tags23[i].charAt(1) == buff[1] 
                   && tags23[i].charAt(2) == buff[2] 
                   && tags23[i].charAt(3) == buff[3])
                {
                    
                    // read frame size
                    buff2 = new byte[size];
                    
                    f.read(buff2, 0, size);
                    
                    if(buff2[size-1]==0)
                    {
                        buff2[size-1]=' ';
                    }
                    
                    // trick to read multiple tags for the artist name
                    int insert = i;
                    if(insert>=cols.length)
                    {
                        insert = cols.length-1;
                    }
                    
                    // UTF-16 ?
                    if(size >= 2 && 
                            ( (buff2[0] == 0xFFFFFFFF && buff2[1] == 0xFFFFFFFE) 
                                    || (buff2[0] == 0xFFFFFFFE && buff2[1]==0xFFFFFFFF)))
                    {
                        val = new String(buff2, "UTF-16");
                    }
                    else
                    {
                        val = new String(buff2);
                    }
                    
                    val = val.trim();

                    cv.put(cols[insert], val);
                    
                    break;
                }
            }
            
            if(i==tags23.length)
            {
                skipSure(f, size);
            }
            
            pos += size;
            
            f.read(buff, 0, 4); // read next frame header
            pos += 4;
        }

        // if we didn't get any info we try ID3V1
        if(cv.size() == 0)
        {
            long count = fileSize-131-pos; // so we remember what we have already read
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

    public Map<String, String> getValues(){
        return cv;
    }

}
