/* Return a list of coordinates 
   implemented from Pseudocode on wikipedia! */

/*     Requirements: Function move_cost((x,y)) giving the cost of moving onto a square. Can be 999 to prevent it.
	 gsx, gsy determine the size of the rectangular/cartesian  grid which we can move on.
	 It's assumed that you can't move to x,y coordinates where x<0, y<0, x>= gsx or y>=gsy.

	 astar will return a list of coordinate pairs, from the start point to one short of the end point. */
package com.srimech.faultline;

import android.graphics.Point;
import android.util.Log;
import java.util.LinkedList;

public class AStar
{
    public int distance[][];
    private int wallType[][];
    private final int WEST = 1;
    private final int EAST = 2;
    private final int SOUTH = 4;
    private final int NORTH = 8;
    private Point destination;
    private Point origin;
    public AStar(int gsx, int gsy, int[][] wallType) {
	this.gsx = gsx;
	this.gsy = gsy;
	this.wallType = wallType;
	distance = new int [gsx][gsy];
    }
    private int gsx,gsy;
    private void floodFill(int x, int y, int dist) {
	if (distance[x][y] != 0 && distance[x][y] < dist) return;
	Log.i("AStar", "Dist "+dist+" Flood filling: "+x+","+y+" wall type "+wallType[x][y]);
	distance[x][y] = dist;
	if (passable(x,y,-1, 0)) floodFill(x-1,y, dist+1);
	if (passable(x,y, 1, 0)) floodFill(x+1,y, dist+1);
	if (passable(x,y, 0,-1)) floodFill(x,y-1, dist+1);
	if (passable(x,y, 0, 1)) floodFill(x,y+1, dist+1);
    }

    private boolean passable(int x, int y, int dx, int dy) {
	if(x+dx < 0 || x+dx >= gsx || y+dy < 0 || y+dy >= gsy) return false;
	if(Math.abs(dx) == 1 ^ Math.abs(dy) == 1) {
	    if (dx == -1 && ((wallType[x][y] & WEST)>0) && ((wallType[x-1][y] & EAST)>0)) return true;
	    if (dx == 1 && ((wallType[x][y] & EAST)>0) && ((wallType[x+1][y] & WEST)>0)) return true;
	    if (dy == -1 && ((wallType[x][y] & NORTH)>0) && ((wallType[x][y-1] & SOUTH)>0)) return true;
	    if (dy == 1 && ((wallType[x][y] & SOUTH)>0) && ((wallType[x][y+1] & NORTH)>0)) return true;
	}
	return false;
    }

    public boolean routeable(int x1, int y1, int x2, int y2) {
	origin = new Point(x1,y1);
	destination = new Point(x2,y2);
	floodFill(x1, y1, 1);
	return (distance[x2][y2] > 0);
    }

    public Point nextInRoute(int x, int y, int dist) {
	Point[] candidates = {new Point(1,0), new Point(-1,0), new Point(0,1), new Point(0,-1)};
	for(int i=0;i<4;i++) {
	    int dx = candidates[i].x;
	    int dy = candidates[i].y;
	    if(x+dx < gsx && y+dy < gsy && x+dx >= 0 && y+dy >= 0) {
		int nextDist = distance[x+dx][y+dy];
		Log.i("AStar", "Routing: candidate at "+(x+dx)+","+(y+dy)+" has distance "+nextDist);
		if(passable(x,y,dx,dy)) {
			if(nextDist == dist-1) {
			    return new Point(x+dx,y+dy);
			}
		    }
	    }
	}
	return null;
    }

    public LinkedList<Point> getRoute()
    {
	LinkedList<Point> result = new LinkedList<Point>();
	result.addLast(destination);
	int x,y;
	x = destination.x;
	y = destination.y;
	int dist = distance[x][y];
	while(dist > 1) {
	    Log.i("AStar", "Routing: dist "+dist+" at point "+x+","+y);
	    Point p = nextInRoute(x,y,dist);
	    result.addLast(p);
	    if(p==null) {
		Log.e("AStar", "Cannot retrace route");
		return null;
	    }
	    dist--;
	    x = p.x; y = p.y;
	}
	return result;
    }
}
