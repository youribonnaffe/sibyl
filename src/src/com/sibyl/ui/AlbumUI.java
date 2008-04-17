package com.sibyl.ui;


import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.sibyl.MusicDB;
import com.sibyl.R;

/*
 * This activity list all the albums and display their cover. The albums are sorted by the artists order.
 * This activity allows the user to manage the cover: search new covers on the web (not implemented),
 * associate cover/album (done in CoverUI)
 */
public class AlbumUI extends ListActivity {

    private static final String TAG = "ALBUMUI";
    private MusicDB mdb;    //the database
    //constants for the cursor: position of the data in the cursor
    private final static int ARTIST = 0;
    private final static int ALBUM = 1;
    private final static int COVER_URL = 2;
    private final static int ALBUM_ID = 3;
    //constant menu
    private static final int BACK_ID = Menu.FIRST;
    private static final int MANUAL_ID = Menu.FIRST+1;
    
    private static Cursor listAlbum;
    private AlbumCoverView selectedAlbumView; //the view selected
    private static int selectedAlbum; // the position of the view selected
    
    @Override
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
    
    /*
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
    
    
    /*
     * Fill the ListView with the association of thealbum and its cover
     */
    private void fillData(){
        
        listAlbum = mdb.getAlbumCover();
        AlbumCoverListAdapter rows = new AlbumCoverListAdapter(this);
        
        startManagingCursor(listAlbum);
        //for each album we associate its cover.
        while(listAlbum.next()){
            //Log.v(TAG,listAlbum.getString(ARTIST)+"-"+listAlbum.getString(ALBUM));
            
            if( listAlbum.isNull(COVER_URL)){
                rows.add(new IconifiedText( listAlbum.getString(ARTIST)+'\n'+listAlbum.getString(ALBUM),
                        getResources().getDrawable(R.drawable.logo))); //default cover
            }
            else{
                rows.add(new IconifiedText( listAlbum.getString(ARTIST)+'\n'+listAlbum.getString(ALBUM),
                        Drawable.createFromPath(listAlbum.getString(COVER_URL)))); //real cover
            }
        }
        //  listAlbum.close();        
        setListAdapter(rows);
        getListView().setSelection(selectedAlbum);
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, BACK_ID, R.string.alb_back);
        menu.add(0, MANUAL_ID, R.string.alb_manual);
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
            case MANUAL_ID:
                displayCoverUI(getListView().getSelectedItemPosition());
                break;
        }
        return true;
    }
    
    /*
     * Launch the CoverUI. It keeps the number of the selected rows.
     * If selectedRow is negative, we do nothing (no album selected)
     * @param selectedRow: the position of the View selected in the ListView
     */
    private void displayCoverUI(int selectedRow) 
    {
        if( selectedRow >= 0){
            selectedAlbum = selectedRow;
            Intent i = new Intent(this, CoverUI.class);
            startSubActivity(i, 0);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, java.lang.String, android.os.Bundle)
     */
    protected void onActivityResult(int requestCode, int resultCode, String data, Bundle extras){
        if(resultCode == RESULT_OK){
            Log.v(TAG, "selected"+((Integer) selectedAlbum).toString());
            listAlbum.requery(); //recover the cursor
            listAlbum.moveTo(selectedAlbum);
            mdb.setCover(listAlbum.getInt(ALBUM_ID), data);
            
            //we get the View if the user click on the ListView. If he uses the Menu, we don't get the view, so we use fillData instead
            if( selectedAlbumView != null){
                selectedAlbumView.setIcon(Drawable.createFromPath(data));
                selectedAlbumView = null; //we reset the selectedView in the end
            }
            else{
                fillData();
            }
        }
    }
    
}
