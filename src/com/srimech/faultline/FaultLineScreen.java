package com.srimech.faultline;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

class GameThread extends Thread
{
    private FaultLineScreen called;
    private boolean cont;
    private int delay;
    public GameThread(FaultLineScreen called)
    {
        this.called = called;
        cont = true;
    }
    public void setDelay(int newDelay) {
	delay = newDelay;
    }
    public void run()
    {
        while(cont) {
            called.updateGameLoop();
            called.lockedDraw();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                // meh
            }
        }
    }
    public void halt()
    {
        cont = false;
    }
}

public class FaultLineScreen extends SurfaceView
{
    private double d = 0;
    private int animatingRow = -1;
    private int animatingColumn = -1;
    private int animationProgress = -1;
    private GameThread loop;
    Bitmap wallBitmap;

    private void setup() {
	Resources r = this.getContext().getResources();
	Drawable wall = r.getDrawable(R.drawable.brickwall);
	wallBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
	Canvas bitmapCanvas = new Canvas(wallBitmap);
	wall.setBounds(0, 0, 64, 64);
	wall.draw(bitmapCanvas);

        loop = new GameThread(this);
        loop.start();

	// To test animation
	animatingRow = 2;
	animationProgress = 0;
	loop.interrupt();
	loop.setDelay(100);
    }
    
    public FaultLineScreen(Context context) {
        super(context);
        setup();
    }

    public FaultLineScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }
    public FaultLineScreen(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    void updateGameLoop() {
	d += 0.1;
	if(animationProgress >= 0) animationProgress += 1;
	if(animationProgress == 64) { 
	    animationProgress = -1;
	    loop.setDelay(1000);
	}
    }

    public void onDraw(Canvas canvas) {
	draw(canvas);
    }

    public void lockedDraw() {
	SurfaceHolder sh = getHolder();
	Canvas c = sh.lockCanvas();
	if(c != null) {
            draw(c);
            sh.unlockCanvasAndPost(c);
	}
    }
    public void draw(Canvas canvas) {
	int width = getWidth();
	int height = getHeight();
	// Just draws a moving circle at the moment
	Paint p = new Paint();
	p.setColor(0xff0000ff);
	canvas.drawRect(0,0,width,height,p);
	double x = 128 * Math.cos(d);
	double y = 128 * Math.sin(d);
	Paint lightBluePaint = new Paint();
	lightBluePaint.setColor(0xff7f7fff);
	canvas.drawCircle((float)(width/2+x), (float)(height/2+y), 16, lightBluePaint);

	for(int gx=0;gx<4;gx++) {
	    if(animatingColumn == gx) continue;
	    for(int gy=0;gy<6;gy++) {
		if(animatingRow == gy) continue;
		canvas.drawBitmap(wallBitmap, null, new RectF(gx*64,gy*64,gx*64+64,gy*64+64), null);
	    }
	}

	if(animatingColumn >= 0) {
	    for(int gy=-1;gy<6;gy++) {
		canvas.drawBitmap(wallBitmap, null, new RectF(animatingColumn*64,gy*64+animationProgress,animatingColumn*64+64,gy*64+64+animationProgress), null);
	    }
	}
	if(animatingRow >= 0) {
	    for(int gx=-1;gx<4;gx++) {
		canvas.drawBitmap(wallBitmap, null, new RectF(gx*64+animationProgress,animatingRow*64,gx*64+64+animationProgress,animatingRow*64+64), null);
	    }
	}
    }

}
