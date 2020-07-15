package com.camera.zcc.cameraface.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Process;
import android.view.Surface;
import android.view.WindowManager;

import org.webrtc.Logging;

import java.util.List;

import static java.lang.Math.abs;

public class CameraUtils {
    private final static String TAG = CameraUnity.class.getName();

    /**
     * Get orientation of the camera image. The value is the angle that
     * the camera image needs to be rotated clockwise so it shows
     * correctly on the display in its natural orientation. It should
     * be 0, 90, 180, or 270.
     *
     * @param face the desired camera face
     * @return orientation of the face camera image
     */
    public static int getCameraOrientation(int face) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == face) {
                break;
            }
        }
        return info.orientation;
    }

    public static int getDeviceOrientation(WindowManager manager) {
        int orientation = 0;

        int rotation = manager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_90:
                orientation = 90;
                break;
            case Surface.ROTATION_180:
                orientation = 180;
                break;
            case Surface.ROTATION_270:
                orientation = 270;
                break;
            case Surface.ROTATION_0:
            default:
                orientation = 0;
                break;
        }
        return orientation;
    }

    public static void setDisplayOrientation(
            Camera camera, Camera.CameraInfo info, WindowManager manager) {
        int rotation = manager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        Logging.e("result=====",result+"");
        camera.setDisplayOrientation(result);
    }

    public static int[] choosePreviewSize(
            Camera.Parameters parameters, int w, int h) {
        Camera.Size ppsfv = parameters.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Logging.d(TAG, "camera preferred preview size for"
                    + " video is " + ppsfv.width + "x" + ppsfv.height);
        }

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width == w && size.height == h) {
                parameters.setPreviewSize(w, h);
                return new int[]{w, h};
            }
        }

        Logging.w(TAG, "unable to set preview size to "
                + w + "x" + h);
        if (ppsfv != null) {
            parameters.setPreviewSize(ppsfv.width, ppsfv.height);
            return new int[]{ppsfv.width, ppsfv.height};
        }
        // else use whatever the default size is
        return new int[]{0, 0};
    }

    public static int[] getFpsRange(Camera.Parameters parameters, int framerate) {
        List<int[]> listFpsRange = parameters.getSupportedPreviewFpsRange();
        int[] bestRange = null;
        int bestRangeDiff = Integer.MAX_VALUE;
        for (int[] range : listFpsRange) {
            int rangeDiff =
                    abs(framerate -range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX])
                    + abs(range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] - framerate);
            if (bestRangeDiff > rangeDiff) {
                bestRange = range;
                bestRangeDiff = rangeDiff;
            }
        }
        return bestRange;
    }

    // Checks if the process has as specified permission or not.
    public static boolean hasPermission(Context context, String permission) {
        return context.checkPermission(permission, Process.myPid(), Process.myUid())
                == PackageManager.PERMISSION_GRANTED;
    }
}
