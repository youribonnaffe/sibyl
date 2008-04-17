package com.sibyl.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public class AlbumCoverListAdapter extends IconifiedTextListAdapter {

    public AlbumCoverListAdapter(Context context)
    {
        super(context);
    }
    
    public View getView(int position, View convertView, ViewGroup parent) 
    {
        AlbumCoverView ipv;
        if(convertView == null)
        {
            ipv = new AlbumCoverView(mContext, mList.get(position));
        }
        else
        {
            ipv = (AlbumCoverView) convertView;
            ipv.setText(mList.get(position).getText());
            ipv.setIcon(mList.get(position).getIcon());
        }
        return ipv;
    }
}
