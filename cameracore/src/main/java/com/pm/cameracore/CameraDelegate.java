package com.pm.cameracore;

/**
 * @author pm
 * @date 2019/9/19
 * @email puming@zdsoft.cn
 */
public interface CameraDelegate {
    void openCamera(int width,int height);
    void closeCamera();
    void startBackgroundThread();
    void stopBackgroundThread();
}
