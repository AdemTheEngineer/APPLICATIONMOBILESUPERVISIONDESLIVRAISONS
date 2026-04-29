package com.example.applicationmobilesupervisiondeslivraisons.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom donut-chart View that draws proportional coloured arcs
 * representing the delivery status distribution.
 *
 * Colours:
 *   Pending   → #F59E0B  (amber)
 *   In Transit→ #3B82F6  (blue)
 *   Delivered → #10B981  (green)
 *   Failed    → #EF4444  (red)
 */
public class DonutChartView extends View {

    // ── Data ────────────────────────────────────────────────────────────────────
    private int pending   = 0;
    private int transit   = 0;
    private int delivered = 0;
    private int failed    = 0;

    // ── Paints ──────────────────────────────────────────────────────────────────
    private final Paint arcPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect    = new RectF();

    // ── Colours ─────────────────────────────────────────────────────────────────
    private static final int COLOR_PENDING   = 0xFFF59E0B;
    private static final int COLOR_TRANSIT   = 0xFF3B82F6;
    private static final int COLOR_DELIVERED = 0xFF10B981;
    private static final int COLOR_FAILED    = 0xFFEF4444;
    private static final int COLOR_EMPTY     = 0xFFE5E7EB;  // light gray when no data

    private static final float STROKE_WIDTH_DP = 18f;

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);
        bgPaint.setColor(COLOR_EMPTY);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        textPaint.setColor(0xFF111827);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        labelPaint.setColor(0xFF6B7280);
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Update the chart with new data.  The view will be redrawn automatically.
     */
    public void setData(int pending, int transit, int delivered, int failed) {
        this.pending   = pending;
        this.transit   = transit;
        this.delivered = delivered;
        this.failed    = failed;
        invalidate();
    }

    // ── Drawing ─────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float density = getResources().getDisplayMetrics().density;
        float strokeWidth = STROKE_WIDTH_DP * density;

        arcPaint.setStrokeWidth(strokeWidth);
        bgPaint.setStrokeWidth(strokeWidth);

        // Compute the bounding rectangle for the arcs
        float half = strokeWidth / 2f;
        float size = Math.min(w, h);
        float left = (w - size) / 2f + half;
        float top  = (h - size) / 2f + half;
        arcRect.set(left, top, left + size - strokeWidth, top + size - strokeWidth);

        int total = pending + transit + delivered + failed;

        if (total == 0) {
            // Draw a full gray ring when there is no data
            canvas.drawArc(arcRect, 0, 360, false, bgPaint);
        } else {
            // Gap angle between segments (degrees)
            float gap = total > 1 ? 3f : 0f;
            int segments = countNonZero();
            float totalGap = gap * segments;

            float startAngle = -90f; // start at top

            startAngle = drawSegment(canvas, startAngle, pending,   total, totalGap, gap, COLOR_PENDING);
            startAngle = drawSegment(canvas, startAngle, transit,   total, totalGap, gap, COLOR_TRANSIT);
            startAngle = drawSegment(canvas, startAngle, delivered, total, totalGap, gap, COLOR_DELIVERED);
            drawSegment(canvas, startAngle, failed, total, totalGap, gap, COLOR_FAILED);
        }

        // ── Center text ─────────────────────────────────────────────────────────
        float centerX = w / 2f;
        float centerY = h / 2f;

        textPaint.setTextSize(28f * density);
        labelPaint.setTextSize(11f * density);

        // Draw "total" number
        canvas.drawText(String.valueOf(total), centerX, centerY + 4 * density, textPaint);
        // Draw "Total" label
        canvas.drawText("Total", centerX, centerY + 18 * density, labelPaint);
    }

    private float drawSegment(Canvas canvas, float startAngle, int value, int total,
                              float totalGap, float gap, int color) {
        if (value <= 0) return startAngle;

        float sweep = ((float) value / total) * (360f - totalGap);
        arcPaint.setColor(color);
        canvas.drawArc(arcRect, startAngle, sweep, false, arcPaint);
        return startAngle + sweep + gap;
    }

    private int countNonZero() {
        int c = 0;
        if (pending   > 0) c++;
        if (transit   > 0) c++;
        if (delivered > 0) c++;
        if (failed    > 0) c++;
        return c;
    }
}
