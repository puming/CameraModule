package com.pm.cameraui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * @author pm
 */
public abstract class BaseCameraFragment extends Fragment implements TextureView.SurfaceTextureListener {
    private static final String TAG = "BaseCameraFragment";


    private static final int REQUEST_CAMERA_PERMISSION = 0;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    /**
     * 是否录制视频
     */
    boolean mIsRecord;

    public BaseCameraFragment(boolean isRecord) {
        // Required empty public constructor
        mIsRecord = isRecord;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated: ");
    }

    /**
     * 打开相机前的准备，权限的检查
     */
    protected void onPrepareCamera(int width, int height) {
        ArrayList<String> permission = new ArrayList<String>(3);
        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permission.add(Manifest.permission.CAMERA);
        }
        if (mIsRecord && ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permission.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permission.isEmpty()) {
            requestCameraPermission((String[]) permission.toArray(new String[permission.size()]));
            return;
        }
        openCamera(width, height);
    }

    /**
     * 打开相机的逻辑子类实现
     */
    protected abstract void openCamera(int width, int height);

    /**
     * 关闭相机的逻辑子类实现
     */
    protected abstract void closeCamera();


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    private void requestCameraPermission(String[] permissions) {
        for (String permission : permissions) {
            Log.d(TAG, "requestCameraPermission: " + permission);

        }
//        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
        if (false) {
//            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(permissions, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: ");
        ArrayList<String> denied = new ArrayList<>(3);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    denied.add(permissions[i]);
                }
            }
        }

        if (!denied.isEmpty()) {
            Toast.makeText(getActivity(), "请打开摄像头必要的权限", Toast.LENGTH_SHORT).show();
        } else {

        }
    }

    public void onButtonPressed(Uri uri) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
