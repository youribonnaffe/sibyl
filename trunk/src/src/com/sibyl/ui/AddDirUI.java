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

import java.io.File;

import android.app.ListActivity;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.widget.ListView;

import com.sibyl.MusicDB;
import com.sibyl.R;

/**
 * Activité - Interface Utilisateur - permettant l'ajout de répertoires contenant des musiques
 * L'interface se présente comment une liste d'éléments
 * Elle se présente comme un navigateur dans l'arboressence d'android
 * 
 * @author Sibyl-project
 *
 */
public class AddDirUI extends ListActivity
{
    private static final int ADD_ID = Menu.FIRST; // Elément du ménu permettant l'ajout d'un répertoire
    private static final int BACK_ID = Menu.FIRST +1; // Elément du ménu permettant l'arret de l'activité
    private static final String TAG = "ADD_DIR"; // TAG servant au débugage
    
    private IconifiedTextListAdapter ipla; // liste des éléments à afficher (texte + icone associée) 
    private String parent;  // répertoire parent
    private String path;    // répertoire courant
    private MusicDB mdb;    //the database
        /* TODO sachant qu'on l'utilise effectivement que dans une 
         * seule méthode, est il judicieux d'en faire une variable à ce 
         * niveau, ou une simple variable locale suffirait*/

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) 
    {
    	super.onCreate(icicle);
        setContentView(R.layout.add_dir);
        
        // répertoire par défaut : /data/musique
        path = "/data/music";
        
        /* Association des élément aux fichiers XML */
        setTitle(getText(R.string.dir)+path);

        /* création du navigateur */
        ipla = fillBD(path);
        
        setListAdapter(ipla); 
        
        /* TODO utile ?*/
    	try
    	{
    	    mdb = new MusicDB(this);
    	}
    	catch(SQLiteDiskIOException ex)
    	{
    	    Log.v(TAG, ex.toString());
    	}   
    }

    /**
     * Méthode gérant les actions a effectuer en fonction de l'objet de la liste sélectionné
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
        if( ipla.isSelectable(position))
        {
            String lPath = ((IconifiedText) ipla.getItem(position)).getText();
            if(lPath == "..")
            {
                path = parent;
                ipla = fillBD(path);
            }
            else
            {
                path = lPath;
                ipla = fillBD(lPath);
            }
        	setListAdapter(ipla);
            setTitle(getText(R.string.dir)+path);
        }
        else
        {
            setSelection(0);
        }
    }

    /**
     * Fill the table Song with mp3 found in path 
     * @param path chemin du répertoire a afficher dans le navigateur
     * @return liste iconnifiée contenant tous les élément a afficher
     */
    private IconifiedTextListAdapter fillBD (String path)
    {
        IconifiedTextListAdapter itlab = new IconifiedTextListAdapter(this);
        
        File dir = new File(path);
        String parent = dir.getParent();
        // might have to check !=null
        if (parent != null)
        {
            this.parent = parent;
            itlab.add(new IconifiedText("..",getResources().getDrawable(R.drawable.folder)));
        }
        Log.v(TAG,"tooooooooyt"+itlab);
        File[] listeFile = dir.listFiles();
        if (listeFile != null)
        {
            for(File f: listeFile)
            {
                if (f.isDirectory())
                {
                    itlab.add(new IconifiedText(f.getPath(),getResources().getDrawable(R.drawable.folder)));
                }
                else
                {
                    if(f.getName().endsWith(".mp3"))
                    {
                        itlab.add(new IconifiedText(f.getPath(),getResources().getDrawable(R.drawable.audio),false));
                    }
                }
            }
        }
        return itlab;
    }

    /**
     * Création du menu et ajout des différentes options
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_ID, R.string.menu_add);
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
        case ADD_ID:
            mdb.insertDir(path);
            finish();
            break;
        case BACK_ID:
            finish();
            break;
        }
        
        return true;
    }
}
    