package me.sisko.partygames.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
/*

Class representing a maze.

Coordinate system: increasing x is right, increasing y is up
*/

public class Maze {
    // Implementing private variables
    private int height;
    private int width;
    private int area_;
    // Creating a class cellwall which holds boolean expressions
    private class cellwall {
        int xPos;
        int yPos;
        boolean right_;
        boolean down_;
        cellwall() {}
    }
    private ArrayList<cellwall> maze_ = new ArrayList<cellwall>();
    private DisjointSet path_;
    // construct and solve a new maze
    public Maze(final int height, final int width) {
        this.height = height;
        this.width = width;
        this.area_ = height*width;
        //
        path_.addelements(area_);
        cellwall newcell = new cellwall();
        newcell.right_ = true;
        newcell.down_ = true;

        // Iterate through 
        for(int i = 0; i < this.height; i++) {
            for(int j = 0; j < this.width; j++) {
                newcell.xPos = j;
                newcell.yPos = i;
                maze_.add(newcell);
            }
        }

        while(path_.size(0) < this.getArea()) {
            Random rand = new Random();
            int randX = rand.nextInt(this.getWidth());
            int randY = rand.nextInt(this.getHeight());

            int travX = randX;
            int travY = randY;
            int dir = rand.nextInt(2);

            if(dir == 0) {
                travX++;
            }
            else if(dir == 1) {
                travY++;
            }
            int currentIdx = randY * this.getWidth() + randX;
            int travIdx = travY * * this.getWidth() + travX;
            if((travX >= 0 && travX < this.getWidth()) && (travY >= 0 && travY < this.getHeight()) && !canTravel(randX, randY, dir)) {
                if(path_.find(currentIdx) != path_.find(travIdx)) {
                    setWall(randX, randY, dir, false);
                    path_.union(currentIdx, travIdx);
                }
            }
        }
    }

    // return an int array representing start of maze
    // a[0] is x, a[1] is y
    public final int[] getStart() {
        int[] arr = {0,0};
        return arr;
    }

    // return an int array representing end of maze
    // a[0] is x, a[1] is y
    public final int[] getEnd() {
        int[] arr = {0,0};
        return arr;
    }

    // whether it is possible to move from (x,y) to (x, y+1)
    public final boolean canTravel(final int x, final int y, final int dir) {
        int idx = y * this.getWidth() + x;
        if(dir == 0) {
            return !maze_.get(idx).right_;
        }
        if(dir == 1) {
            return !maze_.get(idx).down_;
        }
        if(dir == 2 && x-1 >= 0) {
            return !maze_.get(y*this.getWidth() + (x-1)).right_;
        }
        if(dir == 3 && y-1 >= 0) {
            return !maze_.get((y-1)*this.getWidth() + x).down_;
        }
        return false;
    }
    
    public void setWall(final int x, final int y, final int dir, final boolean exists) {
        int idx = y * this.getWidth() + x;
        if(dir == 0) {
            maze_.get(idx).right_ = exists;
        }
        if(dir == 1) {
            maze_.get(idx).down_ = exists;
        }
    }
    /*
    Returns a list of moves representing the solution to a maze

    0 = right
    1 = down
    2 = left
    3 = up
    */
    public final List<Integer> getSolution() {
        return new ArrayList<Integer>();
    }
    
    public final int getHeight() {
        return height;
    }

    public final int getWidth() {
        return width;
    }
    public final int getArea() {
        return area_;
    }
}
