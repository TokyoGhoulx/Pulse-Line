package com.pulseline.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.pulseline.app.engine.Alert;
import com.pulseline.app.engine.AlertWriter;
import com.pulseline.app.engine.FaultMode;
import com.pulseline.app.engine.Machine;
import com.pulseline.app.engine.MachineType;
import com.pulseline.app.engine.Reading;
import com.pulseline.app.engine.RoiCalculator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The app's brain. Runs the Sense -> Process -> Alert -> Learn loop once per second for every machine,
 * keeps the alert list and pilot statistics, and saves everything on the device.
 */
public class PulseEngine {
    public interface Listener {
        void onTick();

        void onData();
    }

    private static PulseEngine instance;

    public static synchronized PulseEngine get(Context c) {
        if (instance == null) instance = new PulseEngine(c.getApplicationContext());
        return instance;
    }

    private static final String PREFS = "pulseline";

    private final Context ctx;
    private final SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Notifier notifier;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private PhoneSensorSource phone;
    private boolean running;

    public final List<Machine> machines = new ArrayList<>();
    public final List<Alert> alerts = new ArrayList<>();
    public final RoiCalculator.Inputs roi = new RoiCalculator.Inputs();
    public final Set<String> doneTasks = new HashSet<>();
    public int avoided;
    public int unplanned;
    public int useful;
    public int falseAlarms;
    public long samples;
    public boolean setupSeen;
    private long nextMachineId = 1;
    private long nextAlertId = 1;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick();
            if (running) handler.postDelayed(this, 1000);
        }
    };

    private PulseEngine(Context c) {
        ctx = c;
        prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        notifier = new Notifier(c);
        load();
    }

    // ---------------------------------------------------------------- lifecycle

    public void addListener(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public void start() {
        if (running) return;
        running = true;
        ensurePhone();
        handler.postDelayed(ticker, 1000);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(ticker);
        if (phone != null) {
            phone.stop();
            phone = null;
        }
    }

    private void ensurePhone() {
        if (phone == null && phoneMachine() != null) {
            PhoneSensorSource p = new PhoneSensorSource(ctx);
            if (p.start()) phone = p;
        }
    }

    // ---------------------------------------------------------------- the loop

    private void tick() {
        long now = System.currentTimeMillis();
        boolean dataChanged = false;
        for (Machine m : machines) {
            if (m.isFailed()) continue;
            Reading r;
            if (m.sim != null) {
                r = m.sim.next(now);
            } else if (phone != null) {
                r = phone.read(now);
            } else {
                continue;
            }
            samples++;
            Alert a = m.process(r);
            if (a != null) {
                publish(a);
                dataChanged = true;
            }
            if (m.sim != null && m.sim.isFailed()) {
                m.markFailed();
                unplanned++;
                publish(AlertWriter.breakdown(m));
                dataChanged = true;
            }
        }
        for (Listener l : listeners) l.onTick();
        if (dataChanged) {
            save();
            fireData();
        }
    }

    private void publish(Alert a) {
        a.id = nextAlertId++;
        alerts.add(0, a);
        while (alerts.size() > 100) alerts.remove(alerts.size() - 1);
        notifier.post(a);
    }

    private void fireData() {
        for (Listener l : listeners) l.onData();
    }

    // ---------------------------------------------------------------- queries

    public Machine machine(long id) {
        for (Machine m : machines) if (m.id == id) return m;
        return null;
    }

    public Machine phoneMachine() {
        for (Machine m : machines) if (m.type == MachineType.PHONE) return m;
        return null;
    }

    public Alert alert(long id) {
        for (Alert a : alerts) if (a.id == id) return a;
        return null;
    }

    public int openAlertCount() {
        int n = 0;
        for (Alert a : alerts) if (!a.resolved) n++;
        return n;
    }

    public RoiCalculator.Output roiOutput() {
        return RoiCalculator.compute(roi, machines.size(), avoided, unplanned, useful, falseAlarms);
    }

    // ---------------------------------------------------------------- actions

    public Machine addMachine(String name, MachineType type) {
        long id = nextMachineId++;
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) n = type.label + " " + id;
        Machine m = new Machine(id, n, type);
        machines.add(m);
        save();
        fireData();
        return m;
    }

    /** Returns false if this phone has no motion sensor. */
    public boolean addPhoneMachine() {
        if (phoneMachine() != null) return true;
        if (!PhoneSensorSource.isAvailable(ctx)) return false;
        addMachine("My phone", MachineType.PHONE);
        ensurePhone();
        return true;
    }

    public void removeMachine(long id) {
        Machine m = machine(id);
        if (m == null) return;
        machines.remove(m);
        if (m.type == MachineType.PHONE && phone != null) {
            phone.stop();
            phone = null;
        }
        save();
        fireData();
    }

    public void injectFault(long id, FaultMode f) {
        Machine m = machine(id);
        if (m == null) return;
        m.injectFault(f);
        fireData();
    }

    /** Operator repaired a stopped machine. */
    public void repairStopped(long id) {
        Machine m = machine(id);
        if (m == null) return;
        m.repair();
        for (Alert a : alerts) if (a.machineId == id) a.resolved = true;
        save();
        fireData();
    }

    /** Operator scheduled the repair suggested by an alert, before the machine failed. */
    public void scheduleRepair(long alertId) {
        Alert a = alert(alertId);
        if (a == null) return;
        Machine m = machine(a.machineId);
        if (m != null && m.sim != null && !m.isFailed() && m.sim.fault() != FaultMode.NONE) {
            avoided++;
            m.repair();
        }
        for (Alert x : alerts) if (x.machineId == a.machineId) x.resolved = true;
        save();
        fireData();
    }

    /** LEARN step. */
    public void feedback(long alertId, boolean wasUseful) {
        Alert a = alert(alertId);
        if (a == null || a.feedback != 0 || a.level >= 3) return;
        a.feedback = wasUseful ? 1 : 2;
        if (wasUseful) useful++;
        else falseAlarms++;
        Machine m = machine(a.machineId);
        if (m != null) m.detector.adjust(wasUseful);
        save();
        fireData();
    }

    public void clearAlerts() {
        alerts.clear();
        save();
        fireData();
    }

    public void setTask(String key, boolean done) {
        if (done) doneTasks.add(key);
        else doneTasks.remove(key);
        save();
    }

    public void markSetupSeen() {
        setupSeen = true;
        save();
    }

    public void saveRoi() {
        save();
        fireData();
    }

    public void resetDemo() {
        if (phone != null) {
            phone.stop();
            phone = null;
        }
        machines.clear();
        alerts.clear();
        doneTasks.clear();
        avoided = 0;
        unplanned = 0;
        useful = 0;
        falseAlarms = 0;
        samples = 0;
        addDefaults();
        save();
        fireData();
    }

    private void addDefaults() {
        machines.add(new Machine(nextMachineId++, "Motor 1", MachineType.MOTOR));
        machines.add(new Machine(nextMachineId++, "Pump A", MachineType.PUMP));
        machines.add(new Machine(nextMachineId++, "Conveyor 3", MachineType.CONVEYOR));
    }

    // ---------------------------------------------------------------- persistence

    private void save() {
        try {
            JSONArray ms = new JSONArray();
            for (Machine m : machines) {
                JSONObject o = new JSONObject();
                o.put("id", m.id);
                o.put("name", m.name);
                o.put("type", m.type.name());
                o.put("sens", m.detector.getSensitivity());
                ms.put(o);
            }
            JSONArray as = new JSONArray();
            for (Alert a : alerts) {
                JSONObject o = new JSONObject();
                o.put("id", a.id);
                o.put("machineId", a.machineId);
                o.put("machineName", a.machineName);
                o.put("level", a.level);
                o.put("channel", a.channel);
                o.put("title", a.title);
                o.put("body", a.body);
                o.put("action", a.action);
                o.put("health", a.health);
                o.put("eta", a.etaMin);
                o.put("time", a.time);
                o.put("feedback", a.feedback);
                o.put("resolved", a.resolved);
                as.put(o);
            }
            JSONArray ts = new JSONArray();
            for (String t : doneTasks) ts.put(t);
            prefs.edit()
                    .putString("machines", ms.toString())
                    .putString("alerts", as.toString())
                    .putString("tasks", ts.toString())
                    .putLong("nextMachineId", nextMachineId)
                    .putLong("nextAlertId", nextAlertId)
                    .putInt("avoided", avoided)
                    .putInt("unplanned", unplanned)
                    .putInt("useful", useful)
                    .putInt("falseAlarms", falseAlarms)
                    .putBoolean("setupSeen", setupSeen)
                    .putFloat("costPerHour", (float) roi.costPerHour)
                    .putFloat("hoursPerBreakdown", (float) roi.hoursPerBreakdown)
                    .putFloat("kitPrice", (float) roi.kitPrice)
                    .putFloat("monthlyPlan", (float) roi.monthlyPlan)
                    .putInt("months", roi.months)
                    .apply();
        } catch (Exception ignored) {
            // saving is best-effort; the live app keeps working
        }
    }

    private void load() {
        nextMachineId = prefs.getLong("nextMachineId", 1);
        nextAlertId = prefs.getLong("nextAlertId", 1);
        avoided = prefs.getInt("avoided", 0);
        unplanned = prefs.getInt("unplanned", 0);
        useful = prefs.getInt("useful", 0);
        falseAlarms = prefs.getInt("falseAlarms", 0);
        setupSeen = prefs.getBoolean("setupSeen", false);
        roi.costPerHour = prefs.getFloat("costPerHour", (float) roi.costPerHour);
        roi.hoursPerBreakdown = prefs.getFloat("hoursPerBreakdown", (float) roi.hoursPerBreakdown);
        roi.kitPrice = prefs.getFloat("kitPrice", (float) roi.kitPrice);
        roi.monthlyPlan = prefs.getFloat("monthlyPlan", (float) roi.monthlyPlan);
        roi.months = prefs.getInt("months", roi.months);

        String ms = prefs.getString("machines", null);
        try {
            if (ms != null) {
                JSONArray arr = new JSONArray(ms);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Machine m = new Machine(o.getLong("id"), o.getString("name"), MachineType.valueOf(o.getString("type")));
                    m.detector.setSensitivity(o.optDouble("sens", 1.0));
                    machines.add(m);
                }
            }
        } catch (Exception e) {
            machines.clear();
            ms = null;
        }
        if (ms == null) addDefaults();

        try {
            String as = prefs.getString("alerts", null);
            if (as != null) {
                JSONArray arr = new JSONArray(as);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Alert a = new Alert();
                    a.id = o.getLong("id");
                    a.machineId = o.getLong("machineId");
                    a.machineName = o.getString("machineName");
                    a.level = o.getInt("level");
                    a.channel = o.getInt("channel");
                    a.title = o.getString("title");
                    a.body = o.getString("body");
                    a.action = o.getString("action");
                    a.health = o.getDouble("health");
                    a.etaMin = o.getInt("eta");
                    a.time = o.getLong("time");
                    a.feedback = o.getInt("feedback");
                    a.resolved = o.getBoolean("resolved");
                    alerts.add(a);
                }
            }
            String ts = prefs.getString("tasks", null);
            if (ts != null) {
                JSONArray arr = new JSONArray(ts);
                for (int i = 0; i < arr.length(); i++) doneTasks.add(arr.getString(i));
            }
        } catch (Exception e) {
            alerts.clear();
        }
    }
}
