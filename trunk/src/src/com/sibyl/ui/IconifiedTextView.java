package com.sibyl.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IconifiedTextView extends LinearLayout
{
    protected TextView mText;
    protected ImageView mIcon;
    
    public IconifiedTextView(Context context, IconifiedText aIconifiedPath)
    {
        super(context);
        
        this.setOrientation(HORIZONTAL);
        
        mIcon = new ImageView(context);
        mIcon.setImageDrawable(aIconifiedPath.getIcon());
        mIcon.setPadding(0, 5, 5, 0);
        
        addView(mIcon, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        
        mText = new TextView(context);
        mText.setText(aIconifiedPath.getText());
        mText.setTextSize(20);
        mText.setSingleLine(true);
        
        addView(mText, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }
    
    public IconifiedTextView(Context context){
        super(context);
    }
    
    public void setText(String texte)
    {
        mText.setText(texte);
    }
    
    public void setIcon(Drawable icon)
    {
        mIcon.setImageDrawable(icon);
    }   
    
    public CharSequence getText(){
        return mText.getText();
    }
}