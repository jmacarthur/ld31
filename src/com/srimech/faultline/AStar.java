/* Return a list of coordinates 
   implemented from Pseudocode on wikipedia! */

/*     Requirements: Function move_cost((x,y)) giving the cost of moving onto a square. Can be 999 to prevent it.
	 gsx, gsy determine the size of the rectangular/cartesian  grid which we can move on.
	 It's assumed that you can't move to x,y coordinates where x<0, y<0, x>= gsx or y>=gsy.

	 astar will return a list of coordinate pairs, from the start point to one short of the end point. */
package com.srimech.faultline;

import android.util.Log;

public class AStar
{
    public int distance[][];
    private int wallType[][];
    private class Coord
    {
	public int x, y;
	public Coord(int x, int y)
	{
 	    this.x = x;
 	    this.y = y;
 	}
    }
    private final int WEST = 1;
    private final int EAST = 2;
    private final int SOUTH = 4;
    private final int NORTH = 8;
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
	if (x>0 && ((wallType[x][y] & WEST)>0) && ((wallType[x-1][y] & EAST)>0)) floodFill(x-1,y, dist+1);
	if (x<(gsx-1) && ((wallType[x][y] & EAST)>0) && ((wallType[x+1][y] & WEST)>0)) floodFill(x+1,y, dist+1);
	if (y>0 && ((wallType[x][y] & NORTH)>0) && ((wallType[x][y-1] & SOUTH)>0)) floodFill(x,y-1, dist+1);
	if (y<(gsy-1) && ((wallType[x][y] & SOUTH)>0) && ((wallType[x][y+1] & NORTH)>0)) floodFill(x,y+1, dist+1);
    }
    public boolean routeable(int x1, int y1, int x2, int y2) {
	floodFill(x1, y1, 1);
	return (distance[x2][y2] > 0);
    }
}
