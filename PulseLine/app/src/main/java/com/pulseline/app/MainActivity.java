package com.pulseline.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pulseline.app.ui.AlertsScreen;
import com.pulseline.app.ui.DetailScreen;
import com.pulseline.app.ui.HomeScreen;
import com.pulseline.app.ui.PilotScreen;
import com.pulseline.app.ui.RoiScreen;
import com.pulseline.app.ui.Screen;
import com.pulseline.app.ui.Ui;

/** Single-activity shell: header, content area and a four-tab bottom bar. */
public class MainActivity extends Activity implements PulseEngine.Listener {
    private static final String[] TAB_NAMES = {"Machines", "Alerts", "ROI", "Roadmap"};

    private PulseEngine engine;
    private FrameLayout content;
    private final TextView[] tabs = new TextView[4];
    private Screen current;
    private int tab = 0;
    private boolean inDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        engine = PulseEngine.get(this);

        LinearLayout root = Ui.vbox(this);
        root.setBackgroundColor(Ui.NAVY);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });

        LinearLayout header = Ui.hbox(this);
        header.setBackgroundColor(Ui.NAVY);
        int hp = Ui.dp(this, 16);
        header.setPadding(hp, Ui.dp(this, 12), hp, Ui.dp(this, 12));
        TextView t1 = Ui.text(this, "Pulse", 20, Ui.WHITE, true);
        TextView t2 = Ui.text(this, "Line", 20, Ui.GREEN, true);
        header.addView(t1);
        header.addView(t2);
        root.addView(header);

        content = new FrameLayout(this);
        content.setLayoutParams(new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));
        content.setBackgroundColor(Ui.BG);
        root.addView(content);

        LinearLayout bar = Ui.hbox(this);
        bar.setBackgroundColor(Ui.WHITE);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            TextView t = Ui.text(this, TAB_NAMES[i], 13, Ui.MUTED, true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
            t.setLayoutParams(Ui.weight(1));
            t.setOnClickListener(v -> showTab(idx));
            tabs[i] = t;
            bar.addView(t);
        }
        root.addView(bar);
        setContentView(root);

        showTab(0);
        if (!engine.setupSeen) {
            showSetupGuide();
        } else {
            askNotificationPermission();
        }
        engine.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        engine.addListener(this);
        if (current != null) current.onData();
        updateBadge();
    }

    @Override
    protected void onStop() {
        super.onStop();
        engine.removeListener(this);
    }

    @Override
    public void onTick() {
        if (current != null) current.onTick();
    }

    @Override
    public void onData() {
        if (current != null) current.onData();
        updateBadge();
    }

    public void showTab(int i) {
        tab = i;
        inDetail = false;
        Screen s;
        switch (i) {
            case 1:
                s = new AlertsScreen(this);
                break;
            case 2:
                s = new RoiScreen(this);
                break;
            case 3:
                s = new PilotScreen(this);
                break;
            default:
                s = new HomeScreen(this);
                break;
        }
        show(s);
        for (int k = 0; k < tabs.length; k++) {
            tabs[k].setTextColor(k == i ? Ui.NAVY : Ui.MUTED);
            tabs[k].setBackgroundColor(k == i ? 0xFFE3F8EF : Color.WHITE);
        }
        updateBadge();
    }

    public void showDetail(long machineId) {
        inDetail = true;
        show(new DetailScreen(this, machineId));
    }

    private void show(Screen s) {
        current = s;
        content.removeAllViews();
        content.addView(s.view());
    }

    private void updateBadge() {
        int open = engine.openAlertCount();
        tabs[1].setText(open > 0 ? "Alerts (" + open + ")" : "Alerts");
    }

    @Override
    public void onBackPressed() {
        if (inDetail) {
            showTab(tab);
        } else {
            super.onBackPressed();
        }
    }

    /** Guided setup: the four steps in plain words. */
    public void showSetupGuide() {
        new AlertDialog.Builder(this)
                .setTitle("Welcome to PulseLine")
                .setMessage("Same machine. Smarter tomorrow.\n\n"
                        + "1. Sense — clip-on sensors measure temperature, vibration and electrical current.\n\n"
                        + "2. Process — PulseLine spends about 30 seconds learning what normal looks like for each machine, "
                        + "then spots anything abnormal.\n\n"
                        + "3. Alert — you get a plain-language message saying what is wrong and what to do.\n\n"
                        + "4. Learn — tell PulseLine if an alert was useful or a false alarm and it gets sharper.\n\n"
                        + "Try it: open a machine, inject a fault, and schedule the repair when the alert arrives.")
                .setPositiveButton("Start monitoring", (d, w) -> {
                    engine.markSetupSeen();
                    askNotificationPermission();
                })
                .show();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }
}
