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

package com.sibyl.ui.animation;

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

    private float posX; //center of the rotation (x coordinate)
    private float posY; //center of the rotation (y coordinate)
    private boolean reversed;//is the rotation done in the reversed sense
                    //if true: second part of the animation will be done (rot from -90¡ to 0¡)
                    //else: first part is done (rot from 0¡ to 90¡)
    private Camera camera;
    private int sense;

    /**
     * Constructor of CoverAnim: constructs a new animation
     *  if reversed is true: second part of the animation will be done (rot from -90¡ to 0¡)
     *  else: first part is done (rot from 0¡ to 90¡)
     * 
     * @param aPosX      X position of the center of the rotation
     * @param aPosY      Y position of the center of the rotation
     * @param aReversed  part of the animation (1st half or second half)
     * @param aSense     sense of rotation (for previous or next image): value: 1 or -1
     */
    public CoverAnim(float aPosX, float aPosY, boolean aReversed, int aSense) {
        posX = aPosX;
        posY = aPosY;
        reversed = aReversed;
        sense = aSense;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        camera = new Camera();
    }
    
    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) 
    {
        float startDegrees = 0.0f;
        float endDegrees = sense*90.0f;
        if(reversed) {
            startDegrees=sense*(-90.0f);
            endDegrees=0.0f;
        }
        
        Matrix matrix = t.getMatrix();
        
        //do the rotation
        camera.save();
        camera.rotateY(startDegrees+((endDegrees-startDegrees)*interpolatedTime));
        camera.getMatrix(matrix);
        camera.restore();
        
        //to keep the image centered
        matrix.preTranslate(-posX, -posY);
        matrix.postTranslate(posX, posY);
    }
}
