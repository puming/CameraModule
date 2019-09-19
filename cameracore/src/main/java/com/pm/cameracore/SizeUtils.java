package com.pm.cameracore;

import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author pm
 * @date 2019/9/17
 * @email puming@zdsoft.cn
 */
public class SizeUtils {
    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices        The list of sizes that the camera supports for the intended output
     *                       class
     * @param surfaceSize    The surfaceSize of the texture view relative to sensor coordinate
     * @param maxPreviewSize The maximum maxPreviewSize that can be chosen
     * @param aspectRatio    The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    public static Size chooseOptimalSize(Size[] choices, Size surfaceSize, Size maxPreviewSize, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            boolean isRange = option.getWidth() <= maxPreviewSize.getWidth() && option.getHeight() <= maxPreviewSize.getHeight();
            if (isRange && option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= surfaceSize.getWidth() &&
                        option.getHeight() >= surfaceSize.getHeight()) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e("error", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Size chooseThumbSize(Size[] sizes) {
        Size size = sizes[sizes.length - 1];
        for (int i = 0; i < sizes.length; i++) {
            Size s = sizes[i];
            int width = s.getWidth();
            int height = s.getHeight();
            if (height != 0 && width / height == 16 / 9) {
                size = s;
            }
        }
        return size;
    }

    public static Size chooseOutputSize(Size[] sizes,Size customOutputSize,Size aspectRatio,boolean matchLargest ){
        List<Size> largests = new ArrayList<>(6);
        List<Size> leasts = new ArrayList<>(6);
        int h = aspectRatio.getHeight();
        int w = aspectRatio.getWidth();
        for (int i = 0; i < sizes.length; i++) {
            Size choices = sizes[i];
            if(choices.getWidth()>= customOutputSize.getWidth()&& choices.getHeight()>= customOutputSize.getHeight()){
                leasts.add(choices);
            }
            if(choices.getHeight() == choices.getWidth() * h/w){
                largests.add(choices);
            }
        }

        if(!matchLargest){
            return Collections.min(leasts,new CompareSizesByArea());
        }

        if(!largests.isEmpty()){
            return Collections.max(largests, new CompareSizesByArea());
        }

        return customOutputSize;
    }

}
