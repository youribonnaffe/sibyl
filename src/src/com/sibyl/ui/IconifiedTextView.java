package com.sibyl.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IconifiedTextView extends LinearLayout
{
    private TextView mText;
    private ImageView mIcon;
    
    public IconifiedTextView(Context context, IconifiedText aIconifiedPath)
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