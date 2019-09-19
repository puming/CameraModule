package com.pm.cameraui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * @author pm
 */
public class CameraController extends FrameLayout {

    ControllerCallback mCallback;

    /**
     * 拍照按钮
     */
    private CaptureButton mBtnCapture;
    /**
     * 确认按钮
     */
    private TypeButton mBtnConfirm;
    /**
     * 取消按钮
     */
    private TypeButton mBtnCancel;
    /**
     * 返回按钮,关闭camera
     */
    private CloseButton mBtnClose;
    /**
     * 左边自定义按钮
     */
    private ImageView mIvCustomLeft;
    /**
     * 右边自定义按钮
     */
    private ImageView mivCustomRight;
    /**
     * 提示文本
     */
    private TextView mTxtTip;

    private int layoutWidth;
    private int layoutHeight;
    private int buttonSize;
    private int iconLeft = 0;
    private int iconRight = 0;

    private boolean isFirst = true;

    public CameraController(Context context) {
        this(context, null);
    }

    public CameraController(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraController(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            layoutWidth = outMetrics.widthPixels;
        } else {
            layoutWidth = outMetrics.widthPixels / 2;
        }
        buttonSize = (int) (layoutWidth / 4.5f);
        layoutHeight = buttonSize + (buttonSize / 5) * 2 + 100;

        initView();
        setupView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(layoutWidth, layoutHeight);
    }

    private void setupView() {
        //默认Typebutton为隐藏
        mivCustomRight.setVisibility(GONE);
        mBtnCancel.setVisibility(GONE);
        mBtnConfirm.setVisibility(GONE);
        setRecordPressMode(CaptureButton.PressMode.CLICK);
    }

    private void initView() {
        setWillNotDraw(false);
        //拍照按钮
        mBtnCapture = new CaptureButton(getContext(), buttonSize);
        LayoutParams btnCaptureParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        btnCaptureParam.gravity = Gravity.CENTER;
        mBtnCapture.setLayoutParams(btnCaptureParam);

        //取消按钮
        mBtnCancel = new TypeButton(getContext(), TypeButton.TYPE_CANCEL, buttonSize);
        final LayoutParams mBtnCancelParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mBtnCancelParam.gravity = Gravity.CENTER_VERTICAL;
        mBtnCancelParam.setMargins((layoutWidth / 4) - buttonSize / 2, 0, 0, 0);
        mBtnCancel.setLayoutParams(mBtnCancelParam);

        //确认按钮
        mBtnConfirm = new TypeButton(getContext(), TypeButton.TYPE_CONFIRM, buttonSize);
        LayoutParams btnConfirmParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        btnConfirmParam.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        btnConfirmParam.setMargins(0, 0, (layoutWidth / 4) - buttonSize / 2, 0);
        mBtnConfirm.setLayoutParams(btnConfirmParam);

        //返回按钮
        mBtnClose = new CloseButton(getContext(), (int) (buttonSize / 2.5f));
        LayoutParams btnReturnParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        btnReturnParam.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
        btnReturnParam.setMargins(layoutWidth / 6, 0, 0, 0);
        mBtnClose.setLayoutParams(btnReturnParam);

        //左边自定义按钮
        mIvCustomLeft = new ImageView(getContext());
        LayoutParams ivCustomParamLeft = new LayoutParams((int) (buttonSize / 2.5f), (int) (buttonSize / 2.5f));
        ivCustomParamLeft.gravity = Gravity.CENTER_VERTICAL;
        ivCustomParamLeft.setMargins(layoutWidth / 6, 0, 0, 0);
        mIvCustomLeft.setLayoutParams(ivCustomParamLeft);

        //右边自定义按钮
        mivCustomRight = new ImageView(getContext());
        LayoutParams ivCustomParamRight = new LayoutParams((int) (buttonSize / 2.5f), (int) (buttonSize / 2.5f));
        ivCustomParamRight.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        ivCustomParamRight.setMargins(0, 0, layoutWidth / 6, 0);
        mivCustomRight.setLayoutParams(ivCustomParamRight);

        //文字
        mTxtTip = new TextView(getContext());
        LayoutParams txtParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        txtParam.gravity = Gravity.CENTER_HORIZONTAL;
        txtParam.setMargins(0, 0, 0, 0);
        mTxtTip.setText("轻触拍照，长按摄像");
        mTxtTip.setTextColor(0xFFFFFFFF);
        mTxtTip.setGravity(Gravity.CENTER);
        mTxtTip.setLayoutParams(txtParam);

        this.addView(mBtnCapture);
        this.addView(mBtnCancel);
        this.addView(mBtnConfirm);
        this.addView(mBtnClose);
//        this.addView(mIvCustomLeft);
//        this.addView(mivCustomRight);
        this.addView(mTxtTip);

        registerListener();
    }

    private void registerListener() {
        mBtnCapture.setCaptureListener(new CaptureButton.CaptureListener() {
            @Override
            public void takePictures() {
                if (mCallback != null) {
                    mCallback.takePicture();
                }
            }

            @Override
            public void recordShort(long time) {
                // TODO: 2019/9/18
                startAlphaAnimation();
            }

            @Override
            public void recordStart() {
                if (mCallback != null) {
                    mCallback.recordStart();
                }
                startAlphaAnimation();
                mBtnClose.setVisibility(GONE);
            }

            @Override
            public void recordEnd(long time) {
                if (mCallback != null) {
                    mCallback.recordStop();
                }
                startAlphaAnimation();
                startTypeBtnAnimator();
            }

            @Override
            public void recordZoom(float zoom) {
                // TODO: 2019/9/18
            }

            @Override
            public void recordError() {
                // TODO: 2019/9/18
            }
        });
        mBtnCancel.setOnClickListener(view -> {
            if (mCallback != null) {
                mCallback.onCancel();
            }
            startAlphaAnimation();
            resetCaptureLayout();
        });
        mBtnConfirm.setOnClickListener(view -> {
            if (mCallback != null) {
                mCallback.onConfirm();
            }
            startAlphaAnimation();
            resetCaptureLayout();
        });

        mBtnClose.setOnClickListener(v -> {
            if (mCallback != null) {
                mCallback.onClose();
            }
        });
    }

    public void setControllerCallback(ControllerCallback callback) {
        this.mCallback = callback;
    }

    public void setAction(CaptureButton.Action action) {
        mBtnCapture.setAction(action);
    }

    public void setRecordPressMode(CaptureButton.PressMode pressMode) {
        mBtnCapture.setRecordPressMode(pressMode);
    }

    public void resetCaptureLayout() {
        mBtnCapture.resetState();
        mBtnCancel.setVisibility(GONE);
        mBtnConfirm.setVisibility(GONE);
        mBtnCapture.setVisibility(VISIBLE);
        if (this.iconLeft != 0) {
            mIvCustomLeft.setVisibility(VISIBLE);
        } else {
            mBtnClose.setVisibility(VISIBLE);
        }
        if (this.iconRight != 0) {
            mivCustomRight.setVisibility(VISIBLE);
        }
    }

    public void startTypeBtnAnimator() {
        //拍照录制结果后的动画
        if (this.iconLeft != 0) {
            mIvCustomLeft.setVisibility(GONE);
        } else {
            mBtnClose.setVisibility(GONE);
        }
        if (this.iconRight != 0) {
            mivCustomRight.setVisibility(GONE);
        }
        mBtnCapture.setVisibility(GONE);
        mBtnCancel.setVisibility(VISIBLE);
        mBtnConfirm.setVisibility(VISIBLE);
        mBtnCancel.setClickable(false);
        mBtnConfirm.setClickable(false);
        ObjectAnimator animator_cancel = ObjectAnimator.ofFloat(mBtnCancel, "translationX", layoutWidth / 4, 0);
        ObjectAnimator animator_confirm = ObjectAnimator.ofFloat(mBtnConfirm, "translationX", -layoutWidth / 4, 0);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animator_cancel, animator_confirm);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mBtnCancel.setClickable(true);
                mBtnConfirm.setClickable(true);
            }
        });
        set.setDuration(200);
        set.start();
    }

    public void startAlphaAnimation() {
        if (isFirst) {
            ObjectAnimator animator_txt_tip = ObjectAnimator.ofFloat(mTxtTip, "alpha", 1f, 0f);
            animator_txt_tip.setDuration(500);
            animator_txt_tip.start();
            isFirst = false;
        }
    }

    public void setTextWithAnimation(String tip) {
        mTxtTip.setText(tip);
        ObjectAnimator animator_txt_tip = ObjectAnimator.ofFloat(mTxtTip, "alpha", 0f, 1f, 1f, 0f);
        animator_txt_tip.setDuration(2500);
        animator_txt_tip.start();
    }

    public void setDuration(int duration) {
        mBtnCapture.setDuration(duration);
    }

    public void setButtonFeatures(int state) {
        mBtnCapture.setButtonFeatures(state);
    }

    public void setTip(String tip) {
        mTxtTip.setText(tip);
    }

    public void showTip() {
        mTxtTip.setVisibility(VISIBLE);
    }

    public void showRightCustomView() {
        mivCustomRight.setVisibility(VISIBLE);
    }

    public void showLeftCustomView() {
        mivCustomRight.setVisibility(VISIBLE);
    }

    public void setIconSrc(int iconLeft, int iconRight) {
        this.iconLeft = iconLeft;
        this.iconRight = iconRight;
        if (this.iconLeft != 0) {
            mIvCustomLeft.setImageResource(iconLeft);
            mIvCustomLeft.setVisibility(VISIBLE);
            mBtnClose.setVisibility(GONE);
        } else {
            mIvCustomLeft.setVisibility(GONE);
            mBtnClose.setVisibility(VISIBLE);
        }
        if (this.iconRight != 0) {
            mivCustomRight.setImageResource(iconRight);
            mivCustomRight.setVisibility(VISIBLE);
        } else {
            mivCustomRight.setVisibility(GONE);
        }
    }

    public void setLeftClickListener(OnClickListener leftClickListener) {
        mIvCustomLeft.setOnClickListener(leftClickListener);
    }

    public void setRightClickListener(OnClickListener rightClickListener) {
        mivCustomRight.setOnClickListener(rightClickListener);
    }

    public interface ControllerCallback {
        void takePicture();

        void recordStart();

        void recordStop();

        void onCancel();

        void onConfirm();

        void onClose();
    }
}
