package com.pm.cameracore;


import android.util.Size;

import java.util.Comparator;

/**
 * Compares two {@code Size}s based on their areas.
 *
 * @author pm
 * @date 2019/9/17
 * @email puming@zdsoft.cn
 */
public class CompareSizesByArea implements Comparator<Size> {

    @Override
    public int compare(Size lhs, Size rhs) {
        // We cast here to ensure the multiplications won't overflow
        long lhsArea = (long) lhs.getWidth() * lhs.getHeight();
        long rhsArea = (long) rhs.getWidth() * rhs.getHeight();
        return Long.signum(lhsArea - rhsArea);
    }

}
