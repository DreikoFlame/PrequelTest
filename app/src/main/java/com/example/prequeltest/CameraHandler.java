package com.example.prequeltest;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Handles camera operation requests from other threads.  Necessary because the Camera
 * must only be accessed from one thread.
 * <p>
 * The object is created on the UI thread, and all handlers run there.  Messages are
 * sent from other threads, using sendMessage().
 */
class CameraHandler extends Handler {
    private static final String TAG = CameraHandler.class.getSimpleName();
    static final int MSG_SET_SURFACE_TEXTURE = 0;
    static final int MSG_TAKE_PHOTO = 1;

    // Weak reference to the Activity; only access this from the UI thread.
    private WeakReference<CameraCaptureActivity> mWeakActivity;

    public CameraHandler(CameraCaptureActivity activity) {
        mWeakActivity = new WeakReference<>(activity);
    }

    /**
     * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
     * attempts to access a stale Activity through a handler are caught.
     */
    public void invalidateHandler() {
        mWeakActivity.clear();
    }

    @Override  // runs on UI thread
    public void handleMessage(Message inputMessage) {
        int what = inputMessage.what;
        Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

        CameraCaptureActivity activity = mWeakActivity.get();
        if (activity == null) {
            Log.w(TAG, "CameraHandler.handleMessage: activity is null");
            return;
        }

        switch (what) {
            case MSG_SET_SURFACE_TEXTURE:
                activity.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                break;
            case MSG_TAKE_PHOTO:
                activity.takePhoto();
                break;
            default:
                throw new RuntimeException("unknown msg " + what);
        }
    }
}
