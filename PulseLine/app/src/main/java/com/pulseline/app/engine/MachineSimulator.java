package com.pulseline.app.engine;

import java.util.Random;

/**
 * Stand-in for the clip-on sensor kit. Produces realistic readings for a machine and can
 * slowly degrade it (a fault grows from 0 to 1 over FAULT_TICKS ticks; at 1 the machine fails).
 */
public class MachineSimulator {
    public static final int FAULT_TICKS = 120;
    private static final double STEP = 1.0 / FAULT_TICKS;

    private final MachineType type;
    private final Random rnd;
    private FaultMode fault = FaultMode.NONE;
    private double progress;

    public MachineSimulator(MachineType type, long seed) {
        this.type = type;
        this.rnd = new Random(seed);
    }

    public void inject(FaultMode f) {
        fault = f;
        progress = 0;
    }

    public void clearFault() {
        fault = FaultMode.NONE;
        progress = 0;
    }

    public FaultMode fault() {
        return fault;
    }

    public double progress() {
        return progress;
    }

    public boolean isFailed() {
        return fault != FaultMode.NONE && progress >= 1.0;
    }

    public Reading next(long time) {
        if (fault != FaultMode.NONE && progress < 1.0) {
            progress = Math.min(1.0, progress + STEP);
        }
        double p = progress;
        double temp = type.temp, vib = type.vib, cur = type.cur;
        switch (fault) {
            case BEARING_WEAR:
                vib *= 1 + 2.2 * p;
                temp *= 1 + 0.10 * p;
                cur *= 1 + 0.05 * p;
                break;
            case OVERHEAT:
                temp *= 1 + 0.55 * p;
                vib *= 1 + 0.10 * p;
                break;
            case OVERLOAD:
                cur *= 1 + 0.90 * p;
                temp *= 1 + 0.25 * p;
                vib *= 1 + 0.15 * p;
                break;
            default:
                break;
        }
        temp += rnd.nextGaussian() * type.temp * 0.012;
        vib += rnd.nextGaussian() * type.vib * 0.04;
        cur += rnd.nextGaussian() * type.cur * 0.02;
        return new Reading(time, temp, Math.max(0, vib), Math.max(0, cur));
    }
}
