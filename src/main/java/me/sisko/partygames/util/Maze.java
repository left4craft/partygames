package me.sisko.partygames.util;

import java.util.ArrayList;
import java.util.List;

/*

Class representing a maze.

Coordinate system: increasing x is right, increasing y is up
*/

public class Maze {
    private int height;
    private int width;
    private int area_;
    private class cellwall {
        boolean right_;
        boolean down_;
    }
    private ArrayList<cellwall> maze_ = new ArrayList<cellwall>();
    private DisjointSet path_;
    // construct and solve a new maze
    public Maze(final int height, final int width) {
        this.height = height;
        this.width = width;
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
    public final boolean canMoveDown(final int x, final int y) {
        return false;
    }

    // whether it is possible to move from (x,y) to (x+1, y)
    public final boolean canMoveRight(final int x, final int y) {
        return false;
    }
    
    /*
    Returns a list of moves representing the solution to a maze

    1 = up
    2 = right
    3 = down
    4 = left
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
}
