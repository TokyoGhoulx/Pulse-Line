import com.pulseline.app.engine.*;

/** Plain-Java check of the detection engine (no Android needed). Run with engine-check/run.sh */
public class EngineCheck {
    static int failures = 0;

    static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        // 1. No false alarms on healthy machines across many seeds and all types
        int falseAlerts = 0, runs = 0;
        for (MachineType t : MachineType.simulatedTypes()) {
            for (long seed = 1; seed <= 60; seed++) {
                Machine m = new Machine(seed, "M" + seed, t);
                for (int i = 0; i < 600; i++) {
                    if (m.process(m.sim.next(i)) != null) falseAlerts++;
                }
                runs++;
            }
        }
        check(falseAlerts == 0, "healthy machines raise no alerts (" + runs + " runs x 600 ticks, false alerts = " + falseAlerts + ")");

        // 2. Each fault is caught early, in the right order, with the right channel, before failure
        FaultMode[] faults = {FaultMode.BEARING_WEAR, FaultMode.OVERHEAT, FaultMode.OVERLOAD};
        int[] expectedChannel = {1, 0, 2};
        for (int f = 0; f < faults.length; f++) {
            Machine m = new Machine(5, "Motor 1", MachineType.MOTOR);
            for (int i = 0; i < 40; i++) m.process(m.sim.next(i));
            m.injectFault(faults[f]);
            int warn = -1, crit = -1, fail = -1;
            int warnCh = -2;
            for (int i = 0; i < 200 && fail < 0; i++) {
                Alert a = m.process(m.sim.next(1000 + i));
                if (a != null && a.level == 1 && warn < 0) { warn = i; warnCh = a.channel; }
                if (a != null && a.level == 2 && crit < 0) crit = i;
                if (m.sim.isFailed()) fail = i;
            }
            System.out.println("      " + faults[f] + ": warning@" + warn + " critical@" + crit + " failure@" + fail);
            check(warn >= 0 && crit > warn && fail > crit, faults[f] + " warns, then escalates, before the machine fails");
            check(warn >= 0 && warn < fail / 2, faults[f] + " first warning arrives in the first half of the fault");
            check(warnCh == expectedChannel[f], faults[f] + " blames the right sensor (channel " + warnCh + ")");
        }

        // 3. Alert text is plain language and carries an action
        Machine pm = new Machine(9, "Pump A", MachineType.PUMP);
        Alert text = AlertWriter.create(pm, 1, 1, 60, 30);
        check(text.title.startsWith("Check soon: Pump A") && text.body.contains("30 min") && !text.action.isEmpty(),
                "alert message names the machine, gives an ETA and a next step: \"" + text.title + "\"");

        // 4. LEARN: feedback moves sensitivity in the right direction, within limits
        AnomalyDetector d = new AnomalyDetector(MachineType.MOTOR.minStd());
        double s0 = d.getSensitivity();
        d.adjust(false);
        check(d.getSensitivity() > s0, "'false alarm' makes alerts less sensitive");
        double s1 = d.getSensitivity();
        d.adjust(true);
        check(d.getSensitivity() < s1, "'useful' makes alerts slightly sharper");
        for (int i = 0; i < 50; i++) d.adjust(false);
        check(d.getSensitivity() <= AnomalyDetector.SENS_MAX, "sensitivity is capped");

        // 5. Higher sensitivity factor delays the alert on the same fault
        int[] firstWarn = new int[2];
        for (int k = 0; k < 2; k++) {
            Machine m = new Machine(5, "Motor 1", MachineType.MOTOR);
            if (k == 1) m.detector.setSensitivity(1.8);
            for (int i = 0; i < 40; i++) m.process(m.sim.next(i));
            m.injectFault(FaultMode.BEARING_WEAR);
            firstWarn[k] = -1;
            for (int i = 0; i < 200 && firstWarn[k] < 0; i++) {
                Alert a = m.process(m.sim.next(1000 + i));
                if (a != null) firstWarn[k] = i;
                if (m.sim.isFailed()) break;
            }
        }
        check(firstWarn[1] > firstWarn[0], "after 'false alarm' feedback the same fault alerts later (" + firstWarn[0] + " -> " + firstWarn[1] + ")");

        // 6. Repair resets the machine and it can alert again
        Machine rm = new Machine(7, "Conveyor 3", MachineType.CONVEYOR);
        for (int i = 0; i < 40; i++) rm.process(rm.sim.next(i));
        rm.injectFault(FaultMode.OVERLOAD);
        for (int i = 0; i < 200 && !rm.sim.isFailed(); i++) rm.process(rm.sim.next(i));
        rm.markFailed();
        check(rm.isFailed() && rm.status == Status.STOPPED, "an unrepaired fault ends in a breakdown");
        rm.repair();
        int after = 0;
        for (int i = 0; i < 100; i++) if (rm.process(rm.sim.next(i)) != null) after++;
        check(!rm.isFailed() && after == 0 && rm.status == Status.HEALTHY, "repair returns the machine to healthy with no alerts");

        // 7. Live phone machine: no current sensor (NaN) must not break anything
        Machine phone = new Machine(11, "My phone", MachineType.PHONE);
        java.util.Random r = new java.util.Random(3);
        int phoneAlerts = 0;
        for (int i = 0; i < 100; i++) {
            if (phone.process(new Reading(i, 31 + r.nextGaussian() * 0.1, 0.15 + Math.abs(r.nextGaussian()) * 0.05, Double.NaN)) != null) phoneAlerts++;
        }
        check(phoneAlerts == 0, "phone mode is quiet while the phone is still");
        Alert shake = null;
        for (int i = 0; i < 20 && shake == null; i++) shake = phone.process(new Reading(200 + i, 31, 25 + r.nextGaussian(), Double.NaN));
        check(shake != null && shake.channel == 1, "phone mode alerts when the phone is shaken (vibration channel)");

        // 8. ROI maths and formatting
        RoiCalculator.Inputs in = new RoiCalculator.Inputs();
        RoiCalculator.Output o = RoiCalculator.compute(in, 3, 2, 1, 3, 1);
        check(o.hoursAvoided == 8 && o.valueAvoided == 160000 && o.pilotCost == 25500 && o.net == 134500 && o.accuracy == 75.0,
                "ROI: avoided=" + RoiCalculator.inr(o.valueAvoided) + ", pilot cost=" + RoiCalculator.inr(o.pilotCost) + ", net=" + RoiCalculator.inr(o.net));
        check(RoiCalculator.inr(125000).equals("₹1,25,000") && RoiCalculator.inr(999).equals("₹999")
                && RoiCalculator.inr(12345678).equals("₹1,23,45,678") && RoiCalculator.inr(-5000).equals("-₹5,000"),
                "Indian rupee formatting");

        System.out.println(failures == 0 ? "\nALL CHECKS PASSED" : "\n" + failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
