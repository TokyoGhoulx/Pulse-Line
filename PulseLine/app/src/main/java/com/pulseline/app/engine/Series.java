package com.pulseline.app.engine;

/** Fixed-size ring buffer used for the live charts. get(0) is the oldest value. */
public class Series {
    private final double[] buf;
    private int start;
    private int size;

    public Series(int capacity) {
        buf = new double[capacity];
    }

    public void add(double v) {
        if (size < buf.length) {
            buf[(start + size) % buf.length] = v;
            size++;
        } else {
            buf[start] = v;
            start = (start + 1) % buf.length;
        }
    }

    public int size() {
        return size;
    }

    public double get(int i) {
        return buf[(start + i) % buf.length];
    }

    public double last() {
        return size == 0 ? Double.NaN : get(size - 1);
    }
}
