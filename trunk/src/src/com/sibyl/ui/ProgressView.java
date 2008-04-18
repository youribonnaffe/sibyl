package com.sibyl.ui;

import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class ProgressView extends View {
    //style
    private Paint ptLine; //the elapsed time
    private Paint ptFull;   //the background of the progressbarre
    private Paint ptBorder; //the border of the progressbarre
    //shape
    private Rect elapse; //the elapsed time
    private Rect fullView; //the full background
    //time
    private int progress;
    private int total;
    //dimension of the view
    private int width;
    private int height;

    private boolean draw;
    /*
     * Initialize the view.
     */
    public ProgressView(Context context, AttributeSet attr, Map inflateParams){
        super(context, attr, inflateParams);
        Log.v("PROGRESS","Create");
        progress = 0;
        total = 1; //not 0 (divide by 0)
        draw = true;
        ptLine = new Paint();
        ptLine.setAntiAlias(true);
        ptLine.setARGB(255, 255, 100, 0); //time elapsed color
        
        ptFull = new Paint();
        ptFull.setAntiAlias(true);
        ptFull.setARGB(255, 255, 210, 50); //background color
        
        ptBorder = new Paint();
        ptBorder.setAntiAlias(true);
        ptBorder.setARGB(255, 255, 255, 255); //border color
        
        elapse = new Rect(0,0,getWidth(),getHeight());
        fullView = new Rect(0,0,0,0);//init at zero because we don't now the dimension of the view for the moment
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        //background and border
        canvas.drawRect(fullView,ptFull );
        //elapsed time.
        elapse.set(1, 1, Math.round(((float)progress)/total*width), height-1);
        canvas.drawRect(elapse, ptLine);
        //border
        canvas.drawLine(0,0, width, 0, ptBorder);
        canvas.drawLine(0,0, 0, height, ptBorder);
        canvas.drawLine(0,height, width, height, ptBorder);
        canvas.drawLine(width,0, width, height, ptBorder);
        //log for debugging
        Log.v("PROGRESS", ((Integer)progress).toString()+"/"+((Integer)total).toString());
        Log.v("PROGRESS_INIT", ((Integer)getWidth()).toString()+"-"+((Integer)height).toString());

        //redraw the view.
        /*Log.v("PROGRESS","draw");
        if( draw){
            invalidate();
        }*/
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
        redraw();
    }
    
    /*
     * set the elapsed time
     * @param prog time elapsed
     */
    public void setProgress(int prog){
        progress = prog;
        redraw();
    }
        
    /*
     * set the total time
     */
    public void setTotal(int tot){
        total = tot;
    }
    
    /*
     * force the redrawing of the view
     */
    public void redraw(){
        draw = true;
        invalidate();
    }
    
    /*
     * stop drawing the view
     */
    public void stopDrawing(){
        draw = false;
    }

}
