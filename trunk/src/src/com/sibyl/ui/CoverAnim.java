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

import android.view.animation.Animation;
import android.graphics.Camera;
import android.view.animation.Transformation;
import android.graphics.Matrix;

/**
 * Class handling animation for covers:
 *  a rotation of 90¡ of the image is done
 *
 */
public class CoverAnim extends Animation {

    private float mPosX; //center of the rotation (x coordinate)
    private float mPosY; //center of the rotation (y coordinate)
    private boolean mReversed;//is the rotation done in the reversed sense
                    //if true: second part of the animation will be done (rot from -90¡ to 0¡)
                    //else: first part is done (rot from 0¡ to 90¡)
    private Camera mCamera;

    /**
     * Constructor of CoverAnim: constructs a new animation
     *  if reversed is true: second part of the animation will be done (rot from -90¡ to 0¡)
     *  else: first part is done (rot from 0¡ to 90¡)
     * 
     * @param posX      X position of the center of the rotation
     * @param posY      Y position of the center of the rotation
     * @param reversed  sense of rotation of the animation
     */
    public CoverAnim(float posX, float posY, boolean reversed) {
        mPosX = posX;
        mPosY = posY;
        mReversed = reversed;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        mCamera = new Camera();
    }
    
    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) 
    {
        float startDegrees = 0.0f;
        float endDegrees = 90.0f;
        if(mReversed) {
            startDegrees=-90.0f;
            endDegrees=0.0f;
        }
        
        Matrix matrix = t.getMatrix();
        
        //do the rotation
        mCamera.save();
        mCamera.rotateY(startDegrees+((endDegrees-startDegrees)*interpolatedTime));
        mCamera.getMatrix(matrix);
        mCamera.restore();
        
        //to keep the image centered
        matrix.preTranslate(-mPosX, -mPosY);
        matrix.postTranslate(mPosX, mPosY);
    }
}
