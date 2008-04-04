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
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu.Item;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sibyl.MusicDB;
import com.sibyl.R;

public class AddDirUI extends ListActivity
{
    private static final int ADD_ID = Menu.FIRST;
    private static final int BACK_ID = Menu.FIRST +1;

    private static final String TAG = "ADD_DIR";
    private IconifiedPathListAdapter ipla;
    private String parent;
    private String path;

    private MusicDB mdb;    //the database

    public void onCreate(Bundle icicle) 
    {
    	super.onCreate(icicle);
        setContentView(R.layout.add_dir);
        path = "/data/music";
        setTitle(getText(R.string.dir)+path);
    	try
    	{
            ipla = fillBD(path);
            setListAdapter(ipla);
    	    mdb = new MusicDB(this);
    	}
    	catch(SQLiteDiskIOException ex)
    	{
    	    Log.v(TAG, ex.toString());
    	}   
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
        
        if( ipla.isSelectable(position))
        {
            String lPath = ((IconifiedPath) ipla.getItem(position)).getText();
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
            setTitle("Répertoire : "+path);
        }
        else
        {
            setSelection(0);
        }
    }

//  Fill the table Song with mp3 found in path
    private IconifiedPathListAdapter fillBD (String path)
    {
        IconifiedPathListAdapter str = new IconifiedPathListAdapter(this);
        // get all mp3 files in path
        File dir = new File(path);
        String parent = dir.getParent();
        // might have to check !=null
        if (parent != null)
        {
            this.parent = parent;
            str.add(new IconifiedPath("..",getResources().getDrawable(R.drawable.folder)));
        }
        
        //Log.v(TAG,"taille dir : "+dir.list().length);
        for(File f: dir.listFiles())
        {
            try
            {
            	if (f.isDirectory())
            	{
            	    str.add(new IconifiedPath(f.getPath(),getResources().getDrawable(R.drawable.folder)));
            	}
                else
                {
                    if(f.getName().endsWith(".mp3"))
                    {
                        str.add(new IconifiedPath(f.getPath(),getResources().getDrawable(R.drawable.audio),false));
                    }
                }
            }
            catch(SQLiteException sqle)
            {
                Log.v(TAG,sqle.toString());
            }
        }
        return str;
    }

    @Override
    protected void onDestroy() 
    {
        super.onDestroy();     
    }

    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_ID, R.string.menu_add);
        menu.add(0, BACK_ID, R.string.menu_back);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, Item item) 
    {
        super.onMenuItemSelected(featureId, item);
        switch(item.getId()) 
        {
        case ADD_ID:
            Log.v(TAG, "Insert");
            Log.v(TAG,"Ajout dans la table du repertoire :"+path);
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

class IconifiedPath implements Comparable<IconifiedPath>
{
    private String mText ="";
    private Drawable mIcon;
    private boolean mSelectable = true;
    
    public IconifiedPath(String text, Drawable img)
    {
        mIcon = img;
        mText = text;
    }
    
    public IconifiedPath(String text, Drawable img, boolean selectable)
    {
        mIcon = img;
        mText = text;
        mSelectable = selectable;
    }
    
    public boolean isSelectable()
    {
        return mSelectable;
    }
    
    public String getText()
    {
        return mText;
    }
    
    public void setText(String text)
    {
        mText = text;
    }
    
    public void setIcon(Drawable icon)
    {
        mIcon = icon;
    }
    
    public Drawable getIcon()
    {
        return mIcon;
    }
    
    public int compareTo(IconifiedPath iP) 
    {
        if(this.mText !=null)
            return this.mText.compareTo(iP.getText());
        else
            throw new IllegalArgumentException();
    }
}

class IconifiedPathView extends LinearLayout
{
    private TextView mText;
    private ImageView mIcon;
    
    public IconifiedPathView(Context context, IconifiedPath aIconifiedPath)
    {
        super(context);
        
        this.setOrientation(HORIZONTAL);
        
        mIcon = new ImageView(context);
        mIcon.setImageDrawable(aIconifiedPath.getIcon());
        mIcon.setPadding(0, 2, 5, 0);
        
        addView(mIcon, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        
        mText = new TextView(context);
        mText.setText(aIconifiedPath.getText());
        
        addView(mText, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }
    
    public void setText(String texte)
    {
        mText.setText(texte);
    }
    
    public void setIcon(Drawable icon)
    {
        mIcon.setImageDrawable(icon);
    }   
}

class IconifiedPathListAdapter extends BaseAdapter
{
    private Context mContext;

    private List<IconifiedPath> mList = new ArrayList<IconifiedPath>();
    
    public IconifiedPathListAdapter(Context context)
    {
        mContext = context;
    }
    
    public void add(IconifiedPath i)
    {
        mList.add(i);
    }
    
    public void setList(List<IconifiedPath> list)
    {
        mList = list;
    }
    
    public int getCount() 
    {
        return mList.size();
    }

    public Object getItem(int position) 
    {
        return mList.get(position);
    }
    
    public boolean areAllListSelectable()
    {
        return false;
    }
    
    public boolean isSelectable(int position)
    {
        try
        {
            return mList.get(position).isSelectable();
        }
        catch (IndexOutOfBoundsException iobe)
        {
            return super.isSelectable(position);
        }
    }

    public long getItemId(int position) 
    {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) 
    {
        IconifiedPathView ipv;
        if(convertView == null)
        {
            ipv = new IconifiedPathView(mContext, mList.get(position));
        }
        else
        {
            ipv = (IconifiedPathView) convertView;
            ipv.setText(mList.get(position).getText());
            ipv.setIcon(mList.get(position).getIcon());
        }
        return ipv;
    }
    
}
    