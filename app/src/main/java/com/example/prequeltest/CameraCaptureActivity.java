package com.example.prequeltest;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.example.prequeltest.effects.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CameraCaptureActivity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener {
    static final String TAG = "CameraCaptureActivity";
    private static final boolean VERBOSE = false;

    private GLSurfaceView mGLView;
    private ProgressBar mProgressBar;
    private ImageView mTakePhotoBtn;
    private CameraSurfaceRenderer mRenderer;
    private Camera mCamera;
    private CameraHandler mCameraHandler;

    private List<FilterEffect> mFilters = new ArrayList<>();
    private int mSelectedFilterPosition = 0;
    private int mCameraPreviewWidth, mCameraPreviewHeight;
    private boolean mIsPhotoTaken = false;

    //swipe detection
    private float xStart, xEnd;
    private final int MIN_DISTANCE = 150;
    private final int DESIRED_WIDTH = 720;
    private final int DESIRED_HEIGHT = 1280;
    private final int BTN_ANIMATION_TIME = 300;
    private final float BTN_SCALE_UP_SIZE = 1.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera_capture);

        // Define a handler that receives camera-control messages from other threads.  All calls
        // to Camera must be made on the same thread.  Note we create this before the renderer
        // thread, so we know the fully-constructed object will be visible.
        mCameraHandler = new CameraHandler(this);

        // Configure the GLSurfaceView.  This will start the Renderer thread, with an
        // appropriate EGL context.
        mGLView = findViewById(R.id.camera_preview_surfaceView);
        mGLView.setEGLContextClientVersion(2);     // select GLES 2.0

        mRenderer = new CameraSurfaceRenderer(mCameraHandler);
        mGLView.setRenderer(mRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mProgressBar = findViewById(R.id.progress_bar);
        mTakePhotoBtn = findViewById(R.id.take_photo_btn);

        initTakePhotoBtn();

        Log.d(TAG, "onCreate complete: " + this);
    }

    private void initTakePhotoBtn() {
        mTakePhotoBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(mTakePhotoBtn,
                            "scaleX", BTN_SCALE_UP_SIZE);
                    ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(mTakePhotoBtn,
                            "scaleY", BTN_SCALE_UP_SIZE);
                    scaleUpX.setDuration(BTN_ANIMATION_TIME);
                    scaleUpY.setDuration(BTN_ANIMATION_TIME);

                    AnimatorSet scaleUp = new AnimatorSet();
                    scaleUp.play(scaleUpX).with(scaleUpY);

                    scaleUp.start();
                    if (!mIsPhotoTaken) {
                        mIsPhotoTaken = true;
                        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
                                CameraHandler.MSG_TAKE_PHOTO, null));
                    }

                    break;

                case MotionEvent.ACTION_OUTSIDE:
                case MotionEvent.ACTION_UP:
                    mIsPhotoTaken = false;
                    ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(
                            mTakePhotoBtn, "scaleX", 1f);
                    ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(
                            mTakePhotoBtn, "scaleY", 1f);
                    scaleDownX.setDuration(BTN_ANIMATION_TIME);
                    scaleDownY.setDuration(BTN_ANIMATION_TIME);

                    AnimatorSet scaleDown = new AnimatorSet();
                    scaleDown.play(scaleDownX).with(scaleDownY);

                    scaleDown.start();

                    break;

            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume -- acquiring camera");
        super.onResume();

        if (PermissionHelper.hasCameraPermission(this)) {
            if (mCamera == null) {
                openCamera(DESIRED_WIDTH, DESIRED_HEIGHT);      // updates mCameraPreviewWidth/Height
            }

        } else {
            PermissionHelper.requestCameraPermission(this, false);
        }

        mGLView.onResume();
        mGLView.queueEvent(() -> mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight));
        Log.d(TAG, "onResume complete: " + this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause -- releasing camera");
        super.onPause();
        releaseCamera();
        mGLView.queueEvent(() -> {
            // Tell the renderer that it's about to be paused so it can clean up.
            mRenderer.notifyPausing();
        });
        mGLView.onPause();
        Log.d(TAG, "onPause complete");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mCameraHandler.invalidateHandler();     // paranoia
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        // The SurfaceTexture uses this to signal the availability of a new frame.  The
        // thread that "owns" the external texture associated with the SurfaceTexture (which,
        // by virtue of the context being shared, *should* be either one) needs to call
        // updateTexImage() to latch the buffer.
        //
        // Once the buffer is latched, the GLSurfaceView thread can signal the encoder thread.
        // This feels backward -- we want recording to be prioritized over rendering -- but
        // since recording is only enabled some of the time it's easier to do it this way.
        //
        // Since GLSurfaceView doesn't establish a Looper, this will *probably* execute on
        // the main UI thread.  Fortunately, requestRender() can be called from any thread,
        // so it doesn't really matter.
        if (VERBOSE) Log.d(TAG, "ST onFrameAvailable");
        mGLView.requestRender();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xStart = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                xEnd = event.getX();

                if (xEnd - xStart > MIN_DISTANCE) {
                    onNewFilterSelected(true);
                } else if (xStart - xEnd > MIN_DISTANCE) {
                    onNewFilterSelected(false);
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!PermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            PermissionHelper.launchPermissionSettings(this);
            finish();
        } else {
            openCamera(DESIRED_WIDTH, DESIRED_HEIGHT);      // updates mCameraPreviewWidth/Height
        }
    }

    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     * <p>
     * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
     */
    private void openCamera(int desiredWidth, int desiredHeight) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            Log.d(TAG, "No back-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();

        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);

        // leave the frame rate set to default
        mCamera.setParameters(parms);

        int[] fpsRange = new int[2];
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        parms.getPreviewFpsRange(fpsRange);

        mCameraPreviewWidth = mCameraPreviewSize.width;
        mCameraPreviewHeight = mCameraPreviewSize.height;

        fillFilterList();

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        if (display.getRotation() == Surface.ROTATION_0) {
            mCamera.setDisplayOrientation(90);
        } else if (display.getRotation() == Surface.ROTATION_270) {
            mCamera.setDisplayOrientation(180);
        }
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }

    private void fillFilterList() {
        mFilters.add(new NoFilter());
        mFilters.add(new GrainEffect(0.2f, mCameraPreviewWidth, mCameraPreviewHeight));
        mFilters.add(new NegativeEffect());
        mFilters.add(new SepiaEffect());
    }

    public void takePhoto() {
        if (PermissionHelper.hasWriteStoragePermission(this)) {
            mProgressBar.setVisibility(View.VISIBLE);
            File pictures = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().getTime());
            File photoFile = new File(pictures, "img_" + timeStamp + ".jpg");
            mCamera.takePicture(null, null, (data, camera) -> {
                try {
                    FileOutputStream fos = new FileOutputStream(photoFile);
                    fos.write(data);
                    fos.flush();
                    fos.close();
                    Toast.makeText(CameraCaptureActivity.this, "Image saved", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mProgressBar.setVisibility(View.GONE);
                    addPhotoToMedia(photoFile);
                    mCamera.startPreview();
                }
            });
        } else {
            PermissionHelper.requestWriteStoragePermission(this);
        }
    }

    //send broadcast to media scanner to add photo into gallery
    private void addPhotoToMedia(File photo) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(photo);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     */
    void handleSetSurfaceTexture(SurfaceTexture st) {
        st.setOnFrameAvailableListener(this);
        try {
            mCamera.setPreviewTexture(st);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }

    private void onNewFilterSelected(boolean isLeftToRightSwipe) {
        FilterEffect newFilter;
        if (isLeftToRightSwipe) {
            mSelectedFilterPosition++;
            if (mSelectedFilterPosition > mFilters.size() - 1) {
                mSelectedFilterPosition = 0;
            }
        } else {
            mSelectedFilterPosition--;
            if (mSelectedFilterPosition < 0) {
                mSelectedFilterPosition = mFilters.size() - 1;
            }
        }
        newFilter = mFilters.get(mSelectedFilterPosition);
        mGLView.queueEvent(() -> {
            // notify the renderer that we want to change the encoder's state
            mRenderer.changeFilter(newFilter);
        });
    }
}

