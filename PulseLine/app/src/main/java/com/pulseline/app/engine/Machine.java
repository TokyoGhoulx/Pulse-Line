package com.pulseline.app.engine;

/** One monitored machine: its sensors, its learned baseline, its health history and its alert state. */
public class Machine {
    public static final int HISTORY = 90;

    public final long id;
    public String name;
    public final MachineType type;
    /** null for the live phone machine. */
    public final MachineSimulator sim;
    public final AnomalyDetector detector;
    public final Series temp = new Series(HISTORY);
    public final Series vib = new Series(HISTORY);
    public final Series cur = new Series(HISTORY);
    public final Series health = new Series(HISTORY);

    public Status status = Status.LEARNING;
    public AnomalyDetector.Result result;
    public Reading last;

    private final HealthTrend trend = new HealthTrend();
    private boolean failed;
    private int lastAlertLevel;
    private int pendingLevel;
    private int pendingCount;
    private int healthyStreak;

    public Machine(long id, String name, MachineType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.sim = type.simulated ? new MachineSimulator(type, id * 7919L + 13L) : null;
        this.detector = new AnomalyDetector(type.minStd());
    }

    public boolean isFailed() {
        return failed;
    }

    public Series series(int channel) {
        return channel == 0 ? temp : (channel == 1 ? vib : cur);
    }

    public int etaMinutes() {
        return trend.etaMinutes(20);
    }

    /** Feeds one reading through the PROCESS step. Returns a new alert when one should be raised. */
    public Alert process(Reading r) {
        if (failed) return null;
        last = r;
        temp.add(r.temp);
        vib.add(r.vib);
        cur.add(r.cur);
        AnomalyDetector.Result res = detector.update(r);
        result = res;
        status = res.status;
        health.add(res.health);
        trend.add(res.health);
        if (res.status == Status.LEARNING) return null;

        int level = res.status.level;
        if (level == 0) {
            pendingCount = 0;
            healthyStreak++;
            if (healthyStreak >= 10) lastAlertLevel = 0;
            return null;
        }
        healthyStreak = 0;
        if (level > lastAlertLevel) {
            // require 3 consecutive readings at the new level so one noisy sample never raises an alert
            if (level == pendingLevel) {
                pendingCount++;
            } else {
                pendingLevel = level;
                pendingCount = 1;
            }
            if (pendingCount >= 3) {
                lastAlertLevel = level;
                pendingCount = 0;
                int ch = res.dominant < 0 ? 0 : res.dominant;
                return AlertWriter.create(this, level, ch, res.health, etaMinutes());
            }
        } else {
            pendingCount = 0;
        }
        return null;
    }

    public void markFailed() {
        failed = true;
        status = Status.STOPPED;
    }

    public void injectFault(FaultMode f) {
        if (sim != null && !failed) sim.inject(f);
    }

    /** Maintenance done: clear the fault, keep the learned baseline. */
    public void repair() {
        failed = false;
        if (sim != null) sim.clearFault();
        detector.resetAfterRepair();
        trend.clear();
        lastAlertLevel = 0;
        pendingLevel = 0;
        pendingCount = 0;
        healthyStreak = 0;
        status = detector.baselineReady() ? Status.HEALTHY : Status.LEARNING;
    }
}
