package com.camera.zcc.cameraface;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;


public class CameraProgressBar extends View {
    /**
     * 默认缩小值
     */
    public static final float DEF_SCALE = 1.0F;
    /**
     * 默认缩小值
     */
    private float scale = DEF_SCALE;

    /**
     * 背景颜色
     */
    private int backgroundColor = Color.TRANSPARENT;
    /**
     * 外圆颜色
     */
    private int outerColor = Color.parseColor("#e8e8e8");
    /**
     * 进度颜色
     */
    private int progressColor = Color.parseColor("#efb4d9");
    /**
     * 进度宽
     */
    private int progressWidth = 4;
    /**
     * 内圆宽度
     */
    private int innerRadio = 10;
    /**
     * 进度
     */
    private int progress;
    /**
     * 最大进度
     */
    private int maxProgress = 100;
    /**
     * paint
     */
    private Paint backgroundPaint, progressPaint;
    /**
     * 圆的中心坐标点, 进度百分比
     */
    private float sweepAngle;
    /**
     * 手识识别
     */
    private GestureDetectorCompat mDetector;
    /**
     * 是否为长按录制
     */
    private boolean isLongClick;
    /**
     * 是否产生滑动
     */
    private boolean isBeingDrag;
    /**
     * 滑动单位
     */
    private int mTouchSlop;
    /**
     * 记录上一次Y轴坐标点
     */
    private float mLastY;
    /**
     * 是否长按放大
     */
    private boolean isLongScale;


    public CameraProgressBar(Context context) {
        super(context);
        init(context, null);
    }

    public CameraProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CameraProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraProgressBar);
            outerColor = a.getColor(R.styleable.CameraProgressBar_outerColor, outerColor);
            progressColor = a.getColor(R.styleable.CameraProgressBar_progressColor, progressColor);
            innerRadio = a.getDimensionPixelOffset(R.styleable.CameraProgressBar_innerRadio, innerRadio);
            progressWidth = a.getDimensionPixelOffset(R.styleable.CameraProgressBar_progressWidth, progressWidth);
            progress = a.getInt(R.styleable.CameraProgressBar_progress, progress);
            scale = a.getFloat(R.styleable.CameraProgressBar_scale, scale);
            isLongScale = a.getBoolean(R.styleable.CameraProgressBar_isLongScale, isLongScale);
            maxProgress = a.getInt(R.styleable.CameraProgressBar_maxProgress, maxProgress);
            a.recycle();
        }
        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(backgroundColor);

        progressPaint = new Paint();
        progressPaint.setAntiAlias(true);
        progressPaint.setStrokeWidth(progressWidth);
        progressPaint.setStyle(Paint.Style.STROKE);

        mDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                isLongClick = false;
                if (CameraProgressBar.this.listener != null) {
                    CameraProgressBar.this.listener.onClick(CameraProgressBar.this);
                }
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                isLongClick = true;
                postInvalidate();
                mLastY = e.getY();
                if (CameraProgressBar.this.listener != null) {
                    CameraProgressBar.this.listener.onLongClick(CameraProgressBar.this);
                }
            }
        });
        mDetector.setIsLongpressEnabled(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (width > height) {
            setMeasuredDimension(height, height);
        } else {
            setMeasuredDimension(width, width);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        float circle = width / 2.0f;

        if (/*isLongScale && */!isLongClick) {
            canvas.scale(scale, scale, circle, circle);
        }

        //画内圆
        float backgroundRadio = circle - progressWidth - innerRadio;
        canvas.drawCircle(circle, circle, backgroundRadio, backgroundPaint);

        //花外圆
        progressPaint.setColor(outerColor);
        float halfOuterWidth = progressWidth / 2.0f;
        RectF outerRectF = new RectF(halfOuterWidth, halfOuterWidth, getWidth() - halfOuterWidth, getWidth() - halfOuterWidth);
        canvas.drawArc(outerRectF, -90, 360, true, progressPaint);

        //画进度条
        progressPaint.setColor(progressColor);
        canvas.drawArc(outerRectF, -90, sweepAngle, false, progressPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isLongScale) {
            return super.onTouchEvent(event);
        }
        this.mDetector.onTouchEvent(event);
        switch(MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN:
                isLongClick = false;
                isBeingDrag = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isLongClick) {
                    float y = event.getY();
                    if (isBeingDrag) {
                        boolean isUpScroll = y < mLastY;
                        mLastY = y;
                        if (this.listener != null) {
                            this.listener.onZoom(isUpScroll);
                        }
                    } else {
                        isBeingDrag = Math.abs(y - mLastY) > mTouchSlop;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isBeingDrag = false;
                if (isLongClick) {
                    isLongClick = false;
                    postInvalidate();
                    if (this.listener != null) {
                        this.listener.onLongClickUp(this);
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (isLongClick) {
                    if (this.listener != null) {
                        this.listener.onPointerDown(event.getRawX(), event.getRawY());
                    }
                }
                break;
        }
        return true;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        Parcelable superData = super.onSaveInstanceState();
        bundle.putParcelable("superData", superData);
        bundle.putInt("progress", progress);
        bundle.putInt("maxProgress", maxProgress);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        Parcelable superData = bundle.getParcelable("superData");
        progress = bundle.getInt("progress");
        maxProgress = bundle.getInt("maxProgress");
        super.onRestoreInstanceState(superData);
    }

    /**
     * 设置进度
     * @param progress
     */
    public void setProgress(int progress) {
        if (progress <= 0) progress = 0;
        if (progress >= maxProgress) progress = maxProgress;
        if (progress == this.progress) return;
        this.progress = progress;
        this.sweepAngle = ((float) progress / maxProgress) * 360;
        if(progress == 99){
            sweepAngle = 360;
        }
        postInvalidate();
    }

    /**
     * 还原到初始状态
     */
    public void reset() {
        isLongClick = false;
        this.progress = 0;
        this.sweepAngle = 0;
        postInvalidate();
    }

    public int getProgress() {
        return progress;
    }

    public void setLongScale(boolean longScale) {
        isLongScale = longScale;
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
    }

    private OnProgressTouchListener listener;

    public void setOnProgressTouchListener(OnProgressTouchListener listener) {
        this.listener = listener;
    }

    /**
     * 进度触摸监听
     */
    public interface OnProgressTouchListener {
        /**
         * 单击
         * @param progressBar
         */
        void onClick(CameraProgressBar progressBar);

        /**
         * 长按
         * @param progressBar
         */
        void onLongClick(CameraProgressBar progressBar);

        /**
         * 移动
         * @param zoom true放大
         */
        void onZoom(boolean zoom);

        /**
         * 长按抬起
         * @param progressBar
         */
        void onLongClickUp(CameraProgressBar progressBar);

        /**
         * 触摸对焦
         * @param rawX
         * @param rawY
         */

        void onPointerDown(float rawX, float rawY);
    }

    public void start() {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 360);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                sweepAngle = (float) valueAnimator.getAnimatedValue();
                //获取到需要绘制的角度，重新绘制
                invalidate();
            }
        });
        //这里是时间获取和赋值
        ValueAnimator animator1 = ValueAnimator.ofInt(10, 0);
        animator1.setInterpolator(new LinearInterpolator());
        animator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int time = (int) valueAnimator.getAnimatedValue();
            }
        });
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animator, animator1);
        set.setDuration(10 * 1000);
        set.setInterpolator(new LinearInterpolator());
        set.start();
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                clearAnimation();
                isLongClick = false;
                postInvalidate();
                if (listener != null) {
//                    listener.onFinish();
                }
            }
        });

    }

}
