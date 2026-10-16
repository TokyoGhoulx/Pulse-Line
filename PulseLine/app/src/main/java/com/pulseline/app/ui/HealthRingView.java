package com.pulseline.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Circular health score (0-100). */
public class HealthRingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int percent = 100;
    private String center = "--";
    private String sub = "";

    public HealthRingView(Context c) {
        super(c);
        track.setStyle(Paint.Style.STROKE);
        track.setColor(Ui.LINE);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        big.setTextAlign(Paint.Align.CENTER);
        big.setFakeBoldText(true);
        big.setColor(Ui.NAVY);
        small.setTextAlign(Paint.Align.CENTER);
        small.setColor(Ui.MUTED);
    }

    public void set(int percent, int color, String center, String sub) {
        this.percent = Math.max(0, Math.min(100, percent));
        this.center = center;
        this.sub = sub;
        arc.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float s = Math.min(w, h);
        float stroke = s * 0.11f;
        track.setStrokeWidth(stroke);
        arc.setStrokeWidth(stroke);
        float left = (w - s) / 2 + stroke / 2, top = (h - s) / 2 + stroke / 2;
        rect.set(left, top, left + s - stroke, top + s - stroke);
        canvas.drawArc(rect, -90, 360, false, track);
        canvas.drawArc(rect, -90, 360f * percent / 100f, false, arc);
        big.setTextSize(s * 0.27f);
        small.setTextSize(s * 0.11f);
        canvas.drawText(center, w / 2, h / 2 + s * 0.06f, big);
        canvas.drawText(sub, w / 2, h / 2 + s * 0.24f, small);
    }
}
