package util;

public class Vector2 {

    public float x, y;

    public Vector2() {
        x = 0;
        y = 0;
    }

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void add(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public void sub(float x, float y) {
        this.x -= x;
        this.y -= y;
    }

    public void mult(float x, float y) {
        this.x *= x;
        this.y *= y;
    }

    public void div(float x, float y) {
        this.x /= x;
        this.y /= y;
    }
}