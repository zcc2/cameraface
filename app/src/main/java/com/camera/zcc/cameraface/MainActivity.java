package com.camera.zcc.cameraface;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.camera.zcc.cameraface.recorder.AVRecorder;
import com.camera.zcc.cameraface.until.OnClickUtils;
import com.coremedia.iso.boxes.Container;
import com.faceunity.fup2a.FaceUnity;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class MainActivity extends BaseAvaterActivity implements View.OnClickListener {

    MainActivity mActivity;
    private final static String TAG = MainActivity.class.getName();
    private final static String WATER_MARK_NAME = "watermark.png";

    private LinearLayout mPublishContent;
    private LinearLayout mBtnClose;
    private FrameLayout fl_record;

    private CameraProgressBar mProgressbar;

    /**  change avatar **/
    private ImageView iv_change_avatar;
    private ImageView iv_delete;
    private ImageView iv_filter;
    private ImageView iv_preview;

    private boolean isRecordFinish = false;


    /** btn start record video after 3 seconds` animation */
    private AnimationImageView iv_anim;
    /**
     * 最小录制时间
     */
    private static final int MIN_RECORD_TIME = 1 * 1000;
    /**
     * 发布最长录制时间
     */
    private static final int PUSH_MAX_RECORD_TIME = 30 * 1000; /**
     * 发布消息最长录制时间
     */
    private static final int PUSH_MAX_RECORD_TIME_MESSAGE = 10 * 1000;

    /**
     * 打招呼最长录制时间
     */
    private static final int MESSAGE_MAX_RECORD_TIME = 5 * 1000;
    /**
     * 刷新进度的间隔时间
     */
    private static final int PLUSH_PROGRESS = 100;

    private boolean isFirst = true;

    private Subscription progressSubscription = null;

    public int isFromType;

    public int max;

    public String videoPath;

    public ImageView iv_change_camera;

    private TextView tv_time_lapse;
    private TextView no_av_tip;
    /**  record button */
    public ImageView iv_record;
    /** record isrecording */
    public boolean isrecording = false;

    /** record file paths **/
    private List<String> paths = new ArrayList<>();

    private  boolean mIsSendMessage = false;

    private boolean hasFocus = false;

    private SeekBar sb_top_progress;
    private TextView tv_record_time;
    private TextView tv_end;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity=this;

    }
    protected View GetChild() {
        View view = View.inflate(this,R.layout.activity_publish,null);
        mPublishContent = (LinearLayout) view.findViewById(R.id.publish_activity_content);
        mBtnClose = (LinearLayout)view.findViewById(R.id.btn_close);
//        mBtnClose.setPadding(0, ScreenUtils.getStatusBarHeight(
//                FApplication.getContext()),0,0);
        mBtnClose.setOnClickListener(this);
        iv_change_avatar = view.findViewById(R.id.iv_change_avatar);
        iv_change_avatar.setOnClickListener(this);
        mProgressbar = (CameraProgressBar)view.findViewById(R.id.mProgressbar);
        mProgressbar.setLongScale(false);
        sb_top_progress = (SeekBar)view.findViewById(R.id.sb_top_progress);
//        mTvShowText = (TextView)view.findViewById(R.id.tv_show_text);
        iv_change_camera = view.findViewById(R.id.iv_change_camera);
        iv_change_camera.setOnClickListener(this);
        iv_delete = view.findViewById(R.id.iv_delete);
        iv_delete.setOnClickListener(this);
        iv_filter = view.findViewById(R.id.iv_filter);
        iv_filter.setOnClickListener(this);
        iv_preview = view.findViewById(R.id.iv_preview);
        iv_preview.setOnClickListener(this);

        iv_record = view.findViewById(R.id.iv_record);
        iv_record.setOnClickListener(this);

        iv_delete.setVisibility(View.GONE);
        iv_preview.setVisibility(View.GONE);


        iv_anim = (AnimationImageView) view.findViewById(R.id.iv_anim);

        tv_time_lapse = (TextView) view.findViewById(R.id.tv_time_lapse);
        tv_time_lapse.setOnClickListener(this);

        tv_record_time = (TextView) view.findViewById(R.id.tv_start);

        fl_record = (FrameLayout) view.findViewById(R.id.fl_record);
        no_av_tip = (TextView) view.findViewById(R.id.tv_show_tips);
        tv_end = (TextView) view.findViewById(R.id.tv_end);
        fl_record.setOnClickListener(this);
        initView();
        return view;
    }

    private void initView() {
        record = AVRecorder.getInstance(this);
        Utils.copyFilesFromAssets(this,
                WATER_MARK_NAME, Constants.f2Path, WATER_MARK_NAME);
        mBtnClose.setOnClickListener(this);
        mPublishContent.setOnClickListener(this);
        mLPreviewContainer.setOnClickListener(this);
        sb_top_progress.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("ACETEST", "监听");
                return true;
            }
        });
        Intent intent = getIntent();
        isFromType = intent.getIntExtra(Constants.FROM, -1);
        mIsSendMessage = intent.getBooleanExtra(
                Constants.FROM_SEND_MESSAGW, false);
        if(isFromType == Constants.IMAGE_REQUEST_CODE3) {
            mPublishContent.setVisibility(View.GONE);
            ObjectAnimator.ofFloat(mAvatarSelectLayout, "translationY",
                    0, -Utils.dip2px(80)).setDuration(600).start();
        }else if(isFromType == Constants.IMAGE_REQUEST_CODE2){
            if(!mIsSendMessage) {
                max = PUSH_MAX_RECORD_TIME / PLUSH_PROGRESS;
                mProgressbar.setMaxProgress(max);
                sb_top_progress.setMax(max);
                videoPath = Constants.topicPath;
                tv_end.setText("30s");
//              setTextWithAnimation(mTvShowText, "长按录制10s话题小视频");
            } else {
                tv_end.setText("30s");
                max = PUSH_MAX_RECORD_TIME/ PLUSH_PROGRESS;
                mProgressbar.setMaxProgress(max);
                sb_top_progress.setMax(max);
                videoPath = Constants.topicPath;
//                setTextWithAnimation(mTvShowText, "长按录制5s打招呼小视频");
            }
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_close:
                if(isFromType != Constants.IMAGE_REQUEST_CODE3 && !mMainButtonShowed) {
                    if(mFilterShowed) {
                        showMLayoutMainButton_Filter(mPublishContent, mFilterSelectLayout);
                        mFilterShowed = false;
                    }else {
                        showMLayoutMainButton(mPublishContent, mAvatarSelectLayout);
                    }
                    return;
                } else if(mProgressbar.getProgress() <= 0) {
                    finish();
                }else {
//                    new AlertDialog(this, "", getResources().getString(R.string.delete_video_info),
//                            getResources().getString(R.string.delete_dialog_left), "确定",
//                            false, false,
//                            false, true)
//                            .builder()
//                            .setPositiveButtonDouble_left(new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    finish();
//                                }
//                            }).setPositiveButtonDouble_right(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//
//                        }
//                    }).show();

                }

                //关闭窗体动画显示
                overridePendingTransition(0, R.anim.slide_out_to_top);
                break;
            case R.id.publish_activity_content:
//                startAlphaAnimation(mTvShowText);
                break;
            case R.id.iv_change_avatar:  // 换脸
//                MobclickAgent.onEvent(this,"publish_change_face");
//                startAlphaAnimation(mTvShowText);
                showMLayoutChooseAvatar(mPublishContent, mAvatarSelectLayout);
                break;
            case R.id.avatar_preview_container:
//                startAlphaAnimation(mTvShowText);
                if(isFromType != Constants.IMAGE_REQUEST_CODE3 && !mMainButtonShowed) {
                    if(OnClickUtils.isFastClick()){
                        if(mFilterShowed) {
                            showMLayoutMainButton_Filter(mPublishContent, mFilterSelectLayout);
                            mFilterShowed = false;
                        }else {
                            showMLayoutMainButton(mPublishContent, mAvatarSelectLayout);
                        }

                    }
                }
                break;
            case R.id.iv_change_camera:   //    切换摄像头
                changeCamera();
                break;
            case R.id.iv_record:        //     录制视频

                if(FaceUnity.fuIsTracking() <= 0) {
                    ToastUtils.getToast(MainActivity.this, getResources().getString(R.string.mianju));
                    return;
                }

                if(isrecording) {
                    stopRecorder();
//                    mergeVideo();
                } else if(!isrecording) {

                    progressSubscription = Observable.interval(PLUSH_PROGRESS,
                            TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread()
                    ).take(max).subscribe(new Subscriber<Long>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(Long aLong) {
                            mProgressbar.setProgress(mProgressbar.getProgress() + 1);
                            sb_top_progress.setProgress(sb_top_progress.getProgress() + 1);

                            if((mProgressbar.getProgress() % 10) == 0) {
                                tv_record_time.setText(mProgressbar.getProgress() / 10 + "s");
                            }


                            if(mProgressbar.getProgress() >= max) {
                                stopRecorder();
                                isRecordFinish = true;
//                                mProgressbar.reset();
//                                sb_top_progress.setProgress(0);
//                                tv_record_time.setText(0 + "s");

                                File file = new File(Constants.topicPath);
                                if (file.exists()) {
//                                    Intent intent1 = new Intent(mActivity, SmallVideoActivity.class);
//                                    intent1.putExtra(Constants.FROM, isFromType);
//                                    intent1.putExtra(Constants.FROM_SEND_MESSAGW, mIsSendMessage);
//                                    startActivity(intent1);
                                } else {
                                    ToastUtils.getToast(mActivity, "视频录制失败");
                                }

                            }
                        }
                    });


                    Log.e("gyz", "recording");
                    iv_delete.setVisibility(View.GONE);
                    iv_preview.setVisibility(View.GONE);
                    tv_time_lapse.setVisibility(View.GONE);
                    isrecording = true;
                    iv_record.setImageDrawable(getResources().getDrawable(R.drawable.icon_recording_new));
                    String path = getRecorderPath();
                    Log.e("gyz", "record file path=======>" + path);
                    paths.add(path);
                    File file = new File(path);
                    if(file.exists()) {
                        Log.d(TAG, "remove old record file");
                        file.delete();
                    }

                    if (record == null) {
                        record = AVRecorder.getInstance(this);
                    }


                    String image = Constants.f2Path + WATER_MARK_NAME;
                    Log.e(TAG, "start record: " + image);

                    boolean ret = record.startRecord(path, image,
                            Constants.RECORD_W, Constants.RECORD_H,
                            mCamera.getRotation());
                    if (!ret) {
                        ToastUtils.getToast(mActivity, "录制失败");
                    }
                }
                break;
            case R.id.iv_preview:       //  预览
                File file = new File(Constants.topicPath);
//                if (file.exists()) {
//                    Intent intent1 = new Intent(mActivity, SmallVideoActivity.class);
//                    intent1.putExtra(Constants.FROM, isFromType);
//                    if (mIsSendMessage)
//                        intent1.putExtra(Constants.FROM_SEND_MESSAGW, true);
//                        int subjectid = getIntent().getIntExtra(Constants.SUBJECT_VIDEO_ID, 0);
//                        int position = getIntent().getIntExtra(Constants.POSITION, -1);
//                        intent1.putExtra(Constants.SUBJECT_VIDEO_ID, subjectid);
//                        intent1.putExtra(Constants.POSITION, position);
//                    } else {
//                    intent1.putExtra(Constants.FROM_SEND_MESSAGW, false);
//                    }
//                    startActivity(intent1);
                break;

            case R.id.iv_filter:
//                mEffectRecyclerAdapter.setOwnerRecyclerViewType(EffectAndFilterSelectAdapter.RECYCLEVIEW_TYPE_FILTER);
//                filterLevelSeekbar.setVisibility(View.VISIBLE);
                showMLayoutFilter(mPublishContent, mFilterSelectLayout);
                mFilterShowed = true;
                break;
            case R.id.tv_time_lapse:
                iv_anim.setVisibility(View.VISIBLE);
                iv_anim.loadAnimation(R.drawable.start_video_later,
                        new AnimationImageView.OnFrameAnimationListener() {

                            @Override
                            public void onStart() {
                                // 动画刚播放时
                            }
                            @Override
                            public void onEnd() {
                                // 动画结束播放时
                                iv_anim.setVisibility(View.GONE);
                                if(FaceUnity.fuIsTracking() <= 0) {
                                    ToastUtils.getToast(MainActivity.this, getResources().getString(R.string.mianju));
                                    return;
                                }
                                Log.e("gyz", "recording");
                                progressSubscription = Observable.interval(PLUSH_PROGRESS,
                                        TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread()
                                ).take(max).subscribe(new Subscriber<Long>() {
                                    @Override
                                    public void onCompleted() {
                                    }

                                    @Override
                                    public void onError(Throwable e) {

                                    }

                                    @Override
                                    public void onNext(Long aLong) {
                                        mProgressbar.setProgress(mProgressbar.getProgress() + 1);
                                        sb_top_progress.setProgress(sb_top_progress.getProgress() + 1);

                                        if((mProgressbar.getProgress() % 10) == 0) {
                                            tv_record_time.setText(mProgressbar.getProgress() / 10 + "s");
                                        }
                                        if(mProgressbar.getProgress() >= max) {
                                            stopRecorder();
                                            isRecordFinish = true;
                                            File file = new File(Constants.topicPath);
                                            if (file.exists()) {
//                                                Intent intent1 = new Intent(mActivity, SmallVideoActivity.class);
//                                                intent1.putExtra(Constants.FROM, isFromType);
//                                                intent1.putExtra(Constants.FROM_SEND_MESSAGW, false);
//                                                startActivity(intent1);
                                            } else {
                                                ToastUtils.getToast(MainActivity.this, "视频录制失败");
                                            }

                                        }
                                    }
                                });


                                Log.e("gyz", "recording");
                                iv_delete.setVisibility(View.GONE);
                                iv_preview.setVisibility(View.GONE);
                                isrecording = true;
                                iv_record.setImageDrawable(getResources().getDrawable(R.drawable.icon_recording_new));
                                String path = getRecorderPath();
                                Log.e("gyz", "record file path=======>" + path);
                                paths.add(path);
                                File file = new File(path);
                                if(file.exists()) {
                                    Log.d(TAG, "remove old record file");
                                    file.delete();
                                }

                                if (record == null) {
                                    record = AVRecorder.getInstance(MainActivity.this);
                                }

                                String image = Constants.f2Path + WATER_MARK_NAME;
                                Log.e(TAG, "start record: " + image);

                                boolean ret = record.startRecord(path, image,
                                        Constants.RECORD_W, Constants.RECORD_H,
                                        mCamera.getRotation());
                                if (!ret) {
                                    ToastUtils.getToast(mActivity, "录制失败");
                                }
                            }
                        });

                break;
            case R.id.iv_delete:
//                new AlertDialog(this, "", getResources().getString(R.string.delete_video_info),
//                        getResources().getString(R.string.delete_dialog_left), getResources().getString(R.string.delete_dialog_right),
//                        false, false,
//                        false, true)
//                        .builder()
//                        .setPositiveButtonDouble_left(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                paths.clear();
//                                mProgressbar.reset();
//                                sb_top_progress.setProgress(0);
//                                tv_record_time.setText(0 + "s");
//                                iv_delete.setVisibility(View.GONE);
//                                iv_preview.setVisibility(View.GONE);
//                                FileUtils.deleteDirectory(Constants.cachePath);
//                            }
//                        }).setPositiveButtonDouble_right(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//
//                    }
//                }).show();
                break;
        }
        //super.onClick(v);
    }
    // 录制文件存放目录
    private String getRecorderPath() {
//        File file = Environment.getExternalStorageDirectory();
        File file = new File(Constants.cachePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        String path = Constants.cachePath + System.currentTimeMillis() + ".mp4";
        Log.e("MainActivity", "path=" + path);
        return path;
    }

    /**
     * 合成视频
     * paths 所有视频路径
     */
    private void mergeVideo() {

        Log.e("gyz", "mergeVideo");
        long begin = System.currentTimeMillis();

        List<Movie> movies = new ArrayList<>();
        try {
            for (int i = 0; i < paths.size(); i++) {
                Movie movie = MovieCreator.build(paths.get(i));
                movies.add(movie);
            }
            List<Track> videoTracks = new ArrayList<>();
            List<Track> audioTracks = new ArrayList<>();
            for (Movie movie : movies) {
                for (Track track : movie.getTracks()) {
                    if ("vide".equals(track.getHandler())) {
                        videoTracks.add(track);
                    }
                    if ("soun".equals(track.getHandler())) {
                        audioTracks.add(track);
                    }
                }
            }
            Movie result = new Movie();
            if (videoTracks.size() > 0) {
                result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
            }
            if (audioTracks.size() > 0) {
                result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }

            Container container = new DefaultMp4Builder().build(result);

            File outputFile = new File(Constants.topicPath);
            FileChannel fc = new FileOutputStream(outputFile).getChannel();
            container.writeContainer(fc);
            fc.close();
        }  catch (IOException e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        Log.e("gyz", "mergeVideo end");
    }
    /**
     * 停止拍摄
     */
    protected void stopRecorder() {
        if(!isrecording) {
            return;
        }

//        if (progressSubscription != null) {
//            progressSubscription.unsubscribe();
//        }

        Log.e("gyz", "stop recording");
        isrecording = false;
        iv_record.setImageDrawable(getResources().getDrawable(R.drawable.icon_recording));

        /**
         * 录制视频的时间,毫秒
         */
        int recordSecond = mProgressbar.getProgress() * PLUSH_PROGRESS;
        if(recordSecond >= 3000) {
            iv_delete.setVisibility(View.VISIBLE);
            iv_preview.setVisibility(View.VISIBLE);
        } else {
            iv_delete.setVisibility(View.GONE);
            iv_preview.setVisibility(View.GONE);
        }
        Log.d(TAG,"recordSecond=="+recordSecond);
        // 停止录制
        if(record != null) {
            Log.d(TAG, "stop record");
            record.stopRecord();
        }
        mergeVideo();
    }

    @Override
    protected void showNoFacetip() {

    }

    @Override
    protected void hideNoAvaTip() {

    }


}
