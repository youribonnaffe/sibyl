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

import com.sibyl.ui.ProgressBarClickable.OnProgressChangeListener;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/*
 * An evoluate progress bar. The progress bar display is update when the progress is incremented.
 * If the View is clicked, the progress is moved to the new position.
 */
public class ProgressView extends View {
    //style
    private Paint ptLine; //the elapsed time
    private Paint ptLine2; //the effect
    private Paint ptFull;   //the background of the progressbarre
    private Paint ptBorder; //the border of the progressbarre
    //shape
    private Rect elapse; //the elapsed time
    private Rect elapse2; // the effect
    private Rect fullView; //the full background
    //time
    private int progress;
    private int total;
    //dimension of the view
    private int width;
    private int height;

    private OnProgressChangeListener listener;
    private static final int padding = 1; //width of the boder

    /*
     * Initialize the view.
     */
    public ProgressView(Context context, AttributeSet attr, Map inflateParams){
        super(context, attr, inflateParams);
        Log.v("PROGRESS","Create");
        progress = 0;
        total = 1; //not 0 (divide by 0)

        ptLine = new Paint();
        ptLine.setAntiAlias(true);
        ptLine.setARGB(255, 255, 120, 40); //time elapsed color
        
        ptLine2 = new Paint();
        ptLine2.setAntiAlias(true);
        ptLine2.setARGB(40, 255, 255, 255); //effect: white with alpha
        
        ptFull = new Paint();
        ptFull.setAntiAlias(true);
        ptFull.setARGB(255, 255, 210, 80); //background color
        
        ptBorder = new Paint();
        ptBorder.setAntiAlias(true);
        ptBorder.setARGB(255, 255, 255, 255); //border color
        
        elapse = new Rect(0,0,getWidth(),getHeight());
        elapse2 = new Rect(0,0,getWidth(),getHeight()*2/5);
        fullView = new Rect(0,0,0,0);//init at zero because we don't now the dimension of the view for the moment
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        //background and border
        canvas.drawRect(fullView,ptFull );
        //elapsed time.
        elapse.set(1, 1, Math.round(((float)progress)/total*width), height-1); //normal elapsed time
        elapse2.set(1, 1, Math.round(((float)progress)/total*width), height/2); //effect: just the upper part
        canvas.drawRect(elapse, ptLine);
        canvas.drawRect(elapse2, ptLine2);
        //border
        canvas.drawLine(0,0, width, 0, ptBorder);
        canvas.drawLine(0,0, 0, height, ptBorder);
        canvas.drawLine(0,height, width, height, ptBorder);
        canvas.drawLine(width,0, width, height, ptBorder);
        //log for debugging
        //Log.v("PROGRESS", ((Integer)progress).toString()+"/"+((Integer)total).toString());
        //Log.v("PROGRESS_INIT", ((Integer)getWidth()).toString()+"-"+((Integer)height).toString());
    }
    
    /*
     * initialize the progressbar: draw background and border 
     */
    public void initializeProgress(){
        width = getWidth();
        height = getHeight();
        progress = 0;
        Log.v("PROGRESS_INIT", ((Integer)getWidth()).toString()+"-"+((Integer)height).toString());
        fullView.set(0,0,width,height);
        invalidate();
    }
    
    /*
     * set the elapsed time
     * @param prog time elapsed
     */
    public void setProgress(int prog){
        progress = prog;
        invalidate();
    }
        
    /*
     * set the total time
     */
    public void setTotal(int tot){
        total = tot;
    }
    
    /*
     * set the adapter
     */
    public void setOnProgressChangeListener(OnProgressChangeListener l) {
        listener = l;
    }

    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                float x_mouse = event.getX() - padding;
                float width = getWidth() - 2*padding;
                int progress = Math.round((float) total * (x_mouse / width));
                if (progress < 0){
                    progress = 0;
                }
                this.setProgress(progress);
                if (listener != null){
                    listener.onProgressChanged(this, progress);
                }
        }
        return true;
    }

}
