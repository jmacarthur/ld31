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
    public GameThread(FaultLineScreen called)
    {
        this.called = called;
        cont = true;
    }
    public void run()
    {
        while(cont) {
            called.updateGameLoop();
            called.lockedDraw();
            try {
                Thread.sleep(1000);
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
    Bitmap wallBitmap;

    private void setup() {
	Resources r = this.getContext().getResources();
	Drawable wall = r.getDrawable(R.drawable.brickwall);
	wallBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
	Canvas bitmapCanvas = new Canvas(wallBitmap);
	wall.setBounds(0, 0, 64, 64);
	wall.draw(bitmapCanvas);

        Thread loop = new GameThread(this);
        loop.start();
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

	canvas.drawBitmap(wallBitmap, null, new RectF(0,0,64,64), null);
    }

}
