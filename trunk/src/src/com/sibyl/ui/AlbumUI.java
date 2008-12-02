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

package com.sibyl.ui;


import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.sibyl.CoverDownloader;
import com.sibyl.Music;
import com.sibyl.MusicDB;
import com.sibyl.R;
import com.sibyl.ui.animation.ActivityTransition;

/*
 * This activity list all the albums and display their cover. The albums are sorted by the artists order.
 * This activity allows the user to manage the cover: search new covers on the web (not implemented),
 * associate cover/album (done in CoverUI)
 */
public class AlbumUI extends ListActivity {

    private static final String TAG = "ALBUMUI";
    //constants for the cursor
    static private final String[] res = {"album_name","cover_url"}; 
    static private final int[] to = {R.id.album, R.id.img};
    //constant menu
    private static final int BACK_ID = Menu.FIRST;
    private static final int DOWNLOAD_ID = Menu.FIRST+1;

    private LinearLayout groupView;
    private MusicDB mdb;    //the database
    private Cursor listAlbum; // the cursor where the data displayed are
    private int selectedAlbum; // the id of the album selected when going to coverui
    private String[] artists; // artist names for each albums

    // thread to run covertask
    Thread coverThread;
    // UI handler
    private Handler coverTaskHandler = new Handler();

    /* ----------------------- ACTIVITY THREADS -------------------------------*/

    // to refresh a specific album cover in the listview
    private class CoverUpdate implements Runnable{
        public void run(){
            listAlbum.requery();
        }
    }

    private class CoverTask implements Runnable{
        // album ids
        private int[] albums;

        public CoverTask(Cursor listAlbum){
            // copy album ids into array
            listAlbum.moveToPosition(0);
            albums = new int[listAlbum.getCount()];
            int i = 0;
            while(i<albums.length){
                albums[i++] = listAlbum.getInt(listAlbum.getColumnIndex(Music.ALBUM.ID));
                listAlbum.moveToNext();
            }
        }

        public void run(){
            // position in the list
            int pos = 0;
            for(int album : albums){
                if(Thread.interrupted()){
                    // stop thread
                    return;
                }
                if(mdb.getAlbumCover(album) == null){
                    // download undefined cover
                    if(CoverDownloader.retrieveCover(mdb, album)){
                        // refresh cover in list
                        coverTaskHandler.post(new CoverUpdate());
                    }
                    // next item in the list
                    pos ++;
                }
            }
        }
    };

    /* ----------------------- ACTIVITY STATES -------------------------------*/

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTitle(R.string.cover_manager_title);
        setContentView(R.layout.album);
        groupView = (LinearLayout) findViewById(R.id.group);
        //Log.v(TAG,"CoverUI is launched");
        selectedAlbum = 0;
        try
        {
            mdb = new MusicDB(this);
            fillData();
        }
        catch(SQLiteDiskIOException ex)
        {
            //Log.v(TAG, ex.toString());
        }
    }

    @Override
    protected void onResume() 
    {
        super.onResume();
        final ActivityTransition trans = new ActivityTransition( true );
        trans.setDuration(500);
        trans.setFillAfter(true);
        trans.setInterpolator(new AccelerateInterpolator());
        groupView.startAnimation(trans);
    }

    @Override
    protected void onPause() 
    {
        final ActivityTransition trans = new ActivityTransition( false );
        trans.setDuration(500);
        trans.setFillAfter(true);
        trans.setInterpolator(new DecelerateInterpolator());
        groupView.startAnimation(trans);
        super.onPause();
    }

    protected void onDestroy(){
        
        if( coverThread != null ){
            coverThread.interrupt();
        }
        listAlbum.close();
        
        mdb.close();
        
        super.onDestroy();
    }

    /* ----------------------- ACTIVITY EVENTS -------------------------------*/

    /**
     *
     * @see android.app.Activity#onActivityResult(int, int, java.lang.String, android.os.Bundle)
     */
    protected void onActivityResult(int requestCode, int resultCode, String data, Bundle extras){
        switch(resultCode){
            case RESULT_OK :
                ////Log.v(TAG, "selected"+((Integer) selectedAlbum).toString());
                mdb.setCover(selectedAlbum, data);
                listAlbum.requery(); //recover the cursor
                break;
            case RESULT_FIRST_USER :
                // reset image
                mdb.deleteCover(selectedAlbum);
                listAlbum.requery(); //recover the cursor
                break;
        }
    }

    protected void onListItemClick(ListView l, View v, int position, long id){
        displayCoverUI(position);
    }

    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, BACK_ID, Menu.NONE, R.string.alb_back);
        menu.add(0, DOWNLOAD_ID, Menu.NONE, R.string.cov_download);
        return true;
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) 
    {
        super.onMenuItemSelected(featureId, item);
        switch(item.getItemId()) 
        {
            case BACK_ID:
                finish();
                break;
            case DOWNLOAD_ID :
                new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.star_big_off)
                .setTitle(R.string.download_cover_dialog_title)
                .setMessage(R.string.download_cover_dialog_message)
                .setPositiveButton(R.string.download_cover_dialog_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // yes ? will download the covers
                        coverThread = new Thread(new CoverTask(listAlbum));
                        coverThread.start();
                    }
                })
                .setNegativeButton(R.string.download_cover_dialog_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // no ? do nothing
                    }
                }).show();
                break;
        }
        return true;
    }


    /* --------------------------- utils -----------------------------------*/

    /**
     * Fill the ListView with the association of the album and its cover
     */
    private void fillData(){

        listAlbum = mdb.getAlbumCovers();
        startManagingCursor(listAlbum);
        SimpleCursorAdapter rows = new SimpleCursorAdapter(this, R.layout.album_cover_row, listAlbum, res, to){
            // need to override this method to set default image if undefined
            public void setViewImage(ImageView v, String url){
                //Log.v(TAG, "SET IMAGE "+url);
                if(url == null || url.equals("")){
                    v.setImageResource(R.drawable.logo);
                }else{
                    v.setImageDrawable(Drawable.createFromPath(url));
                }
            }
        };
        
        // retrieve artist names
        artists = new String[listAlbum.getCount()];
        int i = 0;
        while(listAlbum.moveToNext()){
            String artist = new String();
            Cursor c = mdb.getArtistFromAlbum(listAlbum.getString(listAlbum.getColumnIndex(Music.ALBUM.NAME)));
            if(c.moveToFirst()){
                artist+=c.getString(c.getColumnIndex(Music.ARTIST.NAME));
            }
            while(c.moveToNext()){
                artist+=", "+c.getString(c.getColumnIndex(Music.ARTIST.NAME));
            }
            artists[i++] = artist;
            c.close();
        }
        listAlbum.move(0);
        
        rows.setViewBinder(new SimpleCursorAdapter.ViewBinder(){
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if(columnIndex == cursor.getColumnIndex(Music.ALBUM.NAME)){
                    ((TextView)view.getRootView().findViewById(R.id.artist)).setText(artists[cursor.getPosition()]);
                }
                return false;
            }
        });
        setSelection(0);
        setListAdapter(rows);
    }

    /**
     * Launch the CoverUI. It keeps the number of the selected rows.
     * If selectedRow is negative, we do nothing (no album selected)
     * @param selectedRow: the position of the View selected in the ListView
     */
    private void displayCoverUI(int selectedRow) 
    {
        if( selectedRow >= 0){
            listAlbum.moveToPosition(selectedRow);
            selectedAlbum = listAlbum.getInt(listAlbum.getColumnIndex(Music.ALBUM.ID));
            Intent i = new Intent(this, CoverUI.class);
            i.putExtra(CoverUI.ALBUM_ID, listAlbum.getInt(listAlbum.getColumnIndex(Music.ALBUM.ID)));
            i.putExtra(CoverUI.ALBUM_NAME, listAlbum.getString(listAlbum.getColumnIndex(Music.ALBUM.NAME)));
            startActivity(i);
        }
    }

}
