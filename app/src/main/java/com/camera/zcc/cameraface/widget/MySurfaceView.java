package com.camera.zcc.cameraface.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.shuzijiayuan.sidsdk.SidSDKWrapper;

import org.webrtc.Logging;

public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private final static String TAG  = MySurfaceView.class.getName();
    private boolean bExternalCapture = true;
    private boolean bSurfaceCreate = false;
    private SidSDKWrapper mSidSdk = null;
    private boolean bInitCheck = false;
    private boolean isLocal = false;
    public Context mContext = null;
    private String mUserName = "";
    private int mChannelId = 0;

    public MySurfaceView(Context context) {
        super(context);
        mContext = context;
        getHolder().addCallback(this);
    }

    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Logging.d(TAG, "surfaceCreated: " + mUserName);
        if (bInitCheck) {
            if (isLocal && bExternalCapture) {
                mSidSdk.initDisplayVFrame((int) Long.parseLong(mUserName));
                mSidSdk.setSurfaceForDisplayVFrame((int) Long.parseLong(mUserName), holder);
            } else {
                mSidSdk.setSurface(holder, mUserName, mChannelId, isLocal);
            }
        }
        getHolder().setKeepScreenOn(true);
        bSurfaceCreate = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logging.d(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Logging.d(TAG, "surfaceDestroyed: " + mUserName);
        if (bInitCheck) {
            if(isLocal && bExternalCapture) {
                mSidSdk.setSurfaceForDisplayVFrame(
                        (int) Long.parseLong(mUserName), null);
                mSidSdk.unitDisplayVFrame((int) Long.parseLong(mUserName));
            } else {
                mSidSdk.setSurface(null, mUserName, mChannelId, isLocal);
            }
        }
        bSurfaceCreate = false;
    }

    public void init(SidSDKWrapper sidsdk,
                     String user, int channelid, boolean islocal, boolean bextcapture) {
        assert sidsdk != null;
        assert user != null;

        mSidSdk = sidsdk;
        mUserName = user;
        mChannelId = channelid;
        isLocal= islocal;
        bSurfaceCreate = false;
        bExternalCapture = bextcapture;

        bInitCheck = true;
    }

    public void setUser(String user) {
        mUserName = user;
    }

    public String getUser() {
        return mUserName;
    }

    public int getChannelId() {
        return mChannelId;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public boolean isSurfaceCreate() {
        return bSurfaceCreate;
    }
}

