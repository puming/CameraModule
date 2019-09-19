package com.pm.cameracore;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.FragmentActivity;


/**
 * @author pm
 * @date 2019/9/17
 * @email puming@zdsoft.cn
 */
public class CaptureDelegate {
    private static final String TAG = "CaptureDelegate";
    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private final MediaActionSound mMediaActionSound;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private State mState = State.STATE_PREVIEW;

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Context mContext;
    private DelegateCallback mDelegateCallback;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Handler mMainHandler;


    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharacteristics;
    private WindowManager mWindowManager;
    private Integer mSensorOrientation;
    private ImageReader mImageReader;
    private Size mThumbSize;
    private Size mPreviewSize;
    private boolean mFlashSupported;
    private String mCameraId;


    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            //step4 开始预览相机
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
//            Activity activity = getActivity();
//            if (null != activity) {
//                activity.finish();
//            }
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
//            Log.d(TAG, "onCaptureStarted: ");
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.d(TAG, "onCaptureProgressed: ");
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
//            Log.d(TAG, "onCaptureCompleted: ");
            process(result);
        }
    };

    public CaptureDelegate(Context mContext, DelegateCallback callback) {
        this.mContext = mContext;
        this.mDelegateCallback = callback;
        mMediaActionSound = new MediaActionSound();
        mMainHandler = new Handler(mContext.getMainLooper(), msg -> {
            if(msg.what == 1){
                if(msg.obj instanceof Bitmap){
                    Bitmap bitmap = (Bitmap) msg.obj;
                    mDelegateCallback.onCaptureResult(bitmap);
                }
            }
            return true;
        });
    }

    @SuppressLint("MissingPermission")
    public void openCamera(int width, int height) {
        //step1 选择相机预览的最佳尺寸
        setupCameraOutputs(width, height);
        //step2 配置相机预览的方向
        configureTransform(width, height);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            //step3 打开照相机
            mCameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void setupCameraOutputs(int width, int height) {
        if (null == mContext) {
            return;
        }
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                mCameraCharacteristics = characteristics;
                //获取硬件支持等级
                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (null == map) {
                    continue;
                }
                prepareImageReader(map);


                mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                //mSensorOrientation is 90
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                //缩略图尺寸
                Size[] thumbnailSizes = characteristics.get(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES);
                for (Size size : thumbnailSizes) {
                    Log.d(TAG, "setupCameraOutputs: thumb size = " + size.toString());
                }
                mThumbSize = SizeUtils.chooseThumbSize(thumbnailSizes);

                //找出相对于相机传感器坐标的最佳预览尺寸
                //rotation is 0 or 1,2,3
                int rotation = mWindowManager.getDefaultDisplay().getRotation();
                boolean swappedDimensions = isSwappedDimen(rotation);
                DisplayMetrics displayMetrics = new DisplayMetrics();
                mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
                int maxPreviewWidth = displayMetrics.widthPixels;
                int maxPreviewHeight = displayMetrics.heightPixels;
                if (swappedDimensions) {
                    width = width ^ height;
                    height = width ^ height;
                    width = width ^ height;

                    maxPreviewWidth = maxPreviewWidth ^ maxPreviewHeight;
                    maxPreviewHeight = maxPreviewWidth ^ maxPreviewHeight;
                    maxPreviewWidth = maxPreviewWidth ^ maxPreviewHeight;
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                Size[] supportedPreviewSizes = map.getOutputSizes(SurfaceTexture.class);
                Size surfaceSize = new Size(width, height);
                mPreviewSize = SizeUtils.chooseOptimalSize(supportedPreviewSizes, surfaceSize, new Size(maxPreviewWidth, maxPreviewHeight), new Size(16, 9));
                changeTextureViewSize();
                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;
                mCameraId = cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
//            ErrorDialog.newInstance(getString(R.string.camera_error))
//                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            // TODO: 2019/9/17  
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mPreviewSize || null == mContext) {
            return;
        }
        int rotation = mWindowManager.getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mDelegateCallback.onTransformView(matrix);
    }

    private void changeTextureViewSize() {
        // We fit the aspect ratio of TextureView to the size of preview we picked.
        mDelegateCallback.onChangeViewSize(mPreviewSize);
    }

    /**
     * 根据屏幕方向和相机传感器方向确定是否需要交换尺寸
     *
     * @param rotation 手机屏幕方向
     * @return ture 需要交换
     */
    private boolean isSwappedDimen(int rotation) {
        boolean swappedDimensions = false;
        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                    swappedDimensions = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                    swappedDimensions = true;
                }
                break;
            default:
                break;
        }
        return swappedDimensions;
    }


    private void createCameraPreviewSession() {
        SurfaceTexture texture = mDelegateCallback.getSurfaceTexture();
        assert texture != null;

        // We configure the size of default buffer to be the size of camera preview we want.
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        Surface surface = new Surface(texture);
        Surface readerSurface = mImageReader.getSurface();
//        List<Surface> surfaces = Collections.singletonList(surface);
        try {
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface, readerSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    // The camera is already closed
                    if (null == mCameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    mCaptureSession = session;
                    // Auto focus should be continuous for camera preview.
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    // Flash is automatically enabled when necessary.
                    setAutoFlash(mPreviewBuilder);
                    // Finally, we start displaying the camera preview.
//                    mPreviewRequest = mPreviewBuilder.build();
                    try {
                        session.setRepeatingRequest(mPreviewBuilder.build(), mCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
//                    mTextureView.post(() -> {
//                        mTextureView.setOnClickListener(v ->
//                                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START));
//                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
//                    showToast("失败");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void prepareImageReader(StreamConfigurationMap map) {
        if (mImageReader != null) {
            mImageReader.close();
        }
        // For still image captures, we use the largest available size.
        Size[] outputPictureSizes = map.getOutputSizes(ImageFormat.JPEG);
        Size largest = Collections.max(Arrays.asList(outputPictureSizes), new CompareSizesByArea());
        for (Size size : outputPictureSizes) {
            Log.d(TAG, "prepareImageReader: largest=" + size.toString());
        }
        Size outputSize = SizeUtils.chooseOutputSize(outputPictureSizes, new Size(1080, 23), new Size(16, 9), true);
        mImageReader = ImageReader.newInstance(outputSize.getWidth(), outputSize.getHeight(), ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(reader -> {
            //step7 保存图片到外部存储私有目录
            File picturesDir = Objects.requireNonNull(mContext).getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSSS", Locale.getDefault());
            String dateStr = dateFormat.format(new Date());
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            File file = new File(picturesDir, dateStr + ".jpg");

            //decode and show
            decodeByteToBitmap(data,file.getAbsolutePath());
            //save image
            mBackgroundHandler.post(() -> {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                    fos.write(data);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    image.close();
                    if (null != fos) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }, mBackgroundHandler);
    }

    @WorkerThread
    private void decodeByteToBitmap(byte[] data,String absolutePath) {
        if (mContext == null) {
            return;
        }
        //decode byte to bitmap
        try {
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, true);
            int decoderWidth = decoder.getWidth();
            int decoderHeight = decoder.getHeight();
            Log.d(TAG, "decodeByteToBitmap: decoderWidth=" + decoderWidth);
            Log.d(TAG, "decodeByteToBitmap: decoderHeight=" + decoderHeight);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        Matrix matrix = new Matrix();
//                bitmap = createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);


        Message message = mMainHandler.obtainMessage();
        message.obj = bitmap;
        message.what = 1;
        mMainHandler.sendMessage(message);
        /*activity.runOnUiThread(() -> {
            mImageView.setVisibility(View.VISIBLE);
            mImageView.setImageBitmap(bitmap);
        });*/
        // TODO: 2019/9/18
    }


    public void takePicture() {
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            //step5 预拍照
            // This is how to tell the camera to lock focus.
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = State.STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void process(CaptureResult result) {
        Integer af = result.get(CaptureResult.CONTROL_AF_STATE);
        Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
//        Log.d(TAG, "process: afState=" + af + "\taeState=" + ae);
        switch (mState) {
            case STATE_PREVIEW:
                // We have nothing to do when the camera preview is working normally.
                break;
            case STATE_WAITING_LOCK:
                capturePicture(result);
                break;
            case STATE_WAITING_PRECAPTURE: {
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    mState = State.STATE_WAITING_NON_PRECAPTURE;
                }
            }
            break;
            case STATE_WAITING_NON_PRECAPTURE: {
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    mState = State.STATE_PICTURE_TAKEN;
                    captureStillPicture();
                }
            }
            break;
            case STATE_PICTURE_TAKEN:
                break;
            default:
                break;
        }
    }

    private void capturePicture(CaptureResult result) {
        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
        if (null == afState) {
            captureStillPicture();
        } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
            // CONTROL_AE_STATE can be null on some devices
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                mState = State.STATE_PICTURE_TAKEN;
                captureStillPicture();
            } else {
                runPreCaptureSequence();
            }
        }
    }

    private void runPreCaptureSequence() {
        // This is how to tell the camera to trigger.
        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        mState = State.STATE_WAITING_PRECAPTURE;
        try {
            mCaptureSession.capture(mPreviewBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        if (null == mContext || null == mCameraDevice) {
            return;
        }

        try {
            // This is the CaptureRequest.Builder that we use to take a picture.
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            // 设置对焦模式
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.FLASH_MODE,CameraMetadata.FLASH_MODE_TORCH);
            setAutoFlash(captureBuilder);
            // Orientation
            int rotation = mWindowManager.getDefaultDisplay().getRotation();
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 98);
            //thumb size
//            captureBuilder.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, mThumbSize);
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    mMediaActionSound.play(MediaActionSound.SHUTTER_CLICK);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    // TODO: 2019/9/18
//                    showToast("拍照成功");
                    unlockFocus();
                }
            };
            mCaptureSession.stopRepeating();
            //华为手机不能执行这一句
//            mCaptureSession.abortCaptures();
            //step6 正着拍照
            mCaptureSession.capture(captureBuilder.build(), captureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            // 设置曝光模式
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//            requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewBuilder);
            mCaptureSession.capture(mPreviewBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = State.STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    public void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundHandler = null;
            mBackgroundThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    enum State {
        /**
         * Camera state: Showing camera preview.
         */
        STATE_PREVIEW,
        /**
         * Camera state: Waiting for the focus to be locked.
         */
        STATE_WAITING_LOCK,
        /**
         * Camera state: Waiting for the exposure to be precapture state.
         */
        STATE_WAITING_PRECAPTURE,
        /**
         * Camera state: Waiting for the exposure state to be something other than precapture.
         */
        STATE_WAITING_NON_PRECAPTURE,
        /**
         * Camera state: Picture was taken.
         */
        STATE_PICTURE_TAKEN
    }
}
