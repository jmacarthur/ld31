package com.srimech.faultline;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
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
import java.util.LinkedList;
import java.util.ListIterator;
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

class Entity
{
    public int x = 0;
    public int y = 0;
    public int oldx;
    public int oldy;
    public int type = 0;
    Entity(int x, int y, int type) {
	this.x = x;
	this.y = y;
	oldx = x; oldy = y;
	this.type = type;
    }
    void move(int newx, int newy)
    {
	oldx = x;
	oldy = y;
	x = newx;
	y = newy;
    }
    /* setX/setY should be used when an entity is moved by dungeon rotation, rather than 
       moving itself */
    void setX(int newx) {
	oldx = newx;
	x = newx;
    }
    void setY(int newy) {
	oldy = newy;
	y = newy;
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
    private final int SWORD = 3;
    private final int GSX = 5;
    private final int GSY = 7;

    private final int SLIDE_RIGHT = 1;
    private final int SLIDE_DOWN = 2;
    private final int SLIDE_LEFT = 3;
    private final int SLIDE_UP = 4;
    private final int ROUTE_PLAYER = 5;
    private final int ROUTE_MONSTER = 6;

    private final int SLIDE = 0;
    private final int MOVE = 1;
    private final int MONMOVE = 2;
    private final int LAST_CYCLE = MONMOVE;
    private Paint darkGreyPaint = new Paint();
    private Paint lightBluePaint = new Paint();
    private Paint redPaint = new Paint();
    private final int ANIMATIONLEN = 64;
    private AStar astarSystem;

    private int mode = SLIDE;    
    private Entity[] entities;
    private TextView status;
    private Path arrowPath;
    private LinkedList<Point> route;

    Bitmap wallBitmaps[];
    Bitmap meanieBitmap;
    Bitmap swordBitmap;

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
	swordBitmap = loadImage(R.drawable.sword);
	setOnTouchListener(this);
	cellContents = new int[GSX][GSY];
	wallType = new int[GSX][GSY];
	astarSystem = new AStar(GSX,GSY,wallType);

	entities = new Entity[3];
	entities[0] = new Entity(0,0,PLAYER);
	entities[1] = new Entity(GSX-1,GSY-1,MEANIE);
	entities[2] = new Entity(0,GSY-1,SWORD);

	Random rn = new Random();

	for(int x=0;x<GSX;x++) {
	    for(int y=0;y<GSY;y++) {
		cellContents[x][y] = 0;
		wallType[x][y] = rn.nextInt(16);
	    }
	}
	cellContents[0][0] = PLAYER;
	cellContents[GSX-1][GSY-1] = MEANIE;
	cellContents[0][GSY-1] = SWORD;

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
        darkGreyPaint.setColor(0xff101010);
	lightBluePaint.setColor(0xff7f7fff);
	redPaint.setColor(0xffff0000);
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
	if(animationProgress >= 0) animationProgress += 4;
	if(animationProgress >= ANIMATIONLEN) {
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
	for(int i=0;i<entities.length;i++)
	    if(entities[i].y == row) entities[i].setX((entities[i].x+GSX-dir)%GSX);
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
	for(int i=0;i<entities.length;i++)
	    if(entities[i].x == column) entities[i].setY((entities[i].y+GSY-dir)%GSY);
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
	Log.i("Faultline", "Completed animation; was in mode "+mode);
	mode = (mode + 1) % (LAST_CYCLE+1);
	Log.i("Faultline", "Completed animation; moved to mode "+mode);
	if(mode == 2) 	    startMonMove();
    }

    private void moveEntity(Entity e, int x, int y)
    {
	cellContents[e.x][e.y] = 0;
	e.move(x,y);
	cellContents[x][y] = e.type;
    }

    private void startMonMove()
    {
	startAnimation(ROUTE_MONSTER); // Always start this even if we can't move; it updates the state machine
	// Can the monster move to the player?
	if (canMove(entities[1].x, entities[1].y, entities[0].x, entities[0].y)) {
	    Log.i("FaultLine", "Moving meanie to player (fight!)");
	    route = astarSystem.getRoute();
	    moveEntity(entities[1], entities[0].x, entities[0].y);
	}
	else {
	    // OK, can we move anywhere nearer?
	    int leastDist = 100;
	    int tx = -1;
	    int ty = -1;
	    for(int x=0;x<GSX;x++) {
		for(int y=0;y<GSY;y++) {
		    if(astarSystem.distance[x][y] > 0) {
			int dist = Math.abs(x-entities[0].x)+Math.abs(y-entities[0].y);
			if(dist < leastDist) {
			    tx = x;
			    ty = y;
			    leastDist = dist;
			}
		    }
		}
		// Yes, I know I can move there (so calling canMove is irrelevant) but this
		// also sets the route destination, which we'll need.
	    }
	    if(tx>-1 && canMove(entities[1].x, entities[1].y, tx, ty)) {
		route = astarSystem.getRoute();
		moveEntity(entities[1], tx, ty);
	    } else {
		Log.i("FaultLine", "Meanie can't move to player");
		animationProgress = 64;
	    }

	}
	loop.interrupt();
    }

    private boolean canMove(int fromX, int fromY, int toX, int toY) {
	astarSystem.reset();
	boolean result = astarSystem.routeable(fromX, fromY, toX, toY);
	if(result) route = astarSystem.getRoute();
	return result;
    }

    private void startMove(int x, int y) {
	if(canMove(entities[0].x, entities[0].y, x, y)) {
	    startAnimation(ROUTE_PLAYER);
	    Log.i("FaultLine", "Move OK from "+entities[0].x+","+entities[0].y+" to "+x+","+y);
	    moveEntity(entities[0], x, y);
	    loop.interrupt();
	}
	else {
	    Log.i("FaultLine", "Move denied from "+entities[0].x+","+entities[0].y+" to "+x+","+y);
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
	    }
	    if(dx < -64 && Math.abs(dy)<32) {
		startSlideRow((int) (dragStartY / 64), SLIDE_LEFT);
	    }
	    else if(dy > 64 && Math.abs(dx)<32) {
		startSlideCol((int) (dragStartX / 64), SLIDE_DOWN);
	    }
	    else if(dy < -64 && Math.abs(dx)<32) {
		startSlideCol((int) (dragStartX / 64), SLIDE_UP);
	    }
	}
	return true;
    }

    public void startAnimation(int type)
    {
	animationProgress = 0;
	animationType = type;
	loop.setDelay(10);
	loop.interrupt();
    }

    public void startSlideRow(int row, int type)
    {
	animatingRow = row;
	startAnimation(type);
    }

    public void startSlideCol(int column, int type)
    {
	animatingColumn = column;
	startAnimation(type);
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
	if(animationType == ROUTE_PLAYER) {
	    if(route != null) {
		ListIterator<Point> listIterator = route.listIterator();
		while (listIterator.hasNext()) {
		    Point pr = listIterator.next();
		    canvas.drawCircle(pr.x*64+32, pr.y*64+32, 8, lightBluePaint);
		}
	    }
	    int xpos = (entities[0].oldx * (ANIMATIONLEN-animationProgress) + entities[0].x * (animationProgress));
	    int ypos = (entities[0].oldy * (ANIMATIONLEN-animationProgress) + entities[0].y * (animationProgress));
	    drawPlayer(canvas, xpos, ypos);
	}
	if(animationType == ROUTE_MONSTER) {
	    if(route != null) {
		ListIterator<Point> listIterator = route.listIterator();
		while (listIterator.hasNext()) {
		    Point pr = listIterator.next();
		    canvas.drawCircle(pr.x*64+32, pr.y*64+32, 8, lightBluePaint);
		}
	    }
	    int xpos = (entities[1].oldx * (ANIMATIONLEN-animationProgress) + entities[1].x * (animationProgress));
	    int ypos = (entities[1].oldy * (ANIMATIONLEN-animationProgress) + entities[1].y * (animationProgress));
	    drawMeanie(canvas, xpos, ypos);
	}

	// Draw 'grid'
	for(int gx=0; gx<=GSX; gx++) {
	    for(int gy=0; gy<=GSY; gy++) {
		canvas.drawCircle(gx*64, gy*64, 4, darkGreyPaint);
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
    private void drawSword(Canvas canvas, int xpos, int ypos) {
	canvas.drawBitmap(swordBitmap, null, new RectF(xpos,ypos,xpos+64,ypos+64), null);
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
	    if(contents == PLAYER && animationType != ROUTE_PLAYER) {
		drawPlayer(canvas, xpos, ypos);
	    }
	    else if(contents == MEANIE && animationType != ROUTE_MONSTER) {
		drawMeanie(canvas, xpos, ypos);
	    }
	    else if(contents == SWORD) {
		drawSword(canvas, xpos, ypos);
	    }
	}
    }
}
