package com.example.speed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class DrawArcView extends View {
    private Paint paint;
    private RectF rectF;
    private int centerX, centerY, radius;
    private float startAngle, sweepAngle;

    public DrawArcView(Context context, int centXInPx, int centYInPx, int radiusInPx, float degreeStart, float degreeStop) {
        super(context);

        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth( 3.85f * context.getResources().getDisplayMetrics().density);
        paint.setStrokeCap(Paint.Cap.ROUND);

        this.centerX = centXInPx;
        this.centerY = centYInPx;
        this.radius = radiusInPx;
        this.startAngle = degreeStart;
        this.sweepAngle = degreeStop - degreeStart;

        rectF = new RectF(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(rectF, 90-startAngle, -sweepAngle, false, paint);
    }
}
