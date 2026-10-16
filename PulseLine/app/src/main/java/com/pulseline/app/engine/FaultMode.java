package com.pulseline.app.engine;

/** Faults the demo simulator can inject into a machine. */
public enum FaultMode {
    NONE("Normal"),
    BEARING_WEAR("Bearing wear"),
    OVERHEAT("Overheating"),
    OVERLOAD("Overload");

    public final String label;

    FaultMode(String label) {
        this.label = label;
    }
}
