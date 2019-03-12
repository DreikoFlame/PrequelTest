package com.example.prequeltest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import com.example.prequeltest.effects.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class CameraCaptureActivity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener {
    static final String TAG = "CameraCaptureActivity";
    private static final boolean VERBOSE = false;


    private GLSurfaceView mGLView;
    private CameraSurfaceRenderer mRenderer;
    private Camera mCamera;
    private CameraHandler mCameraHandler;

    private List<FilterEffect> mFilters = new ArrayList<>();
    private int mSelectedFilterPosition = 0;

    private int mCameraPreviewWidth, mCameraPreviewHeight;

    //swipe detection
    private float xStart, xEnd;
    private final int MIN_DISTANCE = 150;
    private final int DESIRED_WIDTH = 720;
    private final int DESIRED_HEIGHT = 1280;

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

        Log.d(TAG, "onCreate complete: " + this);
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

    public void takePhoto(View view) {
        if (PermissionHelper.hasWriteStoragePermission(this)) {
            File pictures = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            //todo пофиксить формат имени файла и фриз превью после фото(попробовать заюзать CameraHandler)
            File photoFile = new File(pictures, "myphoto" + Calendar.getInstance().getTime().toString() + ".jpg");
            //todo сменить на camera2(уйти от deprecated классов)
            //todo для обработки картинок глянуть в примере использоание класса GeneratedTexture(возможно поможет)+ поискать там примеры работы с bitmap
            //todo альтернативно глянуть FullFrameRect
            mCamera.takePicture(null, null, (data, camera) -> {
                try {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                    FileOutputStream fos = new FileOutputStream(photoFile);
//                    fos.write(data);
//                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } else {
            PermissionHelper.requestWriteStoragePermission(this);
        }
    }

    private void applyEffects(Bitmap bitmap) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }
        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        SurfaceTexture texture = new SurfaceTexture(textureHandle[0]);
    }

    //todo удалить если не нужно
    private void saveBitmap(Bitmap bitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            Log.i("TAG", "Image SAVED==========" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

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
}

