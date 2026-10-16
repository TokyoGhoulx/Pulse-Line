package com.pulseline.app.engine;

/** One sensor sample: temperature (deg C), vibration (mm/s), current (A). NaN = channel not available. */
public final class Reading {
    public final long time;
    public final double temp;
    public final double vib;
    public final double cur;

    public Reading(long time, double temp, double vib, double cur) {
        this.time = time;
        this.temp = temp;
        this.vib = vib;
        this.cur = cur;
    }
}
