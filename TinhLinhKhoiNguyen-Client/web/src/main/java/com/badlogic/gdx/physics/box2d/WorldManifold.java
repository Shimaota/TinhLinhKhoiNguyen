package com.badlogic.gdx.physics.box2d;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.cpp.b2WorldManifold;

public class WorldManifold {
    protected final b2WorldManifold b2WorldManifold = new b2WorldManifold();
    private final Vector2 normal = new Vector2();
    private final Vector2[] points = { new Vector2(), new Vector2() };
    private final float[] separations = new float[2];
    protected int numContactPoints = 0;

    protected WorldManifold() {
    }

    public void setValues(int count, float nx, float ny, float p0x, float p0y, float p1x, float p1y, float s0, float s1) {
        this.numContactPoints = count;
        this.normal.set(nx, ny);
        this.points[0].set(p0x, p0y);
        this.points[1].set(p1x, p1y);
        this.separations[0] = s0;
        this.separations[1] = s1;
    }

    public void refresh() {
    }

    public Vector2 getNormal() {
        return normal;
    }

    public Vector2[] getPoints() {
        return points;
    }

    public float[] getSeparations() {
        return separations;
    }

    public int getNumberOfContactPoints() {
        return numContactPoints;
    }
}
