package com.srimech.faultline;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
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
    private final int MEANIE = 2;

    private final int GSX = 5;
    private final int GSY = 7;

    private final int SLIDE_RIGHT = 1;
    private final int SLIDE_DOWN = 2;
    private final int SLIDE_LEFT = 3;
    private final int SLIDE_UP = 4;

    private final int SLIDE = 0;
    private final int MOVE = 1;
    private final int MONMOVE = 2;
    private final int LAST_CYCLE = MONMOVE;

    private int mode = SLIDE;    
    private int playerX = 0;
    private int playerY = 0;
    private int meanieX = GSX-1;
    private int meanieY = GSY-1;
    private int temp_astar_map[][] = null;
    private TextView status;
    private Path arrowPath;

    Bitmap wallBitmaps[];
    Bitmap meanieBitmap;

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
	int[] tileList = { R.drawable.brickwall, R.drawable.brickwall_w,
			   R.drawable.brickwall_e, R.drawable.brickwall_ew,
			   R.drawable.brickwall_s, R.drawable.brickwall_sw,
			   R.drawable.brickwall_se, R.drawable.brickwall_sew,
			   R.drawable.brickwall_n, R.drawable.brickwall_nw,
			   R.drawable.brickwall_ne, R.drawable.brickwall_new,
			   R.drawable.brickwall_ns, R.drawable.brickwall_nsw,
			   R.drawable.brickwall_nse, R.drawable.brickwall_nsew };
	for(int i=0;i<16;i++) { wallBitmaps[i] = loadImage(tileList[i]); }

	Resources r = this.getContext().getResources();
	status = (TextView) findViewById(R.id.textView);
	meanieBitmap = loadImage(R.drawable.meanie);
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
	cellContents[GSX-1][GSY-1] = MEANIE;
        loop = new GameThread(this);
        loop.start();

	Matrix rotateMatrix = new Matrix();
	rotateMatrix.setRotate((float)90.0);
	arrowPath = new Path();
	for(int d=0;d<4;d++) {
	    arrowPath.moveTo(16,16);
	    arrowPath.lineTo(0,32);
	    arrowPath.lineTo(-16,16);
	    arrowPath.close();
	    arrowPath.transform(rotateMatrix);
	}
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
	if(animationProgress >= 0) animationProgress += 2;
	if(animationProgress == 64) { 
	    finishAnimation();
	}
    }

    void rotateHorizontal(int row, int start, int stop, int dir)
    {
	int temp = getCellContents(start, row);
	int tempWall = getWallType(start, row);
	for(int x=start;x!=stop;x+=dir) {
	    cellContents[x][row] = cellContents[x+dir][row];
	    wallType[x][row] = wallType[x+dir][row];
	}
	cellContents[stop][row] = temp;
	wallType[stop][row] = tempWall;
	if(playerY == row) playerX = (playerX+GSX-dir)%GSX;
	if(meanieY == row) meanieX = (meanieX+GSX-dir)%GSX;
    }

    void rotateCellContentsRight(int row) {
	rotateHorizontal(row, GSX-1, 0, -1);
    }
    void rotateCellContentsLeft(int row) {
	rotateHorizontal(row, 0, GSX-1, 1);
    }   

    void rotateVertical(int column, int start, int stop, int dir) {
	int temp = getCellContents(column, start);
	int tempWall = getWallType(column, start);
	for(int y=start;y!=stop;y+=dir) {
	    cellContents[column][y] = cellContents[column][y+dir];
	    wallType[column][y] = wallType[column][y+dir];
	}
	cellContents[column][stop] = temp;
	wallType[column][stop] = tempWall;
	if(playerX == column) playerY = (playerY+GSY-dir)%GSY;
	if(meanieX == column) meanieY = (meanieY+GSY-dir)%GSY;
    }

    void rotateCellContentsDown(int column) {
	rotateVertical(column, GSY-1, 0, -1);
    }

    void rotateCellContentsUp(int column) {
	rotateVertical(column, 0, GSY-1, 1);
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
	mode = (mode + 1) % LAST_CYCLE;
	Log.i("Faultline", "Completed animation; moved to mode "+mode);
    }

    private void startMonMove()
    {
	mode = MONMOVE;
	// Can the monster move anywhere?
	if (canMove(meanieX, meanieY, playerX, playerY)) {
	    Log.i("FaultLine", "Moving meanie to player (fight!)");
	    cellContents[meanieX][meanieY] = 0;
	    meanieX = playerX;
	    meanieY = playerY;
	    cellContents[meanieX][meanieY] = MEANIE;
	}
	else {
	    Log.i("FaultLine", "Meanie can't move to player");
	}
	mode = SLIDE;
    }

    private boolean canMove(int fromX, int fromY, int toX, int toY) {
	AStar a = new AStar(GSX,GSY,wallType);
	boolean result = a.routeable(fromX, fromY, toX, toY);
	temp_astar_map = a.distance;
	return result;
    }

    private void startMove(int x, int y) {
	if(canMove(playerX, playerY, x, y)) {
	    Log.i("FaultLine", "Move OK from "+playerX+","+playerY+" to "+x+","+y);
	    cellContents[playerX][playerY] = 0;
	    playerX = x;
	    playerY = y;
	    cellContents[playerX][playerY] = PLAYER;	    
	    loop.interrupt();
	    startMonMove();
	}
	else {
	    Log.i("FaultLine", "Move denied from "+playerX+","+playerY+" to "+x+","+y);
	}
    }

    public boolean onTouch(View v, MotionEvent me) {
	if (animationProgress != -1) {
	    return true; // We don't care about any events when animating
	}
	Log.i("FaultLine", "Touch event in mode "+mode);

	if(me.getAction() == MotionEvent.ACTION_DOWN) {
	    if(mode==SLIDE) {
		dragStartX = me.getX(0);
		dragStartY = me.getY(0);
	    }
	    else if (mode==MOVE) {
		startMove((int)(me.getX(0)/64),(int)(me.getY(0)/64));
	    }
	}
	else if(me.getAction() == MotionEvent.ACTION_MOVE && mode == SLIDE) {
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
	loop.setDelay(10);
	loop.interrupt();
    }

    public void startSlideCol(int column, int type)
    {
	animatingColumn = column;
	animationProgress = 0;
	animationType = type;
	loop.setDelay(10);
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
	    for(int gy=-1;gy<=GSY;gy++) {
		int dir = animationType == SLIDE_DOWN?1:-1;
		drawTile(canvas, animatingColumn, (gy+GSY)%GSY, animatingColumn*64,gy*64+animationProgress*dir);
	    }
	}
	if(animatingRow >= 0) {
	    for(int gx=-1;gx<=GSX;gx++) {
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
    private void drawPlayer(Canvas canvas, int xpos, int ypos) {
	Paint redPaint = new Paint();
	redPaint.setColor(0xffff0000);
	if (mode == MOVE) {
	    Path offsetArrow = new Path();
	    offsetArrow.addPath(arrowPath, xpos+32, ypos+32);
	    canvas.drawPath(offsetArrow, redPaint);
	} else {
	    canvas.drawCircle(xpos+32, ypos+32, 16, redPaint);
	}
    }
    private void drawMeanie(Canvas canvas, int xpos, int ypos) {
	canvas.drawBitmap(meanieBitmap, null, new RectF(xpos,ypos,xpos+64,ypos+64), null);
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
	    if(contents == PLAYER) {
		drawPlayer(canvas, xpos, ypos);
	    }
	    else if(contents == MEANIE) {
		drawMeanie(canvas, xpos, ypos);
	    }
	}
    }
}
