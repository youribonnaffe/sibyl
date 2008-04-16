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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.content.ContentValues;
import android.util.Log;

//string replace optimiser
public class ID3TagReader {

    private ContentValues cv;

    private static void skipSure(BufferedReader f, long n) throws IOException{
	while(n > 0){
	    n -= f.skip(n);
	}
    }

    private static void skipSure(BufferedReader f, int n) throws IOException{
	while(n > 0){
	    n -= f.skip(n);
	}
    }

    // could be static
    public ID3TagReader(String filename) throws FileNotFoundException, IOException {

	cv = new ContentValues();

	File fi = new File(filename);
	BufferedReader f = new BufferedReader(new FileReader(fi), 8192);
	char[] buff = new char[3];
	f.read(buff);

	if ( buff[0] == 'I' && buff[1] == 'D' && buff[2] == '3' ){
	    int size = f.read()<<21;
	    size += f.read()<<14;
	    size += f.read()<<7;
	    size += f.read();
	    readID3v2Tags(f, size);
	}else{
	    long count = fi.length()-131;
	    skipSure(f, count);
	    if( f.read() == 'T' && f.read() == 'A' && f.read() == 'G' ){
		readID3v1Tags(f);
	    }
	}
	f.close();
    }

    private void readID3v2Tags(BufferedReader f, int taille) throws IOException{
	String[] tags = { "TALB", "TCON", "TIT2", "TRCK", "TPE1"};
	String[] cols = { Music.ALBUM.NAME, Music.GENRE.NAME, Music.SONG.TITLE, Music.SONG.TRACK, Music.ARTIST.NAME};
	char[] buff = new char[4];
	int pos = 0;
	skipSure(f, 3); // skip tags header
	f.read(buff, 0, 4); // read frame header
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
		    char[] buff2 = new char[size-1];
		    // read frame content
		    skipSure(f, 1);
		    f.read(buff2, 0, size-1);
		    pos+=size;
		    cv.put(cols[i], new String(buff2).trim().replace("'", "''"));
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
	Log.v("mp3", cv.toString());
    }

    private void readID3v1Tags(BufferedReader f) throws IOException{
	char[] buff = new char[30];
	f.read(buff, 0, 30);
	cv.put(Music.SONG.TITLE, new String(buff).trim());

	f.read(buff, 0, 30);
	cv.put(Music.ARTIST.NAME, new String(buff).trim());

	f.read(buff, 0, 30);
	cv.put(Music.ALBUM.NAME, new String(buff).trim());

	skipSure(f, 32);
	if(f.read() == 0){
	    cv.put(Music.SONG.TRACK, f.read());
	}else{
	    f.read();
	}
	int t = f.read();
	cv.put(Music.GENRE.ID, t>0 && t<147 ? t : 1);
    }

    public ContentValues getValues(){
	return cv;
    }

}
