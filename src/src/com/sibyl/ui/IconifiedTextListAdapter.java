package com.sibyl.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class IconifiedTextListAdapter extends BaseAdapter
{
    protected Context mContext;

    protected List<IconifiedText> mList = new ArrayList<IconifiedText>();
    
    public IconifiedTextListAdapter(Context context)
    {
        mContext = context;
    }
    
    public void add(IconifiedText i)
    {
        mList.add(i);
    }
    
    public void setList(List<IconifiedText> list)
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
        IconifiedTextView ipv;
        if(convertView == null)
        {
            ipv = new IconifiedTextView(mContext, mList.get(position));
        }
        else
        {
            ipv = (IconifiedTextView) convertView;
            ipv.setText(mList.get(position).getText());
            ipv.setIcon(mList.get(position).getIcon());
        }
        return ipv;
    }
    
}

