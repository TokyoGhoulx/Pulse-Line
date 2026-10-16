package com.pulseline.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import com.pulseline.app.engine.Series;

/** Live line chart with the machine's learned "normal range" shaded behind the line. */
public class LineChartView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint band = new Paint();
    private final Paint grid = new Paint();
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private Series series;
    private int color = Ui.GREEN;
    private double bandLo = Double.NaN;
    private double bandHi = Double.NaN;

    public LineChartView(Context c) {
        super(c);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(c, 2.2f));
        line.setStrokeJoin(Paint.Join.ROUND);
        band.setColor(0x2219D48C);
        grid.setColor(Ui.LINE);
        grid.setStrokeWidth(Ui.dp(c, 1));
        label.setColor(Ui.MUTED);
        label.setTextSize(Ui.dp(c, 10));
    }

    public void setSeries(Series s, int color) {
        this.series = s;
        this.color = color;
    }

    public void setBand(double lo, double hi) {
        bandLo = lo;
        bandHi = hi;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float padL = Ui.dp(getContext(), 4), padR = Ui.dp(getContext(), 4);
        float padT = Ui.dp(getContext(), 6), padB = Ui.dp(getContext(), 6);
        canvas.drawLine(padL, h - padB, w - padR, h - padB, grid);
        if (series == null || series.size() < 2) {
            canvas.drawText("Waiting for data…", padL + 8, h / 2, label);
            return;
        }
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int i = 0; i < series.size(); i++) {
            double v = series.get(i);
            if (Double.isNaN(v)) continue;
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (min > max) return;
        if (!Double.isNaN(bandLo)) min = Math.min(min, bandLo);
        if (!Double.isNaN(bandHi)) max = Math.max(max, bandHi);
        double span = Math.max(max - min, 1e-6);
        min -= span * 0.1;
        max += span * 0.1;
        span = max - min;
        float cw = w - padL - padR, ch = h - padT - padB;

        if (!Double.isNaN(bandLo) && !Double.isNaN(bandHi)) {
            float y1 = (float) (padT + (max - bandHi) / span * ch);
            float y2 = (float) (padT + (max - bandLo) / span * ch);
            canvas.drawRect(padL, Math.max(padT, y1), w - padR, Math.min(h - padB, y2), band);
        }

        path.reset();
        int n = series.size();
        int cap = 90; // Machine.HISTORY: keep the line anchored to the right edge as data fills in
        for (int i = 0; i < n; i++) {
            float x = padL + cw * (cap - n + i) / (cap - 1f);
            float y = (float) (padT + (max - series.get(i)) / span * ch);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        line.setColor(color);
        canvas.drawPath(path, line);
    }
}
