package com.pulseline.app.engine;

public enum Status {
    LEARNING("Learning", 0),
    HEALTHY("Healthy", 0),
    WARNING("Check soon", 1),
    CRITICAL("Act now", 2),
    STOPPED("Stopped", 3);

    public final String label;
    /** Alert level: 0 none, 1 check soon, 2 act now, 3 breakdown. */
    public final int level;

    Status(String label, int level) {
        this.label = label;
        this.level = level;
    }
}
