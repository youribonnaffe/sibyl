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

package com.sibyl.ui.widget;

import java.util.Map;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Class for image buttons without border
 *
 */
public class ImageControl extends ImageView {

    public class ResImageStates 
    {
        public int defaultResId;//image to draw from resource when the control is in default state
        public int selectedResId;//image to draw from resource when the control is in selected state
        public int disabledResId;//image to draw from resource when the control is in disabled state
        
        public ResImageStates(int d, int s, int dis) {
            defaultResId = d;
            selectedResId = s;
            disabledResId = dis;
        }
    }
    
    private ResImageStates[] states;
    private int nbImages;//number of different images, the control can support and switch to
    private int currentImage;//image shown
    private boolean mEnabled;
    
    /**
     * @param context
     */
    public ImageControl(Context context) {
        super(context);
        init();
    }

    /**
     * @param context
     * @param attrs
     * @param inflateParams
     */
    public ImageControl(Context context, AttributeSet attrs, Map inflateParams) {
        super(context, attrs, inflateParams);
        init();
    }

    /**
     * @param context
     * @param attrs
     * @param inflateParams
     * @param defStyle
     */
    public ImageControl(Context context, AttributeSet attrs, Map inflateParams, int defStyle) {
        super(context, attrs, inflateParams, defStyle);
        init();
    }
    
    public void init()
    {
        mEnabled = true;
        setNumberOfImages(1);
    }
    
    public void setNumberOfImages(int n)
    {
        nbImages = n;
        states = new ResImageStates[nbImages];
        currentImage = 0;
    }
    
    public void changeImageUsedTo(int n) 
    {
        currentImage = n-1;
        setImageResource(states[currentImage].defaultResId);
    }
    
    public void setStatesImgFromRes(int[] defaultResId, int[] selectedResId, int[] disabledResId) 
    {
        for(int i=0; i<nbImages; i++)
        {
            states[i] = new ResImageStates(defaultResId[i], selectedResId[i], disabledResId[i]);
        }
    }
    
    public void setStatesImgFromRes(int defaultResId, int selectedResId, int disabledResId ) 
    {
        states[0] = new ResImageStates(defaultResId, selectedResId, disabledResId);
    }
    
    @Override
    public void setSelected(boolean selected)
    {
        super.setSelected(selected);
        if(selected) {
            setImageResource(states[currentImage].selectedResId);
        }
        else {
            setImageResource(states[currentImage].defaultResId);
        }
    }
    
    @Override
    public void setEnabled(boolean enabled) 
    {
        mEnabled = enabled;
        if(!enabled) {
            setImageResource(states[currentImage].disabledResId);
        }
        else {
            setImageResource(states[currentImage].defaultResId);
        }
    }
    
    @Override
    public boolean isEnabled()
    {
        return mEnabled;
    }
    
}
