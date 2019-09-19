package com.pm.cameraui;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

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
public class VideoFragment extends BaseCameraFragment {

    private AutoFitTextureView mTextureView;
    private VideoView mVideoView;
    private CameraController mController;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment VideoFragment.
     */
    public static VideoFragment newInstance() {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        // TODO: 2019/7/26
        fragment.setArguments(args);
        return fragment;
    }

    public VideoFragment() {
        super(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mVideoView = view.findViewById(R.id.videoView);
        mController = view.findViewById(R.id.controller);

        mController.setTip("点击录制");
        mController.setDuration(10);
        mController.setAction(CaptureButton.Action.RECORD_VIDEO);
        mController.setRecordPressMode(CaptureButton.PressMode.CLICK);
        mController.setControllerCallback(new CameraController.ControllerCallback() {
            @Override
            public void takePicture() {
            }

            @Override
            public void recordStart() {

            }

            @Override
            public void recordStop() {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onConfirm() {

            }

            @Override
            public void onClose() {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTextureView.isAvailable()) {
            onPrepareCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    @Override
    protected void openCamera(int width, int height) {
    }

    @Override
    protected void closeCamera() {
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
}
