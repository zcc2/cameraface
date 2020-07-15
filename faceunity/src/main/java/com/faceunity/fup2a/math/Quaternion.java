package com.faceunity.fup2a.math;

import android.support.annotation.NonNull;

/**
 * reference https://users.aalto.fi/~ssarkka/pub/quat.pdf
 * Created by lirui on 2017/7/20.
 */

public class Quaternion {

    private double x, y, z, w;

    public Quaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + ", " + w + "]";
    }

    public double getW() {
        return w;
    }

    public void setW(double w) {
        this.w = w;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public Quaternion multiply(@NonNull Quaternion quat) {
        Quaternion quaternion = new Quaternion(x, y, z, w);
        quaternion.x = w * quat.x + x * quat.w + y * quat.z - z * quat.y;
        quaternion.y = w * quat.y - x * quat.z + y * quat.w + z * quat.x;
        quaternion.z = w * quat.z + x * quat.y - y * quat.x + z * quat.w;
        quaternion.w = w * quat.w - x * quat.x - y * quat.y - z * quat.z;
        return quaternion;
    }
}
