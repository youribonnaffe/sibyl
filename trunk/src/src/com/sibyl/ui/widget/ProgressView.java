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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
//incompatible SDK1: import android.util.DateUtils;
import android.view.MotionEvent;
import android.view.View;

import com.sibyl.ui.widget.ProgressBarClickable.OnProgressChangeListener;

/*
 * An evoluated progress bar. The progress bar display is update when the progress is incremented.
 * If the View is clicked, the progress is moved to the new position.
 */
public class ProgressView extends View {
    //style
    private Paint ptLine; //the elapsed time
    private Paint ptLine2; //the effect
    private Paint ptLine3; //the effect
    private Paint ptLine4; //the effect
    private Paint ptBorder; //the border of the progress bar
    private Paint ptText; 
    //shape
    private Rect elapse; //the elapsed time
    private Rect fullView2; // the effect
    private Rect fullView3;
    private Rect fullView4;

    //time
    private int progress;
    private int total;
    //dimension of the view
    private final static int size = 20;
    private int width;
    private int height;
    private final String sep = "/";

    private OnProgressChangeListener listener;
    private static final int padding = 1; //width of the border

    /*
     * Initialize the view.
     */
    public ProgressView(Context context, AttributeSet attr){
        super(context, attr);
        progress = 0;
        total = 1; //not 0 (divide by 0)

        ptLine = new Paint();
        ptLine.setAntiAlias(true);
        ptLine.setARGB(255, 255, 120, 40); //time elapsed color

        ptLine2 = new Paint();
        ptLine2.setAntiAlias(true);
        ptLine2.setARGB(30, 255, 255, 255); //effect: white with alpha
        
        ptLine3 = new Paint();
        ptLine3.setAntiAlias(true);
        ptLine3.setARGB(50, 255, 255, 255); //effect: white with alpha
        
        ptLine4 = new Paint();
        ptLine4.setAntiAlias(true);
        ptLine4.setARGB(80, 255, 255, 255); //effect: white with alpha
        
        ptBorder = new Paint();
        ptBorder.setAntiAlias(true);
        ptBorder.setARGB(255, 255, 255, 255); //border color
        
        ptText = new Paint();
        ptText.setAntiAlias(true);
        ptText.setARGB(255, 70, 70, 70); //text color
        ptText.setTextSize(size);
        
        elapse = new Rect(0,0,getWidth(),getHeight());
        fullView2 = new Rect(0,0,getWidth(),getHeight()/2);
        //init at zero because we don't now the dimension of the view for the moment
        fullView3 = new Rect(0,0,0,0);
        fullView4 = new Rect(0,0,0,0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(width != 0){
            String elaps = "";//incompatible SDK1: DateUtils.formatElapsedTime(progress/1000);
            String tot = "";//incompatible SDK1: DateUtils.formatElapsedTime(total/1000);
            //background and border
            //canvas.drawRect(fullView,ptFull );
            canvas.drawARGB(255, 255, 210, 80); 
            //elapsed time.
            elapse.set(1, 1, Math.round(((float)progress)/total*width), height-1); //normal elapsed time
            canvas.drawRect(elapse, ptLine);
            canvas.drawRect(fullView2, ptLine2);
            canvas.drawRect(fullView3, ptLine3);
            canvas.drawRect(fullView4, ptLine4);
            //border
            canvas.drawLine(0,0, width, 0, ptBorder);
            canvas.drawLine(0,0, 0, height, ptBorder);
            canvas.drawLine(0,height, width, height, ptBorder);
            canvas.drawLine(width,0, width, height, ptBorder);
            //be carefull: height and size are not in the same units. height is in pixel and size in something else
            canvas.drawText(elaps, width/2-ptText.measureText(elaps), height/2+size/3, ptText);
            canvas.drawText( sep, width/2, height/2+size/3, ptText);
            canvas.drawText(tot, width/2+ptText.measureText(sep), height/2+size/3, ptText);
        }
    }
    
    /*
     * initialize the progress bar: draw background and border 
     */
    public void initializeProgress(){
        width = getWidth();
        height = getHeight();
        progress = 0;
        fullView2.set(1, 1, width, height/2); //effect: just the upper part
        fullView3.set(1, 1, width, height/3); //effect: just the upper part
        fullView4.set(1, 1, width, height/4); //effect: just the upper part
        invalidate();
    }
    
    /*
     * set the elapsed time
     * @param prog time elapsed
     */
    public void setProgress(int prog){
        progress = prog;
        invalidate();
        //Log.v("PROGRESS","setProgress:"+((Integer)prog).toString()+"/"+((Integer)progress).toString());
    }
        
    /*
     * set the total time
     */
    public void setTotal(int tot){
        total = tot;
        invalidate();
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
                //Log.v("PROGRESS", "Clicked:"+ ((Integer)progress).toString());
                this.setProgress(progress);
                if (listener != null){
                    listener.onProgressChanged(this, progress);
                }
        }
        return true;
    }
}
