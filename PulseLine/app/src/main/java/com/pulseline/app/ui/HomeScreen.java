package com.pulseline.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.pulseline.app.MainActivity;
import com.pulseline.app.PulseEngine;
import com.pulseline.app.engine.Machine;
import com.pulseline.app.engine.MachineType;
import com.pulseline.app.engine.Status;

import java.util.HashMap;
import java.util.Map;

/** Plant overview: the four-step pipeline (Sense, Process, Alert, Learn) and every machine's health. */
public class HomeScreen implements Screen {
    private static class Refs {
        Machine m;
        TextView chip;
        TextView vals;
        TextView note;
        ProgressBar bar;
    }

    private final MainActivity act;
    private final Context c;
    private final PulseEngine eng;
    private final ScrollView scroll;
    private final LinearLayout col;
    private final Map<Long, Refs> refs = new HashMap<>();
    private TextView tSense;
    private TextView tProcess;
    private TextView tAlert;
    private TextView tLearn;

    public HomeScreen(MainActivity act) {
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
    public void onData() {
        rebuild();
    }

    @Override
    public void onTick() {
        tSense.setText(eng.samples + " readings");
        tProcess.setText(eng.machines.size() + " machines");
        tAlert.setText(eng.alerts.size() + " alerts");
        tLearn.setText((eng.useful + eng.falseAlarms) + " feedback");
        for (Refs r : refs.values()) updateCard(r);
    }

    private void rebuild() {
        col.removeAllViews();
        refs.clear();

        LinearLayout hero = Ui.vbox(c);
        hero.setBackground(Ui.bg(c, Ui.NAVY, 16));
        int hp = Ui.dp(c, 18);
        hero.setPadding(hp, hp, hp, hp);
        LinearLayout.LayoutParams hlp = Ui.lp(Ui.MATCH, Ui.WRAP);
        hlp.bottomMargin = Ui.dp(c, 12);
        hero.setLayoutParams(hlp);
        hero.addView(Ui.text(c, "Smarter Machines.", 22, Ui.WHITE, true));
        hero.addView(Ui.text(c, "Fewer Surprises.", 22, Ui.GREEN, true));
        TextView sub = Ui.text(c, "PulseLine watches each machine and warns your team in plain language before it fails.",
                13, 0xFFC5D2E0, false);
        sub.setPadding(0, Ui.dp(c, 6), 0, 0);
        hero.addView(sub);
        col.addView(hero);

        LinearLayout pipe = Ui.hbox(c);
        pipe.setLayoutParams(Ui.lp(Ui.MATCH, Ui.WRAP));
        tSense = new TextView(c);
        tProcess = new TextView(c);
        tAlert = new TextView(c);
        tLearn = new TextView(c);
        tile(pipe, "1", "Sense", Ui.GREEN, tSense);
        tile(pipe, "2", "Process", Ui.BLUE, tProcess);
        tile(pipe, "3", "Alert", Ui.AMBER, tAlert);
        tile(pipe, "4", "Learn", Ui.PURPLE, tLearn);
        col.addView(pipe);

        col.addView(Ui.sectionTitle(c, "Your machines"));
        for (Machine m : eng.machines) col.addView(machineCard(m));

        LinearLayout buttons = Ui.hbox(c);
        buttons.addView(Ui.button(c, "+ Add machine", Ui.NAVY, Ui.WHITE, v -> showAddDialog()));
        View g = new View(c);
        g.setLayoutParams(Ui.lp(Ui.dp(c, 8), 1));
        buttons.addView(g);
        buttons.addView(Ui.button(c, "Use this phone as a machine", Ui.WHITE, Ui.NAVY, v -> addPhone()));
        col.addView(buttons);

        TextView foot = Ui.text(c, "Demo speed: 1 second = 1 minute of plant time. Simulated machines stand in for the clip-on sensor kit.",
                11, Ui.MUTED, false);
        foot.setPadding(0, Ui.dp(c, 12), 0, 0);
        col.addView(foot);

        onTick();
    }

    private void tile(LinearLayout row, String num, String name, int color, TextView value) {
        LinearLayout t = Ui.vbox(c);
        t.setBackground(Ui.bg(c, Ui.CARD, 12));
        int p = Ui.dp(c, 8);
        t.setPadding(p, p, p, p);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = Ui.weight(1);
        lp.rightMargin = Ui.dp(c, 6);
        t.setLayoutParams(lp);
        TextView n = Ui.text(c, num, 12, Ui.WHITE, true);
        n.setGravity(Gravity.CENTER);
        n.setBackground(Ui.bg(c, color, 12));
        n.setLayoutParams(Ui.lp(Ui.dp(c, 24), Ui.dp(c, 24)));
        t.addView(n);
        TextView nm = Ui.text(c, name, 13, Ui.NAVY, true);
        nm.setPadding(0, Ui.dp(c, 4), 0, 0);
        t.addView(nm);
        value.setTextSize(10);
        value.setTextColor(Ui.MUTED);
        value.setGravity(Gravity.CENTER);
        t.addView(value);
        row.addView(t);
    }

    private View machineCard(final Machine m) {
        LinearLayout card = Ui.card(c);
        card.setOnClickListener(v -> act.showDetail(m.id));
        Refs r = new Refs();
        r.m = m;

        LinearLayout top = Ui.hbox(c);
        TextView name = Ui.text(c, m.name, 17, Ui.NAVY, true);
        name.setLayoutParams(Ui.weight(1));
        top.addView(name);
        r.chip = Ui.chip(c, "", Ui.BLUE);
        top.addView(r.chip);
        card.addView(top);

        card.addView(Ui.text(c, m.type.label, 12, Ui.MUTED, false));

        r.bar = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        r.bar.setMax(100);
        LinearLayout.LayoutParams blp = Ui.lp(Ui.MATCH, Ui.dp(c, 8));
        blp.topMargin = Ui.dp(c, 10);
        blp.bottomMargin = Ui.dp(c, 8);
        r.bar.setLayoutParams(blp);
        card.addView(r.bar);

        r.vals = Ui.text(c, "", 13, Ui.TEXT, false);
        card.addView(r.vals);
        r.note = Ui.text(c, "", 12, Ui.RED, true);
        card.addView(r.note);

        refs.put(m.id, r);
        updateCard(r);
        return card;
    }

    private void updateCard(Refs r) {
        Machine m = r.m;
        int color = Ui.statusColor(m.status);
        Ui.setChip(c, r.chip, Ui.statusLabel(m), color);
        int pct;
        if (m.status == Status.LEARNING) {
            pct = m.result == null ? 0 : (int) Math.round(m.result.baselineProgress * 100);
        } else if (m.status == Status.STOPPED) {
            pct = 0;
        } else {
            pct = m.result == null ? 100 : (int) Math.round(m.result.health);
        }
        r.bar.setProgress(pct);
        r.bar.setProgressTintList(ColorStateList.valueOf(color));
        if (m.last == null) {
            r.vals.setText("Waiting for data…");
        } else if (m.type == MachineType.PHONE) {
            r.vals.setText(Ui.f1(m.last.temp) + " °C   ·   vibration " + Ui.f1(m.last.vib));
        } else {
            r.vals.setText(Ui.f1(m.last.temp) + " °C   ·   " + Ui.f1(m.last.vib) + " mm/s   ·   "
                    + Ui.f1(m.last.cur) + " A");
        }
        if (m.status == Status.STOPPED) r.note.setText("Unplanned breakdown. Tap to repair and restart.");
        else if (m.status == Status.LEARNING) r.note.setText("");
        else if (m.status.level > 0) r.note.setText("Tap for details and what to do.");
        else r.note.setText("");
        r.note.setTextColor(m.status.level > 0 ? Ui.RED : Ui.MUTED);
    }

    private void addPhone() {
        if (eng.phoneMachine() != null) {
            Toast.makeText(c, "This phone is already being monitored.", Toast.LENGTH_SHORT).show();
        } else if (!eng.addPhoneMachine()) {
            Toast.makeText(c, "This phone has no motion sensor.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(c, "Put the phone on a table for 30 seconds while it learns, then shake or tap the table.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showAddDialog() {
        LinearLayout box = Ui.vbox(c);
        int p = Ui.dp(c, 20);
        box.setPadding(p, Ui.dp(c, 8), p, 0);
        final EditText name = new EditText(c);
        name.setHint("Machine name, e.g. Press 2");
        name.setSingleLine(true);
        box.addView(name);
        final MachineType[] types = MachineType.simulatedTypes();
        final RadioGroup group = new RadioGroup(c);
        for (int i = 0; i < types.length; i++) {
            RadioButton rb = new RadioButton(c);
            rb.setText(types[i].label);
            rb.setId(1000 + i);
            group.addView(rb);
        }
        group.check(1000);
        box.addView(group);
        new AlertDialog.Builder(act)
                .setTitle("Add a machine")
                .setMessage("Choose the type and name it. PulseLine then spends about 30 seconds learning what normal looks like before it starts alerting.")
                .setView(box)
                .setPositiveButton("Add", (d, w) -> {
                    int idx = group.getCheckedRadioButtonId() - 1000;
                    if (idx < 0 || idx >= types.length) idx = 0;
                    eng.addMachine(name.getText().toString(), types[idx]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
