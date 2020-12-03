package com.pm.cameraui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.pm.cameracore.CaptureDelegate;
import com.pm.cameracore.DelegateCallback;
import com.pm.cameraui.widget.AutoFitTextureView;
import com.pm.cameraui.widget.CameraController;
import com.pm.cameraui.widget.CaptureButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author pm
 * @date 2019/9/17
 * @email puming@zdsoft.cn
 */
public class PictureFragment extends BaseCameraFragment implements DelegateCallback {
    private static final String TAG = "PictureFragment";
    private AutoFitTextureView mTextureView;
    private ImageView mImageView;

    private CaptureDelegate mDelegate;
    private CameraController mController;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PictureFragment.
     */
    public static PictureFragment newInstance() {
        PictureFragment fragment = new PictureFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public PictureFragment() {
        super(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void openCamera(int width, int height) {
        mDelegate.openCamera(width, height);
    }

    @Override
    protected void closeCamera() {
        mDelegate.closeCamera();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_picture, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mImageView = view.findViewById(R.id.iv_result);
        mController = view.findViewById(R.id.controller);

        mController.setAction(CaptureButton.Action.TAKE_PIC);
        mController.setTip("点击拍照");
        mDelegate = new CaptureDelegate(getActivity(), this);
        mController.setControllerCallback(new CameraController.ControllerCallback() {
            @Override
            public void takePicture() {
                mDelegate.takePicture();
            }

            @Override
            public void recordStart() {
            }

            @Override
            public void recordStop() {
            }

            @Override
            public void onCancel() {
                showResultImage(false, null);
            }

            @Override
            public void onConfirm() {
                showResultImage(false, null);
            }

            @Override
            public void onClose() {
                if (getActivity() != null) {
                    getActivity().finishAfterTransition();
                }
            }
        });
    }

    private void showResultImage(boolean visible, Bitmap bitmap) {
        mImageView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        mImageView.setImageBitmap(bitmap);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mDelegate.startBackgroundThread();
        if (mTextureView.isAvailable()) {
            onPrepareCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        closeCamera();
        mDelegate.stopBackgroundThread();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        onPrepareCamera(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onChangeViewSize(Size size) {
        // We fit the aspect ratio of TextureView to the size of preview we picked.
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTextureView.setAspectRatio(size.getWidth(), size.getHeight());
        } else {
            mTextureView.setAspectRatio(size.getHeight(), size.getWidth());
        }
    }

    @Override
    public void onTransformView(Matrix matrix) {
        mTextureView.setTransform(matrix);
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return mTextureView.getSurfaceTexture();
    }

    @Override
    public void onCaptureResult(Bitmap bitmap) {
        Log.d(TAG, "onCaptureResult: bitmap=" + bitmap);
        showResultImage(true, bitmap);
        mController.startAlphaAnimation();
        mController.startTypeBtnAnimator();
    }

    @Override
    public void onRecordResult(Bitmap coverBitmap, String videoAbsolutePath) {
    }
}
