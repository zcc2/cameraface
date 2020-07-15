package com.camera.zcc.cameraface.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import org.webrtc.Logging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

public class CameraUnity implements Camera.PreviewCallback {
    private final static String TAG = CameraUnity.class.getName();
    private final static Object cameraLock = new Object();
    private final static int OBSERVER_PEROID_MS = 5000;
    private final static int MAX_CAMERA_BUFFER_NUM = 2;

    // List of formats supported by all cameras. This list is
    // filled once in order to be able to switch cameras.
    private static List<List<Format>> supportedFormats = null;

    private LinkedList<Frame> frames = new LinkedList<Frame>();
    private volatile boolean pendingChange = false;
    private Camera.PreviewCallback callback = null;
    private boolean needCallBack = false;
    private SurfaceTexture texture = null;
    private STATE state = STATE.INVLID;
    private CameraInfo info = null;
    private int[] glTextures = null;
    private Context context = null;
    private Parameter params= null;
    private Camera camera = null;
    private Handler handler = null;
    private CameraThread thread = null;
    private Frame lastFrame = null;
    private long prevTimeMs = -1;
    private int framesCount = 0;
    private int rotation = 0;
    private int frameMs = 0;
    private int id = 1;

    // Camera state enum
    private static enum STATE {
        INVLID(0),
        OPENED(1),
        STARTED(2);

        // Constructor function
        private STATE(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
        private int value;
    }

    // Camera default observer
    private final Runnable observer = new Runnable() {
        @Override
        public void run() {
            int cameraFps = (framesCount * 1000 + OBSERVER_PEROID_MS / 2)
                    / OBSERVER_PEROID_MS;
            Logging.d(TAG, "camera fps: " + cameraFps);
            if (framesCount == 0) {
                Logging.e(TAG, "camera freezed");
            } else {
                framesCount = 0;
                if (handler != null) {
                    handler.postDelayed(this, OBSERVER_PEROID_MS);
                }
            }
        }
    };

    /**
     * Camera parameter class, package camera face, camera preview
     * picture format, camera preview picture size, camera preview
     * picture frame rate
     */
    public static class Parameter {
        private int facing = CameraInfo.CAMERA_FACING_FRONT;
        private int format = ImageFormat.NV21;
        private int width = 640;
        private int height= 480;
        private int fps = 15;

        private boolean drop = false;

        /**
         * Get the camera face set
         *
         * @return the camera face set
         */
        public int getFacing() {
            return facing;
        }

        /**
         * Sets the camera face
         *
         * @param facing the desired camera face, defined by one of
         * the {@link CameraInfo} constants.
         * (E.g.,
         *   <var>CameraInfo.CAMERA_FACING_BACK</var> (default), or
         *   <var>CameraInfo.CAMERA_FACING_FRONT</var>)
         */
        public void setFacing(int facing) {
            this.facing = facing;
        }

        /**
         * Get the camera preview picture image format set
         *
         * @return the camera preview picture image format set
         */
        public int getFormat() {
            return format;
        }

        /**
         * Sets the camera image format for preview picture
         *
         * @param format the desired preview picture format, defined by
         * one of the {@link ImageFormat} constants.
         * (E.g.,
         *   <var>ImageFormat.NV21</var> (default), or
         *   <var>ImageFormat.YV12</var>)
         */
        public void setFormat(int format) {
            this.format = format;
        }

        /**
         * Get the camera preview picture width(in pixels) set
         *
         * @return the camera preview picture width(in pixels)
         * set
         */
        public int getWidth() {
            return width;
        }

        /**
         * Sets the camera preview picture width(in pixels)
         *
         * @param width the desired preview picture width
         *              (in pixels), default is 640
         */
        public void setWidth(int width) {
            this.width = width;
        }

        /**
         * Get the camera preview picture height(in pixels) set
         *
         * @return the camera preview picture height(in pixels)
         * set
         */
        public int getHeight() {
            return height;
        }

        /**
         * Sets the camera preview picture height(in pixels)
         *
         * @param height the desired preview picture height
         *               (in pixels), default is 480
         */
        public void setHeight(int height) {
            this.height = height;
        }

        /**
         * Get the camera preview picture fps(in Hz) set
         *
         * @return the camera preview picture fps(in Hz) set
         */
        public int getFps() {
            return fps;
        }

        /**
         * Sets the camera preview picture fps(in Hz)
         *
         * @param fps the desired preview picture fps(in Hz),
         *            default is 15
         */
        public void setFps(int fps) {
            this.fps = fps;
        }

        /**
         * Set drop frame flag when camera fps is bigger than
         * required fps
         *
         * @param drop drop flag, true: drop frame, false:
         *             not drop frame
         */
        public void setDrop(boolean drop) {
            this.drop = drop;
        }

        @Override
        public String toString() {
            return "camera parameters: "+ "\n"
                    + "facing=" + facing + "\n"
                    + "format=" + format + "\n"
                    + "width="  + width  + "\n"
                    + "height=" + height + "\n"
                    + "fps=" + fps + "\n";
        }
    };

    /**
     * System support camera format class, package camera preview picture
     * size and camera preview picture frame rate
     */
    public static class Format {
        public final int width;
        public final int height;
        public final int minFps;
        public final int maxFps;

        public Format(int width, int height, int minFps, int maxFps) {
            this.width  = width;
            this.height = height;
            this.minFps = minFps;
            this.maxFps = maxFps;
        }
    }

    /**
     * Camera preview picture class, package preview picture data array,
     * preview picture timestamp and preview picture rotation.
     */
    public static class Frame {
        public byte[] frame = null;
        public long timestamp = -1;
        public int rotation = 0;
        public int facing = 0;
        public int format = 0;
        public int width  = 0;
        public int height = 0;

        public Frame(byte[] frame, int rotation, long timestamp) {
            this.frame = frame;
            this.rotation = rotation;
            this.timestamp = timestamp;
        }
    }

    /**
     * Open camera with input parameter required
     *
     * @param context context environment
     * @param params required parameter
     * @param block wait camera open flag
     */
    public void open( Context context,
                     @Nullable Parameter params, final boolean block) {
        Logging.d(TAG, "open params: "
                + ((params != null)? params.toString() : "null"));
        synchronized(cameraLock) {
            getFormats();
            if(supportedFormats == null) {
                Logging.e(TAG, "not camera available");
                return;
            }
            if (state != STATE.INVLID) {
                Logging.d(TAG,"camera has been opened");
                return;
            }
            this.context = context;
            if(params != null) {
                this.params = params;
            } else {
                this.params = new Parameter();
            }

            Exchanger<Handler> handlerExchanger = new Exchanger<Handler>();
            thread = new CameraThread(handlerExchanger);
            thread.start();
            handler = exchange(handlerExchanger, null);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    openOnThread();
                    if (block) {
                        synchronized (cameraLock) {
                            cameraLock.notifyAll();
                        }
                    }
                }
            });
            try {
                state = STATE.OPENED;
                if(block) {
                    cameraLock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Logging.d(TAG, "open done");
        }
    }

    /**
     * Get camera preview picture width in pixel
     *
     * @return camera preview picture width in pixel
     */
    public int getWidth() {
        return params.getWidth();
    }

    /**
     * Get camera preview picture height in pixel
     *
     * @return camera preview picture height in pixel
     */
    public int getHeight() {
        return params.getHeight();
    }

    /**
     * Get camera preview picture frequency
     *
     * @return camera preview picture frequency
     */
    public int getFps() {
        return params.getFps();
    }

    /**
     * Get camera preview picture rotation in degree
     *
     * @return camera preview picture rotation in degree
     */
    public int getRotation() {
        return rotation;
    }

    /**
     * Start camera, if surfaceTexture is not null, it be used for live
     * preview, if surfaceTexture is null, so we create a SurfaceTexture
     * and hand it over to Camera, but never listen for frame-ready
     * callbacks, and never call updateTexImage on it
     *
     * @param surfaceTexture live preview surface textures
     * @param callback preview callback interface instance
     * @param isNeed whether is need set default preview
     *                 callback or not when callback is null
     */
    public void start(@Nullable SurfaceTexture surfaceTexture,
                      @Nullable Camera.PreviewCallback callback, boolean isNeed) {
        Logging.d(TAG, "start " + surfaceTexture);
        synchronized (cameraLock) {
            if(state == STATE.INVLID) {
                Logging.w(TAG, "camera has not been opened");
                return;
            }
            if(state == STATE.STARTED) {
                Logging.d(TAG, "camera has been started");
                return;
            }
            this.texture = surfaceTexture;
            this.needCallBack = isNeed;
            this.callback = callback;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    startOnThread();
                }
            });
            state = STATE.STARTED;
            Logging.d(TAG, "start done");
        }
    }

    public void start(@Nullable SurfaceTexture surfaceTexture,
                      @Nullable Camera.PreviewCallback callback) {
        start(surfaceTexture, callback, false);
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        push(bytes, camera);
    }

    /**
     * Push a preview picture frame into camera buffer list
     *
     * @param bytes a preview picture frame
     * @param bytes camera object
     */
    public void push(byte[] bytes, Camera camera) {
        if (Thread.currentThread() != thread) {
            Logging.w(TAG, "camera callback not on "
                    + "camera thread");
            return;
        }
        if (camera == null) {
            Logging.d(TAG, "camera object is null");
            return;
        }
        if (this.camera != camera) {
            Logging.w(TAG, "unexpected camera in callback");
            return;
        }
        framesCount++;

        // Get timestamp in nanosecond
        long captureTimeNs =
                TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
        if (params.drop && dropFrame(captureTimeNs)) {
            //Logging.d(TAG, "drop frame with fps: " + params.fps);
            return;
        }
        // Calculate rotation for each preview picture because screen rotate
        calcRotation();
        synchronized (frames) {
            if (frames.size() >= MAX_CAMERA_BUFFER_NUM) {
                frames.removeFirst();
            }
            Frame frame = new Frame(bytes, rotation, captureTimeNs);
            frame.format = params.format;
            frame.width  = params.width;
            frame.height = params.height;
            frame.facing = params.facing;
            frames.add(frame);
            lastFrame = frame;
            frames.notifyAll();
        }
    }

    /**
     * Pop a preview picture frame from camera buffer list
     *
     * @param wait wait frame available flag, true: wait,
     *                false: not wait, return null.
     * @return a preview picture frame{@link Frame}
     */
    public Frame pop(boolean wait) {
        Frame frame = null;
        synchronized (frames) {
            if (frames.isEmpty() && lastFrame == null) {
                if(wait) {
                    try {
                        frames.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!frames.isEmpty()) {
                frame = frames.removeFirst();
            } else {
                frame = lastFrame;
            }
        }
        return frame;
    }

    /**
     * Switch camera to the next valid camera id. This can only be called while
     * the camera is running.
     * Returns true on success. False if the next camera does not support the
     * current resolution.
     *
     * @param switchDoneEvent switch done runnable callback
     * @param surfaceTexture live preview surface textures
     * @return switch camera success or failed
     */
    public boolean change(final Runnable switchDoneEvent,
                          final @Nullable SurfaceTexture surfaceTexture) {
        Logging.d(TAG, "change");
        synchronized (cameraLock) {
            if (Camera.getNumberOfCameras() < 2) {
                return false;
            }
            if (thread == null) {
                Logging.e(TAG, "camera has not been started");
                return false;
            }
            if (pendingChange) {
                // Do not handle multiple camera switch request to avoid blocking
                // camera thread by handling too many switch request from a queue.
                Logging.w(TAG, "ignoring camera switch request");
                return false;
            }

            int new_id = (id + 1) % Camera.getNumberOfCameras();

            Format formatToUse = null;
            List<Format> formats = supportedFormats.get(new_id);
            for (Format format : formats) {
                if (format.width == params.width
                        && format.height == params.height) {
                    formatToUse = format;
                    break;
                }
            }

            if (formatToUse == null) {
                Logging.d(TAG, "no valid format found to switch"
                        + " camera");
                return false;
            }
            pendingChange = true;
            id = new_id;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    changeOnThread(switchDoneEvent, surfaceTexture);
                }
            });
        }
        return true;
    }

    /**
     * change camera face
     */
    public void changeFace(int faceMode)
    {
        stop();
    }

    /**
     * Stop camera and release camera all resources
     */
    public void stop() {
        Logging.d(TAG, "stop");
        synchronized (cameraLock) {
            if (state == STATE.INVLID) {
                Logging.e(TAG, "camera has been stopped");
                return;
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    stopOnThread();
                }
            });
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        state = STATE.INVLID;
        handler = null;
        Logging.d(TAG, "camera stop done");
    }

    private static List<List<Format>> getFormats() {
        if (supportedFormats == null) {
            try {
                Logging.d(TAG, "get supported formats");
                supportedFormats = new ArrayList<
                        List<Format>>(Camera.getNumberOfCameras());
                for (int i = Camera.getNumberOfCameras() - 1; i >= 0; i--) {
                    ArrayList<Format> format = getFormat(i);
                    if (format.size() == 0) {
                        Logging.w(TAG, "fail to get supported "
                                + "formats for camera " + i);
                        if(supportedFormats != null) {
                            supportedFormats.clear();
                            supportedFormats = null;
                        }
                    } else {
                        supportedFormats.add(format);
                    }
                }
                // Reverse the list since it is filled in reverse order.
                if(supportedFormats != null) {
                    Collections.reverse(supportedFormats);
                }
                Logging.d(TAG, "get supported formats done");
            } catch (Exception e) {
                supportedFormats = null;
                Logging.e(TAG, "initStatics failed", e);
            }
        }
        return supportedFormats;
    }

    private static ArrayList<Format> getFormat(int id) {
        ArrayList<Format> formatList = new ArrayList<Format>();

        Camera camera;
        try {
            Logging.d(TAG, "opening camera " + id);
            camera = Camera.open(id);
        } catch (Exception e) {
            Logging.w(TAG, "open camera failed on id " + id);
            return formatList;
        }

        try {
            Camera.Parameters parameters;
            parameters = camera.getParameters();

            List<int[]> listFpsRange =
                    parameters.getSupportedPreviewFpsRange();
            int[] range = {0, 0};
            if (listFpsRange != null)
                range = listFpsRange.get(listFpsRange.size() -1);

            List<Camera.Size> supportedSizes =
                    parameters.getSupportedPreviewSizes();
            for (Camera.Size size : supportedSizes) {
                formatList.add(new Format(size.width, size.height,
                        range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                        range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]));
            }
        } catch (Exception e) {
            Logging.e(TAG, "getFormats failed on id " + id, e);
        }
        camera.release();
        return formatList;
    }

    private void setParams(Camera.Parameters parameters) {
        /* Cancel previous auto focus and set new focus */
        List<String> list = parameters.getSupportedFocusModes();
        if (list.contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
                && parameters.isZoomSupported()) {
            Logging.d(TAG, "set focus mode: "
                    + Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            parameters.setFocusMode(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (list.contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                && parameters.isZoomSupported()) {
            Logging.d(TAG, "set focus mode: "
                    + Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            parameters.setFocusMode(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.cancelAutoFocus();
        } else if (list.contains(Camera.Parameters.FOCUS_MODE_AUTO)
                && parameters.isZoomSupported()) {
            Logging.d(TAG, "set focus mode: "
                    + Camera.Parameters.FOCUS_MODE_AUTO);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else if (list.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            Logging.d(TAG, "set focus mode: "
                    + Camera.Parameters.FOCUS_MODE_FIXED);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        } else {
            Logging.d(TAG, "not set focus mode");
        }
        if (parameters.isVideoStabilizationSupported()) {
            parameters.setVideoStabilization(true);
        }
        // Set camera preview picture format
        if(params.format != ImageFormat.NV21) {
            parameters.setPreviewFormat(params.format);
        }
        // Set camera preview picture fps
        int frameRate = params.fps * 1000;
        int[] range =
                CameraUtils.getFpsRange(parameters, frameRate);
        if (range != null) {
            parameters.setPreviewFpsRange(
                range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
        }
        // Set camera preview picture size
        int[] size = CameraUtils.choosePreviewSize(parameters,
                params.getWidth(), params.getHeight());
        params.setWidth(size[0]);
        params.setHeight(size[1]);
        // Set camera parameter
        camera.setParameters(parameters);
    }

    private void openOnThread() {
        Logging.d(TAG, "openOnThread");
        CameraInfo info = new CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (id == i) {
                camera = Camera.open(i);
                break;
            }
        }
        if (camera == null) {
            Logging.w(TAG, "unable to open camera");
            return;
        }
        this.info = info;
        this.framesCount = 0;
        this.prevTimeMs = -1;
        this.frameMs = 1000 / params.fps;
        Logging.d(TAG, "open camera normal");

        Camera.Parameters parameters = camera.getParameters();
        CameraUtils.setDisplayOrientation(camera,
                info, ((Activity)context).getWindowManager());
        setParams(parameters);
        // Calculate init rotation
        calcRotation();
        // Clear camera frame buffer
        synchronized (frames) {
            frames.clear();
        }
    }

    private void startOnThread() {
        Logging.d(TAG, "startOnThread");
        if(texture == null) {
            glTextures = new int[1];
            // Generate one texture pointer and bind it as an external
            // texture.
            GLES20.glGenTextures(1, glTextures, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    glTextures[0]);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            texture = new SurfaceTexture(glTextures[0]);
            texture.setOnFrameAvailableListener(null);
        }
        try {
            boolean startObserver = true;
            camera.setPreviewTexture(texture);
            if(callback != null) {
                camera.setPreviewCallback(callback);
            } else if(needCallBack) {
                camera.setPreviewCallback(this);
            } else {
                camera.setPreviewCallback(null);
                startObserver = false;
            }
            camera.startPreview();
            if(startObserver) {
                // Start camera observer.
                framesCount = 0;
                handler.postDelayed(
                        observer, OBSERVER_PEROID_MS);
            }
            Logging.d(TAG, "startOnThread done");
            return;
        } catch (IOException e) {
            Logging.e(TAG, "start camera failed", e);
        }
        stopOnThread();
        state = STATE.INVLID;
        handler = null;
    }

    private void calcRotation() {
        // Get rotation information
        int rotation = CameraUtils.getDeviceOrientation(
                ((Activity)context).getWindowManager());
        if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            rotation = 360 - rotation;
        }
        this.rotation = (info.orientation + rotation) % 360;
    }

    private boolean dropFrame(long captureTimeNs) {
        long realCaptureTimeMs = 0;
        long currCaptureTimeMs = 0;

        if (prevTimeMs == -1) {
            prevTimeMs = captureTimeNs / 1000000;
        }
        realCaptureTimeMs = captureTimeNs / 1000000;
        currCaptureTimeMs = prevTimeMs + frameMs;
        if (realCaptureTimeMs < currCaptureTimeMs) {
            return true;
        } else {
            prevTimeMs = prevTimeMs + frameMs;
            return false;
        }
    }

    private void changeOnThread(Runnable switchDoneEvent,
        @Nullable SurfaceTexture surfaceTexture) {
        Logging.d(TAG, "changeOnThread");

        // Release preview camera
        doStopOnThread();

        // Reopen and start new camera
        this.texture = surfaceTexture;
        openOnThread();
        startOnThread();
        pendingChange = false;
        Logging.d(TAG, "changeOnThread done");
        if (switchDoneEvent != null) {
            switchDoneEvent.run();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void stopOnThread() {
        Logging.d(TAG, "stopOnThread");
        doStopOnThread();
        Looper.myLooper().quitSafely();
        Logging.d(TAG, "stopOnThread done");
        return;
    }

    private void doStopOnThread() {
        Logging.d(TAG, "doStopOnThread");
        if (camera == null) {
            return;
        }
        try {
            handler.removeCallbacks(observer);
            Logging.d(TAG, "stop preview");
            camera.stopPreview();
            camera.setPreviewCallback(null);

            camera.setPreviewTexture(null);
            texture = null;
            if (glTextures != null) {
                GLES20.glDeleteTextures(1, glTextures, 0);
                glTextures = null;
            }
            synchronized (frames) {
                lastFrame = null;
                frames.clear();
            }

            Logging.d(TAG, "release camera");
            camera.release();
            camera = null;
        } catch (IOException e) {
            Logging.e(TAG, "failed to stop camera", e);
        }
        Logging.d(TAG, "doStopOnThread done");
    }

    private class CameraThread extends Thread {
        private Exchanger<Handler> exchanger;
        public CameraThread(Exchanger<Handler> exchanger) {
            this.exchanger = exchanger;
        }

        @Override
        public void run() {
            Looper.prepare();
            exchange(exchanger, new Handler());
            Looper.loop();
        }
    }

    private static <T> T exchange(Exchanger<T> exchanger, T value) {
        try {
            return exchanger.exchange(value);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
