package com.pulseline.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pulseline.app.MainActivity;
import com.pulseline.app.PulseEngine;
import com.pulseline.app.engine.Alert;

import java.util.Locale;

/** ALERT + LEARN: plain-language alerts, what to do, and operator feedback that tunes future alerts. */
public class AlertsScreen implements Screen {
    private final Context c;
    private final PulseEngine eng;
    private final ScrollView scroll;
    private final LinearLayout col;

    public AlertsScreen(MainActivity act) {
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
        col.addView(Ui.sectionTitle(c, "Alerts"));
        int reviewed = eng.useful + eng.falseAlarms;
        String acc = reviewed == 0 ? "No feedback yet"
                : String.format(Locale.US, "%d%% of reviewed alerts were useful", Math.round(100.0 * eng.useful / reviewed));
        TextView sum = Ui.text(c, eng.alerts.size() + " alerts · " + eng.openAlertCount() + " open · " + acc, 13, Ui.MUTED, false);
        col.addView(sum);
        TextView learn = Ui.text(c, "Tell PulseLine whether an alert was useful. It learns: false alarms make that machine less sensitive.",
                12, Ui.MUTED, false);
        learn.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 10));
        col.addView(learn);

        if (eng.alerts.isEmpty()) {
            LinearLayout empty = Ui.card(c);
            empty.addView(Ui.text(c, "All quiet", 16, Ui.NAVY, true));
            empty.addView(Ui.text(c, "No alerts yet. Open a machine and inject a fault to see PulseLine warn you before it fails.",
                    13, Ui.MUTED, false));
            col.addView(empty);
            return;
        }
        for (Alert a : eng.alerts) col.addView(card(c, eng, a));
        Button clear = Ui.button(c, "Clear all alerts", Ui.WHITE, Ui.RED, v -> eng.clearAlerts());
        col.addView(clear);
    }

    /** Builds one alert card. Used here and on the machine detail screen. */
    public static LinearLayout card(final Context c, final PulseEngine eng, final Alert a) {
        LinearLayout card = Ui.card(c);
        LinearLayout top = Ui.hbox(c);
        top.addView(Ui.chip(c, Ui.levelLabel(a.level), Ui.levelColor(a.level)));
        top.addView(Ui.text(c, "  " + a.machineName + " · " + Ui.ago(a.time), 12, Ui.MUTED, false));
        if (a.resolved) top.addView(Ui.text(c, "  ✓ handled", 12, Ui.GREEN, true));
        card.addView(top);

        TextView title = Ui.text(c, a.title, 16, Ui.NAVY, true);
        title.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 4));
        card.addView(title);
        card.addView(Ui.text(c, a.body, 14, Ui.TEXT, false));
        TextView act = Ui.text(c, "What to do: " + a.action, 14, Ui.NAVY, true);
        act.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 8));
        card.addView(act);

        LinearLayout row = Ui.hbox(c);
        boolean any = false;
        if (!a.resolved) {
            if (a.level < 3) {
                row.addView(Ui.button(c, "Schedule repair", Ui.NAVY, Ui.WHITE, v -> eng.scheduleRepair(a.id)));
            } else {
                row.addView(Ui.button(c, "Repair & restart", Ui.RED, Ui.WHITE, v -> eng.repairStopped(a.machineId)));
            }
            any = true;
        }
        if (a.level < 3) {
            if (a.feedback == 0) {
                row.addView(gap(c));
                row.addView(Ui.button(c, "Useful", 0xFFE3F8EF, 0xFF0B7A52, v -> eng.feedback(a.id, true)));
                row.addView(gap(c));
                row.addView(Ui.button(c, "False alarm", 0xFFFDE8E8, Ui.RED, v -> eng.feedback(a.id, false)));
                any = true;
            } else {
                TextView fb = Ui.text(c, a.feedback == 1 ? "  Marked useful" : "  Marked false alarm — less sensitive next time",
                        12, Ui.MUTED, false);
                row.addView(fb);
                any = true;
            }
        }
        if (any) card.addView(row);
        return card;
    }

    private static View gap(Context c) {
        View v = new View(c);
        v.setLayoutParams(Ui.lp(Ui.dp(c, 8), 1));
        return v;
    }
}
