/*
 *
 *  SlidingDrawer.java
 *
 *  Created by Garrett Franks on 1/8/13.
 *
 *
 */

package com.unitychat.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import com.unitychat.R;


public class SlidingDrawer extends ViewGroup {

    public static final int ORIENTATION_RTL = 0;
    public static final int ORIENTATION_BTT = 1;
    public static final int ORIENTATION_LTR = 2;
    public static final int ORIENTATION_TTB = 3;

    private static final int TAP_THRESHOLD = 6;
    private static final float MAXIMUM_TAP_VELOCITY = 100.0f;
    private static final float MAXIMUM_MINOR_VELOCITY = 150.0f;
    private static final float MAXIMUM_MAJOR_VELOCITY = 200.0f;
    private static final float MAXIMUM_ACCELERATION = 500.0f;
    private static final int VELOCITY_UNITS = 1000;
    private static final int MSG_ANIMATE = 1000;
    private static final int ANIMATION_FRAME_DURATION = 1000 / 60;

    private static final int EXPANDED_FULL_SHOW = -10001;
    private static final int COLLAPSED_FULL_DISMISS = -10002;

    private final int mHandleId;
    private final int mContentId;

    private View mHandle;
    private View mContent;

    private final Rect mFrame = new Rect();
    private final Rect mInvalidate = new Rect();
    private boolean mTracking;
    private boolean mLocked;

    private VelocityTracker mVelocityTracker;

    private boolean mInvert;
    private boolean mVertical;
    private boolean mExpanded;
    private int mBottomOffset;
    private int mTopOffset;
    private int mHandleHeight;
    private int mHandleWidth;

    private OnSlidingDrawerShowListener mOnSlidingDrawerShowListener;
    private OnSlidingDrawerDismissListener mOnSlidingDrawerDismissListener;
    private OnSlidingDrawerScrollListener mOnSlidingDrawerScrollListener;

    private final Handler mHandler = new SlidingHandler();
    private float mAnimatedAcceleration;
    private float mAnimatedVelocity;
    private float mAnimationPosition;
    private long mAnimationLastTime;
    private long mCurrentAnimationTime;
    private int mTouchDelta;
    private boolean mAnimating;
    private boolean mAllowSingleTap;
    private boolean mAnimateOnClick;

    private final int mTapThreshold;
    private final int mMaximumTapVelocity;
    private int mMaximumMinorVelocity;
    private int mMaximumMajorVelocity;
    private int mMaximumAcceleration;
    private final int mVelocityUnits;

    /**
     * Callback invoked when the drop down view is shown.
     */
    public static interface OnSlidingDrawerShowListener {

        /**
         * Invoked when the drop down view becomes fully shown.
         */
        public void onSlidingDrawerShown();
    }

    /**
     * Callback invoked when the drop down view is dismissed.
     */
    public static interface OnSlidingDrawerDismissListener {

        /**
         * Invoked when the drop down view becomes fully dismissed.
         */
        public void onSlidingDrawerDismissed();
    }

    /**
     * Callback invoked when the drop down view is scrolled.
     */
    public static interface OnSlidingDrawerScrollListener {

        /**
         * Invoked when the user starts dragging/flinging the drop down view's handle.
         */
        public void onScrollStarted();

        /**
         * Invoked when the user stops dragging/flinging the drop down view's handle.
         */
        public void onScrollEnded();
    }

    /**
     * Creates a new SlidingDrawer from a specified set of attributes defined in
     * XML.
     *
     * @param context The application's environment.
     * @param attrs   The attributes defined in XML.
     */
    public SlidingDrawer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Creates a new SlidingDrawer from a specified set of attributes defined in
     * XML.
     *
     * @param context  The application's environment.
     * @param attrs    The attributes defined in XML.
     * @param defStyle The style to apply to this widget.
     */
    public SlidingDrawer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingDrawer, defStyle, 0);

        int orientation = a.getInt(R.styleable.SlidingDrawer_direction, ORIENTATION_BTT);
        mVertical = (orientation == ORIENTATION_BTT || orientation == ORIENTATION_TTB);
        mBottomOffset = (int) a.getDimension(R.styleable.SlidingDrawer_bottomOffset, 0.0f);
        mTopOffset = (int) a.getDimension(R.styleable.SlidingDrawer_topOffset, 0.0f);
        mAllowSingleTap = a.getBoolean(R.styleable.SlidingDrawer_allowSingleTap, true);
        mAnimateOnClick = a.getBoolean(R.styleable.SlidingDrawer_animateOnClick, true);
        mInvert = (orientation == ORIENTATION_TTB || orientation == ORIENTATION_LTR);

        int handleId = a.getResourceId(R.styleable.SlidingDrawer_handle, 0);
        if (handleId == 0) {
            throw new IllegalArgumentException("The handle attribute is required and must refer "
                    + "to a valid child.");
        }

        int contentId = a.getResourceId(R.styleable.SlidingDrawer_content, 0);
        if (contentId == 0) {
            throw new IllegalArgumentException("The content attribute is required and must refer "
                    + "to a valid child.");
        }

        if (handleId == contentId) {
            throw new IllegalArgumentException("The content and handle attributes must refer "
                    + "to different children.");
        }
        mHandleId = handleId;
        mContentId = contentId;

        final float density = getResources().getDisplayMetrics().density;
        mTapThreshold = (int) (TAP_THRESHOLD * density + 0.5f);
        mMaximumTapVelocity = (int) (MAXIMUM_TAP_VELOCITY * density + 0.5f);
        mMaximumMinorVelocity = (int) (MAXIMUM_MINOR_VELOCITY * density + 0.5f);
        mMaximumMajorVelocity = (int) (MAXIMUM_MAJOR_VELOCITY * density + 0.5f);
        mMaximumAcceleration = (int) (MAXIMUM_ACCELERATION * density + 0.5f);
        mVelocityUnits = (int) (VELOCITY_UNITS * density + 0.5f);

        if (mInvert) {
            mMaximumAcceleration = -mMaximumAcceleration;
            mMaximumMajorVelocity = -mMaximumMajorVelocity;
            mMaximumMinorVelocity = -mMaximumMinorVelocity;
        }

        a.recycle();
        setAlwaysDrawnWithCacheEnabled(false);
    }

    @Override
    protected void onFinishInflate() {
        mHandle = findViewById(mHandleId);
        if (mHandle == null) {
            throw new IllegalArgumentException("The handle attribute is must refer to an" + " existing child.");
        }
        mHandle.setOnClickListener(new SlidingDrawerToggler());

        mContent = findViewById(mContentId);
        if (mContent == null) {
            throw new IllegalArgumentException("The content attribute is must refer to an"
                    + " existing child.");
        }
        mContent.setVisibility(View.GONE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("SlidingDrawer cannot have UNSPECIFIED dimensions");
        }

        final View handle = getHandle();
        final View content = getContent();
        measureChild(handle, widthMeasureSpec, heightMeasureSpec);

        if (mVertical) {
            int height = heightSpecSize - handle.getMeasuredHeight() - mTopOffset;
            content.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, heightSpecMode));
            heightSpecSize = handle.getMeasuredHeight() + mTopOffset + content.getMeasuredHeight();
            widthSpecSize = content.getMeasuredWidth();
            if (handle.getMeasuredWidth() > widthSpecSize)
                widthSpecSize = handle.getMeasuredWidth();
        } else {
            int width = widthSpecSize - handle.getMeasuredWidth() - mTopOffset;
            getContent().measure(MeasureSpec.makeMeasureSpec(width, widthSpecMode), heightMeasureSpec);
            widthSpecSize = handle.getMeasuredWidth() + mTopOffset + content.getMeasuredWidth();
            heightSpecSize = content.getMeasuredHeight();
            if (handle.getMeasuredHeight() > heightSpecSize)
                heightSpecSize = handle.getMeasuredHeight();
        }

        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final long drawingTime = getDrawingTime();
        final View handle = mHandle;
        final boolean isVertical = mVertical;

        drawChild(canvas, handle, drawingTime);

        if (mTracking || mAnimating) {
            final Bitmap cache = mContent.getDrawingCache();
            if (cache != null) {
                if (isVertical) {
                    if (mInvert) {
                        canvas.drawBitmap(cache, 0, handle.getTop() - (getBottom() - getTop()) + mHandleHeight, null);
                    } else {
                        canvas.drawBitmap(cache, 0, handle.getBottom(), null);
                    }
                } else {
                    canvas.drawBitmap(cache, mInvert ? handle.getLeft() - cache.getWidth() : handle.getRight(), 0, null);
                }
            } else {
                canvas.save();
                if (mInvert) {
                    canvas.translate(isVertical ? 0 : handle.getLeft() - mTopOffset - mContent.getMeasuredWidth(), isVertical ? handle.getTop() - mTopOffset - mContent.getMeasuredHeight() : 0);
                } else {
                    canvas.translate(isVertical ? 0 : handle.getLeft() - mTopOffset, isVertical ? handle.getTop() - mTopOffset : 0);
                }
                drawChild(canvas, mContent, drawingTime);
                canvas.restore();
            }
            invalidate();
        } else if (mExpanded) {
            drawChild(canvas, mContent, drawingTime);
        }
    }

    public static final String LOG_TAG = "Sliding";

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mTracking) {
            return;
        }

        final int width = r - l;
        final int height = b - t;

        final View handle = mHandle;

        int handleWidth = handle.getMeasuredWidth();
        int handleHeight = handle.getMeasuredHeight();

        Log.d(LOG_TAG, "handleHeight: " + handleHeight);

        int handleLeft;
        int handleTop;

        final View content = mContent;

        if (mVertical) {
            handleLeft = (width - handleWidth) / 2;
            if (mInvert) {
                Log.d(LOG_TAG, "content.layout(1)");
                handleTop = mExpanded ? height - mBottomOffset - handleHeight : mTopOffset;
                content.layout(0, mTopOffset, content.getMeasuredWidth(), mTopOffset + content.getMeasuredHeight());
            } else {
                handleTop = mExpanded ? mTopOffset : height - handleHeight + mBottomOffset;
                content.layout(0, mTopOffset + handleHeight, content.getMeasuredWidth(), mTopOffset + handleHeight + content.getMeasuredHeight());
            }
        } else {
            handleTop = (height - handleHeight) / 2;
            if (mInvert) {
                handleLeft = mExpanded ? width - mBottomOffset - handleWidth : mTopOffset;
                content.layout(mTopOffset, 0, mTopOffset + content.getMeasuredWidth(), content.getMeasuredHeight());
            } else {
                handleLeft = mExpanded ? mTopOffset : width - handleWidth + mBottomOffset;
                content.layout(mTopOffset + handleWidth, 0, mTopOffset + handleWidth + content.getMeasuredWidth(), content.getMeasuredHeight());
            }
        }

        handle.layout(handleLeft, handleTop, handleLeft + handleWidth, handleTop + handleHeight);
        mHandleHeight = handle.getHeight();
        mHandleWidth = handle.getWidth();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mLocked) {
            return false;
        }

        final int action = event.getAction();

        float x = event.getX();
        float y = event.getY();

        final Rect frame = mFrame;
        final View handle = mHandle;

        handle.getHitRect(frame);
        if (!mTracking && !frame.contains((int) x, (int) y)) {
            return false;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            mTracking = true;

            handle.setPressed(true);
            // Must be called before prepareTracking()
            prepareContent();

            // Must be called after prepareContent()
            if (mOnSlidingDrawerScrollListener != null) {
                mOnSlidingDrawerScrollListener.onScrollStarted();
            }

            if (mVertical) {
                final int top = mHandle.getTop();
                mTouchDelta = (int) y - top;
                prepareTracking(top);
            } else {
                final int left = mHandle.getLeft();
                mTouchDelta = (int) x - left;
                prepareTracking(left);
            }
            mVelocityTracker.addMovement(event);
        }

        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mLocked) {
            return true;
        }

        if (mTracking) {
            mVelocityTracker.addMovement(event);
            final int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    moveHandle((int) (mVertical ? event.getY() : event.getX()) - mTouchDelta);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(mVelocityUnits);

                    float yVelocity = velocityTracker.getYVelocity();
                    float xVelocity = velocityTracker.getXVelocity();
                    boolean negative;

                    final boolean vertical = mVertical;
                    if (vertical) {
                        negative = yVelocity < 0;
                        if (xVelocity < 0) {
                            xVelocity = -xVelocity;
                        }
                        // fix by Maciej Ciemi??ga.
                        if ((!mInvert && xVelocity > mMaximumMinorVelocity) || (mInvert && xVelocity < mMaximumMinorVelocity)) {
                            xVelocity = mMaximumMinorVelocity;
                        }
                    } else {
                        negative = xVelocity < 0;
                        if (yVelocity < 0) {
                            yVelocity = -yVelocity;
                        }
                        // fix by Maciej Ciemi??ga.
                        if ((!mInvert && yVelocity > mMaximumMinorVelocity) || (mInvert && yVelocity < mMaximumMinorVelocity)) {
                            yVelocity = mMaximumMinorVelocity;
                        }
                    }

                    float velocity = (float) Math.hypot(xVelocity, yVelocity);
                    if (negative) {
                        velocity = -velocity;
                    }

                    final int handleTop = mHandle.getTop();
                    final int handleLeft = mHandle.getLeft();
                    final int handleBottom = mHandle.getBottom();
                    final int handleRight = mHandle.getRight();

                    if (Math.abs(velocity) < mMaximumTapVelocity) {
                        boolean c1;
                        boolean c2;
                        boolean c3;
                        boolean c4;

                        if (mInvert) {
                            c1 = (mExpanded && (getBottom() - handleBottom) < mTapThreshold + mBottomOffset);
                            c2 = (!mExpanded && handleTop < mTopOffset + mHandleHeight - mTapThreshold);
                            c3 = (mExpanded && (getRight() - handleRight) < mTapThreshold + mBottomOffset);
                            c4 = (!mExpanded && handleLeft > mTopOffset + mHandleWidth + mTapThreshold);
                        } else {
                            c1 = (mExpanded && handleTop < mTapThreshold + mTopOffset);
                            c2 = (!mExpanded && handleTop > mBottomOffset + getBottom() - getTop() - mHandleHeight - mTapThreshold);
                            c3 = (mExpanded && handleLeft < mTapThreshold + mTopOffset);
                            c4 = (!mExpanded && handleLeft > mBottomOffset + getRight() - getLeft() - mHandleWidth - mTapThreshold);
                        }

                        Log.d(LOG_TAG, "ACTION_UP: " + "c1: " + c1 + ", c2: " + c2 + ", c3: " + c3 + ", c4: " + c4);

                        if (vertical ? c1 || c2 : c3 || c4) {

                            if (mAllowSingleTap) {
                                playSoundEffect(SoundEffectConstants.CLICK);

                                if (mExpanded) {
                                    animateDismiss(vertical ? handleTop : handleLeft);
                                } else {
                                    animateShow(vertical ? handleTop : handleLeft);
                                }
                            } else {
                                performFling(vertical ? handleTop : handleLeft, velocity, false);
                            }
                        } else {
                            performFling(vertical ? handleTop : handleLeft, velocity, false);
                        }
                    } else {
                        performFling(vertical ? handleTop : handleLeft, velocity, false);
                    }
                }
                break;
            }
        }

        return mTracking || mAnimating || super.onTouchEvent(event);
    }

    private void animateDismiss(int position) {
        prepareTracking(position);
        performFling(position, mMaximumAcceleration, true);
    }

    private void animateShow(int position) {
        prepareTracking(position);
        performFling(position, -mMaximumAcceleration, true);
    }

    private void performFling(int position, float velocity, boolean always) {
        mAnimationPosition = position;
        mAnimatedVelocity = velocity;

        boolean c1;
        boolean c2;
        boolean c3;

        if (mExpanded) {
            int bottom = mVertical ? getBottom() : getRight();
            int handleHeight = mVertical ? mHandleHeight : mHandleWidth;

            Log.d(LOG_TAG, "position: " + position + ", velocity: " + velocity + ", mMaximumMajorVelocity: " + mMaximumMajorVelocity);
            c1 = mInvert ? velocity < mMaximumMajorVelocity : velocity > mMaximumMajorVelocity;
            c2 = mInvert ? (bottom - (position + handleHeight)) + mBottomOffset > handleHeight : position > mTopOffset + (mVertical ? mHandleHeight : mHandleWidth);
            c3 = mInvert ? velocity < -mMaximumMajorVelocity : velocity > -mMaximumMajorVelocity;
            Log.d(LOG_TAG, "EXPANDED. c1: " + c1 + ", c2: " + c2 + ", c3: " + c3);
            if (always || (c1 || (c2 && c3))) {
                // We are expanded, So animate to DISMISS!
                mAnimatedAcceleration = mMaximumAcceleration;
                if (mInvert) {
                    if (velocity > 0) {
                        mAnimatedVelocity = 0;
                    }
                } else {
                    if (velocity < 0) {
                        mAnimatedVelocity = 0;
                    }
                }
            } else {
                // We are expanded, but they didn't move sufficiently to cause
                // us to retract. Animate back to the expanded position. so animate BACK to expanded!
                mAnimatedAcceleration = -mMaximumAcceleration;

                if (mInvert) {
                    if (velocity < 0) {
                        mAnimatedVelocity = 0;
                    }
                } else {
                    if (velocity > 0) {
                        mAnimatedVelocity = 0;
                    }
                }
            }
        } else {

            // WE'RE COLLAPSED

            c1 = mInvert ? velocity < mMaximumMajorVelocity : velocity > mMaximumMajorVelocity;
            c2 = mInvert ? (position < (mVertical ? getHeight() : getWidth()) / 2) : (position > (mVertical ? getHeight() : getWidth()) / 2);
            c3 = mInvert ? velocity < -mMaximumMajorVelocity : velocity > -mMaximumMajorVelocity;

            Log.d(LOG_TAG, "COLLAPSED. position: " + position + ", velocity: " + velocity + ", mMaximumMajorVelocity: " + mMaximumMajorVelocity);
            Log.d(LOG_TAG, "COLLAPSED. always: " + always + ", c1: " + c1 + ", c2: " + c2 + ", c3: " + c3);

            if (!always && (c1 || (c2 && c3))) {
                mAnimatedAcceleration = mMaximumAcceleration;

                if (mInvert) {
                    if (velocity > 0) {
                        mAnimatedVelocity = 0;
                    }
                } else {
                    if (velocity < 0) {
                        mAnimatedVelocity = 0;
                    }
                }
            } else {
                mAnimatedAcceleration = -mMaximumAcceleration;

                if (mInvert) {
                    if (velocity < 0) {
                        mAnimatedVelocity = 0;
                    }
                } else {
                    if (velocity > 0) {
                        mAnimatedVelocity = 0;
                    }
                }
            }
        }

        long now = SystemClock.uptimeMillis();
        mAnimationLastTime = now;
        mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
        mAnimating = true;
        mHandler.removeMessages(MSG_ANIMATE);
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE), mCurrentAnimationTime);
        stopTracking();
    }

    private void prepareTracking(int position) {
        mTracking = true;
        mVelocityTracker = VelocityTracker.obtain();
        boolean showing = !mExpanded;

        if (showing) {
            mAnimatedAcceleration = mMaximumAcceleration;
            mAnimatedVelocity = mMaximumMajorVelocity;
            if (mInvert)
                mAnimationPosition = mTopOffset;
            else
                mAnimationPosition = mBottomOffset + (mVertical ? getHeight() - mHandleHeight : getWidth() - mHandleWidth);
            moveHandle((int) mAnimationPosition);
            mAnimating = true;
            mHandler.removeMessages(MSG_ANIMATE);
            long now = SystemClock.uptimeMillis();
            mAnimationLastTime = now;
            mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
            mAnimating = true;
        } else {
            if (mAnimating) {
                mAnimating = false;
                mHandler.removeMessages(MSG_ANIMATE);
            }
            moveHandle(position);
        }
    }

    private void moveHandle(int position) {
        final View handle = mHandle;

        if (mVertical) {
            if (position == EXPANDED_FULL_SHOW) {
                if (mInvert)
                    handle.offsetTopAndBottom(mBottomOffset + getBottom() - getTop() - mHandleHeight);
                else
                    handle.offsetTopAndBottom(mTopOffset - handle.getTop());
                invalidate();
            } else if (position == COLLAPSED_FULL_DISMISS) {
                if (mInvert) {
                    handle.offsetTopAndBottom(mTopOffset - handle.getTop());
                } else {
                    handle.offsetTopAndBottom(mBottomOffset + getBottom() - getTop() - mHandleHeight - handle.getTop());
                }
                invalidate();
            } else {
                final int top = handle.getTop();
                int deltaY = position - top;
                if (position < mTopOffset) {
                    deltaY = mTopOffset - top;
                } else if (deltaY > mBottomOffset + getBottom() - getTop() - mHandleHeight - top) {
                    deltaY = mBottomOffset + getBottom() - getTop() - mHandleHeight - top;
                }

                handle.offsetTopAndBottom(deltaY);

                final Rect frame = mFrame;
                final Rect region = mInvalidate;

                handle.getHitRect(frame);
                region.set(frame);

                region.union(frame.left, frame.top - deltaY, frame.right, frame.bottom - deltaY);
                region.union(0, frame.bottom - deltaY, getWidth(), frame.bottom - deltaY + mContent.getHeight());

                invalidate(region);
            }
        } else {
            if (position == EXPANDED_FULL_SHOW) {
                if (mInvert)
                    handle.offsetLeftAndRight(mBottomOffset + getRight() - getLeft() - mHandleWidth);
                else
                    handle.offsetLeftAndRight(mTopOffset - handle.getLeft());
                invalidate();
            } else if (position == COLLAPSED_FULL_DISMISS) {
                if (mInvert)
                    handle.offsetLeftAndRight(mTopOffset - handle.getLeft());
                else
                    handle.offsetLeftAndRight(mBottomOffset + getRight() - getLeft() - mHandleWidth - handle.getLeft());
                invalidate();
            } else {
                final int left = handle.getLeft();
                int deltaX = position - left;
                if (position < mTopOffset) {
                    deltaX = mTopOffset - left;
                } else if (deltaX > mBottomOffset + getRight() - getLeft() - mHandleWidth - left) {
                    deltaX = mBottomOffset + getRight() - getLeft() - mHandleWidth - left;
                }
                handle.offsetLeftAndRight(deltaX);

                final Rect frame = mFrame;
                final Rect region = mInvalidate;

                handle.getHitRect(frame);
                region.set(frame);

                region.union(frame.left - deltaX, frame.top, frame.right - deltaX, frame.bottom);
                region.union(frame.right - deltaX, 0, frame.right - deltaX + mContent.getWidth(), getHeight());

                invalidate(region);
            }
        }
    }

    private void prepareContent() {
        if (mAnimating) {
            return;
        }

        // Something changed in the content, we need to honor the layout request
        // before creating the cached bitmap
        final View content = mContent;
        if (content.isLayoutRequested()) {

            if (mVertical) {
                final int handleHeight = mHandleHeight;
                int height = getBottom() - getTop() - handleHeight - mTopOffset;
                content.measure(MeasureSpec.makeMeasureSpec(getRight() - getLeft(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

                Log.d(LOG_TAG, "content.layout(2)");

                if (mInvert)
                    content.layout(0, mTopOffset, content.getMeasuredWidth(), mTopOffset + content.getMeasuredHeight());
                else
                    content.layout(0, mTopOffset + handleHeight, content.getMeasuredWidth(), mTopOffset + handleHeight + content.getMeasuredHeight());

            } else {

                final int handleWidth = mHandle.getWidth();
                int width = getRight() - getLeft() - handleWidth - mTopOffset;
                content.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getBottom() - getTop(), MeasureSpec.EXACTLY));

                if (mInvert)
                    content.layout(mTopOffset, 0, mTopOffset + content.getMeasuredWidth(), content.getMeasuredHeight());
                else
                    content.layout(handleWidth + mTopOffset, 0, mTopOffset + handleWidth + content.getMeasuredWidth(), content.getMeasuredHeight());
            }
        }
        // Try only once... we should really loop but it's not a big deal
        // if the draw was cancelled, it will only be temporary anyway
        content.getViewTreeObserver().dispatchOnPreDraw();
        content.buildDrawingCache();

        content.setVisibility(View.GONE);
    }

    private void stopTracking() {
        mHandle.setPressed(false);
        mTracking = false;

        if (mOnSlidingDrawerScrollListener != null) {
            mOnSlidingDrawerScrollListener.onScrollEnded();
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void doAnimation() {
        if (mAnimating) {
            incrementAnimation();

            if (mInvert) {
                if (mAnimationPosition < mTopOffset) {
                    mAnimating = false;
                    dismissSlidingDrawer();
                } else if (mAnimationPosition >= mTopOffset + (mVertical ? getHeight() : getWidth()) - 1) {
                    mAnimating = false;
                    showSlidingDrawer();
                } else {
                    moveHandle((int) mAnimationPosition);
                    mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
                    mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE), mCurrentAnimationTime);
                }
            } else {
                if (mAnimationPosition >= mBottomOffset + (mVertical ? getHeight() : getWidth()) - 1) {
                    mAnimating = false;
                    dismissSlidingDrawer();
                } else if (mAnimationPosition < mTopOffset) {
                    mAnimating = false;
                    showSlidingDrawer();
                } else {
                    moveHandle((int) mAnimationPosition);
                    mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
                    mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE), mCurrentAnimationTime);
                }
            }
        }
    }

    private void incrementAnimation() {
        long now = SystemClock.uptimeMillis();
        float t = (now - mAnimationLastTime) / 1000.0f; // ms -> s
        final float position = mAnimationPosition;
        final float v = mAnimatedVelocity; // px/s
        final float a = mInvert ? mAnimatedAcceleration : mAnimatedAcceleration; // px/s/s
        mAnimationPosition = position + (v * t) + (0.5f * a * t * t); // px
        mAnimatedVelocity = v + (a * t); // px/s
        mAnimationLastTime = now; // ms
    }

    /**
     * Toggles the drop down view show and dismiss. Takes effect immediately.
     *
     * @see #show()
     * @see #dismiss()
     * @see #animateDismiss()
     * @see #animateShow()
     * @see #animateToggle()
     */
    public void toggle() {
        if (!mExpanded) {
            showSlidingDrawer();
        } else {
            dismissSlidingDrawer();
        }
        invalidate();
        requestLayout();
    }

    /**
     * Toggles the drop down view show and dismiss with an animation.
     *
     * @see #show()
     * @see #dismiss()
     * @see #animateDismiss()
     * @see #animateShow()
     * @see #toggle()
     */
    public void animateToggle() {
        if (!mExpanded) {
            animateShow();
        } else {
            animateDismiss();
        }
    }

    /**
     * Shows the drop down view immediately.
     *
     * @see #toggle()
     * @see #dismiss()
     * @see #animateShow()
     */
    public void show() {
        showSlidingDrawer();
        invalidate();
        requestLayout();

        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    /**
     * Dismisses the drop down view immediately.
     *
     * @see #toggle()
     * @see #show()
     * @see #animateDismiss()
     */
    public void dismiss() {
        dismissSlidingDrawer();
        invalidate();
        requestLayout();
    }

    /**
     * Dismisses the drop down view with an animation.
     *
     * @see #dismiss()
     * @see #show()
     * @see #animateShow()
     * @see #animateToggle()
     * @see #toggle()
     */
    public void animateDismiss() {
        prepareContent();
        final OnSlidingDrawerScrollListener scrollListener = mOnSlidingDrawerScrollListener;
        if (scrollListener != null) {
            scrollListener.onScrollStarted();
        }
        animateDismiss(mVertical ? mHandle.getTop() : mHandle.getLeft());

        if (scrollListener != null) {
            scrollListener.onScrollEnded();
        }
    }

    /**
     * Shows the drop down view with an animation.
     *
     * @see #dismiss()
     * @see #show()
     * @see #animateDismiss()
     * @see #animateToggle()
     * @see #toggle()
     */
    public void animateShow() {
        prepareContent();
        final OnSlidingDrawerScrollListener scrollListener = mOnSlidingDrawerScrollListener;
        if (scrollListener != null) {
            scrollListener.onScrollStarted();
        }
        animateShow(mVertical ? mHandle.getTop() : mHandle.getLeft());

        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

        if (scrollListener != null) {
            scrollListener.onScrollEnded();
        }
    }

    private void dismissSlidingDrawer() {
        moveHandle(COLLAPSED_FULL_DISMISS);
        mContent.setVisibility(View.GONE);
        mContent.destroyDrawingCache();

        if (!mExpanded) {
            return;
        }

        mExpanded = false;
        if (mOnSlidingDrawerDismissListener != null) {
            mOnSlidingDrawerDismissListener.onSlidingDrawerDismissed();
        }
    }

    private void showSlidingDrawer() {
        moveHandle(EXPANDED_FULL_SHOW);
        mContent.setVisibility(View.VISIBLE);

        if (mExpanded) {
            return;
        }

        mExpanded = true;

        if (mOnSlidingDrawerShowListener != null) {
            mOnSlidingDrawerShowListener.onSlidingDrawerShown();
        }
    }

    /**
     * Sets the listener that receives a notification when the drop down view becomes
     * shown.
     *
     * @param OnSlidingDrawerShowListener The listener to be notified when the drop down view is shown.
     */
    public void setOnSlidingDrawerShowListener(OnSlidingDrawerShowListener onSlidingDrawerShowListener) {
        mOnSlidingDrawerShowListener = onSlidingDrawerShowListener;
    }

    /**
     * Sets the listener that receives a notification when the drop down view becomes
     * dismissed.
     *
     * @param OnSlidingDrawerDismissListener The listener to be notified when the drop down view is dismissed.
     */
    public void setOnSlidingDrawerDismissListener(OnSlidingDrawerDismissListener onSlidingDrawerDismissListener) {
        mOnSlidingDrawerDismissListener = onSlidingDrawerDismissListener;
    }

    /**
     * Sets the listener that receives a notification when the drop down view starts or
     * ends a scroll. A fling is considered as a scroll. A fling will also
     * trigger a drop down view shown or drop down view dismissed event.
     *
     * @param onSlidingDrawerScrollListener The listener to be notified when scrolling starts or stops.
     */
    public void setOnSlidingDrawerScrollListener(OnSlidingDrawerScrollListener onSlidingDrawerScrollListener) {
        mOnSlidingDrawerScrollListener = onSlidingDrawerScrollListener;
    }

    /**
     * Returns the handle of the drop down view.
     *
     * @return The View reprenseting the handle of the drop down view, identified by the
     * "handle" id in XML.
     */
    public View getHandle() {
        return mHandle;
    }

    /**
     * Returns the content of the drop down view.
     *
     * @return The View reprenseting the content of the drop down view, identified by the
     * "content" id in XML.
     */
    public View getContent() {
        return mContent;
    }

    /**
     * Unlocks the SlidingDrawer so that touch events are processed.
     *
     * @see #lock()
     */
    public void unlock() {
        mLocked = false;
    }

    /**
     * Locks the SlidingDrawer so that touch events are ignores.
     *
     * @see #unlock()
     */
    public void lock() {
        mLocked = true;
    }

    /**
     * Indicates whether the drop down view is currently fully shown.
     *
     * @return True if the drop down view is shown, false otherwise.
     */
    public boolean isShowing() {
        return mExpanded;
    }

    /**
     * Indicates whether the drop down view is scrolling or flinging.
     *
     * @return True if the drop down view is scroller or flinging, false otherwise.
     */
    public boolean isMoving() {
        return mTracking || mAnimating;
    }

    private class SlidingDrawerToggler implements OnClickListener {

        public void onClick(View v) {
            if (mLocked) {
                return;
            }
            // mAllowSingleTap isn't relevant here; you're *always*
            // allowed to show/dismiss the drop down view by clicking with the
            // trackball.

            if (mAnimateOnClick) {
                animateToggle();
            } else {
                toggle();
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private class SlidingHandler extends Handler {

        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_ANIMATE:
                    doAnimation();
                    break;
            }
        }
    }
}
