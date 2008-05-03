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


import java.util.Map;

import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;

import com.sibyl.ui.CoverAnim;
/**
 * SubClass of ImageView supporting animations when changing the image
 * Animations can be deactivated, but are active by default
 * 
 */
public class AnimatedCover extends ImageView {

    private Handler animHandler;   // handler for managing the end of the first part of the animation
    private Drawable nextDrawable; // next image to be drawn when animation is finished
    private boolean animationActivated; //is the animation activated? 
                //if true an animation is done when changing image
    private int sense;//sense of the animation: next or previous image
    
    public static int NEXT = -1;
    public static int PREV = 1;
    public static int NO_ANIM = 0;
    
    //constructors needed by main.xml else exception
    public AnimatedCover(Context c)
    {
        super(c);
        initWidget();
    }
    
    public AnimatedCover(Context context, AttributeSet attrs, Map inflateParams)
    {
        super(context, attrs, inflateParams);
        initWidget();
    }
    
    public AnimatedCover(Context context, AttributeSet attrs, Map inflateParams, int defStyle)
    {
        super(context, attrs, inflateParams, defStyle);
        initWidget();
    }
    
    private void initWidget()
    {
        animHandler = new Handler();
        animationActivated = true;  
        sense = NEXT;
    }
    
    /** 
     * Activate or deactivate animations
     * @param a     if true, animations are activated
     */
    public void setAnimationActivated(boolean a)
    {
        animationActivated = a;
    }
    
    /**
     * Replaces the current image by the image 'drawable', with an animation, only 
     * if the new image is different than the previous one and
     * if animation is not deactivated (animationActivated=true)
     * 
     * @param drawable  the new image to be displayed
     */
    public void setImageDrawable(Drawable drawable, int aSense)
    {
        Log.v("AnimatedCover", ">>>AnimatedCover::setImageDrawable(): aSense="+aSense);
        if( getDrawable() == drawable )
        {
            return;
        }
        
        if(animationActivated && aSense != NO_ANIM)
        {
            sense = aSense;
            nextDrawable=drawable;
            applyAnimation();
        }
        else 
        {
            setImageDrawableWithoutAnim(drawable);
        }
    }
    
    /**
     * Replaces the current image by the image 'drawable' without animation
     * 
     * @param drawable  the new image to be displayed
     */
    public void setImageDrawableWithoutAnim(Drawable drawable)
    {
        super.setImageDrawable(drawable);
    }
    
    
    /**
     * Creates, initializes and starts the animation
     */
    private void applyAnimation() {
        CoverAnim anim = new CoverAnim( getWidth()/2.0f, getHeight()/2.0f, false, sense);
        anim.setDuration(500);
        anim.setFillAfter(true);
        anim.setInterpolator(new LinearInterpolator());
        anim.setAnimationListener(new AnimListener());
        startAnimation(anim);
    }
    
    /**
     * Class to handle events on the animation (end of animation)
     *
     */
    private class AnimListener implements Animation.AnimationListener 
    {
        public AnimListener()
        {
            
        }
        
        public void onAnimationEnd()
        {
            animHandler.post(new EndAnimation());
        }
        
        public void onAnimationRepeat()
        {
            
        }
        
        public void onAnimationStart()
        {
            
        }
    }
    
    /**
     * Class starting the second part of the animation when the first part is finished
     * This class works in a different thread than the one of the ImageView
     *
     */
    private class EndAnimation implements Runnable
    {
        public void run() {
            setImageDrawableWithoutAnim(nextDrawable);
            CoverAnim anim = new CoverAnim( getWidth()/2.0f, getHeight()/2.0f, true, sense);
            anim.setDuration(500);
            anim.setFillAfter(true);
            anim.setInterpolator(new LinearInterpolator());

            startAnimation(anim);
        }
        
    }
}
