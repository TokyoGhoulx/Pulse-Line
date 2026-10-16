package com.pulseline.app.engine;

/** A plain-language alert for the operator. */
public class Alert {
    public long id;
    public long machineId;
    public String machineName;
    /** 1 check soon, 2 act now, 3 breakdown. */
    public int level;
    public int channel;
    public String title;
    public String body;
    public String action;
    public double health;
    public int etaMin;
    public long time;
    /** 0 = no feedback yet, 1 = useful, 2 = false alarm. */
    public int feedback;
    public boolean resolved;

    public Alert() {
    }
}
