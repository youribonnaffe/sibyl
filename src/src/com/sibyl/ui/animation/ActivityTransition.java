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
import android.view.animation.Transformation;


public class ActivityTransition extends Animation {

    private boolean mReverse;//sense of the transition: if false, from beginning to the end
    //else from end to beginning

    
    /**
     * Constructor of ActivityTransition: constructs a new transition
     * 
     * @param reversed  sense of the transition
     */
    public ActivityTransition(boolean reverse) {
        mReverse = reverse;

    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) 
    {
        float alpha = interpolatedTime;
        if(!mReverse) 
        {
            alpha = 1 - interpolatedTime;
        }
        t.setAlpha(alpha);
    }

}
