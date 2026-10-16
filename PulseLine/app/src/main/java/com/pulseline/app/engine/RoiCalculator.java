package com.pulseline.app.engine;

/** The built-in ROI dashboard maths: downtime avoided vs. what the pilot costs. */
public final class RoiCalculator {
    private RoiCalculator() {
    }

    /** Editable assumptions. The defaults are illustrative only; pricing would be validated during the pilot. */
    public static class Inputs {
        public double costPerHour = 20000;
        public double hoursPerBreakdown = 4;
        public double kitPrice = 8000;
        public double monthlyPlan = 500;
        public int months = 1;
    }

    public static class Output {
        public double hoursAvoided;
        public double valueAvoided;
        public double hoursLost;
        public double valueLost;
        public double pilotCost;
        public double net;
        /** Percent of reviewed alerts marked useful, or -1 if none reviewed yet. */
        public double accuracy;
    }

    public static Output compute(Inputs in, int machines, int avoided, int unplanned, int useful, int falseAlarms) {
        Output o = new Output();
        o.hoursAvoided = avoided * in.hoursPerBreakdown;
        o.valueAvoided = o.hoursAvoided * in.costPerHour;
        o.hoursLost = unplanned * in.hoursPerBreakdown;
        o.valueLost = o.hoursLost * in.costPerHour;
        o.pilotCost = machines * (in.kitPrice + in.monthlyPlan * in.months);
        o.net = o.valueAvoided - o.pilotCost;
        int reviewed = useful + falseAlarms;
        o.accuracy = reviewed == 0 ? -1 : 100.0 * useful / reviewed;
        return o;
    }

    /** Rupees with Indian digit grouping, e.g. 1,25,000. */
    public static String inr(double v) {
        long n = Math.round(Math.abs(v));
        String s = Long.toString(n);
        String out;
        if (s.length() <= 3) {
            out = s;
        } else {
            String head = s.substring(0, s.length() - 3);
            String tail = s.substring(s.length() - 3);
            StringBuilder sb = new StringBuilder();
            int first = head.length() % 2;
            if (first > 0) sb.append(head, 0, first);
            for (int i = first; i < head.length(); i += 2) {
                if (sb.length() > 0) sb.append(',');
                sb.append(head, i, i + 2);
            }
            out = sb + "," + tail;
        }
        return (v < 0 ? "-" : "") + "₹" + out;
    }
}
