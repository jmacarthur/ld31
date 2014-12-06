package com.srimech.faultline;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import java.util.Random;

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

public class FaultLineScreen extends SurfaceView implements View.OnTouchListener
{
    private double d = 0;
    private int animatingRow = -1;
    private int animatingColumn = -1;
    private int animationProgress = -1;
    private int animationType = 0;
    private GameThread loop;
    private float dragStartX = 0;
    private float dragStartY = 0;
    private int cellContents[][];
    private int wallType[][];
    private final int PLAYER = 1;

    private final int GSX = 5;
    private final int GSY = 8;

    private final int SLIDE_RIGHT = 1;
    private final int SLIDE_DOWN = 2;
    private final int SLIDE_LEFT = 3;
    private final int SLIDE_UP = 4;

    Bitmap wallBitmaps[];

    private Bitmap loadImage(int index)
    {
	Resources r = this.getContext().getResources();
	Drawable wall = r.getDrawable(index);
	Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);	
	Canvas bitmapCanvas = new Canvas(bitmap);
	wall.setBounds(0, 0, 64, 64);
	wall.draw(bitmapCanvas);
	return bitmap;
    }

    private void setup() {
	wallBitmaps = new Bitmap[16]; // NSEW
	wallBitmaps[0] = loadImage(R.drawable.brickwall);
	wallBitmaps[1] = loadImage(R.drawable.brickwall_w);
	wallBitmaps[2] = loadImage(R.drawable.brickwall_e);
	wallBitmaps[3] = loadImage(R.drawable.brickwall_ew);
	wallBitmaps[4] = loadImage(R.drawable.brickwall_s);
	wallBitmaps[5] = loadImage(R.drawable.brickwall_sw);
	wallBitmaps[6] = loadImage(R.drawable.brickwall_se);
	wallBitmaps[7] = loadImage(R.drawable.brickwall_sew);
	wallBitmaps[8] = loadImage(R.drawable.brickwall_n);
	wallBitmaps[9] = loadImage(R.drawable.brickwall_ne);
	wallBitmaps[10] = loadImage(R.drawable.brickwall_nw);
	wallBitmaps[11] = loadImage(R.drawable.brickwall_new);
	wallBitmaps[12] = loadImage(R.drawable.brickwall_ns);
	wallBitmaps[13] = loadImage(R.drawable.brickwall_nse);
	wallBitmaps[14] = loadImage(R.drawable.brickwall_nsw);
	wallBitmaps[15] = loadImage(R.drawable.brickwall_nsew);


	setOnTouchListener(this);
	cellContents = new int[GSX][GSY];
	wallType = new int[GSX][GSY];

	Random rn = new Random();

	for(int x=0;x<GSX;x++) {
	    for(int y=0;y<GSY;y++) {
		cellContents[x][y] = 0;
		wallType[x][y] = rn.nextInt(16);
	    }
	}
	cellContents[0][0] = PLAYER;
        loop = new GameThread(this);
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
	if(animationProgress >= 0) animationProgress += 1;
	if(animationProgress == 64) { 
	    finishAnimation();
	}
    }

    void rotateCellContentsRight(int row) {
	int temp = getCellContents(GSX-1, row);
	int tempWall = getWallType(GSX-1, row);
	for(int x=GSX-1;x>0;x--) {
	    cellContents[x][row] = cellContents[x-1][row];
	    wallType[x][row] = wallType[x-1][row];
	}
	cellContents[0][row] = temp;
	wallType[0][row] = tempWall;
    }
    void rotateCellContentsLeft(int row) {
	int temp = getCellContents(0, row);
	int tempWall = getWallType(0, row);
	for(int x=0;x<GSX-1;x++) {
	    cellContents[x][row] = cellContents[x+1][row];
	    wallType[x][row] = wallType[x+1][row];
	}
	cellContents[GSX-1][row] = temp;
	wallType[GSX-1][row] = tempWall;
    }   
    void rotateCellContentsDown(int column) {
	int temp = getCellContents(column, GSY-1);
	int tempWall = getWallType(column, GSY-1);
	for(int y=GSY-1;y>0;y--) {
	    cellContents[column][y] = cellContents[column][y-1];
	    wallType[column][y] = wallType[column][y-1];
	}
	cellContents[column][0] = temp;
	wallType[column][0] = tempWall;
    }
    void rotateCellContentsUp(int column) {
	int temp = getCellContents(column, 0);
	int tempWall = getWallType(column, 0);
	for(int y=0;y<GSY-1;y++) {
	    cellContents[column][y] = cellContents[column][y+1];
	    wallType[column][y] = wallType[column][y+1];
	}
	cellContents[column][GSY-1] = temp;
	wallType[column][GSY-1] = tempWall;
    }

    void finishAnimation() {
	// Move any contents
	switch(animationType) {
	case SLIDE_RIGHT:
	    rotateCellContentsRight(animatingRow); break;
	case SLIDE_LEFT:
	    rotateCellContentsLeft(animatingRow); break;
	case SLIDE_UP:
	    rotateCellContentsUp(animatingColumn); break;
	case SLIDE_DOWN:
	    rotateCellContentsDown(animatingColumn); break;
	}
	animationProgress = -1;
	animatingRow = -1;
	animatingColumn = -1;
	loop.setDelay(1000);
	animationType = 0;
    }

    public boolean onTouch(View v, MotionEvent me) {
	if (animationProgress != -1) {
	    return true; // We don't care about any events when animating
	}
	if(me.getAction() == MotionEvent.ACTION_DOWN) {
	    dragStartX = me.getX(0);
	    dragStartY = me.getY(0);
	}
	else if(me.getAction() == MotionEvent.ACTION_MOVE) {
	    float dx = me.getX(0) - dragStartX;
	    float dy = me.getY(0) - dragStartY;
	    if(dx > 64 && Math.abs(dy)<32) {
		startSlideRow((int) (dragStartY / 64), SLIDE_RIGHT);
		return true;
	    }
	    if(dx < -64 && Math.abs(dy)<32) {
		startSlideRow((int) (dragStartY / 64), SLIDE_LEFT);
		return true;
	    }
	    else if(dy > 64 && Math.abs(dx)<32) {
		startSlideCol((int) (dragStartX / 64), SLIDE_DOWN);
		return true;
	    }
	    else if(dy < -64 && Math.abs(dx)<32) {
		startSlideCol((int) (dragStartX / 64), SLIDE_UP);
		return true;
	    }
	}
	return true;
    }

    public void startSlideRow(int row, int type)
    {
	animatingRow = row;
	animationProgress = 0;
	animationType = type;
	loop.setDelay(50);
	loop.interrupt();	
    }

    public void startSlideCol(int column, int type)
    {
	animatingColumn = column;
	animationProgress = 0;
	animationType = type;
	loop.setDelay(50);
	loop.interrupt();	
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

	for(int gx=0; gx<GSX; gx++) {
	    if(animatingColumn == gx) continue;
	    for(int gy=0; gy<GSY; gy++) {
		if(animatingRow == gy) continue;
		drawTile(canvas, gx, gy, gx*64, gy*64);
	    }
	}

	if(animatingColumn >= 0) {
	    for(int gy=-1;gy<GSY;gy++) {
		int dir = animationType == SLIDE_DOWN?1:-1;
		drawTile(canvas, animatingColumn, (gy+GSY)%GSY, animatingColumn*64,gy*64+animationProgress*dir);
	    }
	}
	if(animatingRow >= 0) {
	    for(int gx=-1;gx<GSX;gx++) {
		int dir = animationType == SLIDE_RIGHT?1:-1;
		drawTile(canvas, (gx+GSX)%GSX, animatingRow, gx*64+animationProgress*dir,animatingRow*64);
	    }
	}
    }

    private int getCellContents(int gx, int gy)
    {
	if(gx<0 || gy<0 || gx>=GSX || gy>=GSY) {
	    return 0;
	}
	return cellContents[gx][gy];
    }

    private int getWallType(int gx, int gy)
    {
	if(gx<0 || gy<0 || gx>=GSX || gy>=GSY) {
	    return 0;
	}
	return wallType[gx][gy];
    }

    private void drawTile(Canvas canvas, int gx, int gy, int xpos, int ypos)
    {
	int t = getWallType(gx,gy);
	if (t>15 || t<0) {
	    // This should never happen
	    Log.i("FaultLine", "wallType at "+gx+","+gy+" is "+t);
	    return;
	}
	canvas.drawBitmap(wallBitmaps[t], null, new RectF(xpos,ypos,xpos+64,ypos+64), null);
	int contents = getCellContents(gx,gy);
	if(contents > 0) {
	    Paint redPaint = new Paint();
	    redPaint.setColor(0xffff0000);
	    canvas.drawCircle(xpos+32, ypos+32, 16, redPaint);
	}
    }

}
