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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sibyl.Music;
import com.sibyl.MusicDB;
import com.sibyl.R;

/**
 * Activité - Interface Utilisateur - permettant la suppression de répertoires contenant des musiques
 * L'interface se présente comment une liste d'éléments
 * 
 * @author Sibyl-project
 */
public class DelDirUI extends ListActivity
{

    private static final int DEL_ID = Menu.FIRST; // Elément du ménu permettant la suppression d'un répertoire
    private static final int BACK_ID = Menu.FIRST +1; // Elément du ménu permettant l'arret de l'activité
    private static final String TAG = "DEL_DIR"; // TAG servant au débugage

    private MusicDB mdb;    //the database
    private ArrayList<String> mStrings; // Liste des répertoires de musiques
    
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        setTitle(R.string.del_dir_title);
        setContentView(R.layout.del_dir);
        
        mStrings = new ArrayList<String>();
        
        try
        {
            mdb = new MusicDB(this);
            fillBD();
        }
        catch(SQLiteDiskIOException ex)
        {
            Log.v(TAG, ex.toString());
        }   
        
        setListAdapter(new ArrayAdapter<String>(this,R.layout.del_dir_row,R.id.text1, mStrings));
    }
    
    /**
     * Met a jour la liste des répertoires contenant des musiques
     */
    private void fillBD ()
    {
        ArrayList<String> listDir = mStrings;
        Cursor c = mdb.getDir();
        while (c.next())
        {
            listDir.add(c.getString(c.getColumnIndex(Music.DIRECTORY.DIR)));
        }
        c.close();
    }
    
    /**
     * Création du menu et ajout des différentes options
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, DEL_ID, R.string.menu_del);
        menu.add(0, BACK_ID, R.string.menu_back);
        return true;
    }
    
    /**
     * Appellé lorsqu'un élément du ménu est sélectionné.
     * Gère les actions mises sur les éléments du menu
     */
    @Override
    public boolean onMenuItemSelected(int featureId, Item item) 
    {
        super.onMenuItemSelected(featureId, item);
        switch(item.getId()) 
        {
        case DEL_ID:
            int i = getSelectedItemPosition();
            mdb.delDir(mStrings.get(i));
            finish();
            break;
        case BACK_ID:
            finish();
            break;
        }
        return true;
    }
    
    /**
     * Méthode gérant les actions a effectuer en fonction de l'objet de la liste sélectionné
     */
    @Override
    protected void onListItemClick(ListView l, View v, final int position, long id) 
    {
        new AlertDialog.Builder(DelDirUI.this)
                .setIcon(R.drawable.play)
                .setTitle(R.string.dial_deldir)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {
                        int i = position;
                        mdb.delDir(mStrings.get(i));
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
