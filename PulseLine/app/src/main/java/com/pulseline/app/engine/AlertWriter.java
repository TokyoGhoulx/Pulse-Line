package com.pulseline.app.engine;

/** ALERT step: turns a number into a sentence an operator can act on. */
public final class AlertWriter {
    private AlertWriter() {
    }

    public static Alert create(Machine m, int level, int channel, double health, int etaMin) {
        String n = m.name;
        String title;
        String body;
        String action;
        boolean act = level >= 2;
        switch (channel) {
            case 0:
                title = act ? n + " is overheating" : n + " is running hotter than usual";
                body = act
                        ? "Temperature is well above its normal range. Running on risks damage or a fire hazard."
                        : "Heat is building up. Common causes are a blocked vent, low lubrication or a failing cooling fan.";
                action = act
                        ? "Stop the machine safely and let it cool before checking it."
                        : "Check ventilation, cooling and lubrication at the next planned stop.";
                break;
            case 2:
                title = act ? n + " is overloaded" : n + " is drawing more power than usual";
                body = act
                        ? "Electrical load is far above normal and rising. The motor or drive could trip or burn out."
                        : "The machine is working harder than normal. This can mean a jam, extra load or worn parts.";
                action = act
                        ? "Stop the machine safely, clear the load and check for jams before restarting."
                        : "Check for jams, extra load and belt or coupling wear soon.";
                break;
            default:
                title = act ? n + " is vibrating heavily" : n + " is vibrating more than usual";
                body = act
                        ? "Vibration is far above normal and rising. The machine may stop without further warning."
                        : "The machine is shaking more than it normally does. This is often an early sign of a worn bearing or loose mounting.";
                action = act
                        ? "Stop the machine safely and inspect the bearings before restarting."
                        : "Plan a bearing and mounting check at the next planned stop.";
                break;
        }
        if (etaMin > 0) {
            body += " If nothing changes it could fail in about " + etaMin + " min.";
        }
        Alert a = new Alert();
        a.machineId = m.id;
        a.machineName = n;
        a.level = level;
        a.channel = channel;
        a.title = (act ? "Act now: " : "Check soon: ") + title;
        a.body = body;
        a.action = action;
        a.health = health;
        a.etaMin = etaMin;
        a.time = System.currentTimeMillis();
        return a;
    }

    public static Alert breakdown(Machine m) {
        Alert a = new Alert();
        a.machineId = m.id;
        a.machineName = m.name;
        a.level = 3;
        a.channel = -1;
        a.title = m.name + " has stopped";
        a.body = "Unplanned breakdown: the fault was not repaired in time and the line is halted.";
        a.action = "Repair the fault, then restart the machine from its detail screen.";
        a.health = 0;
        a.etaMin = -1;
        a.time = System.currentTimeMillis();
        return a;
    }
}
