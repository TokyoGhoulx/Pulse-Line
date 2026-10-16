package com.pulseline.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.pulseline.app.MainActivity;
import com.pulseline.app.PulseEngine;
import com.pulseline.app.engine.RoiCalculator;

import java.util.Locale;

/** Built-in ROI dashboard: downtime avoided made visible to leadership. */
public class RoiScreen implements Screen {
    private final MainActivity act;
    private final Context c;
    private final PulseEngine eng;
    private final ScrollView scroll;
    private final LinearLayout col;

    public RoiScreen(MainActivity act) {
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
        RoiCalculator.Output o = eng.roiOutput();
        RoiCalculator.Inputs in = eng.roi;

        col.addView(Ui.sectionTitle(c, "ROI dashboard"));

        LinearLayout hero = Ui.vbox(c);
        hero.setBackground(Ui.bg(c, Ui.NAVY, 16));
        int hp = Ui.dp(c, 18);
        hero.setPadding(hp, hp, hp, hp);
        LinearLayout.LayoutParams hlp = Ui.lp(Ui.MATCH, Ui.WRAP);
        hlp.bottomMargin = Ui.dp(c, 12);
        hero.setLayoutParams(hlp);
        hero.addView(Ui.text(c, "Downtime avoided", 13, 0xFFC5D2E0, false));
        hero.addView(Ui.text(c, RoiCalculator.inr(o.valueAvoided), 30, Ui.GREEN, true));
        hero.addView(Ui.text(c, fmt(o.hoursAvoided) + " h of unplanned downtime prevented by acting on "
                + eng.avoided + " early warning" + (eng.avoided == 1 ? "" : "s"), 13, Ui.WHITE, false));
        col.addView(hero);

        LinearLayout r1 = Ui.hbox(c);
        r1.setLayoutParams(Ui.lp(Ui.MATCH, Ui.WRAP));
        r1.addView(tile("Unplanned breakdowns", String.valueOf(eng.unplanned), eng.unplanned == 0 ? Ui.GREEN : Ui.RED));
        r1.addView(tile("Lost to breakdowns", RoiCalculator.inr(o.valueLost), o.valueLost == 0 ? Ui.GREEN : Ui.RED));
        col.addView(r1);

        LinearLayout r2 = Ui.hbox(c);
        r2.setLayoutParams(Ui.lp(Ui.MATCH, Ui.WRAP));
        r2.addView(tile("Pilot cost", RoiCalculator.inr(o.pilotCost), Ui.NAVY));
        r2.addView(tile("Net value", RoiCalculator.inr(o.net), o.net >= 0 ? Ui.GREEN : Ui.RED));
        col.addView(r2);

        LinearLayout r3 = Ui.hbox(c);
        r3.setLayoutParams(Ui.lp(Ui.MATCH, Ui.WRAP));
        r3.addView(tile("Alert accuracy", o.accuracy < 0 ? "No feedback yet" : Math.round(o.accuracy) + "%", Ui.BLUE));
        r3.addView(tile("Machines monitored", String.valueOf(eng.machines.size()), Ui.PURPLE));
        col.addView(r3);

        col.addView(Ui.sectionTitle(c, "Assumptions"));
        LinearLayout a = Ui.card(c);
        a.addView(row("Cost of downtime per hour", RoiCalculator.inr(in.costPerHour)));
        a.addView(row("Hours lost per unplanned breakdown", fmt(in.hoursPerBreakdown) + " h"));
        a.addView(row("Sensor kit price per machine", RoiCalculator.inr(in.kitPrice)));
        a.addView(row("Monthly plan per machine", RoiCalculator.inr(in.monthlyPlan)));
        a.addView(row("Pilot length", in.months + (in.months == 1 ? " month" : " months")));
        TextView note = Ui.text(c, "These are illustrative defaults. Pricing is not fixed and would be validated during the pilot, "
                + "so replace them with your plant's real numbers.", 12, Ui.MUTED, false);
        note.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 10));
        a.addView(note);
        a.addView(Ui.button(c, "Edit assumptions", Ui.NAVY, Ui.WHITE, v -> showEdit()));
        col.addView(a);

        TextView how = Ui.text(c, "How it is counted: each time an operator schedules the repair from an alert before the machine fails, "
                + "that counts as one avoided breakdown. A fault left alone until the machine stops counts as an unplanned breakdown.",
                12, Ui.MUTED, false);
        col.addView(how);
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.1f", v);
    }

    private View tile(String label, String value, int color) {
        LinearLayout t = Ui.vbox(c);
        t.setBackground(Ui.bg(c, Ui.CARD, 12));
        int p = Ui.dp(c, 12);
        t.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = Ui.weight(1);
        lp.rightMargin = Ui.dp(c, 8);
        lp.bottomMargin = Ui.dp(c, 8);
        t.setLayoutParams(lp);
        t.addView(Ui.text(c, label, 12, Ui.MUTED, false));
        t.addView(Ui.text(c, value, 18, color, true));
        return t;
    }

    private View row(String label, String value) {
        LinearLayout r = Ui.hbox(c);
        r.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 4));
        TextView l = Ui.text(c, label, 13, Ui.TEXT, false);
        l.setLayoutParams(Ui.weight(1));
        r.addView(l);
        r.addView(Ui.text(c, value, 13, Ui.NAVY, true));
        return r;
    }

    private void showEdit() {
        final RoiCalculator.Inputs in = eng.roi;
        LinearLayout box = Ui.vbox(c);
        int p = Ui.dp(c, 20);
        box.setPadding(p, Ui.dp(c, 8), p, 0);
        final EditText cost = field(box, "Cost of downtime per hour (₹)", in.costPerHour);
        final EditText hours = field(box, "Hours lost per unplanned breakdown", in.hoursPerBreakdown);
        final EditText kit = field(box, "Sensor kit price per machine (₹)", in.kitPrice);
        final EditText plan = field(box, "Monthly plan per machine (₹)", in.monthlyPlan);
        final EditText months = field(box, "Pilot length (months)", in.months);
        ScrollView sv = new ScrollView(c);
        sv.addView(box);
        new AlertDialog.Builder(act)
                .setTitle("Edit assumptions")
                .setView(sv)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        in.costPerHour = Math.max(0, parse(cost));
                        in.hoursPerBreakdown = Math.max(0, parse(hours));
                        in.kitPrice = Math.max(0, parse(kit));
                        in.monthlyPlan = Math.max(0, parse(plan));
                        in.months = (int) Math.max(1, Math.round(parse(months)));
                        eng.saveRoi();
                    } catch (NumberFormatException e) {
                        Toast.makeText(c, "Please enter numbers only.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private EditText field(LinearLayout box, String label, double value) {
        TextView t = Ui.text(c, label, 12, Ui.MUTED, false);
        t.setPadding(0, Ui.dp(c, 8), 0, 0);
        box.addView(t);
        EditText e = new EditText(c);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setSingleLine(true);
        e.setText(fmt(value));
        box.addView(e);
        return e;
    }

    private static double parse(EditText e) {
        return Double.parseDouble(e.getText().toString().trim());
    }
}
