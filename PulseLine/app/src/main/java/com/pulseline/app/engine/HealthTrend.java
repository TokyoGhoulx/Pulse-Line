package com.pulseline.app.engine;

/** Fits a line through the last few health scores to estimate how long until the machine fails. */
public class HealthTrend {
    private final double[] v = new double[15];
    private int n;

    public void add(double h) {
        if (n < v.length) {
            v[n++] = h;
        } else {
            System.arraycopy(v, 1, v, 0, v.length - 1);
            v[v.length - 1] = h;
        }
    }

    public void clear() {
        n = 0;
    }

    /** Minutes (demo clock: 1 tick = 1 minute) until health reaches failHealth, or -1 if not clearly worsening. */
    public int etaMinutes(double failHealth) {
        if (n < 8) return -1;
        double sx = 0, sy = 0, sxy = 0, sxx = 0;
        for (int i = 0; i < n; i++) {
            sx += i;
            sy += v[i];
            sxy += i * v[i];
            sxx += (double) i * i;
        }
        double den = n * sxx - sx * sx;
        if (den == 0) return -1;
        double slope = (n * sxy - sx * sy) / den;
        if (slope > -0.15) return -1;
        double cur = v[n - 1];
        if (cur <= failHealth) return 1;
        return (int) Math.max(1, Math.round((cur - failHealth) / -slope));
    }
}
