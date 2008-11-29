package com.sibyl.ui;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;


public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<String> listImg;
    
        public ImageAdapter(Context c) {
            mContext = c;
            listImg = new ArrayList<String>();            
        }

        public void add(String path){
            listImg.add(path);
        }
        public int getCount() {
            return listImg.size();
        }

        public Object getItem(int position) {
            return listImg.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(mContext);
            i.setLayoutParams(new GridView.LayoutParams(90, 90));
            i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            i.setAdjustViewBounds(true);
            i.setImageDrawable(Drawable.createFromPath(listImg.get(position)));
            return i;
        }

        public float getAlpha(boolean focused, int offset) {
            return Math.max(0, 1.0f - (0.2f * Math.abs(offset)));
        }

        public float getScale(boolean focused, int offset) {
            return Math.max(0, 1.0f - (0.2f * Math.abs(offset)));
        }
}