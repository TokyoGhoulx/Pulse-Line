package com.pulseline.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pulseline.app.MainActivity;
import com.pulseline.app.PulseEngine;
import com.pulseline.app.engine.Alert;
import com.pulseline.app.engine.FaultMode;
import com.pulseline.app.engine.Machine;
import com.pulseline.app.engine.MachineType;
import com.pulseline.app.engine.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One machine: health ring, live charts with the learned normal range, current advice and demo fault controls. */
public class DetailScreen implements Screen {
    private static class ChartRef {
        int channel;
        String unit;
        TextView value;
        LineChartView chart;
    }

    private final MainActivity act;
    private final Context c;
    private final PulseEngine eng;
    private final long id;
    private final ScrollView scroll;
    private final LinearLayout col;
    private final List<ChartRef> charts = new ArrayList<>();
    private HealthRingView ring;
    private TextView statusChip;
    private TextView msg;
    private TextView sens;
    private TextView faultInfo;
    private LinearLayout advice;
    private LinearLayout demoNormal;
    private LinearLayout demoStopped;

    public DetailScreen(MainActivity act, long machineId) {
        this.act = act;
        this.c = act;
        this.eng = PulseEngine.get(act);
        this.id = machineId;
        scroll = new ScrollView(c);
        scroll.setBackgroundColor(Ui.BG);
        col = Ui.vbox(c);
        int p = Ui.dp(c, 16);
        col.setPadding(p, p, p, p);
        scroll.addView(col);
        build();
    }

    @Override
    public View view() {
        return scroll;
    }

    @Override
    public void onTick() {
        if (ring == null) return;
        refresh();
    }

    @Override
    public void onData() {
        if (ring == null) {
            act.showTab(0);
            return;
        }
        rebuildAdvice();
        refresh();
    }

    private void build() {
        final Machine m = eng.machine(id);
        if (m == null) return;

        LinearLayout nav = Ui.hbox(c);
        Button back = Ui.button(c, "← Machines", Ui.WHITE, Ui.NAVY, v -> act.showTab(0));
        nav.addView(back);
        View grow = new View(c);
        grow.setLayoutParams(Ui.weight(1));
        nav.addView(grow);
        nav.addView(Ui.button(c, "Remove", Ui.WHITE, Ui.RED, v -> confirmRemove()));
        LinearLayout.LayoutParams nlp = Ui.lp(Ui.MATCH, Ui.WRAP);
        nlp.bottomMargin = Ui.dp(c, 10);
        nav.setLayoutParams(nlp);
        col.addView(nav);

        LinearLayout hero = Ui.card(c);
        LinearLayout row = Ui.hbox(c);
        ring = new HealthRingView(c);
        ring.setLayoutParams(Ui.lp(Ui.dp(c, 120), Ui.dp(c, 120)));
        row.addView(ring);
        LinearLayout info = Ui.vbox(c);
        info.setPadding(Ui.dp(c, 14), 0, 0, 0);
        info.setLayoutParams(Ui.weight(1));
        info.addView(Ui.text(c, m.name, 20, Ui.NAVY, true));
        info.addView(Ui.text(c, m.type.label, 12, Ui.MUTED, false));
        statusChip = Ui.chip(c, "", Ui.BLUE);
        LinearLayout.LayoutParams clp = Ui.lp(Ui.WRAP, Ui.WRAP);
        clp.topMargin = Ui.dp(c, 6);
        clp.bottomMargin = Ui.dp(c, 6);
        statusChip.setLayoutParams(clp);
        info.addView(statusChip);
        msg = Ui.text(c, "", 13, Ui.TEXT, false);
        info.addView(msg);
        row.addView(info);
        hero.addView(row);
        col.addView(hero);

        advice = Ui.vbox(c);
        advice.setLayoutParams(Ui.lp(Ui.MATCH, Ui.WRAP));
        col.addView(advice);

        addChart("Temperature", "°C", 0, Ui.AMBER);
        addChart("Vibration", m.type == MachineType.PHONE ? "" : "mm/s", 1, Ui.BLUE);
        if (m.type != MachineType.PHONE) addChart("Electrical current", "A", 2, Ui.PURPLE);

        TextView note = Ui.text(c, "Shaded band = this machine's learned normal range. Leaving it is what triggers an alert.",
                11, Ui.MUTED, false);
        note.setPadding(0, 0, 0, Ui.dp(c, 8));
        col.addView(note);

        sens = Ui.text(c, "", 12, Ui.MUTED, false);
        sens.setPadding(0, 0, 0, Ui.dp(c, 12));
        col.addView(sens);

        if (m.sim != null) {
            col.addView(Ui.sectionTitle(c, "Demo controls"));
            LinearLayout demo = Ui.card(c);
            demo.addView(Ui.text(c, "Simulate a fault developing on this machine. It worsens over about two minutes, then the machine stops "
                    + "unless you schedule the repair from the alert.", 12, Ui.MUTED, false));
            faultInfo = Ui.text(c, "", 13, Ui.NAVY, true);
            faultInfo.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8));
            demo.addView(faultInfo);

            demoNormal = Ui.vbox(c);
            LinearLayout r1 = Ui.hbox(c);
            r1.addView(faultButton("Bearing wear", FaultMode.BEARING_WEAR));
            r1.addView(faultButton("Overheating", FaultMode.OVERHEAT));
            demoNormal.addView(r1);
            LinearLayout r2 = Ui.hbox(c);
            r2.addView(faultButton("Overload", FaultMode.OVERLOAD));
            Button fix = Ui.button(c, "Fix now", Ui.WHITE, Ui.NAVY, v -> eng.injectFault(id, FaultMode.NONE));
            LinearLayout.LayoutParams flp = Ui.weight(1);
            flp.topMargin = Ui.dp(c, 8);
            fix.setLayoutParams(flp);
            r2.addView(fix);
            demoNormal.addView(r2);
            demo.addView(demoNormal);

            demoStopped = Ui.vbox(c);
            demoStopped.addView(Ui.button(c, "Repair & restart", Ui.RED, Ui.WHITE, v -> eng.repairStopped(id)));
            demo.addView(demoStopped);
            col.addView(demo);
        }

        rebuildAdvice();
        refresh();
    }

    private Button faultButton(String label, final FaultMode f) {
        Button b = Ui.button(c, label, Ui.NAVY, Ui.WHITE, v -> eng.injectFault(id, f));
        LinearLayout.LayoutParams lp = Ui.weight(1);
        lp.topMargin = Ui.dp(c, 8);
        lp.rightMargin = Ui.dp(c, 8);
        b.setLayoutParams(lp);
        return b;
    }

    private void addChart(String title, String unit, int channel, int color) {
        LinearLayout card = Ui.card(c);
        LinearLayout head = Ui.hbox(c);
        TextView t = Ui.text(c, title, 14, Ui.NAVY, true);
        t.setLayoutParams(Ui.weight(1));
        head.addView(t);
        TextView value = Ui.text(c, "", 14, color, true);
        head.addView(value);
        card.addView(head);
        LineChartView chart = new LineChartView(c);
        chart.setLayoutParams(Ui.lp(Ui.MATCH, Ui.dp(c, 100)));
        Machine m = eng.machine(id);
        chart.setSeries(m.series(channel), color);
        card.addView(chart);
        col.addView(card);
        ChartRef r = new ChartRef();
        r.channel = channel;
        r.unit = unit;
        r.value = value;
        r.chart = chart;
        charts.add(r);
    }

    private void rebuildAdvice() {
        advice.removeAllViews();
        Alert latest = null;
        for (Alert a : eng.alerts) {
            if (a.machineId == id && !a.resolved) {
                latest = a;
                break;
            }
        }
        if (latest != null) advice.addView(AlertsScreen.card(c, eng, latest));
    }

    private void refresh() {
        Machine m = eng.machine(id);
        if (m == null) {
            act.showTab(0);
            return;
        }
        int color = Ui.statusColor(m.status);
        Ui.setChip(c, statusChip, Ui.statusLabel(m), color);

        if (m.status == Status.LEARNING) {
            int pct = m.result == null ? 0 : (int) Math.round(m.result.baselineProgress * 100);
            ring.set(pct, color, pct + "%", "learning");
            msg.setText("Learning what normal looks like for this machine. Keep it running as usual.");
        } else if (m.status == Status.STOPPED) {
            ring.set(0, color, "0", "stopped");
            msg.setText("Unplanned breakdown. Repair the fault, then restart.");
        } else {
            int h = m.result == null ? 100 : (int) Math.round(m.result.health);
            ring.set(h, color, String.valueOf(h), "health");
            if (m.status == Status.HEALTHY) {
                msg.setText("Running normally.");
            } else {
                int eta = m.etaMinutes();
                msg.setText(eta > 0 ? "Getting worse. Could fail in about " + eta + " min if nothing changes."
                        : "Getting worse. See the advice below.");
            }
        }

        for (ChartRef r : charts) {
            r.chart.setBand(m.detector.bandLow(r.channel), m.detector.bandHigh(r.channel));
            double v = m.series(r.channel).last();
            r.value.setText(Ui.f1(v) + (r.unit.isEmpty() ? "" : " " + r.unit));
            r.chart.invalidate();
        }

        sens.setText(String.format(Locale.US,
                "Alert threshold: %.2f× standard. Rises after “false alarm” feedback, falls after “useful”.",
                m.detector.getSensitivity()));

        if (m.sim != null) {
            boolean stopped = m.isFailed();
            demoNormal.setVisibility(stopped ? View.GONE : View.VISIBLE);
            demoStopped.setVisibility(stopped ? View.VISIBLE : View.GONE);
            if (stopped) {
                faultInfo.setText("Stopped: " + m.sim.fault().label + " was not repaired in time.");
            } else if (m.sim.fault() == FaultMode.NONE) {
                faultInfo.setText("No fault. Pick one to simulate:");
            } else {
                faultInfo.setText("Simulating: " + m.sim.fault().label + " ("
                        + Math.round(m.sim.progress() * 100) + "% of the way to failure)");
            }
        }
    }

    private void confirmRemove() {
        new AlertDialog.Builder(act)
                .setTitle("Remove this machine?")
                .setMessage("Its history and alerts for this session will no longer be monitored.")
                .setPositiveButton("Remove", (d, w) -> {
                    eng.removeMachine(id);
                    act.showTab(0);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
