package com.pulseline.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pulseline.app.engine.Machine;
import com.pulseline.app.engine.Status;

import java.util.Locale;

/** Small view-building helpers and the PulseLine colour palette (taken from the project infographic). */
public final class Ui {
    public static final int NAVY = 0xFF0B1B2E;
    public static final int GREEN = 0xFF19D48C;
    public static final int BG = 0xFFF2F6FA;
    public static final int CARD = 0xFFFFFFFF;
    public static final int TEXT = 0xFF14202E;
    public static final int MUTED = 0xFF5B6B7C;
    public static final int LINE = 0xFFE1E8EF;
    public static final int RED = 0xFFE5484D;
    public static final int AMBER = 0xFFF5A524;
    public static final int BLUE = 0xFF2F7DF6;
    public static final int PURPLE = 0xFF7C5CFA;
    public static final int WHITE = 0xFFFFFFFF;

    public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private Ui() {
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable bg(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, WRAP, w);
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = vbox(c);
        l.setBackground(bg(c, CARD, 14));
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = lp(MATCH, WRAP);
        lp.bottomMargin = dp(c, 10);
        l.setLayoutParams(lp);
        return l;
    }

    public static TextView chip(Context c, String s, int color) {
        TextView t = text(c, s, 11, WHITE, true);
        t.setBackground(bg(c, color, 10));
        t.setPadding(dp(c, 8), dp(c, 3), dp(c, 8), dp(c, 3));
        return t;
    }

    public static void setChip(Context c, TextView t, String s, int color) {
        t.setText(s);
        t.setBackground(bg(c, color, 10));
    }

    public static Button button(Context c, String s, int bgColor, int fg, View.OnClickListener l) {
        Button b = new Button(c);
        b.setAllCaps(false);
        b.setText(s);
        b.setTextColor(fg);
        b.setTextSize(13);
        b.setBackground(bg(c, bgColor, 10));
        b.setStateListAnimator(null);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setMinimumWidth(0);
        b.setMinimumHeight(dp(c, 40));
        b.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        b.setOnClickListener(l);
        return b;
    }

    public static View spacer(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(lp(MATCH, dp(c, heightDp)));
        return v;
    }

    public static TextView sectionTitle(Context c, String s) {
        TextView t = text(c, s, 17, NAVY, true);
        LinearLayout.LayoutParams lp = lp(MATCH, WRAP);
        lp.topMargin = dp(c, 6);
        lp.bottomMargin = dp(c, 8);
        t.setLayoutParams(lp);
        return t;
    }

    public static int statusColor(Status s) {
        switch (s) {
            case HEALTHY:
                return GREEN;
            case WARNING:
                return AMBER;
            case CRITICAL:
            case STOPPED:
                return RED;
            default:
                return BLUE;
        }
    }

    public static String statusLabel(Machine m) {
        if (m.status == Status.LEARNING) {
            int pct = m.result == null ? 0 : (int) Math.round(m.result.baselineProgress * 100);
            return "Learning " + pct + "%";
        }
        return m.status.label;
    }

    public static int levelColor(int level) {
        return level >= 2 ? RED : AMBER;
    }

    public static String levelLabel(int level) {
        return level >= 3 ? "Breakdown" : (level == 2 ? "Act now" : "Check soon");
    }

    public static String ago(long time) {
        long s = Math.max(0, (System.currentTimeMillis() - time) / 1000);
        if (s < 60) return s + "s ago";
        if (s < 3600) return (s / 60) + " min ago";
        if (s < 86400) return (s / 3600) + " h ago";
        return (s / 86400) + " d ago";
    }

    public static String f1(double v) {
        return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.1f", v);
    }
}
