package com.sibyl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.content.ContentValues;
import android.util.Log;
// skip -> test
// string replace optimiser
public class ID3TagReader {

    private ContentValues cv;

    // could be static
    public ID3TagReader(String filename) throws FileNotFoundException, IOException {

	cv = new ContentValues();

	File fi = new File(filename);
	FileReader f = new FileReader(fi);
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
	    while(count > 0){
		count -= f.skip(count);
	    }
	    if( f.read() == 'T' && f.read() == 'A' && f.read() == 'G' ){
		readID3v1Tags(f);
	    }
	}
	f.close();
    }

    private void readID3v2Tags(FileReader f, int taille) throws IOException{
	String[] tags = { "TALB", "TCON", "TIT2", "TRCK", "TPE1"};
	String[] cols = { Music.ALBUM.NAME, Music.GENRE.NAME, Music.SONG.TITLE, Music.SONG.TRACK, Music.ARTIST.NAME};
	char[] buff = new char[4];
	int pos = 0;
	f.skip(3); // skip tags header
	f.read(buff, 0, 4); // read frame header
	while(pos<taille && buff[0] >= 'A' && buff[0] <='Z'){
	    int size;
	    size = f.read()<<21;
	    size += f.read()<<14;
	    size += f.read()<<7;
	    size += f.read();
	    f.skip(2);
	    // search for corresponding tag
	    int i;
	    for(i=0; i<tags.length; i++){
		if(tags[i].charAt(0) == buff[0] && tags[i].charAt(1) == buff[1] 
		                                                             && tags[i].charAt(2) == buff[2] && tags[i].charAt(3) == buff[3]){
		    // read frame size
		    char[] buff2 = new char[size-1];
		    // read frame content
		    f.skip(1);
		    f.read(buff2, 0, size-1);
		    pos+=size;
		    cv.put(cols[i], new String(buff2).replace("'", "''"));
		    break;
		}
	    }
	    if(i==tags.length){
		f.skip(size);
		pos+=size;
	    }
	    f.read(buff, 0, 4); // read next frame header
	    pos += 10;
	}
    }

    private void readID3v1Tags(FileReader f) throws IOException{
	char[] buff = new char[30];
	f.read(buff, 0, 30);
	cv.put(Music.SONG.TITLE, new String(buff).trim());

	f.read(buff, 0, 30);
	cv.put(Music.ARTIST.NAME, new String(buff).trim());

	f.read(buff, 0, 30);
	cv.put(Music.ALBUM.NAME, new String(buff).trim());

	f.skip(32);
	if(f.read() == 0){
	    cv.put(Music.SONG.TRACK, f.read());
	}else{
	    f.read();
	}
	int t = f.read();
	cv.put(Music.GENRE.ID, t>0 && t<147 ? t : 0);
    }

    public  ContentValues getValues(){
	return cv;
    }

}
