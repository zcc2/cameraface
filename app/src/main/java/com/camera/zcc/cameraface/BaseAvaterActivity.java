package com.camera.zcc.cameraface;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.camera.zcc.cameraface.camera.CameraUnity;
import com.camera.zcc.cameraface.data.AvatarItem;
import com.camera.zcc.cameraface.data.FilterItem;
import com.camera.zcc.cameraface.data.db.DataManager;
import com.camera.zcc.cameraface.listener.IFilterItemClickListener;
import com.camera.zcc.cameraface.listener.IItemClickListener;
import com.camera.zcc.cameraface.recorder.AVRecorder;
import com.camera.zcc.cameraface.until.FileUtils;
import com.camera.zcc.cameraface.widget.MySurfaceView;
import com.faceunity.fup2a.FaceUnity;
import com.faceunity.fup2a.gles.FullFrameRect;
import com.faceunity.fup2a.gles.Texture2dProgram;
import com.faceunity.fup2a.misc.MiscUtil;
import com.faceunity.fup2a.ui.AspectFrameLayout;
import com.faceunity.wrapper.faceunity;
import com.shuzijiayuan.sidsdk.SidSDKWrapper;
import com.szjy.gaussblurjar.GaussBlur;

import org.webrtc.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static java.lang.Math.ceil;

public abstract class BaseAvaterActivity extends BaseActivity
        implements IItemClickListener, View.OnClickListener, IFilterItemClickListener {
    private final static String TAG = BaseAvaterActivity.class.getName();
    private final static int MAX_DISPLAY_BUFFER_NUMBER = 2;
    private final static int GAUSS_BLUR_LEVEL = 50;
    private final static boolean SAME_IN_OUT_FORMAT= false;
    private final static boolean ONLY_USING_BUFFER = false;
    private final static boolean EXIST_OUT_BUFFER  = true;

//    static final int  REQUEST_CODE_ASK_CALL_PHONE=122;

    public final static String[] FILTERS_NAME = {"origin", "delta", "electric", "slowlived", "tokyo", "warm"};

    protected FrameLayout.LayoutParams mCameraParams = null;
    protected AspectFrameLayout mLPreviewContainer = null;
    protected LinearLayout mAvatarSelectLayout = null;
    protected LinearLayout mFilterSelectLayout = null;
    protected boolean mMainButtonShowed = true;
    protected boolean mFilterShowed = false;
    protected int mVideoW = Constants.CAMERA_W;
    protected int mVideoH = Constants.CAMERA_H;
    protected int mGaussW = Constants.RECORD_W;
    protected int mGaussH = Constants.RECORD_H;
    protected FrameLayout mFlContent = null;
    protected SidSDKWrapper mWrapper = null;
    protected boolean mIsInVoip = false;
    protected CameraUnity mCamera = null;
    protected AVRecorder record = null;
    protected int heightMainButton = 0;
    protected int heightChooseAvatar = 0;
    protected int heightFilter = 0;
    // 生成的使用2000来代替
    protected int mUsingAvatarIndex = 2000;

    private LinkedList<CameraUnity.Frame> mAvatarFrames =
            new LinkedList<CameraUnity.Frame>();
    private int mGaussFS = Constants.RECORD_FPS;
    private DisplayThread mDisplayThread = null;
    private MySurfaceView mMySurfaceView = null;
    private GLSurfaceView mGLSurfaceView = null;
    private HandlerThread mAsyncThread = null;
    private Handler mAsyncHandler = null;
    private RecyclerView mRvChoose = null;
    private RecyclerView rv_filter = null;
    private ImageAdapter mImageAdapter = null;
    private FilterAdapter mFilterAdapter = null;
    //private RadioButton mRbHot = null;
    //private RadioButton mRbMine= null;
    private List<AvatarItem> mIds = new ArrayList<>();

    private List<FilterItem> filterItems = new ArrayList();

    private DataManager mDataManager = null;
    private FHistory mHistory = null;
    private int mAvatarType = 1;
    private int currentSelectItem = -100;
    private GLRenderer mGLRenderer = null;
    private GaussBlur mGaussBlur = null;
    private int mFaceBeautyItem = 0; //美颜道具
    public static float mFilterLevel = 1.0f;
    public static String mFilterName = FILTERS_NAME[0];

    private HandlerThread mCreateItemThread;
    private Handler mCreateItemHandler;
    private int mEffectItem = 0; //贴纸道具
    private boolean isFace=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logging.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_base_avater);
        mLPreviewContainer =
                (AspectFrameLayout)findViewById(R.id.avatar_preview_container);
        mFlContent = (FrameLayout)findViewById(R.id.ff_content);
        mAvatarSelectLayout =
                (LinearLayout) findViewById(R.id.base_avatar_select);
        mFilterSelectLayout = (LinearLayout) findViewById(R.id.base_filter_select) ;
        mRvChoose = (RecyclerView)findViewById(R.id.rv_choose);
        rv_filter = (RecyclerView)findViewById(R.id.rv_filter);
        //mRbHot = (RadioButton)findViewById(R.id.rb_hot);
        //mRbMine = (RadioButton)findViewById(R.id.rb_mine);

        mAsyncThread = new HandlerThread("AsyncThread");
        mAsyncThread.start();
        mAsyncHandler = new AsyncHandler(mAsyncThread.getLooper());


        mCreateItemThread = new HandlerThread("CreateItemThread");
        mCreateItemThread.start();
//        mCreateItemHandler = new CreateItemHandler(mCreateItemThread.getLooper());


        mCamera = new CameraUnity();
//        Accessibility();
        initView();

    }

    public void changeCamera() {
        if(mGLRenderer == null) {
            return;
        }
        if(isFace){
            isFace=false;
        }else {
            isFace=true;
        }
        mGLRenderer.changeCamera();
    }

    private void initView() {
        mCameraParams = getCameraParams();
        mWrapper = MyApplication.getInstance().getWrapper();
        mIsInVoip = false;
        mFlContent.addView(GetChild());
        mGaussBlur = new GaussBlur(mGaussW, mGaussH, mGaussFS);
        mHistory = MyApplication.getInstance().getHistory();
        mDataManager = new DataManager(this);
        mRvChoose.setLayoutManager(new GridLayoutManager(this, 4));
        rv_filter.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mImageAdapter =
                new ImageAdapter(this, mIds, this);
        mFilterAdapter = new FilterAdapter(this, filterItems, this);
        mRvChoose.setAdapter(mImageAdapter);
        rv_filter.setAdapter(mFilterAdapter);
        //mRbHot.setOnClickListener(this);
        //mRbMine.setOnClickListener(this);

        if(!mIsInVoip) {
            Logging.d(TAG, "use v frame display");
            synchronized (mAvatarFrames) {
                mAvatarFrames.clear();
            }
            mDisplayThread = new DisplayThread();
            mDisplayThread.start();
        }
        processIntentForAvatar();
        processIntentForFilter();
        initAvatarPreview();
        Logging.d(TAG, "initView done");
    }

    private static class AsyncHandler extends Handler {
        public final static int HANDLE_CREATE_ITEM = 1;

        AsyncHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLE_CREATE_ITEM:
                    FaceUnity.changeFUnity((Bundle)msg.obj);
                    break;
                default:
                    Logging.w(TAG, "unknown message: "
                            + msg.what);
                    break;
            }
        }
    }

    private class DisplayThread extends Thread {
        private final String TAG = DisplayThread.class.getName();
        private boolean isRunning = false;

        @Override
        public void run() {
            isRunning = true;
            while (isRunning) {
                CameraUnity.Frame frame = null;
                synchronized (mAvatarFrames) {
                    if (!mAvatarFrames.isEmpty()) {
                        frame = mAvatarFrames.removeFirst();
                    } else {
                        try {
                            mAvatarFrames.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                }

                if (record != null) {
                    record.writeVideo(frame.frame,
                            frame.width, frame.height);
                }

                if ((mMySurfaceView != null)
                        && mMySurfaceView.isSurfaceCreate()) {
                    mWrapper.displayVFrame(
                            Integer.parseInt(mMySurfaceView.getUser()),
                            frame.frame, frame.format, frame.width,
                            frame.height, frame.rotation, isFace,
                            false, true);
                }
            }
        }

        public void exit() {
            Logging.d(TAG, "exit");
            isRunning = false;
            synchronized (mAvatarFrames) {
                mAvatarFrames.notify();
            }
        }
    }

    private class GLRenderer implements GLSurfaceView.Renderer {
        private final static boolean CLEAR_READBACK = false;
        private final static int LOG_DRAW_FPS_MASK = 15;
        private final static int LATENCY_FRAME_NUM = 2;
        private SurfaceTexture mSurfaceTexture = null;
        private int mLatencyCnt = LATENCY_FRAME_NUM;
        private FullFrameRect mCameraDisplay = null;
        private int mCameraTextureId = 0;
        private boolean mIsFirst = true;
        private int mSwitchFlag  = -1;
        private byte[] mVData = null;
        private long mStartTime = 0;
        private int mFrameId = 0;
        private int mDrawCnt = 0;

        final float[] mtx = new float[16];

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            Logging.d(TAG, "onSurfaceCreated");
            mCameraDisplay = new FullFrameRect(new Texture2dProgram(
                    Texture2dProgram.ProgramType.TEXTURE_EXT));
            mCameraTextureId = mCameraDisplay.createTextureObject();
            mSurfaceTexture = new SurfaceTexture(mCameraTextureId);


            try {
//                InputStream is = getAssets().open("v3.bundle");
//                byte[] v3data = new byte[is.available()];
//                int len = is.read(v3data);
//                is.close();
//                faceunity.fuSetup(v3data, null, authpack.A());
//                  faceunity.fuSetMaxFaces(3);//设置最大识别人脸数目
////                Log.e(TAG, "fuSetup v3 len " + len);
////
////                is = getAssets().open("anim_model.bundle");
////                byte[] animModelData = new byte[is.available()];
////                is.read(animModelData);
////                is.close();
////                faceunity.fuLoadAnimModel(animModelData);
////                faceunity.fuSetExpressionCalibration(1);
//
                InputStream is = getAssets().open("face_beautification.bundle");
                byte[] itemData = new byte[is.available()];
                int len = is.read(itemData);
                Log.e(TAG, "beautification len " + len);
                is.close();
                mFaceBeautyItem = faceunity.fuCreateItemFromPackage(itemData);

            } catch (IOException e) {
                e.printStackTrace();
            }

            mGaussBlur.startGaussBlur(GAUSS_BLUR_LEVEL, -1);
            mLatencyCnt = LATENCY_FRAME_NUM;
            mSwitchFlag = -1;
            mVData = null;
            startCamera();
            mFrameId = 0;
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Logging.d(TAG,
                    "onSurfaceChanged: " + width + "-" + height);
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            CameraUnity.Frame frame = null;
            if(mCamera != null) {
                frame = mCamera.pop(false);
            } else {
                Logging.w(TAG, "no camera object");
            }

            if(frame != null) {
                if(mIsFirst) {
                    // Get timestamp in nanosecond
                    mStartTime = TimeUnit.MILLISECONDS.toMillis(
                            SystemClock.elapsedRealtime());
                }
                mDrawCnt++;

                /**
                 * 获取camera数据, 更新到texture
                 */
                try {
                    mSurfaceTexture.updateTexImage();
                    mSurfaceTexture.getTransformMatrix(mtx);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                if(SAME_IN_OUT_FORMAT) {
                    if (frame.format == ImageFormat.YV12) {
                        changeYV12toI420(frame);
                    } else {
                        Logging.w(TAG, "not " +
                                "realize format change");
                    }
                }

//                int flags |= faceunity.FU_ADM_FLAG_ENABLE_READBACK;
//                int oW, oH;
//
//                    oW = Constants.RECORD_W;
//                    oH = Constants.RECORD_H;
//
//                faceunity.fuDualInputToTexture(frame.frame,
//                        mCameraTextureId, flags, frame.width,
//                        frame.height, frame.rotation, mFrameId++,
//                        oW, oH, new int[]{mFaceBeautyItem, mEffectItem});
//                fuTex = draw(mCameraNV21Byte, mFuImgNV21Bytes, mCameraTextureId, mCameraWidth, mCameraHeight, mFrameId++, new int[]{mFaceBeautyItem, mEffectItem}, mCurrentCameraType);

                // Beautify process
                if(!mIsInVoip) {
                    if(CLEAR_READBACK) {
                        if (mSwitchFlag != 0) {
                            faceunity.fuClearReadbackRelated();
                            mSwitchFlag = 0;
                        }
                    }
                    beautifyToImage(frame, false);
                } else {
                    if(CLEAR_READBACK) {
                        if (mSwitchFlag != 1) {
                            faceunity.fuClearReadbackRelated();
                            mSwitchFlag = 1;
                        }
                    }
                    beautifyToImage(frame, true);
                }

                // Gauss process
                if(FaceUnity.fuIsTracking() <= 0) {
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            stopRecorder();
                            showNoFacetip();
                        }
                    });
                    mLatencyCnt = LATENCY_FRAME_NUM;
                } else {
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            hideNoAvaTip();
                        }
                    });
                    if(mLatencyCnt >= 0) {
                        mLatencyCnt--;
                    }
                }
                if(mLatencyCnt >= 0) {
                    mGaussBlur.processFrame(frame.frame,
                            frame.width, frame.height);
                }

                int format = SidSDKWrapper.SUPPORTED_FORMAT_I420;
                if(mIsInVoip && mWrapper != null) {
                    // Send external captuer frame data to sid sdk
                    mWrapper.sendExtCapturerFrameData(frame.frame, format,
                            frame.width, frame.height, frame.rotation,
                            frame.timestamp);
                }

                faceunity.fuItemSetParam(mFaceBeautyItem, "filter_level", mFilterLevel);
                faceunity.fuItemSetParam(mFaceBeautyItem, "filter_name", mFilterName);
                // For video frame display
                if (!mIsInVoip) {
                    synchronized (mAvatarFrames) {
                        if(mAvatarFrames.size()
                                > MAX_DISPLAY_BUFFER_NUMBER) {
                            Logging.d(TAG, "too buffers for display");
                            mAvatarFrames.removeFirst();
                        }
                        frame.format = format;
                        mAvatarFrames.add(frame);
                        mAvatarFrames.notify();
                    }
                }
//
//                faceunity.fuItemSetParam(mFaceBeautyItem, "filter_level", mFilterLevel);
//                faceunity.fuItemSetParam(mFaceBeautyItem, "filter_name", mFilterName);
                if((mDrawCnt & LOG_DRAW_FPS_MASK) == 0) {
                    long endTime = TimeUnit.MILLISECONDS.toMillis(
                            SystemClock.elapsedRealtime());
                    Logging.d(TAG, "draw fps: "
                            + mDrawCnt*1000 / (endTime-mStartTime));
                }

                if(mIsFirst) {
                    Logging.d(TAG, "first frame end");
                    mIsFirst = false;
                }
            }
        }


        public void startCamera() {
            Log.e(TAG, "startCamera: " + mSurfaceTexture);
            if(mSurfaceTexture != null) {
                /* Start camera */
                if (mCamera != null) {
                    mCamera.start(mSurfaceTexture, (bytes, camera) -> {
                        if (mCamera != null) {
                            mCamera.push(bytes, camera);
                        }
                        if (mGLSurfaceView != null) {
                            mGLSurfaceView.requestRender();
                        }
                    });
                }
            }
        }

        /**
         * change camera
         */
        public boolean changeCamera() {
            if(mCamera == null || mSurfaceTexture == null) {
                return false;
            }
            mCamera.change(new Runnable() {
                @Override
                public void run() {

                }
            }, mSurfaceTexture);
            return true;
        }

        public void notifyPause() {
            if (mCameraDisplay != null) {
                mCameraDisplay.release(false);
                mCameraDisplay = null;
            }
            mGaussBlur.stopGaussBlur();
            destroySurfaceTexture();
        }

        private int beautifyToImage(CameraUnity.Frame frame, boolean voip) {
            int fuTex = 0;
            int flags = faceunity.FU_ADM_FLAG_I420_BUFFER;

            if(ONLY_USING_BUFFER) {
                fuTex = FaceUnity.fuRenderToNV21Image(frame.frame,
                        flags, frame.width, frame.height,
                        frame.rotation, frame.facing, mFrameId++);
            } else if(EXIST_OUT_BUFFER) {
                flags |= faceunity.FU_ADM_FLAG_ENABLE_READBACK;
                int oW, oH;
                if(voip) {
                    oW = Constants.VOIP_W; oH = Constants.VOIP_H;
                } else {
                    oW = Constants.RECORD_W;
                    oH = Constants.RECORD_H;
                }
                fuTex = FaceUnity.fuDualInputToTexture(frame.frame,
                        mCameraTextureId, flags, frame.width,
                        frame.height, frame.rotation, mFrameId++,
                        oW, oH, frame.frame, new int[]{mFaceBeautyItem, mEffectItem});
                frame.width = oW;   frame.height = oH;
            } else {
                flags |= faceunity.FU_ADM_FLAG_ENABLE_READBACK;
                fuTex = FaceUnity.fuDualInputToTexture(frame.frame,
                        mCameraTextureId, flags, frame.width,
                        frame.height, frame.rotation, mFrameId++, new int[]{mFaceBeautyItem, mEffectItem});
            }
            return fuTex;
        }

        private void changeYV12toI420(CameraUnity.Frame frame) {
            int width = frame.width, height = frame.height;
            int yStride  = roundUp(width, 16);
            int uvStride = roundUp(yStride / 2, 16);
            int ySize = yStride * height;
            int uvSize = uvStride * height / 2;

            byte[] image = frame.frame;
            if(mVData == null) {
                mVData = new byte[uvSize];
            }
            System.arraycopy(image, ySize,
                    mVData, 0, uvSize);
            System.arraycopy(image, ySize
                    + uvSize, image, ySize, uvSize);
            System.arraycopy(mVData, 0,
                    image, ySize + uvSize, uvSize);
        }

        private int roundUp(int x, int alignment) {
            return (int)ceil(x / (double)alignment) * alignment;
        }

        private void destroySurfaceTexture() {
            if (mSurfaceTexture != null) {
                mSurfaceTexture.release();
                mSurfaceTexture = null;
            }
        }
    }

    private void initAvatarPreview() {
        if (mGLSurfaceView != null) {
            Logging.d(TAG, "mGLRenderer is not null");
            return;
        }
        mGLSurfaceView = new GLSurfaceView(this);
        mMySurfaceView = new MySurfaceView(this);

        mMySurfaceView.setZOrderMediaOverlay(true);
        mMySurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mMySurfaceView.init(mWrapper, "0",
             0, true, true);
        mGLRenderer = new GLRenderer();
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(mGLRenderer);
        mGLSurfaceView.setRenderMode(
             GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mLPreviewContainer.addView(mGLSurfaceView);

        mLPreviewContainer.addView(mMySurfaceView);
    }

    private void openCamera() {
        if(mCamera != null) {
            CameraUnity.Parameter params =
                    new CameraUnity.Parameter();
            if(SAME_IN_OUT_FORMAT) {
                params.setFormat(ImageFormat.YV12);
            }
            params.setFps(Constants.CAMERA_FPS);
            params.setWidth(mVideoW);
            params.setHeight(mVideoH);
            mCamera.open(this, params, false);
        }
    }

    private void startCamera() {
        if(mGLRenderer != null) {
            mGLRenderer.startCamera();
        }
    }

    private void stopCamera() {
        if(mCamera != null) {
            mCamera.stop();
        }
    }



    /**
     * 在此方法中实现子activity的各子布局
     *
     * @return
     */
    protected abstract View GetChild();

    /**
     *  此方法中实现录制的停止
     * @return
     */
    protected abstract void stopRecorder();
 /**
     *  此方法中实现毛玻璃提示
     * @return
     */
    protected abstract void showNoFacetip();/**
     *  此方法中隐藏毛玻璃提示
     * @return
     */
    protected abstract void hideNoAvaTip();

    public FrameLayout.LayoutParams getCameraParams(){
        FrameLayout.LayoutParams cameraParams = new FrameLayout.LayoutParams(
                Utils.dip2px(95), Utils.dip2px(125));
        cameraParams.gravity = Gravity.RIGHT;
        return cameraParams;
    }

    @Override
    public boolean onItemClick(int position) {
        Logging.d(TAG, "onItemClick: " + position);
        /*if (position == -1) {
            startActivity(PublishImageTypeActivity.class,false);
        } else {*/
            if (FaceUnity.isInChangeFaceUnity()) {
                showToast("切换太频繁");
                return false;
            }

            int type = -1;
            if (mHistory != null) {
                type = mHistory.getAvatarType();
            }
            Logging.d(TAG, "type=" + type + ",mAvatarType=" + mAvatarType
                    + ",currentSelectItem=" + currentSelectItem + ",position="
                    + position);
            if ((type == mAvatarType) && currentSelectItem == position) {
                return false;
            } else {
                currentSelectItem = position;
            }

            if (mAvatarType == 1) {
                mUsingAvatarIndex = currentSelectItem;
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("index", String.format("%d", currentSelectItem));
            } else {
                mUsingAvatarIndex = 2000;
            }

            AvatarItem item = mIds.get(position);
            Bundle bundle = mDataManager.getBundleByAvatarItem(item);
            if (mHistory != null) {
                mHistory.setAvatarType(mAvatarType);
                mHistory.setAvatarIndex(position);
//                LocalBroadcastManager.getInstance(FApplication.getContext(
//                )).sendBroadcast(new Intent(Constants.UPDATE_CENTER_AVATER));
            }
            changeAvatar(bundle);
        //}
        return true;
    }

    @Override
    public boolean onLongClick(int position) {
        boolean flag = false;
        boolean checked = mIds.get(position).isChecked();
        if(checked){
            showToast("该形象正在使用中，无法删除。");
            flag = false;
        }else {
            FileUtils.delete(mIds.get(position).imageOriginUri);
            FileUtils.delete(mIds.get(position).photoOriginPath);
            FileUtils.delete(mIds.get(position).avatarDir);
            mDataManager.deleteHistory(mIds.get(position).bundleUri);
            mIds.remove(position);
            for (int i = 0; i < mIds.size(); i++) {
                AvatarItem avatarItem = mIds.get(i);
                if(avatarItem.isChecked()) {
                    mHistory.setAvatarIndex(i);
                }
            }
            flag = true;
        }
        return flag;
    }

    @Override
    public boolean onFilterItemClick(int position) {

        if (FaceUnity.isInChangeFaceUnity()) {
            showToast("切换太频繁");
            return false;
        }

        mFilterName = FILTERS_NAME[position];

//        FaceUnity.setBeautifyParam(mFilterName, mFilterLevel);

//        Bundle bundle  = new Bundle();
//        bundle.putInt(Constant.GENDER_INTENT_KEY, Constant.GENDER_EFCT);
//        bundle.putString(Constant.FINAL_BUNDLE_SRC_FILE_INTENT_KEY, "mayun.bundle");
//        bundle.putBoolean(Constant.IS_LOCAL_PRELOAD_AVATAR, true);
//
//        FaceUnity.changeFUnity(bundle);


        return true;
    }

    @Override
    public boolean onFilterLongClick(int position) {
        return false;
    }

    protected void processIntentForAvatar() {
        Intent intent = getIntent();
        Bundle bundle = null;
        if (intent != null) {
            Logging.d(TAG, "intent: " + intent);
            if (intent.getAction()
                    == "android.intent.action.P2A") {
                bundle = intent.getExtras();
            }
        }
        if (bundle == null) {
            Logging.d(TAG, "intent bundle is null: " + mHistory);
            /* Get history avatar type and index */
            int avatarType = -1, avatarIndex = 0;
            if (mHistory != null) {
                avatarType = mHistory.getAvatarType();
                if (avatarType != -1) {
                    avatarIndex = mHistory.getmAvatarIndex();
                }
            }
            if(avatarIndex < 0) {
                avatarIndex = 0;
            }
            Logging.d(TAG, "avatarType: "
                    + avatarType + ", avatarIndex: " + avatarIndex);
            if (avatarType == -1) {
                avatarType = 1;
                if (mHistory != null) {
                    mHistory.setAvatarType(avatarType);
                    mHistory.setAvatarIndex(avatarIndex);
                }
            }
            changeAvatarItems(avatarType, avatarIndex);

            if (avatarType == 1) {
                mUsingAvatarIndex = avatarIndex;
            } else {
                mUsingAvatarIndex = 2000;
            }
            if (mIds.size() > avatarIndex) {
                AvatarItem item = mIds.get(avatarIndex);
                bundle = mDataManager.getBundleByAvatarItem(item);
            } else {
                MiscUtil.toast(this, "Avatar "
                        + avatarIndex + " not exsist!");
            }

        } else {
            Logging.d(TAG, "intent bundle is avatar bundle");
            mIds.clear();
//            UserInfo info = FApplication.getInstance().getUserInfo();
//            if (info != null) {
//                mIds.addAll(mDataManager.getAllSelfItems(info.getUid()));
//            }
            //mRbMine.setChecked(true);
            //mRbHot.setChecked(false);
            mAvatarType = 2;
            mUsingAvatarIndex = 2000;
            int last = mIds.size() - 1;
            updateImageAdaper(last);
            Logging.d(TAG, "avatar index=" + last);
            currentSelectItem = last;
            if (mHistory != null) {
                mHistory.setAvatarType(2);
                mHistory.setAvatarIndex(last);
            } else {
                Logging.d(TAG, "history is null");
            }
        }
        changeAvatar(bundle);
    }

    protected void processIntentForFilter() {
        for(int i = 0; i < Constants.FILTERS_NAME.length; i++) {
            FilterItem filterItem = new FilterItem();
            filterItem.filterName = Constants.FILTERS_NAME[i];
            filterItem.filterIcon = Constants.FILTER_ITEM_RES_ARRAY[i];
            filterItems.add(filterItem);

//            updateFilterAdapter(1);
            mFilterAdapter = new FilterAdapter(this, filterItems, this);
            rv_filter.setAdapter(mFilterAdapter);
        }
    }

    private void changeAvatar(final Bundle bundle) {
        if(mAsyncHandler != null) {
            mAsyncHandler.removeMessages(
                    AsyncHandler.HANDLE_CREATE_ITEM);
            mAsyncHandler.sendMessage(Message.obtain(
                    mAsyncHandler,
                    AsyncHandler.HANDLE_CREATE_ITEM, bundle));
        } else {
            FaceUnity.changeFUnity(bundle);
        }
    }

    private void changeAvatarItems(int type, int index) {
        if (type == 1) {
            mIds.clear();
            mIds.addAll(mDataManager.getAllEffectItems());
        } else {
            mIds.clear();
//            UserInfo info = FApplication.getInstance().getUserInfo();
//            if (info != null) {
//                mIds.addAll(mDataManager.getAllSelfItems(info.getUid()));
//            }
        }
        mAvatarType = type;
        updateImageAdaper(index);
    }

    private void updateImageAdaper(int index) {
        if (index == -1) {
            for (int i = 0; i < mIds.size(); i++) {
                mIds.get(i).setChecked(false);
            }
        } else {
            if (mIds.size() > index) {
                mIds.get(index).setChecked(true);
                for (int i = 0; i < mIds.size(); i++) {
                    if (i != index) {
                        mIds.get(i).setChecked(false);
                    }
                }
            }
        }
        mImageAdapter.notifyDataSetChanged();
    }

    private void updateFilterAdapter(int index) {
        if (index == -1) {
            for (int i = 0; i < filterItems.size(); i++) {
                filterItems.get(i).setChecked(false);
            }
        } else {
            if (filterItems.size() > index) {
                filterItems.get(index).setChecked(true);
                for (int i = 0; i < filterItems.size(); i++) {
                    if (i != index) {
                        filterItems.get(i).setChecked(false);
                    }
                }
            }
        }
        mFilterAdapter.notifyDataSetChanged();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void destroy() {
        Logging.d(TAG, "destroy");
        if (mDisplayThread != null) {
            mDisplayThread.exit();
            try {
                mDisplayThread.join(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (mAvatarFrames) {
                mAvatarFrames.clear();
            }
            mDisplayThread = null;
        }
        mLPreviewContainer.removeAllViews();
        mGLRenderer = null;
        if(mAsyncThread != null) {
            mAsyncThread.quitSafely();
            mAsyncThread = null;
        }
        mAsyncHandler = null;

        mCreateItemHandler = null;
        mGaussBlur.release();
    }

    @Override
    protected void onResume() {
        Logging.d(TAG, "onResume");
        super.onResume();

//        getPersimmions();
        // Open camera
        openCamera();
        // Rebind bundle items to body
        if(mGLSurfaceView != null) {
            mGLSurfaceView.queueEvent(
                    new Runnable() {
                @Override
                public void run() {
                    FaceUnity.bindBody();
                }
            });
            mGLSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        Logging.d(TAG, "onPause()");
        super.onPause();

        if(mAsyncHandler != null) {
            mAsyncHandler.removeMessages(
                AsyncHandler.HANDLE_CREATE_ITEM);
        }

//        if(mCreateItemHandler != null) {
//            mCreateItemHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
//        }

        // Unbind bundle items from body
        if(mGLSurfaceView != null) {
            mGLSurfaceView.queueEvent(
                new Runnable() {
                    @Override
                    public void run() {
                        mGLRenderer.notifyPause();
                        FaceUnity.unbindBody();
                        FaceUnity.fuOnDeviceLost();
                    }
            });
            mGLSurfaceView.onPause();
        }
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        Logging.d(TAG, "onDestroy");
        super.onDestroy();
        destroy();
    }

    public void showMLayoutMainButton(
            LinearLayout mLayoutMainButton, LinearLayout mLayoutChooseAvatar) {
        ObjectAnimator objectAnimator;
        if (!mMainButtonShowed) {
            ObjectAnimator.ofFloat(mLayoutChooseAvatar, "translationY",
                    -heightChooseAvatar, 0).setDuration(250).start();
            objectAnimator = ObjectAnimator.ofFloat(mLayoutMainButton,
                    "translationY", heightMainButton, 0);
            objectAnimator.setDuration(250);
            objectAnimator.setStartDelay(250);
            objectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMainButtonShowed = true;
                }
            });
            objectAnimator.start();
        }
    }

    public void showMLayoutChooseAvatar(
            LinearLayout mLayoutMainButton, LinearLayout mLayoutChooseAvatar) {
        heightMainButton =  mLayoutMainButton.getHeight() + Utils.dip2px(30);
        heightChooseAvatar = mLayoutChooseAvatar.getHeight();
        ObjectAnimator objectAnimator;
        if (mMainButtonShowed) {
            ObjectAnimator.ofFloat(mLayoutMainButton, "translationY",
                    0, heightMainButton).setDuration(250).start();
            Logging.d(TAG,"mLayoutMainButton.getHeight()="
                    + mLayoutMainButton.getHeight()
                    + ",=heightChooseAvatar="
                    + heightChooseAvatar);
            objectAnimator = ObjectAnimator.ofFloat(
                    mLayoutChooseAvatar, "translationY",
                    0, -heightChooseAvatar);

            objectAnimator.setDuration(250);
            objectAnimator.setStartDelay(250);
            objectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMainButtonShowed = false;
                }
            });
            objectAnimator.start();
        }
    }

    public void showMLayoutFilter(
            LinearLayout mLayoutMainButton, LinearLayout mLayoutFilter) {
        heightMainButton =  mLayoutMainButton.getHeight() + Utils.dip2px(30);
        heightFilter = mLayoutFilter.getHeight();
        ObjectAnimator objectAnimator;
        if (mMainButtonShowed) {
            ObjectAnimator.ofFloat(mLayoutMainButton, "translationY",
                    0, heightMainButton).setDuration(250).start();
            Logging.d(TAG,"mLayoutMainButton.getHeight()="
                    + mLayoutMainButton.getHeight()
                    + ",=heightFilter="
                    + heightFilter);
            objectAnimator = ObjectAnimator.ofFloat(
                    mLayoutFilter, "translationY",
                    0, -heightFilter);

            objectAnimator.setDuration(250);
            objectAnimator.setStartDelay(250);
            objectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMainButtonShowed = false;
                }
            });
            objectAnimator.start();
        }
    }

    public void showMLayoutMainButton_Filter(
            LinearLayout mLayoutMainButton, LinearLayout mLayoutFilter) {
        ObjectAnimator objectAnimator;
        if (!mMainButtonShowed) {
            ObjectAnimator.ofFloat(mLayoutFilter, "translationY",
                    -heightFilter, 0).setDuration(250).start();
            objectAnimator = ObjectAnimator.ofFloat(mLayoutMainButton,
                    "translationY", heightMainButton, 0);
            objectAnimator.setDuration(250);
            objectAnimator.setStartDelay(250);
            objectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMainButtonShowed = true;
                }
            });
            objectAnimator.start();
        }
    }




    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == Constants.SDK_PERMISSION_REQUEST1){
//            getPersimmions();  //从设置页面回来
        }
    }

}
