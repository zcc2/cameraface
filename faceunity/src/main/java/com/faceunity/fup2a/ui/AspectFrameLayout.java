/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.faceunity.fup2a.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import com.faceunity.fup2a.misc.MiscUtil;

/**
 * Layout that adjusts to maintain a specific aspect ratio.
 */
public class AspectFrameLayout extends FrameLayout {
    private static final String TAG = "AFL";

    boolean VERBOSE_LOG = false;

    private double mTargetAspect = -1.0;        // initially use default window size

    private int mTouchSlop;
    private float mLastX;
    private float mLastY;
    private float nowX;
    private float mHorizontalScrollDelta;

    int screenWidth;

    private OnNotScrollTouchListener onNotScrollTouchListener;

    public AspectFrameLayout(Context context) {
        super(context);
        if (VERBOSE_LOG) {
            MiscUtil.Logger(TAG, "AspectFrameLayout constructor 1", false);
        }
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        screenWidth = MiscUtil.getScreenWidth(context);
    }

    public AspectFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        screenWidth = MiscUtil.getScreenWidth(context);
        if (VERBOSE_LOG) {
            MiscUtil.Logger(TAG, "AspectFrameLayout constructor 2 touchSlop "
                    + mTouchSlop + " screenWidth " + screenWidth, false);
        }
    }

    /**
     * Sets the desired aspect ratio.  The value is <code>width / height</code>.
     */
    public void setAspectRatio(double aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        Log.d(TAG, "Setting aspect ratio to " + aspectRatio
                + " (was " + mTargetAspect + ")");
        if (mTargetAspect != aspectRatio) {
            mTargetAspect = aspectRatio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (VERBOSE_LOG) {
            Log.d(TAG, "onMeasure target=" + mTargetAspect +
                    " width=[" + MeasureSpec.toString(widthMeasureSpec) +
                    "] height=[" + MeasureSpec.toString(heightMeasureSpec) + "]");
        }

        // Target aspect ratio will be < 0 if it hasn't been set yet.  In that case,
        // we just use whatever we've been handed.
        if (mTargetAspect > 0) {
            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);

            // factor the padding out
            int horizPadding = getPaddingLeft() + getPaddingRight();
            int vertPadding = getPaddingTop() + getPaddingBottom();
            initialWidth -= horizPadding;
            initialHeight -= vertPadding;

            double viewAspectRatio = (double) initialWidth / initialHeight;
            double aspectDiff = mTargetAspect / viewAspectRatio - 1;

            if (Math.abs(aspectDiff) < 0.01) {
                // We're very close already.  We don't want to risk switching from e.g.
                // non-scaled 1280x720 to scaled 1280x719 because of some floating-point
                // round-off error, so if we're really close just leave it alone.
                Log.d(TAG, "aspect ratio is good (target=" + mTargetAspect
                        + ", view=" + initialWidth + "x" + initialHeight + ")");
            } else {
                if (aspectDiff > 0) {
                    // limited by narrow width; restrict height
                    initialHeight = (int) (initialWidth / mTargetAspect);
                } else {
                    // limited by short height; restrict width
                    initialWidth = (int) (initialHeight * mTargetAspect);
                }
                Log.d(TAG, "new size=" + initialWidth + "x" + initialHeight
                        + " + padding " + horizPadding + "x" + vertPadding);
                initialWidth += horizPadding;
                initialHeight += vertPadding;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        initialWidth, MeasureSpec.EXACTLY);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        initialHeight, MeasureSpec.EXACTLY);
            }
        }
        //Log.d(TAG, "set width=[" + MeasureSpec.toString(widthMeasureSpec) +
        //        "] height=[" + View.MeasureSpec.toString(heightMeasureSpec) + "]");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int fingerMode;
    private final int MODE_DRAG = 1;
    private final int MODE_ZOOM = 2;
    private final int MODE_NONE = 3;
    private float lastTwoTouchDistance = 0;
    private float currentTwoTouchDistance = 0;
    private float deltaTwoTouchDistance = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        nowX = event.getX();
        //float nowY = event.getY();
        if (mLastX == 0) {
            mLastX = nowX;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (VERBOSE_LOG) {
                    MiscUtil.Logger(TAG, "onTouchEvent down", false);
                }
                if (onNotScrollTouchListener != null) {
                    onNotScrollTouchListener.onTouch(event);
                }
                if (VERBOSE_LOG) {
                    MiscUtil.Logger(TAG, "onTouchEvent action down", false);
                }
                mHorizontalScrollDelta = 0;
                mLastX = nowX;
                //mLastY = nowY;
                result = true;
                fingerMode = MODE_DRAG;
                break;
            case MotionEvent.ACTION_UP:
                if (onNotScrollTouchListener != null) {
                    onNotScrollTouchListener.onTouch(event);
                }
                if (VERBOSE_LOG) {
                    MiscUtil.Logger(TAG, "onTouchEvent action up", false);
                }
                mLastX = nowX;
                //mLastY = nowY;
                fingerMode = MODE_NONE;
                lastTwoTouchDistance = 0;
                currentTwoTouchDistance = 0;
                mHorizontalScrollDelta = 0;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (VERBOSE_LOG) {
                    MiscUtil.Logger(TAG, "onTouchEvent action pointer up",
                            false);
                }
                fingerMode = MODE_NONE;
                lastTwoTouchDistance = 0;
                currentTwoTouchDistance = 0;
                mHorizontalScrollDelta = 0;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (VERBOSE_LOG) {
                    MiscUtil.Logger(TAG, "onTouchEvent action pointer down",
                            false);
                }
                lastTwoTouchDistance = spaceTwoTouchEvent(event);
                fingerMode = MODE_ZOOM;
                mHorizontalScrollDelta = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if (fingerMode == MODE_DRAG) {
                    float deltaX = nowX - mLastX;
                    //float deltaY = nowY - mLastY;
                    //if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > mTouchSlop) {
                    //mHorizontalScrollDelta = deltaX / screenWidth;
                    //MiscUtil.Logger(TAG, "deltaX " + deltaX + " screenWidth " + screenWidth
                    // +  " horizontal " + mHorizontalScrollDelta, false);
                    //}
                    mHorizontalScrollDelta = deltaX / screenWidth;
                    if (VERBOSE_LOG) {
                        MiscUtil.Logger(TAG, "onTouchEvent action move deltaX "
                                + deltaX + " horizontal " + mHorizontalScrollDelta,
                                false);
                    }
                } else if (fingerMode == MODE_ZOOM) {
                    mHorizontalScrollDelta = 0;
                    if (VERBOSE_LOG) {
                        MiscUtil.Logger(TAG, "onTouchEvent zoom first "
                                + lastTwoTouchDistance + " current "
                                + currentTwoTouchDistance, false);
                    }
                    currentTwoTouchDistance = spaceTwoTouchEvent(event);
                }
                result = true;
                break;
        }
        if (onTouchListener != null) onTouchListener.onTouch(this, event);
        return true;
    }

    public float getHorizontalScrollDelta() {
        mLastX = nowX;
        return mHorizontalScrollDelta;
    }

    //TODO need a method to judge if the touch is not intended to scroll
    public void setOnNotScrollTouchListener(OnNotScrollTouchListener onNotScrollTouchListener) {
        this.onNotScrollTouchListener = onNotScrollTouchListener;
    }

    OnTouchListener onTouchListener;

    public void setMyOnTouchListener(OnTouchListener v){
        onTouchListener = v;
    }

    public interface OnNotScrollTouchListener {
        public void onTouch(MotionEvent event);
    }

    private float spaceTwoTouchEvent(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return  (float) Math.sqrt(x * x + y * y);
    }

    public float getDeltaTwoTouchDistance() {
        if (fingerMode != MODE_ZOOM) {
            return 0;
        }
        float delata = lastTwoTouchDistance == 0 || currentTwoTouchDistance == 0 ? 0 :
                (currentTwoTouchDistance - lastTwoTouchDistance) / lastTwoTouchDistance;
        lastTwoTouchDistance = currentTwoTouchDistance;
        return delata;
    }
}
