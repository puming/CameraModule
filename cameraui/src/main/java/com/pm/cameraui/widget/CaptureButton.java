package com.pm.cameraui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.pm.cameraui.utils.Utility;


/**
 * @author pm
 */
public class CaptureButton extends View {
    private static final String TAG = "CaptureButton";
    /**
     * 默认最长录制时间为10s
     */
    private static final int DEFAULT_MAX_DURATION = 10 * 1000;
    /**
     * 默认最短录制时间为1.5s
     */
    private static final int DEFAULT_MIN_DURATION = 1500;

    /**
     * 误差时长
     */
    private static final int DEVIATION_DURATION = 100;

    /**
     * 进度条颜色
     */
    private static final int PROGRESS_COLOR = 0xEEEE5959;
    /**
     * 外圆背景色
     */
    private static final int OUTSIDE_COLOR = 0xEE868B96;
    /**
     * 内圆背景色
     */
    private static final int INSIDE_COLOR = 0xFFFFFFFF;
    /**
     * 录制标识内圆背景色
     */
    private static final int INSIDE_RECORD_COLOR = 0xFFEE5959;

    /**
     * 只能拍照
     */
    public static final int BUTTON_FEATURES_ONLY_CAPTURE = 0x101;
    /**
     * 只能录像
     */
    public static final int BUTTON_FEATURES_ONLY_RECORDER = 0x102;
    /**
     * 两者都可以
     */
    public static final int BUTTON_FEATURES_BOTH = 0x103;

    /**
     * 空闲状态
     */
    public static final int STATE_IDLE = 0x001;
    /**
     * 按下状态
     */
    public static final int STATE_PRESS = 0x002;
    /**
     * 长按状态
     */
    public static final int STATE_LONG_PRESS = 0x003;
    /**
     * 录制状态
     */
    public static final int STATE_RECORDERING = 0x004;
    /**
     * 禁止状态
     */
    public static final int STATE_BAN = 0x005;

    /**
     * 当前按钮状态
     */
    private int mButtonState;
    /**
     * 按钮可执行的功能状态,拍照 or 录制 or 两者
     */
    private int mButtonFeatures;


    //Touch_Event_Down时候记录的Y值
    private float event_Y;

    private Paint mPaint;

    //进度条宽度
    private float strokeWidth;
    //长按外圆半径变大的Size
    private int mOutsideIncreaseSize;
    //长安内圆缩小的Size
    private int mInsideReduceSize;

    //中心坐标
    private float center_X;
    private float center_Y;

    //按钮半径
    private float mButtonRadius;
    //外圆半径
    private float mButtonOutsideRadius;
    //内圆半径
    private float mButtonInsideRadius;

    //录制标识园半径
    private float mButtonInsideRecordRadius;
    //按钮大小
    private int mButtonSize;

    //录制视频的进度
    private float progress;
    //录制视频最大时间长度
    private int mMaxDuration;
    //最短录制时间限制
    private int mMinDuration;
    //记录当前录制的时间
    private int mCurrentRecordedTime;

    private RectF rectF;

    //长按后处理的逻辑Runnable
    private LongPressRunnable longPressRunnable;
    //按钮回调接口
    private CaptureListener captureListener;
    //计时器
    private RecordCountDownTimer timer;

    public enum PressMode {
        /**
         * 点击
         */
        CLICK,
        /**
         * 长安
         */
        LONGCLICK
    }

    private PressMode mPressMode = PressMode.LONGCLICK;

    public enum Action {
        /**
         * 拍照
         */
        TAKE_PIC,
        /**
         * 录像
         */
        RECORD_VIDEO
    }

    private Action mAction = Action.TAKE_PIC;

    private boolean mIsTouch;


    public CaptureButton(Context context) {
        super(context);
    }

    public CaptureButton(Context context, int size) {
        super(context);
        this.mButtonSize = size;
        mButtonRadius = size / 2.0f;

        mButtonOutsideRadius = mButtonRadius;
        mButtonInsideRadius = mButtonRadius * 0.75f;
        mButtonInsideRecordRadius = mButtonInsideRadius * 0.75f;

        strokeWidth = size / 15;
        mOutsideIncreaseSize = size / 5;
        mInsideReduceSize = size / 6;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        progress = 0;
        longPressRunnable = new LongPressRunnable();
        //初始化为空闲状态
        mButtonState = STATE_IDLE;
        //初始化按钮为可录制可拍照
        mButtonFeatures = BUTTON_FEATURES_BOTH;
        Log.i(TAG, "CaptureButton: start");
        mMaxDuration = DEFAULT_MAX_DURATION;
        Log.i(TAG, "CaptureButton: end");
        mMinDuration = DEFAULT_MIN_DURATION;

        center_X = (mButtonSize + mOutsideIncreaseSize * 2) / 2;
        center_Y = (mButtonSize + mOutsideIncreaseSize * 2) / 2;

        //进度条的外切矩形
        rectF = new RectF(
                center_X - (mButtonInsideRecordRadius - strokeWidth / 2),
                center_Y - (mButtonInsideRecordRadius - strokeWidth / 2),
                center_X + (mButtonInsideRecordRadius - strokeWidth / 2),
                center_Y + (mButtonInsideRecordRadius - strokeWidth / 2));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mButtonSize + mOutsideIncreaseSize * 2, mButtonSize + mOutsideIncreaseSize * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setStyle(Paint.Style.FILL);
        //外圆（半透明灰色）
        mPaint.setColor(OUTSIDE_COLOR);
        canvas.drawCircle(center_X, center_Y, mButtonOutsideRadius, mPaint);
        //内圆（白色）
        mPaint.setColor(INSIDE_COLOR);
        canvas.drawCircle(center_X, center_Y, mButtonInsideRadius, mPaint);
        if (mAction == Action.RECORD_VIDEO) {
            //录制标识内圆（红色）
            mPaint.setColor(INSIDE_RECORD_COLOR);
            canvas.drawCircle(center_X, center_Y, mButtonInsideRecordRadius, mPaint);
        }
        //如果状态为录制状态，则绘制录制进度条
        if (mButtonState == STATE_RECORDERING) {
            mPaint.setColor(PROGRESS_COLOR);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(strokeWidth);
            canvas.drawArc(rectF, -90, progress, false, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mAction == Action.RECORD_VIDEO && mPressMode == PressMode.CLICK) {
//            performClick();
            if (!mIsTouch) {
                //录制
                //判断按钮是否具有录制特性
                if ((mButtonFeatures == BUTTON_FEATURES_ONLY_RECORDER || mButtonFeatures == BUTTON_FEATURES_BOTH)) {
                    startRecord();
                }
            } else {
                //停止
                timer.cancel(); //停止计时器
                //移除长按逻辑的Runnable
//                removeCallbacks(longPressRunnable);
                recordEnd();    //录制结束
                mButtonState = STATE_IDLE;
            }
            mIsTouch = !mIsTouch;
            return false;
        } else {
            return touchEvent(event);
        }
    }

    private void startRecord() {
        mButtonState = STATE_LONG_PRESS;
        //没有录制权限
        if (Utility.getRecordState() != Utility.STATE_SUCCESS) {
            mButtonState = STATE_IDLE;
            if (captureListener != null) {
                captureListener.onRecordError();
                return;
            }
        }
        //启动按钮动画，外圆变大，内圆缩小
        startRecordAnimation(mButtonOutsideRadius, mButtonOutsideRadius + mOutsideIncreaseSize,
                mButtonInsideRecordRadius, mButtonInsideRecordRadius - mInsideReduceSize
        );
    }


    private boolean touchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.i(TAG, "touchEvent: " + "mButtonState = " + mButtonState);
                if (event.getPointerCount() > 1 || mButtonState != STATE_IDLE) {
                    break;
                }
                //记录Y值
                event_Y = event.getY();
                //修改当前状态为点击按下
                mButtonState = STATE_PRESS;
                if (mAction == Action.TAKE_PIC) {

                } else if (mAction == Action.RECORD_VIDEO) {
                    //判断按钮是否具有录制特性
                    if ((mButtonFeatures == BUTTON_FEATURES_ONLY_RECORDER || mButtonFeatures == BUTTON_FEATURES_BOTH)) {
                        //同时延长500启动长按后处理的逻辑Runnable
                        postDelayed(longPressRunnable, 500);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (captureListener != null
                        && mButtonState == STATE_RECORDERING
                        && (mButtonFeatures == BUTTON_FEATURES_ONLY_RECORDER || mButtonFeatures == BUTTON_FEATURES_BOTH)) {
                    //记录当前Y值与按下时候Y值的差值，调用缩放回调接口
                    captureListener.onRecordZoom(event_Y - event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                //根据当前按钮的状态进行相应的处理
                handlerUpByState();
                break;

            default:
                break;
        }
        return true;
    }

    /**
     * 当手指松开按钮时候处理的逻辑
     */
    private void handlerUpByState() {
        if (mAction == Action.TAKE_PIC) {
            if (captureListener != null && (mButtonFeatures == BUTTON_FEATURES_ONLY_CAPTURE || mButtonFeatures ==
                    BUTTON_FEATURES_BOTH)) {
                startCaptureAnimation(mButtonInsideRadius);
            } else {
                mButtonState = STATE_IDLE;
            }
        } else if (mAction == Action.RECORD_VIDEO && mPressMode == PressMode.LONGCLICK) {
            //移除长按逻辑的Runnable
            removeCallbacks(longPressRunnable);
            //停止计时器
            timer.cancel();
            //录制结束
            recordEnd();
            mButtonState = STATE_IDLE;
        }
    }

    /**
     * 录制结束
     */
    private void recordEnd() {
        if (captureListener != null) {
            if (mCurrentRecordedTime < mMinDuration) {
                //回调录制时间过短
                captureListener.onRecordShort(mCurrentRecordedTime);
            } else {
                //回调录制结束
                captureListener.onRecordStop(mCurrentRecordedTime);
            }
        }
        //重制按钮状态
        resetRecordAnim();
    }

    private void resetRecordAnim() {
        mButtonState = STATE_BAN;
        progress = 0;
        invalidate();
        //还原按钮初始状态动画
        startRecordAnimation(mButtonOutsideRadius, mButtonRadius,
                mButtonInsideRecordRadius,
                mButtonInsideRadius * 0.75f);
    }


    /**
     * 拍摄照片启动内圆动画
     *
     * @param insideStartSize
     */
    private void startCaptureAnimation(float insideStartSize) {
        ValueAnimator inside_anim = ValueAnimator.ofFloat(insideStartSize, insideStartSize * 0.75f, insideStartSize);
        inside_anim.addUpdateListener(animation -> {
            mButtonInsideRadius = (float) animation.getAnimatedValue();
            invalidate();
        });
        inside_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //回调拍照接口
                captureListener.onTakePicture();
                mButtonState = STATE_BAN;
            }
        });
        inside_anim.setDuration(100);
        inside_anim.start();
    }

    /**
     * 录制视频启动内外圆动画
     *
     * @param outsideStartSize
     * @param outsideEndSize
     * @param insideStartSize
     * @param insideEndSize
     */
    private void startRecordAnimation(float outsideStartSize, float outsideEndSize, float insideStartSize, float insideEndSize) {
//        ValueAnimator outsideAnim = ValueAnimator.ofFloat(outsideStartSize, outsideEndSize);
        ValueAnimator outsideAnim = ValueAnimator.ofFloat(outsideStartSize, outsideStartSize);
        ValueAnimator insideAnim = ValueAnimator.ofFloat(insideStartSize, insideEndSize);
        //外圆动画监听
        outsideAnim.addUpdateListener(animation -> {
            mButtonOutsideRadius = (float) animation.getAnimatedValue();
            invalidate();
        });
        //内圆动画监听
        insideAnim.addUpdateListener(animation -> {
//                mButtonInsideRadius = (float) animation.getAnimatedValue();
            mButtonInsideRecordRadius = (float) animation.getAnimatedValue();
            invalidate();
        });
        AnimatorSet set = new AnimatorSet();
        //当内外圆缩放动画结束后启动录像Runnable并且回调录像开始接口
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //设置为录制状态
                if (mButtonState == STATE_LONG_PRESS) {
                    if (captureListener != null) {
                        captureListener.onRecordStart();
                    }
                    mButtonState = STATE_RECORDERING;
                    timer.start();
                }
            }
        });
        set.playTogether(outsideAnim, insideAnim);
        set.setDuration(100);
        set.start();
    }


    private void updateProgress(long millisUntilFinished) {
        mCurrentRecordedTime = (int) (mMaxDuration - millisUntilFinished);
        progress = 360f - millisUntilFinished / (float) mMaxDuration * 360f;
        invalidate();
    }

    /**
     * 设置最长录制时长，单位秒
     *
     * @param duration
     */
    public void setMaxDuration(int duration) {
        this.mMaxDuration = duration * 1000;
        //录制定时器
        timer = new RecordCountDownTimer(mMaxDuration + DEVIATION_DURATION, mMaxDuration / 1000);
    }

    /**
     * 设置最短录制时长，单位秒
     *
     * @param duration
     */
    public void setMinDuration(int duration) {
        this.mMinDuration = duration * 1000;
    }

    /**
     * 设置回调接口
     *
     * @param captureLisTener
     */
    public void setCaptureListener(CaptureListener captureLisTener) {
        this.captureListener = captureLisTener;
    }

    /**
     * 设置按钮功能（拍照和录像）
     *
     * @param features
     */
    public void setButtonFeatures(int features) {
        this.mButtonFeatures = features;
    }

    /**
     * 是否闲置状态
     *
     * @return
     */
    public boolean isIdle() {
        return mButtonState == STATE_IDLE ? true : false;
    }

    /**
     * 重置状态
     */
    public void resetState() {
        mButtonState = STATE_IDLE;
        if(null != timer){
            timer.cancel();
        }
    }

    /**
     * 当前执行的动作，拍照或录像
     *
     * @param action
     */
    public void setAction(Action action) {
        mAction = action;
    }

    /**
     * 启动录像的方式，点击或长按
     *
     * @param mode
     */
    public void setRecordPressMode(PressMode mode) {
        mPressMode = mode;
    }


    private class RecordCountDownTimer extends CountDownTimer {
        RecordCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            updateProgress(millisUntilFinished);
        }

        @Override
        public void onFinish() {
            updateProgress(0);
            removeCallbacks(longPressRunnable);
            recordEnd();    //录制结束
            mButtonState = STATE_IDLE;
            mIsTouch = !mIsTouch;
        }
    }

    private class LongPressRunnable implements Runnable {
        @Override
        public void run() {
            startRecord();
        }
    }

    public interface CaptureListener {
        void onTakePicture();

        void onRecordShort(long time);

        void onRecordStart();

        void onRecordStop(long time);

        void onRecordZoom(float zoom);

        void onRecordError();
    }
}
