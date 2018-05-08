package com.example.gestureunlock;

/**
 * <pre>
 *     author : Clement
 *     time   : 2018/05/08
 *     desc   : 圆圈
 *     version: 1.0
 * </pre>
 */
public class Circle {

    /**
     * position of the circle
     */
    private int position;
    private int x;
    private int y;
    /**
     * the state of circle
     */
    private int state;

    public Circle() {
    }

    public Circle(int position, int x, int y, int state) {
        this.position = position;
        this.x = x;
        this.y = y;
        this.state = state;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
