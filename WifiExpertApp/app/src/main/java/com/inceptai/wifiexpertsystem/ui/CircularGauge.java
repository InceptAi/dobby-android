package com.inceptai.wifiexpertsystem.ui;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.inceptai.wifiexpertsystem.R;


/**
 * Created by arunesh on 4/20/17.
 */

public class CircularGauge extends View {
    private static final int DEFAULT_LONG_POINTER_SIZE = 1;


    private Paint mPaint;        /* The Paint object to be used for the arc */
    private float mStrokeWidth;  /* Goes into the Paint object, default width for the arc */
    private float mBaselineStrokeWidth; /* Width for the baseline arc (not showing data) */
    private int mStrokeColor;    /* Goes into the Paint object */

    private RectF mRect;
    private String mStrokeCap;   /* How the stroke's ending should be */

    /**
     * The starting angle for the arc. The angles are counted clockwise AND they start at zero
     * being the 3'O Clock hand. Thus, 180 would be 9'O Clock.
     */
    private int mStartAngle;
    private int mSweepAngle;    /* Relative span for the arc */

    private int mStartValue;    /* Starting value in the value namespace */
    private int mEndValue;      /* Max value for the setValue() input */
    private int mValue;         /* Current value being shown */

    /**
     * Angle for one unit of value. Used to convert the given input value into angles.
     */
    private double mAnglePerUnitValue;

    /**
     * Current value shown in angle (converted using start angle, and angle per unit value).
     */
    private int mCurrentValueInAngle;

    private int mPointSize;

    private int mPointStartColor;
    private int mPointEndColor;

    private int mDividerColor;
    private int mDividerSize;
    private int mDividerStepAngle;
    private int mDividersCount;
    private boolean mDividerDrawFirst;
    private boolean mDividerDrawLast;
    public CircularGauge(Context context) {
        super(context);
    }

    public CircularGauge(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircularGauge, 0, 0);

        // stroke style
        setStrokeWidth(a.getDimension(R.styleable.CircularGauge_gaugeStrokeWidth, 10));
        setBaselineStrokeWidth(a.getDimension(R.styleable.CircularGauge_gaugeBaselineStrokeWidth, 2));
        setStrokeColor(a.getColor(R.styleable.CircularGauge_gaugeStrokeColor, ContextCompat.getColor(context, android.R.color.darker_gray)));
        setStrokeCap(a.getString(R.styleable.CircularGauge_gaugeStrokeCap));

        // angle start and sweep (opposite direction 0, 270, 180, 90)
        setStartAngle(a.getInt(R.styleable.CircularGauge_gaugeStartAngle, 0));
        setSweepAngle(a.getInt(R.styleable.CircularGauge_gaugeSweepAngle, 360));

        // scale (from mStartValue to mEndValue)
        setStartValue(a.getInt(R.styleable.CircularGauge_gaugeStartValue, 0));
        setEndValue(a.getInt(R.styleable.CircularGauge_gaugeEndValue, 1000));

        // pointer size and color
        setPointSize(a.getInt(R.styleable.CircularGauge_gaugePointSize, 0));
        setPointStartColor(a.getColor(R.styleable.CircularGauge_gaugePointStartColor, ContextCompat.getColor(context, android.R.color.white)));
        setPointEndColor(a.getColor(R.styleable.CircularGauge_gaugePointEndColor, ContextCompat.getColor(context, android.R.color.white)));

        // divider options
        int dividerSize = a.getInt(R.styleable.CircularGauge_gaugeDividerSize, 0);
        setDividerColor(a.getColor(R.styleable.CircularGauge_gaugeDividerColor, ContextCompat.getColor(context, android.R.color.white)));
        int dividerStep = a.getInt(R.styleable.CircularGauge_gaugeDividerStep, 0);
        setDividerDrawFirst(a.getBoolean(R.styleable.CircularGauge_gaugeDividerDrawFirst, true));
        setDividerDrawLast(a.getBoolean(R.styleable.CircularGauge_gaugeDividerDrawLast, true));

        // calculating one point sweep
        mAnglePerUnitValue = ((double) Math.abs(mSweepAngle) / (mEndValue - mStartValue));

        // calculating divider step
        if (dividerSize > 0) {
            mDividerSize = mSweepAngle / (Math.abs(mEndValue - mStartValue) / dividerSize);
            mDividersCount = 100 / dividerStep;
            mDividerStepAngle = mSweepAngle / mDividersCount;
        }
        a.recycle();
        init();
    }

    public CircularGauge(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        // main Paint
        mPaint = new Paint();
        mPaint.setColor(mStrokeColor);
        mPaint.setStrokeWidth(mBaselineStrokeWidth);
        mPaint.setAntiAlias(true);
        if (!TextUtils.isEmpty(mStrokeCap)) {
            if (mStrokeCap.equals("BUTT"))
                mPaint.setStrokeCap(Paint.Cap.BUTT);
            else if (mStrokeCap.equals("ROUND"))
                mPaint.setStrokeCap(Paint.Cap.ROUND);
        } else
            mPaint.setStrokeCap(Paint.Cap.BUTT);
        mPaint.setStyle(Paint.Style.STROKE);
        mRect = new RectF();

        mValue = mStartValue;
        mCurrentValueInAngle = mStartAngle;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float padding = getStrokeWidth();
        float size = getWidth()<getHeight() ? getWidth() : getHeight();
        float width = size - (2*padding);
        float height = size - (2*padding);
        float radius = (width < height ? width/2 : height/2);


        float rectLeft = (getWidth() - (2*padding))/2 - radius + padding;
        float rectTop = (getHeight() - (2*padding))/2 - radius + padding;
        float rectRight = (getWidth() - (2*padding))/2 - radius + padding + width;
        float rectBottom = (getHeight() - (2*padding))/2 - radius + padding + height;

        mRect.set(rectLeft, rectTop, rectRight, rectBottom);

        mPaint.setColor(mStrokeColor);
        mPaint.setShader(null);
        mPaint.setStrokeWidth(mBaselineStrokeWidth);
        canvas.drawArc(mRect, mStartAngle, mSweepAngle, false, mPaint);
        mPaint.setColor(mPointStartColor);
        mPaint.setShader(new LinearGradient(getWidth(), getHeight(), 0, 0, mPointEndColor, mPointStartColor, Shader.TileMode.CLAMP));
        /* Switch to mStrokeWidth when showing the real data */
        mPaint.setStrokeWidth(mStrokeWidth);
        if (mPointSize > 0) { // if size of pointer is defined
            if (mCurrentValueInAngle > mStartAngle + mPointSize/2) {
                canvas.drawArc(mRect, mCurrentValueInAngle - mPointSize/2, mPointSize, false, mPaint);
            }
            else { // to avoid exceeding start/zero point
                canvas.drawArc(mRect, mCurrentValueInAngle, mPointSize, false, mPaint);
            }
        } else { // draw from start point to value point (long pointer)
            if (mValue == mStartValue) // use non-zero default value for start point (to avoid lack of pointer for start/zero value)
                canvas.drawArc(mRect, mStartAngle, DEFAULT_LONG_POINTER_SIZE, false, mPaint);
            else
                canvas.drawArc(mRect, mStartAngle, mCurrentValueInAngle - mStartAngle, false, mPaint);
        }

        if (mDividerSize > 0) {
            mPaint.setColor(mDividerColor);
            mPaint.setShader(null);
            int i = mDividerDrawFirst ? 0 : 1;
            int max = mDividerDrawLast ? mDividersCount + 1 : mDividersCount;
            for (; i < max; i++) {
                canvas.drawArc(mRect, mStartAngle + i* mDividerStepAngle, mDividerSize, false, mPaint);
            }
        }

    }

    public void setValue(int value) {
        mValue = value;
        mCurrentValueInAngle = (int) (mStartAngle + (mValue - mStartValue) * mAnglePerUnitValue);
        invalidate();
    }

    public int getValue() {
        return mValue;
    }

    @SuppressWarnings("unused")
    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        mStrokeWidth = strokeWidth;
    }

    public void setBaselineStrokeWidth(float baselineStrokeWidth) {
        mBaselineStrokeWidth = baselineStrokeWidth;
    }

    @SuppressWarnings("unused")
    public int getStrokeColor() {
        return mStrokeColor;
    }

    public void setStrokeColor(int strokeColor) {
        mStrokeColor = strokeColor;
    }

    @SuppressWarnings("unused")
    public String getStrokeCap() {
        return mStrokeCap;
    }

    public void setStrokeCap(String strokeCap) {
        mStrokeCap = strokeCap;
    }

    @SuppressWarnings("unused")
    public int getStartAngle() {
        return mStartAngle;
    }

    public void setStartAngle(int startAngle) {
        mStartAngle = startAngle;
    }

    @SuppressWarnings("unused")
    public int getSweepAngle() {
        return mSweepAngle;
    }

    public void setSweepAngle(int sweepAngle) {
        mSweepAngle = sweepAngle;
    }

    @SuppressWarnings("unused")
    public int getStartValue() {
        return mStartValue;
    }

    public void setStartValue(int startValue) {
        mStartValue = startValue;
    }

    @SuppressWarnings("unused")
    public int getEndValue() {
        return mEndValue;
    }

    public void setEndValue(int endValue) {
        mEndValue = endValue;
    }

    @SuppressWarnings("unused")
    public int getPointSize() {
        return mPointSize;
    }

    public void setPointSize(int pointSize) {
        mPointSize = pointSize;
    }

    @SuppressWarnings("unused")
    public int getPointStartColor() {
        return mPointStartColor;
    }

    public void setPointStartColor(int pointStartColor) {
        mPointStartColor = pointStartColor;
    }

    @SuppressWarnings("unused")
    public int getPointEndColor() {
        return mPointEndColor;
    }

    public void setPointEndColor(int pointEndColor) {
        mPointEndColor = pointEndColor;
    }

    @SuppressWarnings("unused")
    public int getDividerColor() {
        return mDividerColor;
    }

    public void setDividerColor(int dividerColor) {
        mDividerColor = dividerColor;
    }

    @SuppressWarnings("unused")
    public boolean isDividerDrawFirst() {
        return mDividerDrawFirst;
    }

    public void setDividerDrawFirst(boolean dividerDrawFirst) {
        mDividerDrawFirst = dividerDrawFirst;
    }

    @SuppressWarnings("unused")
    public boolean isDividerDrawLast() {
        return mDividerDrawLast;
    }

    public void setDividerDrawLast(boolean dividerDrawLast) {
        mDividerDrawLast = dividerDrawLast;
    }
}
