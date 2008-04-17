package com.sibyl.ui;

import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AlbumCoverView extends IconifiedTextView {
    
    public AlbumCoverView(Context context, IconifiedText aIconifiedPath)
    {
        super(context);
        this.setOrientation(HORIZONTAL);
               
        mText = new TextView(context);
        mText.setText(aIconifiedPath.getText());
        mText.setTextSize(20);
        
        addView(mText, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,(float)1.0));
        
        mIcon = new ImageView(context);
        mIcon.setAdjustViewBounds(true);
        mIcon.setMaxHeight(80);
        mIcon.setMaxWidth(80);
        mIcon.setImageDrawable(aIconifiedPath.getIcon());
        mIcon.setPadding(0, 5, 5, 0);
        
        addView(mIcon, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,(float)0.0));
        

    }

}
