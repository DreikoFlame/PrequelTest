package com.example.prequeltest

import android.content.res.Resources
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.media.MediaPlayer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.sherazkhilji.videffects.SepiaEffect
import kotlinx.android.synthetic.main.video_activity.*
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import javax.microedition.khronos.opengles.GL10


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class CameraCaptureActivity : AppCompatActivity() {

    private lateinit var mResources: Resources
    private lateinit var mMediaPlayer: MediaPlayer

    private lateinit var mCameraHandler: CameraTest.CameraHandler
//    private lateinit var mRenderer: CameraTestCameraSurfaceRenderer
    private lateinit var mCamera: Camera

    private var mCameraPreviewWidth: Int = 0
    private var mCameraPreviewHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        mResources = resources
        mMediaPlayer = MediaPlayer()

        try {
            // Load video file from SD Card
            // File dir = Environment
            // .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            // File file = new File(dir,
            // "sample.mp4");
            // mMediaPlayer.setDataSource(file.getAbsolutePath());
            // -----------------------------------------------------------------------
            // Load video file from Assets directory
            val afd = assets.openFd("sample.mp4")
            mMediaPlayer.setDataSource(
                afd.fileDescriptor,
                afd.startOffset, afd.length
            )
        } catch (e: Exception) {
            Log.e("test", e.message, e)
        }

        setContentView(R.layout.video_activity)

        videoSurfaceView.init(
            mMediaPlayer,
            SepiaEffect()
        )

//        initCameraStream()
    }

    override fun onResume() {
        super.onResume()

//        checkCameraPermission()

        videoSurfaceView.onResume()
//        videoSurfaceView.queueEvent(Runnable {
//            mRenderer.setCameraPreviewSize(
//                mCameraPreviewWidth,
//                mCameraPreviewHeight
//            )
//        })
    }

    override fun onPause() {
//        Log.d(TAG, "onPause -- releasing camera")
        super.onPause()
//        releaseCamera()
        videoSurfaceView.queueEvent(Runnable {
            // Tell the renderer that it's about to be paused so it can clean up.
//            mRenderer.notifyPausing()
        })
        videoSurfaceView.onPause()
//        Log.d(TAG, "onPause complete")
    }

    override fun onDestroy() {
//        Log.d(TAG, "onDestroy")
        super.onDestroy()
        mCameraHandler.invalidateHandler()     // paranoia
    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (!PermissionHelper.hasCameraPermission(this)) {
//            Toast.makeText(
//                this,
//                "Camera permission is needed to run this application", Toast.LENGTH_LONG
//            ).show()
//            PermissionHelper.launchPermissionSettings(this)
//            finish()
//        } else {
//            openCamera(1280, 720)      // updates mCameraPreviewWidth/Height
//
//        }
//    }
//
//    private fun initCameraStream() {
//        // Define a handler that receives camera-control messages from other threads.  All calls
//        // to Camera must be made on the same thread.  Note we create this before the renderer
//        // thread, so we know the fully-constructed object will be visible.
//        mCameraHandler = CameraHandler(this)
//
//        mRenderer = CameraSurfaceRenderer(mCameraHandler, sVideoEncoder, outputFile)
//        videoSurfaceView.setRenderer(mRenderer)
//        videoSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
//    }
//
//    /**
//     * Opens a camera, and attempts to establish preview mode at the specified width and height.
//     *
//     *
//     * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
//     */
//    private fun openCamera(desiredWidth: Int, desiredHeight: Int) {
//        if (mCamera != null) {
//            throw RuntimeException("camera already initialized")
//        }
//
//        val info = Camera.CameraInfo()
//
//        // Try to find a front-facing camera (e.g. for videoconferencing).
//        val numCameras = Camera.getNumberOfCameras()
//        for (i in 0 until numCameras) {
//            Camera.getCameraInfo(i, info)
//            if (info.facing === Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                mCamera = Camera.open(i)
//                break
//            }
//        }
//        if (mCamera == null) {
//            Log.d(TAG, "No front-facing camera found; opening default")
//            mCamera = Camera.open()    // opens first back-facing camera
//        }
//        if (mCamera == null) {
//            throw RuntimeException("Unable to open camera")
//        }
//
//        val parms = mCamera.getParameters()
//
//        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight)
//
//        // Give the camera a hint that we're recording video.  This can have a big
//        // impact on frame rate.
//        parms.setRecordingHint(true)
//
//        // leave the frame rate set to default
//        mCamera.setParameters(parms)
//
//        val fpsRange = IntArray(2)
//        val mCameraPreviewSize = parms.getPreviewSize()
//        parms.getPreviewFpsRange(fpsRange)
//        var previewFacts = mCameraPreviewSize.width + "x" + mCameraPreviewSize.height
//        if (fpsRange[0] == fpsRange[1]) {
//            previewFacts += " @" + fpsRange[0] / 1000.0 + "fps"
//        } else {
//            previewFacts += " @[" + fpsRange[0] / 1000.0 +
//                    " - " + fpsRange[1] / 1000.0 + "] fps"
//        }
//        val text = findViewById(R.id.cameraParams_text) as TextView
//        text.setText(previewFacts)
//
//        mCameraPreviewWidth = mCameraPreviewSize.width
//        mCameraPreviewHeight = mCameraPreviewSize.height
//
//
//        val layout = findViewById(R.id.cameraPreview_afl) as AspectFrameLayout
//
//        val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
//
//        if (display.rotation == Surface.ROTATION_0) {
//            mCamera.setDisplayOrientation(90)
//            layout.setAspectRatio(mCameraPreviewHeight as Double / mCameraPreviewWidth)
//        } else if (display.rotation == Surface.ROTATION_270) {
//            layout.setAspectRatio(mCameraPreviewHeight as Double / mCameraPreviewWidth)
//            mCamera.setDisplayOrientation(180)
//        } else {
//            // Set the preview aspect ratio.
//            layout.setAspectRatio(mCameraPreviewWidth as Double / mCameraPreviewHeight)
//        }
//    }
//
//    /**
//     * Stops camera preview, and releases the camera to the system.
//     */
//    private fun releaseCamera() {
//        if (mCamera != null) {
//            mCamera.stopPreview()
//            mCamera.release()
//            mCamera = null
//            Log.d(TAG, "releaseCamera -- done")
//        }
//    }
//
//    private fun handleSetSurfaceTexture(st: SurfaceTexture) {
//        st.setOnFrameAvailableListener(this)
//        try {
//            mCamera.setPreviewTexture(st)
//        } catch (ioe: IOException) {
//            throw RuntimeException(ioe)
//        }
//
//        mCamera.startPreview()
//    }
//
//    override fun onFrameAvailable(st: SurfaceTexture) {
//        // The SurfaceTexture uses this to signal the availability of a new frame.  The
//        // thread that "owns" the external texture associated with the SurfaceTexture (which,
//        // by virtue of the context being shared, *should* be either one) needs to call
//        // updateTexImage() to latch the buffer.
//        //
//        // Once the buffer is latched, the GLSurfaceView thread can signal the encoder thread.
//        // This feels backward -- we want recording to be prioritized over rendering -- but
//        // since recording is only enabled some of the time it's easier to do it this way.
//        //
//        // Since GLSurfaceView doesn't establish a Looper, this will *probably* execute on
//        // the main UI thread.  Fortunately, requestRender() can be called from any thread,
//        // so it doesn't really matter.
//        if (VERBOSE) Log.d(TAG, "ST onFrameAvailable")
//        mGLView.requestRender()
//    }
//
//
//    private fun checkCameraPermission() {
//        if (PermissionHelper.hasCameraPermission(this)) {
//            if (mCamera == null) {
//                openCamera(1280, 720)      // updates mCameraPreviewWidth/Height
//            }
//
//        } else {
//            PermissionHelper.requestCameraPermission(this, false);
//        }
//    }
//
//    companion object {
//        val TAG = this.javaClass.simpleName
//        private val VERBOSE = false
//
//        // Camera filters; must match up with cameraFilterNames in strings.xml
//        val FILTER_NONE = 0
//        val FILTER_BLACK_WHITE = 1
//        val FILTER_BLUR = 2
//        val FILTER_SHARPEN = 3
//        val FILTER_EDGE_DETECT = 4
//        val FILTER_EMBOSS = 5
//
//
//        //CameraSurfaceRenderer
//        private val RECORDING_OFF = 0
//        private val RECORDING_ON = 1
//        private val RECORDING_RESUMED = 2
//    }
//
//    /**
//     * Handles camera operation requests from other threads.  Necessary because the Camera
//     * must only be accessed from one thread.
//     *
//     *
//     * The object is created on the UI thread, and all handlers run there.  Messages are
//     * sent from other threads, using sendMessage().
//     */
//    class CameraHandler(activity: CameraCaptureActivity) : Handler() {
//
//        // Weak reference to the Activity; only access this from the UI thread.
//        private val mWeakActivity: WeakReference<CameraCaptureActivity>
//
//        init {
//            mWeakActivity = WeakReference<CameraCaptureActivity>(activity)
//        }
//
//        /**
//         * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
//         * attempts to access a stale Activity through a handler are caught.
//         */
//        fun invalidateHandler() {
//            mWeakActivity.clear()
//        }
//
//        // runs on UI thread
//        override fun handleMessage(inputMessage: Message) {
//            val what = inputMessage.what
//            Log.d(TAG, "CameraHandler [$this]: what=$what")
//
//            val activity = mWeakActivity.get()
//            if (activity == null) {
//                Log.w(TAG, "CameraHandler.handleMessage: activity is null")
//                return
//            }
//
//            when (what) {
//                MSG_SET_SURFACE_TEXTURE -> activity!!.handleSetSurfaceTexture(inputMessage.obj as SurfaceTexture)
//                else -> throw RuntimeException("unknown msg $what")
//            }
//        }
//
//        companion object {
//            val MSG_SET_SURFACE_TEXTURE = 0
//        }
//    }
//
//    /**
//     * Renderer object for our GLSurfaceView.
//     *
//     *
//     * Do not call any methods here directly from another thread -- use the
//     * GLSurfaceView#queueEvent() call.
//     */
//    class CameraSurfaceRenderer
//    /**
//     * Constructs CameraSurfaceRenderer.
//     *
//     *
//     * @param cameraHandler Handler for communicating with UI thread
//     * @param movieEncoder video encoder object
//     * @param outputFile output file for encoded video; forwarded to movieEncoder
//     */
//        (
//        val mCameraHandler: CameraCaptureActivity.CameraHandler,
//        val mVideoEncoder: TextureMovieEncoder, private val mOutputFile: File
//    ) : GLSurfaceView.Renderer {
//
//        private var mFullScreen: FullFrameRect? = null
//
//        private val mSTMatrix = FloatArray(16)
//        private var mTextureId: Int = 0
//
//        private var mSurfaceTexture: SurfaceTexture? = null
//        private var mRecordingEnabled: Boolean = false
//        private var mRecordingStatus: Int = 0
//        private var mFrameCount: Int = 0
//
//        // width/height of the incoming camera preview frames
//        private var mIncomingSizeUpdated: Boolean = false
//        private var mIncomingWidth: Int = 0
//        private var mIncomingHeight: Int = 0
//
//        private var mCurrentFilter: Int = 0
//        private var mNewFilter: Int = 0
//
//
//        init {
//
//            mTextureId = -1
//
//            mRecordingStatus = -1
//            mRecordingEnabled = false
//            mFrameCount = -1
//
//            mIncomingSizeUpdated = false
//            mIncomingHeight = -1
//            mIncomingWidth = mIncomingHeight
//
//            // We could preserve the old filter mode, but currently not bothering.
//            mCurrentFilter = -1
//            mNewFilter = CameraCaptureActivity.FILTER_NONE
//        }
//
//        /**
//         * Notifies the renderer thread that the activity is pausing.
//         *
//         *
//         * For best results, call this *after* disabling Camera preview.
//         */
//        fun notifyPausing() {
//            if (mSurfaceTexture != null) {
//                Log.d(TAG, "renderer pausing -- releasing SurfaceTexture")
//                mSurfaceTexture!!.release()
//                mSurfaceTexture = null
//            }
//            if (mFullScreen != null) {
//                mFullScreen!!.release(false)     // assume the GLSurfaceView EGL context is about
//                mFullScreen = null             //  to be destroyed
//            }
//            mIncomingHeight = -1
//            mIncomingWidth = mIncomingHeight
//        }
//
//        /**
//         * Notifies the renderer that we want to stop or start recording.
//         */
//        fun changeRecordingState(isRecording: Boolean) {
//            Log.d(TAG, "changeRecordingState: was $mRecordingEnabled now $isRecording")
//            mRecordingEnabled = isRecording
//        }
//
//        /**
//         * Changes the filter that we're applying to the camera preview.
//         */
//        fun changeFilterMode(filter: Int) {
//            mNewFilter = filter
//        }
//
//        /**
//         * Updates the filter program.
//         */
//        fun updateFilter() {
//            val programType: Texture2dProgram.ProgramType
//            var kernel: FloatArray? = null
//            var colorAdj = 0.0f
//
//            Log.d(TAG, "Updating filter to $mNewFilter")
//            when (mNewFilter) {
//                CameraCaptureActivity.FILTER_NONE -> programType = Texture2dProgram.ProgramType.TEXTURE_EXT
//                CameraCaptureActivity.FILTER_BLACK_WHITE ->
//                    // (In a previous version the TEXTURE_EXT_BW variant was enabled by a flag called
//                    // ROSE_COLORED_GLASSES, because the shader set the red channel to the B&W color
//                    // and green/blue to zero.)
//                    programType = Texture2dProgram.ProgramType.TEXTURE_EXT_BW
//                CameraCaptureActivity.FILTER_BLUR -> {
//                    programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT
//                    kernel = floatArrayOf(
//                        1f / 16f,
//                        2f / 16f,
//                        1f / 16f,
//                        2f / 16f,
//                        4f / 16f,
//                        2f / 16f,
//                        1f / 16f,
//                        2f / 16f,
//                        1f / 16f
//                    )
//                }
//                CameraCaptureActivity.FILTER_SHARPEN -> {
//                    programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT
//                    kernel = floatArrayOf(0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f)
//                }
//                CameraCaptureActivity.FILTER_EDGE_DETECT -> {
//                    programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT
//                    kernel = floatArrayOf(-1f, -1f, -1f, -1f, 8f, -1f, -1f, -1f, -1f)
//                }
//                CameraCaptureActivity.FILTER_EMBOSS -> {
//                    programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT
//                    kernel = floatArrayOf(2f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, -1f)
//                    colorAdj = 0.5f
//                }
//                else -> throw RuntimeException("Unknown filter mode $mNewFilter")
//            }
//
//            // Do we need a whole new program?  (We want to avoid doing this if we don't have
//            // too -- compiling a program could be expensive.)
//            if (programType !== mFullScreen!!.getProgram().getProgramType()) {
//                mFullScreen!!.changeProgram(Texture2dProgram(programType))
//                // If we created a new program, we need to initialize the texture width/height.
//                mIncomingSizeUpdated = true
//            }
//
//            // Update the filter kernel (if any).
//            if (kernel != null) {
//                mFullScreen!!.getProgram().setKernel(kernel, colorAdj)
//            }
//
//            mCurrentFilter = mNewFilter
//        }
//
//        /**
//         * Records the size of the incoming camera preview frames.
//         *
//         *
//         * It's not clear whether this is guaranteed to execute before or after onSurfaceCreated(),
//         * so we assume it could go either way.  (Fortunately they both run on the same thread,
//         * so we at least know that they won't execute concurrently.)
//         */
//        fun setCameraPreviewSize(width: Int, height: Int) {
//            Log.d(TAG, "setCameraPreviewSize")
//            mIncomingWidth = width
//            mIncomingHeight = height
//            mIncomingSizeUpdated = true
//        }
//
//        fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
//            Log.d(TAG, "onSurfaceCreated")
//
//            // We're starting up or coming back.  Either way we've got a new EGLContext that will
//            // need to be shared with the video encoder, so figure out if a recording is already
//            // in progress.
//            mRecordingEnabled = mVideoEncoder.isRecording()
//            if (mRecordingEnabled) {
//                mRecordingStatus = RECORDING_RESUMED
//            } else {
//                mRecordingStatus = RECORDING_OFF
//            }
//
//            // Set up the texture blitter that will be used for on-screen display.  This
//            // is *not* applied to the recording, because that uses a separate shader.
//            mFullScreen = FullFrameRect(
//                Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT)
//            )
//
//            mTextureId = mFullScreen!!.createTextureObject()
//
//            // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
//            // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
//            // available messages will arrive on the main thread.
//            mSurfaceTexture = SurfaceTexture(mTextureId)
//
//            // Tell the UI thread to enable the camera preview.
//            mCameraHandler.sendMessage(
//                mCameraHandler.obtainMessage(
//                    CameraCaptureActivity.CameraHandler.MSG_SET_SURFACE_TEXTURE, mSurfaceTexture
//                )
//            )
//        }
//
//        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
//            Log.d(TAG, "onSurfaceChanged " + width + "x" + height)
//        }
//
//        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
//        override fun onDrawFrame(unused: GL10) {
//            if (VERBOSE) Log.d(TAG, "onDrawFrame tex=$mTextureId")
//            var showBox = false
//
//            // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
//            // was there before.
//            mSurfaceTexture!!.updateTexImage()
//
//            // If the recording state is changing, take care of it here.  Ideally we wouldn't
//            // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
//            // makes it hard to do elsewhere.
//            if (mRecordingEnabled) {
//                when (mRecordingStatus) {
//                    RECORDING_OFF -> {
//                        Log.d(TAG, "START recording")
//                        // start recording
//                        mVideoEncoder.startRecording(
//                            TextureMovieEncoder.EncoderConfig(
//                                mOutputFile, 640, 480, 1000000, EGL14.eglGetCurrentContext()
//                            )
//                        )
//                        mRecordingStatus = RECORDING_ON
//                    }
//                    RECORDING_RESUMED -> {
//                        Log.d(TAG, "RESUME recording")
//                        mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext())
//                        mRecordingStatus = RECORDING_ON
//                    }
//                    RECORDING_ON -> {
//                    }
//                    else -> throw RuntimeException("unknown status $mRecordingStatus")
//                }// yay
//            } else {
//                when (mRecordingStatus) {
//                    RECORDING_ON, RECORDING_RESUMED -> {
//                        // stop recording
//                        Log.d(TAG, "STOP recording")
//                        mVideoEncoder.stopRecording()
//                        mRecordingStatus = RECORDING_OFF
//                    }
//                    RECORDING_OFF -> {
//                    }
//                    else -> throw RuntimeException("unknown status $mRecordingStatus")
//                }// yay
//            }
//
//            // Set the video encoder's texture name.  We only need to do this once, but in the
//            // current implementation it has to happen after the video encoder is started, so
//            // we just do it here.
//            //
//            // TODO: be less lame.
//            mVideoEncoder.setTextureId(mTextureId)
//
//            // Tell the video encoder thread that a new frame is available.
//            // This will be ignored if we're not actually recording.
//            mVideoEncoder.frameAvailable(mSurfaceTexture)
//
//            if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
//                // Texture size isn't set yet.  This is only used for the filters, but to be
//                // safe we can just skip drawing while we wait for the various races to resolve.
//                // (This seems to happen if you toggle the screen off/on with power button.)
//                Log.i(TAG, "Drawing before incoming texture size set; skipping")
//                return
//            }
//            // Update the filter, if necessary.
//            if (mCurrentFilter != mNewFilter) {
//                updateFilter()
//            }
//            if (mIncomingSizeUpdated) {
//                mFullScreen!!.getProgram().setTexSize(mIncomingWidth, mIncomingHeight)
//                mIncomingSizeUpdated = false
//            }
//
//            // Draw the video frame.
//            mSurfaceTexture!!.getTransformMatrix(mSTMatrix)
//            mFullScreen!!.drawFrame(mTextureId, mSTMatrix)
//
//            // Draw a flashing box if we're recording.  This only appears on screen.
//            showBox = mRecordingStatus == RECORDING_ON
//            if (showBox && ++mFrameCount and 0x04 == 0) {
//                drawBox()
//            }
//        }
//
//        /**
//         * Draws a red box in the corner.
//         */
//        private fun drawBox() {
//            GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
//            GLES20.glScissor(0, 0, 100, 100)
//            GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
//            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//            GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
//        }
//    }
}
