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
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.sibyl.CoverDownloader;
import com.sibyl.Music;
import com.sibyl.MusicDB;
import com.sibyl.R;

/*
 * This activity list all the albums and display their cover. The albums are sorted by the artists order.
 * This activity allows the user to manage the cover: search new covers on the web (not implemented),
 * associate cover/album (done in CoverUI)
 */
public class AlbumUI extends ListActivity {

    private static final String TAG = "ALBUMUI";
    //constants for the cursor: position of the data in the cursor
    private final static int ARTIST = 0;
    private final static int ALBUM = 1;
    private final static int COVER_URL = 2;
    private final static int ALBUM_ID = 3;
    //constant menu
    private static final int BACK_ID = Menu.FIRST;
    private static final int DOWNLOAD_ID = Menu.FIRST+1;

    private MusicDB mdb;    //the database
    private Cursor listAlbum;
    private AlbumCoverView selectedAlbumView; //the view selected
    private int selectedAlbum; // the position of the view selected

    // thread to run covertask
    Thread coverThread;
    // UI handler
    private Handler coverTaskHandler = new Handler();

    // to refresh a specific album cover in the listview
    private class CoverUpdate implements Runnable{
        private int pos;
        private int album;
        public CoverUpdate(int posList, int albumId){
            pos = posList;
            album = albumId;
        }
        public void run(){
            // set new image
            ((AlbumCoverView)getListAdapter().getView(pos, null, null)).setIcon(Drawable.createFromPath(mdb.getAlbumCover(album)));
            // refresh list
            // TODO not working everytime
            ((AlbumCoverListAdapter)getListAdapter()).notifyDataSetChanged();
        }
    }

    private class CoverTask implements Runnable{
        // album ids
        private int[] albums;

        public CoverTask(Cursor listAlbum){
            // copy album ids into array
            listAlbum.moveTo(0);
            albums = new int[listAlbum.count()];
            int i = 0;
            while(i<albums.length){
                albums[i++] = listAlbum.getInt(listAlbum.getColumnIndex(Music.ALBUM.ID));
                listAlbum.next();
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
                        coverTaskHandler.post(new CoverUpdate(pos, album));
                    }
                    // next item in the list
                    pos ++;
                }
            }
        }
    };

    protected void onCreate(Bundle icicle) {
        // TODO Auto-generated method stub
        super.onCreate(icicle);
        setContentView(R.layout.album);
        Log.v(TAG,"CoverUI is launched");
        selectedAlbum = 0;
        selectedAlbumView = null;
        try
        {
            mdb = new MusicDB(this);
            fillData();
        }
        catch(SQLiteDiskIOException ex)
        {
            Log.v(TAG, ex.toString());
        }
        getListView().setOnItemClickListener(ListClick);
    }

    protected void onDestroy(){
        super.onDestroy();
        if( coverThread != null ){
            coverThread.interrupt();
        }
    }

    /**
     *
     * @see android.app.Activity#onActivityResult(int, int, java.lang.String, android.os.Bundle)
     */
    protected void onActivityResult(int requestCode, int resultCode, String data, Bundle extras){
        switch(resultCode){
            case RESULT_OK :
                //Log.v(TAG, "selected"+((Integer) selectedAlbum).toString());
                listAlbum.requery(); //recover the cursor
                listAlbum.moveTo(selectedAlbum);
                mdb.setCover(listAlbum.getInt(ALBUM_ID), data);
                if( selectedAlbumView != null){
                    selectedAlbumView.setIcon(Drawable.createFromPath(data));
                    selectedAlbumView = null; //we reset the selectedView in the end
                }
                break;
            case RESULT_FIRST_USER :
                // reset image
                listAlbum.requery(); //recover the cursor
                listAlbum.moveTo(selectedAlbum);
                mdb.deleteCover(listAlbum.getInt(ALBUM_ID));
                if( selectedAlbumView != null){
                    selectedAlbumView.setIcon(getResources().getDrawable(R.drawable.logo));
                    selectedAlbumView = null; //we reset the selectedView in the end
                }
                break;
        }
    }

    /**
     * manage the click on one row. If no row is selected, we do nothing.
     * @param arg1: the view selected, position: the position of the View in the ListView
     */
    private OnItemClickListener ListClick = new OnItemClickListener()
    {
        public void onItemClick(AdapterView arg0, View arg1, int position, long id) {
            displayCoverUI(position);
            if( position >= 0){
                selectedAlbumView = (AlbumCoverView) arg1;
            }
        }
    }; 

    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, BACK_ID, R.string.alb_back);
        menu.add(0, DOWNLOAD_ID, R.string.cov_download);
        return true;
    }

    public boolean onMenuItemSelected(int featureId, Item item) 
    {
        super.onMenuItemSelected(featureId, item);
        switch(item.getId()) 
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



    /**
     * Fill the ListView with the association of thealbum and its cover
     */
    private void fillData(){

        listAlbum = mdb.getAlbumCovers();
        AlbumCoverListAdapter rows = new AlbumCoverListAdapter(this);

        startManagingCursor(listAlbum);
        //for each album we associate its cover.
        while(listAlbum.next()){
            //Log.v(TAG,listAlbum.getString(ARTIST)+"-"+listAlbum.getString(ALBUM));
            if( listAlbum.isNull(COVER_URL) || listAlbum.getString(COVER_URL).equals("")){
                rows.add(new IconifiedText( listAlbum.getString(ARTIST)+'\n'+listAlbum.getString(ALBUM),
                        getResources().getDrawable(R.drawable.logo))); //default cover
            }
            else{
                rows.add(new IconifiedText( listAlbum.getString(ARTIST)+'\n'+listAlbum.getString(ALBUM),
                        Drawable.createFromPath(listAlbum.getString(COVER_URL)))); //real cover
            }
        }
        setListAdapter(rows);
        getListView().setSelection(selectedAlbum);
    }

    /**
     * Launch the CoverUI. It keeps the number of the selected rows.
     * If selectedRow is negative, we do nothing (no album selected)
     * @param selectedRow: the position of the View selected in the ListView
     */
    private void displayCoverUI(int selectedRow) 
    {
        if( selectedRow >= 0){
            selectedAlbum = selectedRow;
            Intent i = new Intent(this, CoverUI.class);
            listAlbum.moveTo(selectedAlbum);
            i.putExtra(CoverUI.ALBUM_ID, listAlbum.getInt(ALBUM_ID));
            startSubActivity(i, 0);
        }
    }

}
