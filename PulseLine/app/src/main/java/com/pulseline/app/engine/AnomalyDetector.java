package com.pulseline.app.engine;

/**
 * PROCESS step. Learns what "normal" looks like for one machine (first BASELINE_SAMPLES readings),
 * then scores every new reading against that baseline with a smoothed z-score.
 * The alert threshold scales with {@link #sensitivity}, which the LEARN step adjusts from operator feedback.
 */
public class AnomalyDetector {
    public static final int BASELINE_SAMPLES = 30;
    public static final int CHANNELS = 3; // 0 temperature, 1 vibration, 2 current
    public static final double Z_START = 4.0;
    public static final double Z_END = 40.0;
    public static final double SENS_MIN = 0.6;
    public static final double SENS_MAX = 2.0;

    public static class Result {
        public final double health;
        public final Status status;
        public final double[] severity;
        public final double[] z;
        public final int dominant;
        public final double baselineProgress;

        Result(double health, Status status, double[] severity, double[] z, int dominant, double progress) {
            this.health = health;
            this.status = status;
            this.severity = severity;
            this.z = z;
            this.dominant = dominant;
            this.baselineProgress = progress;
        }
    }

    private static class Channel {
        int n;
        double mean;
        double m2;
        double ewma = Double.NaN;

        void learn(double x) {
            n++;
            double d = x - mean;
            mean += d / n;
            m2 += d * (x - mean);
        }

        double std(double floor) {
            double s = n > 1 ? Math.sqrt(m2 / (n - 1)) : 0;
            return Math.max(s, floor);
        }
    }

    private final Channel[] ch = new Channel[CHANNELS];
    private final double[] minStd;
    private int n;
    private double sensitivity = 1.0;

    public AnomalyDetector(double[] minStd) {
        this.minStd = minStd;
        for (int i = 0; i < CHANNELS; i++) ch[i] = new Channel();
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(double s) {
        sensitivity = Math.max(SENS_MIN, Math.min(SENS_MAX, s));
    }

    /** LEARN step: a "false alarm" makes alerts less sensitive, a "useful" alert makes them slightly sharper. */
    public void adjust(boolean useful) {
        setSensitivity(useful ? sensitivity * 0.97 : sensitivity * 1.15);
    }

    public boolean baselineReady() {
        return n >= BASELINE_SAMPLES;
    }

    /** Normal range shown on charts (baseline mean +/- alert threshold). NaN until baseline is learned. */
    public double bandLow(int c) {
        if (!baselineReady() || ch[c].n < 2) return Double.NaN;
        return ch[c].mean - Z_START * sensitivity * ch[c].std(minStd[c]);
    }

    public double bandHigh(int c) {
        if (!baselineReady() || ch[c].n < 2) return Double.NaN;
        return ch[c].mean + Z_START * sensitivity * ch[c].std(minStd[c]);
    }

    /** Called after maintenance: keep the learned baseline, forget the smoothed fault values. */
    public void resetAfterRepair() {
        for (Channel c : ch) c.ewma = c.mean;
    }

    public Result update(Reading r) {
        double[] v = {r.temp, r.vib, r.cur};
        if (n < BASELINE_SAMPLES) {
            for (int i = 0; i < CHANNELS; i++) {
                if (!Double.isNaN(v[i])) ch[i].learn(v[i]);
            }
            n++;
            if (n == BASELINE_SAMPLES) {
                for (Channel c : ch) c.ewma = c.mean;
            }
            return new Result(100, Status.LEARNING, new double[CHANNELS], new double[CHANNELS], -1,
                    (double) n / BASELINE_SAMPLES);
        }
        double[] sev = new double[CHANNELS];
        double[] z = new double[CHANNELS];
        double max = 0, sum = 0;
        int cnt = 0, dom = -1;
        for (int i = 0; i < CHANNELS; i++) {
            Channel c = ch[i];
            if (c.n < 2 || Double.isNaN(v[i])) continue;
            c.ewma = Double.isNaN(c.ewma) ? v[i] : 0.5 * v[i] + 0.5 * c.ewma;
            z[i] = (c.ewma - c.mean) / c.std(minStd[i]);
            double s = (z[i] - Z_START * sensitivity) / ((Z_END - Z_START) * sensitivity);
            s = Math.max(0, Math.min(1, s));
            sev[i] = s;
            sum += s;
            cnt++;
            if (s > max) {
                max = s;
                dom = i;
            }
        }
        double meanSev = cnt > 0 ? sum / cnt : 0;
        double health = 100 - 100 * (0.6 * max + 0.4 * meanSev);
        health = Math.max(0, Math.min(100, health));
        Status st = health >= 75 ? Status.HEALTHY : (health >= 45 ? Status.WARNING : Status.CRITICAL);
        return new Result(health, st, sev, z, dom, 1.0);
    }
}
