package com.pulseline.app.engine;

public enum MachineType {
    MOTOR("Motor", 55, 2.0, 12.0, true),
    PUMP("Pump", 48, 2.8, 8.0, true),
    CONVEYOR("Conveyor", 40, 1.5, 6.0, true),
    COMPRESSOR("Compressor", 70, 3.5, 20.0, true),
    /** The phone itself: real accelerometer (vibration) + real battery temperature. */
    PHONE("This phone (live sensors)", 30, 0.2, Double.NaN, false);

    public final String label;
    public final double temp;
    public final double vib;
    public final double cur;
    public final boolean simulated;

    MachineType(String label, double temp, double vib, double cur, boolean simulated) {
        this.label = label;
        this.temp = temp;
        this.vib = vib;
        this.cur = cur;
        this.simulated = simulated;
    }

    /** Smallest "normal wobble" per channel (temp, vib, current) so tiny noise never looks abnormal. */
    public double[] minStd() {
        return simulated ? new double[]{0.4, 0.06, 0.15} : new double[]{0.3, 0.15, 0.15};
    }

    public static MachineType[] simulatedTypes() {
        return new MachineType[]{MOTOR, PUMP, CONVEYOR, COMPRESSOR};
    }
}
