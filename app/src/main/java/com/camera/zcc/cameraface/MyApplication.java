package com.camera.zcc.cameraface;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.faceunity.fup2a.FaceUnity;
import com.shuzijiayuan.sidsdk.SidSDKWrapper;

import org.webrtc.Logging;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MyApplication extends Application {
    private static MyApplication instance = null;
    private SidSDKWrapper mWrapper;
    private FHistory history;


    public static MyApplication getInstance() {

        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
//        CrashUtil.getInstance().init(this);//集成im
           disableAPIDialog();//去除安卓9弹框
        initWrapper();
        history = FHistory.getInstance(this);
        initFaceUnity();

    }
    public FHistory getHistory() {
        return history;
    }
    public SidSDKWrapper getWrapper() {
        return mWrapper;
    }
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // you must install multiDex whatever tinker is installed!
    }
    private void initWrapper() {
        mWrapper = new SidSDKWrapper();
        mWrapper.enableLogging(Logging.Severity.LS_VERBOSE);
        mWrapper.dumpLogging("/sdcard/f2/",
                Constants.MAX_LOGFILE_SIZE, Logging.Severity.LS_VERBOSE);
        SidSDKWrapper.SidSDKConfigParameters config =
                new SidSDKWrapper.SidSDKConfigParameters();
        config.setServer_ip(Constants.SERVER_ADDRESS);
        config.setServer_port(Constants.SERVER_PORT);
        config.setVideo_width(Constants.VOIP_W);
        config.setVideo_height(Constants.VOIP_H);
        //启用外部captrue
        config.setExternal_capturer(true);
        mWrapper.init(this, config);
        mWrapper.setSendFormat(Constants.VOIP_W,
                Constants.VOIP_H, Constants.VOIP_FPS);
        registerSidSdk();
    }
    private void initFaceUnity() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FaceUnity.setup(instance);
                Logging.d("FaceUnity", "setup done");
            }
        }).start();
    }
    @Override
    public void onTerminate() {
        destroy();
        super.onTerminate();
    }
    private void destroy() {
        if (mWrapper != null) {
            mWrapper.endCall();
            mWrapper.unRegister();
            mWrapper.setCallListener(null);
            mWrapper.setRegisterListener(null);
            mWrapper = null;
        }
        FaceUnity.destroy();
    }
    public void registerSidSdk() {

//                    mWrapper.setRegisterListener(
//                            new MyOnRegisterListener());
//                    mWrapper.setCallListener(new onCallListener());
//                    mWrapper.register(
//                              "");



    }
    /**
     * 反射 禁止弹窗
     */
    private void disableAPIDialog(){
        if (Build.VERSION.SDK_INT < 28)return;
        try {
            Class clazz = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = clazz.getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object activityThread = currentActivityThread.invoke(null);
            Field mHiddenApiWarningShown = clazz.getDeclaredField("mHiddenApiWarningShown");
            mHiddenApiWarningShown.setAccessible(true);
            mHiddenApiWarningShown.setBoolean(activityThread, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
