package com.pulseline.app;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;

import com.pulseline.app.engine.Reading;

/**
 * SENSE step using the phone itself as a real machine:
 * vibration = RMS of the accelerometer (movement), temperature = battery temperature.
 */
public class PhoneSensorSource implements SensorEventListener {
    private final Context ctx;
    private SensorManager sm;
    private boolean linear;
    private double sumSq;
    private int count;

    public PhoneSensorSource(Context ctx) {
        this.ctx = ctx;
    }

    public static boolean isAvailable(Context ctx) {
        SensorManager m = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        return m != null && (m.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
                || m.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null);
    }

    public boolean start() {
        sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        if (sm == null) return false;
        Sensor s = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        linear = s != null;
        if (s == null) s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (s == null) return false;
        return sm.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        if (sm != null) sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        double x = e.values[0], y = e.values[1], z = e.values[2];
        double mag = Math.sqrt(x * x + y * y + z * z);
        double dev = linear ? mag : Math.abs(mag - 9.81);
        sumSq += dev * dev;
        count++;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /** Returns the reading for the last second and resets the accumulator. */
    public Reading read(long time) {
        double rms = count > 0 ? Math.sqrt(sumSq / count) : 0;
        sumSq = 0;
        count = 0;
        return new Reading(time, batteryTempC(), rms * 10.0, Double.NaN);
    }

    private double batteryTempC() {
        Intent i = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int t = i != null ? i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) : 0;
        return t > 0 ? t / 10.0 : 30.0;
    }
}
