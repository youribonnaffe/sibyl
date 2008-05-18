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
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.sibyl.ui.animation.CoverAnim;
/**
 * SubClass of ImageView supporting animations when changing the image
 * Animations can be deactivated, but are active by default
 * 
 */
public class AnimatedCover extends ImageView {

    private Handler animHandler;   // handler for managing the end of the first part of the animation
    private Drawable nextDrawable; // next image to be drawn when animation is finished
    
    private Move sense;//sense of the animation: next or previous image
    private int animType;//rotation or translation of the cover
    
    public static class AnimationType {
        public static final int NO_ANIM = 0;
        public static final int TRANSLATION = 1;
        public static final int ROTATION = 2;
    }
    
    public enum Move { 
        NEXT, PREV, NO_ANIM;
        public int getValue(){
            switch(this){
                case NEXT  : return 1;
                case PREV : return -1; 
                default : return 0;
            }
        }
    }
    
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
        sense = Move.NEXT;
        animType = AnimationType.TRANSLATION;
    }
    
    /** 
     * Sets the type of animation to play when changing image (nothing, rotation or translation)
     * @param type     the type of animation as listed in AnimationType
     */
    public void setAnimationType(int type)
    {
        animType = type;
    }
    
    /**
     * Replaces the current image by the image 'drawable', with an animation, only 
     * if the new image is different than the previous one and
     * if animation is not deactivated (animationActivated=true)
     * 
     * @param drawable  the new image to be displayed
     */
    public void setImageDrawable(Drawable drawable, Move aSense)
    {
        if( getDrawable() == drawable )
        {
            return;
        }
        
        if(animType != AnimationType.NO_ANIM && aSense != Move.NO_ANIM)
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
        Animation anim;
        if( animType == AnimationType.ROTATION )
        {
            anim = new CoverAnim( getWidth()/2.0f, getHeight()/2.0f, false, sense);
        }
        else 
        {
            anim = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0, 
                Animation.RELATIVE_TO_PARENT, sense.getValue()*-1.0f, Animation.RELATIVE_TO_SELF, 0, 
                Animation.RELATIVE_TO_SELF, 0);
        }
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
            Animation anim;
            if( animType == AnimationType.ROTATION )
            {
                anim = new CoverAnim( getWidth()/2.0f, getHeight()/2.0f, true, sense);
            }
            else {
                anim = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, sense.getValue()*1.0f, 
                    Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_SELF, 0, 
                    Animation.RELATIVE_TO_SELF, 0);
            }
            anim.setDuration(500);
            anim.setFillAfter(true);
            anim.setInterpolator(new LinearInterpolator());

            startAnimation(anim);
        }
        
    }
}
