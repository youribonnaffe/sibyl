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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sibyl.Directory;
import com.sibyl.ISibylservice;
import com.sibyl.Music;
import com.sibyl.MusicDB;
import com.sibyl.R;
import com.sibyl.Sibylservice;

/**
 * 
 * Activité - Interface Utilisateur - permettant la configuration des options de Sibyl
 * On y gère en autre, la mise à jour des répertoires de musique, le mode de musique joué.
 * 
 * @author Sibyl-project
 *
 */
public class ConfigUI extends Activity
{
    private static final int BACK_ID = Menu.FIRST; // Elément du ménu permettant l'arret de l'activité
    private static final String TAG = "CONFIG"; // Tag servant au débugage
    
    private ISibylservice mService = null;  //core service s'occupant de la lecture
    private EditText mListDir;  // liste des répertoires de musique
    private Button addDir;    // bouton permettant l'ajout de répertoires musiques
    private Button delDir;    // bouton permettant la suppression de répertoires de musiques
    private Button updateMusic; // bouton servant à mettre a jour la base de donnée
    private TextView repeatMode;    // Affichage du mode de répétition en cours
    private Button repeatMusicNo; // bouton permettant de changer de mode de lecture
    private Button repeatMusicOne; // bouton permettant de changer de mode de lecture
    private Button repeatMusicAll; // bouton permettant de changer de mode de lecture
    
    private TextView playMode;  // Affichage du mode de lecture en cours
    private Button shuffleMode; // bouton permettant de changer passer en mode aléatoire
    private Button normalMode; // bouton permettant de changer passer en mode normal
    
    private ArrayList<String> listFile; // liste des répertoires de musiques /* TODO Utilité de l'objet ?*/
    private MusicDB mdb;    //the database
    
    /**
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        setContentView(R.layout.config);
        
        launchService(); // lancement du service
        
        /* Association des élément aux fichiers XML */
        mListDir = (EditText) findViewById(R.id.musicData);
        addDir = (Button) findViewById(R.id.addMusic);
        delDir = (Button) findViewById(R.id.delMusic);
        updateMusic = (Button) findViewById(R.id.updateMusic);
        repeatMusicNo = (Button) findViewById(R.id.repMusicNo);
        repeatMusicOne = (Button) findViewById(R.id.repMusicOne);
        repeatMusicAll = (Button) findViewById(R.id.repMusicAll);
        repeatMode = (TextView) findViewById(R.id.repMode);
        
        playMode = (TextView) findViewById(R.id.playMode);
        normalMode = (Button) findViewById(R.id.normal);
        shuffleMode = (Button) findViewById(R.id.random);
        
        /* Mise en place des actions correspondantes aux boutons */
        addDir.setOnClickListener(mAddMusic);
        delDir.setOnClickListener(mDelMusic);
        updateMusic.setOnClickListener(mUpdateMusic);
        repeatMusicNo.setOnClickListener(mRepeatMusicNo);
        repeatMusicOne.setOnClickListener(mRepeatMusicOne);
        repeatMusicAll.setOnClickListener(mRepeatMusicAll);
        
        shuffleMode.setOnClickListener(mShuffleMode);
        normalMode.setOnClickListener(mNormalMode);
        
        Log.v(TAG,"ICI");
        
        /* connexion à la base de données */
        try
        {
            mdb = new MusicDB(this);
            updateMusic.setFocusable(true);
        }
        catch(SQLiteDiskIOException ex)
        {
            Log.v(TAG, ex.toString());
        }
    }
    
    /**
     * Appellé quand l'UI devient visible à l'utilisateur
     */
    @Override
    protected void onStart()
    {
        super.onStart();
        
        /* MAJ de la liste des répertoires */
        listFile = new ArrayList<String>();
        Cursor c = mdb.getDir();
        String str ="";
        while (c.next())
        {
            listFile.add(c.getString(0));
            str += c.getString(0)+'\n';
            //Log.v(TAG,"ADD "+c.getString(0));
        }
        mListDir.setText(str);
    }
    
    /**
     * Lance l'UI permettant d'ajouter un répertoire de musiques
     */
    private void displayAddDir() 
    {
        Intent i = new Intent(this, AddDirUI.class);
        startSubActivity(i, 0);
    }
    
    /**
     * Lance l'UI permettant la suppréssion d'un répertoire de musiques
     */
    private void displayDelDir() 
    {
        Intent i = new Intent(this, DelDirUI.class);
        startSubActivity(i, 0);
    }
    
    /**
     * Écouteur placé sur le bouton permettant l'ajout de répertoires de musiques
     */
    private OnClickListener mAddMusic = new OnClickListener()
    {
        public void onClick(View v)
        {
            displayAddDir();
        }
    };
    
    /**
     * Écouteur placé sur le bouton permettant la suppression de répertoires de musiques
     */
    private OnClickListener mDelMusic = new OnClickListener()
    {
        public void onClick(View v)
        {
            displayDelDir();
        }
    };
    
    /**
     * Écouteur placé sur le bouton permettant la mise à jour de la bibliothèque de musiques
     */
    private OnClickListener mUpdateMusic = new OnClickListener()
    {
        public void onClick(View v)
        {
            // lancement de la mise à jour dans un thread
            UpdateTask updateTask = new UpdateTask(mdb);
            Thread t = new Thread (updateTask);
            t.start();
        }
    };
    
    /**
     * Classe gérant la mise a jour de la collection de musiques
     * Gestion faite dans un thread d'ou l'utilisation de Runnable
     * 
     * @author Sibyl-project
     *
     */
    private class UpdateTask implements Runnable 
    {
        private static final String TAG = "UPDATETASK"; /** TAG servant au débugage */
        private MusicDB mDB; /** Base de donnée */
        
        /**
         * Constructeur
         * 
         * @param mDB base de donnée contenant la collection à mettre à jour
         */
        public UpdateTask(MusicDB mDB)
        {
            this.mDB=mDB;
        }
        
        /**
         * Méthode mettant effectivement à jour la collection
         * 
         * @param path Répertoire contenant des musiques à ajouter à la collection
         */
        private void fillBD (String path)
        {
            // get all mp3 files in path
            try
            {
                // long t = System.currentTimeMillis();
                
                // get all mp3 files in path & insert them in the database
                for(String file : Directory.scanFiles(path, ".mp3"))
                { 
                    //ugly string .mp3
                    mdb.insert(file);                
                }
                
                //Log.v(TAG, "temps "+(System.currentTimeMillis()-t)); // Permet de calculer le temps d'ajout
                
            }catch(SQLiteException sqle){
                Log.v(TAG, sqle.toString());
                // warn user
            }catch(FileNotFoundException fnfe){
                Log.v(TAG, fnfe.toString());
                // warn user
            }catch(IOException ioe){
                Log.v(TAG, ioe.toString());
                // warn user
            }
        }
        
        /**
         * Moteur de la tâche permettant la mise à jour de la collection
         */
        public void run() 
        {
            /* TODO Peut ne pas être judicieux de faire ainsi (on vide tout et on remet tout) */
            mDB.clearDB(); 
            for(int i=0; i<listFile.size(); i++)
            {
                fillBD(listFile.get(i));
            }
        }
    };
    
    
    /**
     * Écouteur placé sur le bouton permettant le changement de mode de lecture : sans répétition
     */
    private OnClickListener mRepeatMusicNo = new OnClickListener()
    {
        public void onClick(View v)
        {
            try 
            {
                repeatMode.setText(R.string.rep_no);
                mService.setLoopMode(Music.LoopMode.NO_REPEAT);
            } catch (DeadObjectException e) { }
        }
    };
    
    /**
     * Écouteur placé sur le bouton permettant le changement de mode de lecture : répétition de la chanson en cours
     */
    private OnClickListener mRepeatMusicOne = new OnClickListener()
    {
        public void onClick(View v)
        {
            try 
            {
                repeatMode.setText(R.string.rep_one);
                mService.setLoopMode(Music.LoopMode.REPEAT_SONG);
            } catch (DeadObjectException e) { }
        }
    };
    
    /**
     * Écouteur placé sur le bouton permettant le changement de mode de lecture : répétition de la playlist
     */
    private OnClickListener mRepeatMusicAll = new OnClickListener()
    {
        public void onClick(View v)
        {
            try 
            {
                repeatMode.setText(R.string.rep_all);
                mService.setLoopMode(Music.LoopMode.REPEAT_PLAYLIST);
            } catch (DeadObjectException e) { }
        }
    };
    
    /**
     * Écouteur placé sur le bouton permettant le changement de mode de lecture : non aléatoire
     */
    private OnClickListener mShuffleMode = new OnClickListener()
    {
        public void onClick(View v)
        {
            try 
            {
                playMode.setText(R.string.random);
                mService.setPlayMode(Music.Mode.RANDOM);
            } catch (DeadObjectException e) { }
        }
    };
    
    /**
     * Écouteur placé sur le bouton permettant le changement de mode de lecture : aléatoire
     */
    private OnClickListener mNormalMode = new OnClickListener()
    {
        public void onClick(View v)
        {
            try 
            {
                playMode.setText(R.string.normal);
                mService.setPlayMode(Music.Mode.NORMAL);
            } catch (DeadObjectException e) { }
        }
    };
    
    /**
     * lancement du service
     */
    public void launchService() 
    {
        Intent i = new Intent(ConfigUI.this, Sibylservice.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }
    
    /**
     * gestion de la connection et de la deconnexion du service
     */
    private ServiceConnection mConnection = new ServiceConnection()
    {
        /**
         * This is called when the connection with the service has been
         * established, giving us the service object we can use to
         * interact with the service.  We are communicating with our
         * service through an IDL interface, so get a client-side
         * representation of that from the raw service object.
         */
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            mService = ISibylservice.Stub.asInterface((IBinder)service);
            repeatMusicNo.setFocusable(true);
            repeatMusicOne.setFocusable(true);
            repeatMusicAll.setFocusable(true);
            normalMode.setFocusable(true);
            shuffleMode.setFocusable(true);
            try 
            {
                switch (mService.getLooping()) 
                {
                case 0:
                    repeatMode.setText(R.string.rep_no);
                    break;
                case 1:
                    repeatMode.setText(R.string.rep_one);
                    break;
                case 2:
                    repeatMode.setText(R.string.rep_all);
                    break;
                default :
                    repeatMode.setText("");
                    break;
                }
                
                playMode.setText(R.string.normal);
                
            } catch (DeadObjectException doe) 
            {
                Log.v(TAG,doe.toString());
            }
        }

        /**
         * This is called when the connection with the service has been
         * unexpectedly disconnected -- that is, its process crashed. 
         */
        public void onServiceDisconnected(ComponentName className)
        {
            mService = null;
                        
        }
    };
    
    /**
     * Création du menu et ajout des différentes options
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, BACK_ID, R.string.menu_back);
        return true;
    }
    
    /**
     * Appellé à la destruction de l'activité
     */
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        unbindService(mConnection);
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
        case BACK_ID:
            finish();
            break;
        }
        return true;
    }

}
