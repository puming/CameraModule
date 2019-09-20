package com.pm.cameracore;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.Size;

/**
 * @author pm
 * @date 2019/9/17
 * @email puming@zdsoft.cn
 */
public interface DelegateCallback {
    void onChangeViewSize(Size size);
    void onTransformView(Matrix matrix);
    SurfaceTexture getSurfaceTexture();
    void onCaptureResult(Bitmap bitmap);
    void onRecordResult(Bitmap coverBitmap,String videoAbsolutePath);
}
