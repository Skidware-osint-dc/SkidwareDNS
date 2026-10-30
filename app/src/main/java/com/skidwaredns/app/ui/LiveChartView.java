package com.skidwaredns.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

import com.skidwaredns.app.util.SkidEventBus;

/**
 * Live allowed (emerald) vs blocked (crimson) spark area. Tap left half =
 * allowed, right half = blocked.
 */
public final class LiveChartView extends View {

    public interface SeriesTap {
        void onSeries(boolean blocked);
    }

    private final Paint allowFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint allowLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);
    private SeriesTap tap;

    public LiveChartView(Context ctx) {
        super(ctx);
        allowLine.setStyle(Paint.Style.STROKE);
        allowLine.setStrokeWidth(UiFactory.dp(ctx, 1.8f));
        allowLine.setColor(UiFactory.COLOR_EMERALD);
        allowLine.setStrokeJoin(Paint.Join.ROUND);
        allowLine.setStrokeCap(Paint.Cap.ROUND);
        blockLine.setStyle(Paint.Style.STROKE);
        blockLine.setStrokeWidth(UiFactory.dp(ctx, 1.8f));
        blockLine.setColor(UiFactory.COLOR_CRIMSON);
        blockLine.setStrokeJoin(Paint.Join.ROUND);
        blockLine.setStrokeCap(Paint.Cap.ROUND);
        grid.setColor(UiFactory.COLOR_DIVIDER);
        grid.setStrokeWidth(1f);
        axis.setColor(UiFactory.COLOR_MUTED);
        axis.setTextSize(UiFactory.dp(ctx, 9f));
        setClickable(true);
    }

    public void setSeriesTap(SeriesTap t) { this.tap = t; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = UiFactory.dp(getContext(), 112);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        allowFill.setShader(new LinearGradient(0, 0, 0, h,
                UiFactory.mixColor(UiFactory.COLOR_EMERALD, UiFactory.COLOR_CARD, 0.35f),
                UiFactory.mixColor(UiFactory.COLOR_EMERALD, UiFactory.COLOR_CARD, 0.92f),
                Shader.TileMode.CLAMP));
        blockFill.setShader(new LinearGradient(0, 0, 0, h,
                UiFactory.mixColor(UiFactory.COLOR_CRIMSON, UiFactory.COLOR_CARD, 0.28f),
                UiFactory.mixColor(UiFactory.COLOR_CRIMSON, UiFactory.COLOR_CARD, 0.92f),
                Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float padL = UiFactory.dp(getContext(), 4);
        float padR = UiFactory.dp(getContext(), 4);
        float padT = UiFactory.dp(getContext(), 8);
        float padB = UiFactory.dp(getContext(), 6);
        float innerW = w - padL - padR;
        float innerH = h - padT - padB;
        for (int g = 1; g <= 3; g++) {
            float y = padT + innerH * g / 4f;
            c.drawLine(padL, y, w - padR, y, grid);
        }
        int n = SkidEventBus.historyLen();
        int max = 1;
        for (int i = 0; i < n; i++) {
            int a = SkidEventBus.allowAt(i);
            int b = SkidEventBus.blockAt(i);
            if (a + b > max) max = a + b;
        }
        Path ap = new Path();
        Path bp = new Path();
        ap.moveTo(padL, padT + innerH);
        bp.moveTo(padL, padT + innerH);
        for (int i = 0; i < n; i++) {
            float x = padL + (n == 1 ? innerW / 2f : innerW * i / (n - 1f));
            float ya = padT + innerH - (SkidEventBus.allowAt(i) / (float) max) * innerH * 0.92f;
            float yb = padT + innerH - (SkidEventBus.blockAt(i) / (float) max) * innerH * 0.92f;
            ap.lineTo(x, ya);
            bp.lineTo(x, yb);
        }
        float lastX = padL + innerW;
        ap.lineTo(lastX, padT + innerH);
        ap.close();
        bp.lineTo(lastX, padT + innerH);
        bp.close();
        c.drawPath(ap, allowFill);
        c.drawPath(bp, blockFill);
        Path al = new Path();
        Path bl = new Path();
        for (int i = 0; i < n; i++) {
            float x = padL + (n == 1 ? innerW / 2f : innerW * i / (n - 1f));
            float ya = padT + innerH - (SkidEventBus.allowAt(i) / (float) max) * innerH * 0.92f;
            float yb = padT + innerH - (SkidEventBus.blockAt(i) / (float) max) * innerH * 0.92f;
            if (i == 0) { al.moveTo(x, ya); bl.moveTo(x, yb); }
            else { al.lineTo(x, ya); bl.lineTo(x, yb); }
        }
        c.drawPath(al, allowLine);
        c.drawPath(bl, blockLine);
        RectF split = new RectF(w / 2f - 0.5f, padT, w / 2f + 0.5f, padT + innerH);
        Paint sp = new Paint();
        sp.setColor(UiFactory.mixColor(UiFactory.COLOR_BORDER, UiFactory.COLOR_SKY, 0.15f));
        c.drawRect(split, sp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP && tap != null) {
            tap.onSeries(e.getX() > getWidth() / 2f);
            return true;
        }
        return e.getAction() == MotionEvent.ACTION_DOWN || super.onTouchEvent(e);
    }
}
