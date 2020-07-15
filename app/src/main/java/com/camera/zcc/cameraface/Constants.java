package com.camera.zcc.cameraface;

import android.os.Environment;

import java.io.File;

/**
 * Created by szjy on 2017/11/20.
 */

public class Constants {
    public static final String f2Path = Environment.getExternalStoragePublicDirectory("")
            + File.separator + "f2";
    public static final String cameraPath = Environment.getExternalStoragePublicDirectory("")
            + File.separator + "DCIM"+ File.separator + "Camera"+ File.separator;

    public static final String cachePath = f2Path + File.separator + "f2cache" + File.separator;
    public static final String topicPath = cachePath + "topic.mp4"; //话题视屏

    public static final String recordNewPath = f2Path + File.separator + "f2SmallVideo" + File.separator;

    public final static String CoorType_GCJ02 = "gcj02";
    public final static String CoorType_BD09LL = "bd09ll";
    public final static String CoorType_BD09MC = "bd09";
    /***
     *61 ： GPS定位结果，GPS定位成功。
     *62 ： 无法获取有效定位依据，定位失败，请检查运营商网络或者wifi网络是否正常开启，尝试重新请求定位。
     *63 ： 网络异常，没有成功向服务器发起请求，请确认当前测试手机网络是否通畅，尝试重新请求定位。
     *65 ： 定位缓存的结果。
     *66 ： 离线定位结果。通过requestOfflineLocaiton调用时对应的返回结果。
     *67 ： 离线定位失败。通过requestOfflineLocaiton调用时对应的返回结果。
     *68 ： 网络连接失败时，查找本地离线定位时对应的返回结果。
     *161： 网络定位结果，网络定位定位成功。
     *162： 请求串密文解析失败。
     *167： 服务端定位失败，请您检查是否禁用获取位置信息权限，尝试重新请求定位。
     *502： key参数错误，请按照说明文档重新申请KEY。
     *505： key不存在或者非法，请按照说明文档重新申请KEY。
     *601： key服务被开发者自己禁用，请按照说明文档重新申请KEY。
     *602： key mcode不匹配，您的ak配置过程中安全码设置有问题，请确保：sha1正确，
     *     “;”分号是英文状态；且包名是您当前运行应用的包名，请按照说明文档重新申请KEY。
     *501～700：key验证失败，请按照说明文档重新申请KEY。
     */

    public static float[] EARTH_WEIGHT = {0.1f, 0.2f, 0.4f, 0.6f, 0.8f}; // 推算计算权重_地球
    //public static float[] MOON_WEIGHT = {0.0167f,0.033f,0.067f,0.1f,0.133f};
    //public static float[] MARS_WEIGHT = {0.034f,0.068f,0.152f,0.228f,0.304f};

    public static final String ID = "id";

    public static final int IMAGE_REQUEST_CODE1 = 1;//首页打招呼

    public static final int IMAGE_REQUEST_CODE2 = 2;//发布

    public static final int IMAGE_REQUEST_CODE3 = 3;//换脸


    public static final String FROM = "from";

    public static final String POSITION = "position";

    public static final String FROM_SEND_MESSAGW = "send_video_message";

    public static final String SUBJECT_VIDEO_ID = "subject_video_id";

    public static final int SDK_PERMISSION_REQUEST = 127;

    public static final int REQUEST_CODE = 0x00000011;

    public static final int SDK_PERMISSION_REQUEST1 = 128;

    public static final int PREVIEW_WIDTH = 640;

    public static final int PREVIEW_HEIGHT = 480;
    // 图片验证码无效
    public static final String IMAGE_CODE_INVALID = "IMAGE_CODE_INVALID";
    // 获取验证码超过每日最大次数
    public static final String VERIFY_CODE_DAY_MAX_REPEAT = "VERIFY_CODE_DAY_MAX_REPEAT";
    // 60S内重复请求
    public static final String VERIFY_CODE_SEC_REPEAT = "VERIFY_CODE_SEC_REPEAT";
    // 验证码错误且次数超过5次
    public static final String EXCEED_VERIFY_CODE_INVALID_NUM =
            "EXCEED_VERIFY_CODE_INVALID_NUM";
    // 验证码错误
    public static final String VERIFY_CODE_INVALID = "VERIFY_CODE_INVALID";

    public final static String INTENT_BUTTONID_TAG = "ButtonId";
    /**
     * 同意加时
     */
    public final static int BUTTON_PREV_ID = 1;
    /**
     * 忽略加时
     */
    public final static int BUTTON_PALY_ID = 2;
    /**
     * 下一首 按钮点击 ID
     */
    public final static int BUTTON_NEXT_ID = 3;

    public static final String NOTICE_ID_KEY = "NOTICE_ID";
    public static final int TYPE_Normal = 1;
    public static final int TYPE_Progress = 2;
    public static final int TYPE_BigText = 3;
    public static final int TYPE_Inbox = 4;
    public static final int TYPE_BigPicture = 5;
    public static final int TYPE_Hangup = 6;
    public static final int TYPE_Media = 7;
    public static final int TYPE_Customer = 8;
    public static final int DEFAULT_TIME = 1000;
    public static final String UPDATE_HOME_DATA = "update_home_data";

    /**
     * 广播Action
     */
    public final static String ACTION_BUTTON = "add_time";//后台加时广播
    public static final String ACTION_LOGOUT = "logout"; //退出登录广播
    public static final String NECK_NAME = "nickname"; //修改昵称广播
    //修改个性标签广播
    public static final String UPDATE_CENTER_LABEL = "update_center_label";
    //修改avater广播
    public static final String UPDATE_CENTER_AVATER = "update_center_photo";
    //centerFragment  已经创建了  不会走onCreat了 这时候登陆成功  需要发广播  更新数据
    public static final String LOGIN_SUCCESS = "login_success";
    public static final String REFRESH_MESSAGE = "refresh_message"; //刷新消息
    public static final String NO_MESSAGE = "no_message"; //无消息 弹录制话题提示
    public static final String HAVE_MESSAGE = "have_message"; //有消息 弹录制话题提示消失

    public final static int MAX_LOGFILE_SIZE = 16 * 1024 * 1024;
    public final static String SERVER_ADDRESS = "47.95.36.137";
    public final static int SERVER_PORT = 443;
    public final static int CAMERA_W = 960;
    public final static int CAMERA_H = 540;
    public final static int CAMERA_FPS = 15;
    public final static int RECORD_W = 640;
    public final static int RECORD_H = 360;
    public final static int RECORD_FPS = 15;
    public final static int VOIP_W = 640;
    public final static int VOIP_H = 480;
    public final static int VOIP_FPS = 15;
    public final static int type_agreement = 0;
    public final static int type_rule = 1;
    public final static int type_gold = 2;
    public final static String VIDEO_BASE_URL = "http://resources.kinstalk.com/";
    public final static String INTREST_BUNDLE = "content";
    public final static String INTREST_BUNDLE_LIST = "List";
    public final static String USER_UID = "uid";

    public static final int[] FILTER_ITEM_RES_ARRAY = {
            R.drawable.nature, R.drawable.delta, R.drawable.electric, R.drawable.slowlived, R.drawable.tokyo, R.drawable.warm
    };
    public final static String[] FILTERS_NAME = {"origin", "delta", "electric", "slowlived", "tokyo", "warm"};
    public final static String VIDEO_PATH = "path";


    public static final int ILIVE_APPID = 1400106255;
    public static final int ILIVE_ACCOUNT_TYPE = 30059;
    public static final String USERNAME = "userName";
    public static final String FACEURL = "faceUrl";
    public static final String IDENTIFY = "identify";
    public static final String TAGS = "tags";
}
