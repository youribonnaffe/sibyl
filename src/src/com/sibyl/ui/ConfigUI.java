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
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewInflate;
import android.view.Menu.Item;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

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
    private static final long TIME_LEAP = 500;

    private ISibylservice mService = null;  //core service s'occupant de la lecture
    private EditText mListDir;  // liste des répertoires de musique
    private Button addDir;    // bouton permettant l'ajout de répertoires musiques
    private Button delDir;    // bouton permettant la suppression de répertoires de musiques
    private Button updateMusic; // bouton servant à mettre a jour la base de donnée
    private boolean dirVisible;
    private boolean modeVisible;
    private boolean animVisible;
    private Spinner repeatMusic;
    private Spinner playMode;
    private Spinner coverAnims;

    private ArrayList<String> listFile; // liste des répertoires de musiques /* TODO Utilité de l'objet ?*/
    private MusicDB mdb;    //the database

    private ListView listeMode;
    private ListView listeLibrary;
    private ListView listeCoverAnim;
    private ListView listeCoverManager;

    private long timeLastTouch;

    /**
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        setTitle(R.string.options_title);
        setContentView(R.layout.config);

        launchService(); // lancement du service

        /* Association des élément aux fichiers XML */
        mListDir = (EditText) findViewById(R.id.musicData);
        addDir = (Button) findViewById(R.id.addMusic);
        delDir = (Button) findViewById(R.id.delMusic);
        updateMusic = (Button) findViewById(R.id.updateMusic);
        dirVisible = false;
        modeVisible = false;
        animVisible = false;

        listeMode = (ListView) findViewById(R.id.listConfigMode);
        listeLibrary = (ListView) findViewById(R.id.listConfigLibrary);
        listeCoverAnim = (ListView) findViewById(R.id.listCoverAnim);
        listeCoverManager = (ListView) findViewById(R.id.listCoverManager);

        repeatMusic = (Spinner) findViewById(R.id.repMusic);

        playMode = (Spinner) findViewById(R.id.shuMusic);

        coverAnims = (Spinner) findViewById(R.id.covAnims);

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
        fillData();
    }

    OnItemClickListener mListeCover = new OnItemClickListener()
    {
        public void onItemClick(AdapterView parent, View v, int position, long id)
        {   
            displayAlbumUI();
        }
    };

    OnItemClickListener mListeDir = new OnItemClickListener()
    {
        public void onItemClick(AdapterView parent, View v, int position, long id)
        {   
            dirVisible = !dirVisible;
            mListDir.setVisibility(dirVisible ? View.VISIBLE : View.GONE);
            addDir.setVisibility(dirVisible ? View.VISIBLE : View.GONE);
            delDir.setVisibility(dirVisible ? View.VISIBLE : View.GONE);
            updateMusic.setVisibility(dirVisible ? View.VISIBLE : View.GONE);

        }
    };

    OnItemClickListener mListeMode = new OnItemClickListener()
    {
        public void onItemClick(AdapterView parent, View v, int position, long id)
        {   
            modeVisible = !modeVisible;
            repeatMusic.setVisibility(modeVisible ? View.VISIBLE : View.GONE);
            playMode.setVisibility(modeVisible ? View.VISIBLE : View.GONE);
        }
    };

    OnItemClickListener mListeAnim = new OnItemClickListener()
    {
        public void onItemClick(AdapterView parent, View v, int position, long id)
        {   
            animVisible = !animVisible;
            coverAnims.setVisibility(animVisible ? View.VISIBLE : View.GONE);
        }
    };

    private OnItemSelectedListener mRepeatMusic = new OnItemSelectedListener()
    {
        public void onItemSelected(AdapterView parent, View v, int position, long id)
        {
            try
            {
                mService.setLoopMode(position);
            } catch (DeadObjectException e) { }
        }

        public void onNothingSelected(AdapterView arg0)
        {
        }
    };

    private OnItemSelectedListener mShuffleMode = new OnItemSelectedListener()
    {
        public void onItemSelected(AdapterView parent, View v, int position, long id)
        {
            try
            {
                mService.setPlayMode(position);
            } catch (DeadObjectException doe)
            {
                Log.v(TAG,doe.toString());
            }
        }
        public void onNothingSelected(AdapterView arg0)
        {
        } 
    };

    private OnItemSelectedListener mCoverAnimation = new OnItemSelectedListener()
    {
        public void onItemSelected(AdapterView parent, View v, int position, long id)
        {
            // save animation type for covers
            SharedPreferences.Editor prefs = getSharedPreferences(Music.PREFS, MODE_PRIVATE).edit();
            prefs.putInt("coverAnimType", position);
            prefs.commit();
        }
        public void onNothingSelected(AdapterView arg0)
        {
        } 
    };


    private void fillData()
    {
        String repeatString[] = {(String) getText(R.string.rep_no),
                (String) getText(R.string.rep_one),
                (String) getText(R.string.rep_all)};
        ArrayAdapter<CharSequence> repeatAdapter = new ArrayAdapter<CharSequence>(this, R.layout.spinner_row, repeatString);
        repeatMusic.setAdapter(repeatAdapter);

        String shuffleString[] = { (String) getText(R.string.normal),
                (String) getText(R.string.random)};
        ArrayAdapter<CharSequence> shuffleAdapter = new ArrayAdapter<CharSequence>(this,R.layout.spinner_row, shuffleString);
        playMode.setAdapter(shuffleAdapter);

        String animString[] = { (String) getText(R.string.no_anim),
                (String) getText(R.string.translation_anim),
                (String) getText(R.string.rotation_anim)};
        ArrayAdapter<CharSequence> animAdapter = new ArrayAdapter<CharSequence>(this,R.layout.spinner_row, animString);
        coverAnims.setAdapter(animAdapter);

        coverAnims.setSelection(getSharedPreferences(Music.PREFS, MODE_PRIVATE).getInt("coverAnimType", 1) );
        
        repeatMusic.setOnItemSelectedListener(mRepeatMusic);
        playMode.setOnItemSelectedListener(mShuffleMode);
        coverAnims.setOnItemSelectedListener(mCoverAnimation);

        /* Mise en place des actions correspondantes aux boutons */
        addDir.setOnClickListener(mAddMusic);
        delDir.setOnClickListener(mDelMusic);
        updateMusic.setOnClickListener(mUpdateMusic);

        String[] mode = {(String) getText(R.string.config_mode)};
        ArrayAdapter<String> adapterMode =  new ArrayAdapter<String>(this,R.layout.config_row,R.id.mode,mode);
        listeMode.setAdapter(adapterMode);
        listeMode.setOnItemClickListener(mListeMode);

        String[] library = {(String) getText(R.string.config_library)};
        ArrayAdapter<String> adapterDir =  new ArrayAdapter<String>(this,R.layout.config_row,R.id.mode,library);
        listeLibrary.setAdapter(adapterDir);      
        listeLibrary.setOnItemClickListener(mListeDir);

        String[] anims = {(String) getText(R.string.config_anims)};
        ArrayAdapter<String> adapterAnims =  new ArrayAdapter<String>(this,R.layout.config_row,R.id.mode,anims);
        listeCoverAnim.setAdapter(adapterAnims);      
        listeCoverAnim.setOnItemClickListener(mListeAnim);

        String[] covers = {(String) getText(R.string.config_covers)};
        ArrayAdapter<String> adapterCovers =  new ArrayAdapter<String>(this,R.layout.config_row,R.id.mode,covers);
        listeCoverManager.setOnItemClickListener(mListeCover);
        listeCoverManager.setAdapter(adapterCovers);
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
            listFile.add(c.getString(c.getColumnIndex(Music.DIRECTORY.DIR)));
            str += c.getString(c.getColumnIndex(Music.DIRECTORY.DIR))+'\n';
        }
        mListDir.setText(str);
        c.close();
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
            try{
                // vide la playlist
                mService.clear();
                // lancement de la mise à jour dans un thread
                UpdateTask updateTask = new UpdateTask(mdb);
                Thread t = new Thread (updateTask);
                t.start();
            }catch(DeadObjectException doe){
                Log.v(TAG, doe.toString());
            }
        }
    };

    private void displayAlbumUI()
    {
        startSubActivity(new Intent(this, AlbumUI.class), 0);
    }
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
                for(String ext : Music.SUPPORTED_FILE_FORMAT){
                    for(String file : Directory.scanFiles(path, ext))
                    { 
                        //ugly string .mp3
                        mdb.insert(file);                
                    }
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
            repeatMusic.setFocusable(true);
            playMode.setFocusable(true);
            try
            {
                repeatMusic.setSelection(mService.getLooping());
                playMode.setSelection(mService.getPlayMode());

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

    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN
                && (System.currentTimeMillis()-timeLastTouch) < TIME_LEAP){
            // retrieve viewflipper
            ViewInflate inflate = (ViewInflate) getSystemService(Context.INFLATE_SERVICE);
            ViewFlipper vf = (ViewFlipper)((LinearLayout)inflate.inflate(R.layout.easter_egg, null, null)).findViewById(R.id.easter_egg_vf);
            // set animations
            vf.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
            vf.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
            vf.startFlipping();
            // display dialog
            new AlertDialog.Builder(this)
            .setCancelable(true)
            .setTitle(R.string.easter_egg_title)
            .setView(vf)
            .show();
        }else{
            timeLastTouch = System.currentTimeMillis();
        }
        return super.onTouchEvent(event);
    }

}
