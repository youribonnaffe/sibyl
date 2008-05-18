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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

/**
 * http://www.wotsit.org/list.asp?search=ogg&button=GO!
 * 
 * @author sibyl-dev
 *
 */
public class OggTagReader extends TagReader{

    private static final String[] labels = { "ALBUM", "GENRE", "TITLE", "TRACKNUMBER", "ARTIST"};

    public OggTagReader(String filename) throws IOException, FileNotFoundException{

        cv = new HashMap<String, String>();
        
        BufferedInputStream f = new BufferedInputStream(new FileInputStream(filename));
        
        // first Ogg page
        skipSure(f,58);
        // jump to page segments page
        skipSure(f,26);
        // read size
        short t = (short)f.read();
        // jump
        skipSure(f, t);
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