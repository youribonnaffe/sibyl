package com.sibyl.ui;

import android.graphics.drawable.Drawable;

public class IconifiedText implements Comparable<IconifiedText> {
        private String mText ="";
        private Drawable mIcon;
        private boolean mSelectable = true;
        
        public IconifiedText(String text, Drawable img)
        {
            mIcon = img;
            mText = text;
        }
        
        public IconifiedText(String text, Drawable img, boolean selectable)
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
        
        public int compareTo(IconifiedText iP) 
        {
            if(this.mText !=null)
                return this.mText.compareTo(iP.getText());
            else
                throw new IllegalArgumentException();
        }
}
