package com.sibyl;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import android.database.Cursor;
import android.util.Log;

/**
 * works with amazon for now
 * @author sibyl
 *
 */
public class CoverDownloader {

    private static final String TAG = "CoverDownloader";

    private static final String QUERY_AMAZON = "http://webservices.amazon.com/onca/xml?Service=AWSECommerceService" +
    "&AWSAccessKeyId=0RX30HTGPKA2ZDK3RWG2" +
    "&Operation=ItemSearch" +
    "&SearchIndex=Music" +
    "&ResponseGroup=Images,ItemAttributes,Small";

    /**
     * download the cover for the albumId given and save it into the database
     * 
     * @param mdb database connection
     * @param albumId   album id
     * @return true if cover found, false otherwise
     */
    public static boolean retrieveCover(MusicDB mdb, int albumId){

        // retrieve album information
        Cursor c = mdb.getAlbumInfo(albumId);

        // parse every (album, artist)
        while(c.next()){
            try{
                // build request, search for album
                String q = QUERY_AMAZON + "&Title="+ URLEncoder.encode(c.getString(c.getColumnIndex(Music.ALBUM.NAME)), "UTF-8");
                
                if(c.getInt(c.getColumnIndex(Music.ARTIST.ID)) > 1){
                    // there is an artist associated so we add his name to the request
                    q+= "&Artist=" + URLEncoder.encode(c.getString(c.getColumnIndex(Music.ARTIST.NAME)), "UTF-8");
                }
                
                // amazon answer xml parser
                AmazonParser ap = new AmazonParser();
                // request to amazon website
                SAXParserFactory.newInstance().newSAXParser().parse(new URL(q).openStream(), ap);

                // retrieve images from answer
                String answer = ap.getResult();
                if(answer != null){
                    // save cover and add it to database

                    // test covers directory
                    File f = new File(Music.COVER_DIR);
                    if(!f.isDirectory()){
                        // try to create directory
                        if(!f.mkdir()){
                            throw new IOException("can't create cover folder");
                        }
                    }

                    //Log.v(TAG, "anws "+answer);
                    // get image
                    InputStream  in = new URL(answer).openStream();
                    // filename is image name + cover dir
                    String filename = Music.COVER_DIR + answer.substring(answer.lastIndexOf('/'));
                    // copy image into local file
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename), 8192);
                    byte[] buffer = new byte[1024];
                    int numRead;
                    while ((numRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, numRead);
                    }
                    out.close();
                    
                    Log.v(TAG, "SET cover "+albumId+" "+filename);
                    
                    // save cover path to database
                    mdb.setCover(albumId, filename);
                    // we have found a cover, no need to search again
                    return true;
                }
            }catch(UnsupportedEncodingException uee){
                Log.v(TAG, "characters problem "+ uee.toString());
            }catch(SAXException saxe){
                // shouldn't happen
            }catch(ParserConfigurationException pce){
                // shouldn't happen
            }catch(IOException ioe){
                Log.v(TAG, ioe.toString());
            }
        }
        // we haven't found any cover
        return false;
    }
}


