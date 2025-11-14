package com.example.carekeeper.ui.panic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CircularProgressView extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private final RectF arcRect = new RectF();
    private float progress = 0f;

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(dpToPx(6));
        backgroundPaint.setColor(0x30FFFF00);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStrokeWidth(dpToPx(8));
        progressPaint.setColor(0xFFFFFF00);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float padding = dpToPx(6) + progressPaint.getStrokeWidth() / 2f;
        arcRect.set(padding, padding, getWidth() - padding, getHeight() - padding);

        canvas.drawArc(arcRect, 0, 360, false, backgroundPaint);
        float sweep = progress * 360f;
        canvas.drawArc(arcRect, -90f, sweep, false, progressPaint);
    }

    public void setProgress(float progress) {
        if (progress < 0f) progress = 0f;
        if (progress > 1f) progress = 1f;
        this.progress = progress;
        invalidate();
    }

    public void reset() {
        this.progress = 0f;
        invalidate();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
