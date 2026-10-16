package com.pulseline.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pulseline.app.MainActivity;
import com.pulseline.app.PulseEngine;
import com.pulseline.app.engine.RoiCalculator;

/** Roadmap (Pilot, Refine, Scale), safeguards and the guided setup. */
public class PilotScreen implements Screen {
    private static final String[][] PHASES = {
            {"Phase 1 · Pilot", "One machine, one plant. Measure downtime and alert accuracy.",
                    "p1a|Pick one critical machine to start with",
                    "p1b|Clip on the sensors and let PulseLine learn normal",
                    "p1c|Record downtime and alert accuracy for the pilot period"},
            {"Phase 2 · Refine", "Improve alerts from operator feedback and train staff.",
                    "p2a|Review every alert with the operators (useful or false alarm)",
                    "p2b|Run a short training session on reading the alerts"},
            {"Phase 3 · Scale", "Extend across lines with continuous ROI tracking.",
                    "p3a|Add the remaining critical machines, line by line",
                    "p3b|Review the ROI dashboard with leadership each month"}
    };
    private static final int[] PHASE_COLORS = {Ui.GREEN, Ui.BLUE, Ui.PURPLE};

    private final MainActivity act;
    private final Context c;
    private final PulseEngine eng;
    private final ScrollView scroll;
    private final LinearLayout col;

    public PilotScreen(MainActivity act) {
        this.act = act;
        this.c = act;
        this.eng = PulseEngine.get(act);
        scroll = new ScrollView(c);
        scroll.setBackgroundColor(Ui.BG);
        col = Ui.vbox(c);
        int p = Ui.dp(c, 16);
        col.setPadding(p, p, p, p);
        scroll.addView(col);
        rebuild();
    }

    @Override
    public View view() {
        return scroll;
    }

    @Override
    public void onTick() {
    }

    @Override
    public void onData() {
        rebuild();
    }

    private void rebuild() {
        col.removeAllViews();
        col.addView(Ui.sectionTitle(c, "Roadmap & safeguards"));

        RoiCalculator.Output o = eng.roiOutput();
        LinearLayout live = Ui.card(c);
        live.addView(Ui.text(c, "Pilot so far", 14, Ui.NAVY, true));
        live.addView(Ui.text(c, eng.machines.size() + " machines monitored · " + eng.avoided + " breakdowns avoided · "
                + eng.unplanned + " unplanned · alert accuracy "
                + (o.accuracy < 0 ? "n/a" : Math.round(o.accuracy) + "%"), 13, Ui.TEXT, false));
        col.addView(live);

        for (int i = 0; i < PHASES.length; i++) {
            String[] ph = PHASES[i];
            LinearLayout card = Ui.card(c);
            TextView head = Ui.chip(c, ph[0], PHASE_COLORS[i]);
            card.addView(head);
            TextView sub = Ui.text(c, ph[1], 13, Ui.MUTED, false);
            sub.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 4));
            card.addView(sub);
            for (int j = 2; j < ph.length; j++) {
                String[] kv = ph[j].split("\\|", 2);
                final String key = kv[0];
                CheckBox cb = new CheckBox(c);
                cb.setText(kv[1]);
                cb.setTextSize(14);
                cb.setTextColor(Ui.TEXT);
                cb.setChecked(eng.doneTasks.contains(key));
                cb.setOnCheckedChangeListener((b, checked) -> eng.setTask(key, checked));
                card.addView(cb);
            }
            col.addView(card);
        }

        col.addView(Ui.sectionTitle(c, "Safeguards"));
        LinearLayout sg = Ui.card(c);
        sg.addView(point("Secure by design", "This demo keeps all data on the phone and makes no network connections. "
                + "A live deployment would send sensor data over encrypted links (TLS)."));
        sg.addView(point("Training alongside the technology", "The guided setup walks new users through the four steps."));
        sg.addView(point("Clear alerts at every step", "Every alert says what is wrong, why it matters and what to do next."));
        sg.addView(Ui.button(c, "Show guided setup", Ui.NAVY, Ui.WHITE, v -> act.showSetupGuide()));
        col.addView(sg);

        col.addView(Ui.sectionTitle(c, "About"));
        LinearLayout ab = Ui.card(c);
        ab.addView(Ui.text(c, "PulseLine — Smart automation that systems and teams can trust.", 14, Ui.NAVY, true));
        TextView team = Ui.text(c, "Team: Bhuvan Aaryan, D Veda Chandra, M Pardhiv, G Rishitha, S Sushma, K Hansika, M Kaushik",
                12, Ui.MUTED, false);
        team.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 10));
        ab.addView(team);
        ab.addView(Ui.button(c, "Reset demo data", Ui.WHITE, Ui.RED, v -> new AlertDialog.Builder(act)
                .setTitle("Reset demo data?")
                .setMessage("This removes your machines, alerts, statistics and checklist ticks, and restores the three starter machines.")
                .setPositiveButton("Reset", (d, w) -> eng.resetDemo())
                .setNegativeButton("Cancel", null)
                .show()));
        col.addView(ab);
    }

    private View point(String title, String text) {
        LinearLayout l = Ui.vbox(c);
        l.setPadding(0, 0, 0, Ui.dp(c, 10));
        l.addView(Ui.text(c, "✓ " + title, 14, Ui.NAVY, true));
        l.addView(Ui.text(c, text, 13, Ui.MUTED, false));
        return l;
    }
}
