package com.camera.zcc.cameraface;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;



import org.webrtc.Logging;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by gc on 2017/9/8.
 */

public class Utils {

    private static final String TAG = Utils.class.getName();

    public static enum NetType {
        WIFI,//wifi
        TYPE_MOBILE,//手机网络
        NONE,// 无网络
        TYPE_ETHERNET//有线
    }

    /**
     * 版本号比较
     *
     * @param version1
     * @param version2
     * @return
     */
    public static int compareVersion(String version1, String version2) {
        if (version1.equals(version2)) {
            return 0;
        }
        String[] version1Array = version1.split("\\.");
        String[] version2Array = version2.split("\\.");
        int index = 0;
        // 获取最小长度值
        int minLen = Math.min(version1Array.length, version2Array.length);
        int diff = 0;
        // 循环判断每位的大小
        Log.d("HomePageActivity", "verTag2=2222=" + version1Array[index]);
        while (index < minLen
                && (diff = Integer.parseInt(version1Array[index])
                - Integer.parseInt(version2Array[index])) == 0) {
            index++;
        }
        if (diff == 0) {
            // 如果位数不一致，比较多余位数
            for (int i = index; i < version1Array.length; i++) {
                if (Integer.parseInt(version1Array[i]) > 0) {
                    return 1;
                }
            }

            for (int i = index; i < version2Array.length; i++) {
                if (Integer.parseInt(version2Array[i]) > 0) {
                    return -1;
                }
            }
            return 0;
        } else {
            return diff > 0 ? 1 : -1;
        }
    }

    /**
     * 判断手机格式是否正确
     *
     * @param mobiles
     * @return
     */
    public static boolean isMobileNO(String mobiles) {
        //"[1]"代表第1位为数字1，"[358]"代表第二位可以为3、5、8中的一个，"\\d{9}"代表后面是可以是0～9的数字，有9位。
        String telRegex = "[1][34578]\\d{9}";
        if (TextUtils.isEmpty(mobiles)) return false;
        else return mobiles.matches(telRegex);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     *
     * @param dpValue dp
     * @return px
     */
//    public static int dip2px(float dpValue) {
//        final float scale = FApplication.getInstance().getResources().getDisplayMetrics().density;
//        return (int) (dpValue * scale + 0.5f);
//    }

    /**
     * 获取String资源
     *
     * @param resId 资源id
     * @return 资源内容
     */
    public static String getString(int resId) {
        if (resId > 0) {
            return MyApplication.getInstance().getResources().getString(resId);
        }
        return "";
    }

    /**
     * //根据图片路径把图片用base64加密成String
     *
     * @param filePath
     * @return
     */

    public static String bitmap_filePath_ToBase64(String filePath) {
        if (!isEmpty(filePath)) {
            Bitmap bm = getSmallBitmap(filePath);
//            Bitmap bm = getimage(filePath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            bm.compress(Bitmap.CompressFormat.JPEG, 40, baos);
            byte[] b = baos.toByteArray();
            return Base64.encodeToString(b, Base64.DEFAULT);
        } else {
            return "";
        }
    }

    /**
     * String判空
     *
     * @param s 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String s) {
        if (s != null) {
            if (!s.equalsIgnoreCase("") && !s.equalsIgnoreCase("null")) {
                return false;
            }
        }
        return true;
    }

    public static Bitmap getSmallBitmap(String filePath) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = calculateInSampleSize(options, 480, 800);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    /**
     * 将图片转换成Base64编码的字符串
     *
     * @param path
     * @return base64编码的字符串
     */
    public static String imageToBase64(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        InputStream is = null;
        byte[] data = null;
        String result = null;
        try {
            is = new FileInputStream(path);
            //创建一个字符流大小的数组。
            data = new byte[is.available()];
            //写入数组
            is.read(data);
            //用默认的编码格式进行编码
            result = Base64.encodeToString(data, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return result;
    }

    /**
     * base64编码字符集转化成图片文件。
     *
     * @param base64Str
     * @param path      文件存储路径
     * @return 是否成功
     */
    public static boolean base64ToFile(String base64Str, String path) {
        byte[] data = Base64.decode(base64Str, Base64.DEFAULT);
        for (int i = 0; i < data.length; i++) {
            if (data[i] < 0) {
                //调整异常数据
                data[i] += 256;
            }
        }
        OutputStream os = null;
        try {
            os = new FileOutputStream(path);
            os.write(data);
            os.flush();
            os.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 字符串进行Base64编码
     *
     * @param pwd
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static String decodeStr(String pwd) {
        String encodedString = Base64.encodeToString(pwd.getBytes(), Base64.DEFAULT);
        Log.e("Base64", "Base64---->" + encodedString);
        return encodedString;
    }

    /**
     * 字符串进行Base64解码
     *
     * @param pwd
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static String encodeStr(String pwd) {
        String decodedString = new String(Base64.decode(pwd, Base64.DEFAULT));
        Log.e("Base64", "Base64---->" + decodedString);
        return decodedString;
    }

    /**
     * bitmap转为base64
     *
     * @param bitmap
     * @return
     */
    public static String bitmapToBase64(Bitmap bitmap) {

        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * base64转为bitmap
     *
     * @param base64Data
     * @return
     */
    public static Bitmap base64ToBitmap(String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * uri转path
     *
     * @param context
     * @param uri
     * @return
     */
    public static String getRealFilePath(final Context context, final Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    /**
     * path转uri
     *
     * @param path
     * @return
     */
//    public static Uri getUri(String path) {
//        Uri uri = null;
//        if (path != null) {
//            path = Uri.decode(path);
//            Log.d(TAG, "path2 is " + path);
//            ContentResolver cr = MyApplication.getContext().getContentResolver();
//            StringBuffer buff = new StringBuffer();
//            buff.append("(")
//                    .append(MediaStore.Images.ImageColumns.DATA)
//                    .append("=")
//                    .append("'" + path + "'")
//                    .append(")");
//            Cursor cur = cr.query(
//                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                    new String[]{MediaStore.Images.ImageColumns._ID},
//                    buff.toString(), null, null);
//            int index = 0;
//            for (cur.moveToFirst(); !cur.isAfterLast(); cur
//                    .moveToNext()) {
//                index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID);
//// set _id value
//                index = cur.getInt(index);
//            }
//            if (index == 0) {
////do nothing
//            } else {
//                Uri uri_temp = Uri.parse("content://media/external/images/media/" + index);
//                Log.d(TAG, "uri_temp is " + uri_temp);
//                if (uri_temp != null) {
//                    uri = uri_temp;
//                }
//            }
//        }
//        return uri;
//    }

    /**
     * 两张图片合并一张
     *
     * @param firstBitmap
     * @param secondBitmap
     * @return
     */

    public static Bitmap mergeBitmap(Bitmap firstBitmap, Bitmap secondBitmap) {
        Bitmap bitmap = Bitmap.createBitmap(firstBitmap.getWidth(), firstBitmap.getHeight() + secondBitmap.getHeight(), firstBitmap.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(firstBitmap, new Matrix(), null);
        canvas.drawBitmap(secondBitmap, 0, firstBitmap.getHeight(), null);
        return bitmap;
    }

    /**
     * Android中如何将res里的图片转换成Bitmap
     *
     * @param activity
     * @param res
     * @return
     */
    public static Bitmap resToBitmap(Activity activity, int res) {
        return BitmapFactory.decodeResource(activity.getResources(), res);
    }

    ///////////////////////////////////////软键盘弹起收藏/////////////////////////////////////////

    /**
     * 显示软键盘
     *
     * @param view 控件
     */
    public static void showSoftInput(final View view) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ((InputMethodManager) MyApplication.getInstance().getApplicationContext().
                        getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(view, 0);//第一个参数中view必须是EditText，或者EditText的子类，如果是其他类型的View，如Button，TextView等，showSoftInput()方法不起作用。
            }
        }, 150);
    }

    /**
     * 软键盘显示时  掉此方法  就关闭了   反之....
     *
     * @param
     */
    public static void toggleSoftInput() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ((InputMethodManager) MyApplication.getInstance().getApplicationContext().
                        getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        }, 150);
    }

    /**
     * 收起软键盘
     */
    public static void collapseSoftInputMethod(Activity activity) {
        ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static NetType getNetWorkState(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            int type = networkInfo.getType();
            Logging.d(TAG, "network type: " + type);
            if (type == ConnectivityManager.TYPE_WIFI) {
                return NetType.WIFI;
            } else if (type == ConnectivityManager.TYPE_MOBILE) {
                return NetType.TYPE_MOBILE;
            } else if (type == ConnectivityManager.TYPE_ETHERNET) {
                return NetType.TYPE_ETHERNET;
            } else {
                return NetType.NONE;
            }
        } else {
            return NetType.NONE;
        }
    }
    ////////////////////////////////////////////////////////////////////系统工具类//////////////////////////////////////////////////////////////////////

    /**
     * 获取当前手机系统语言。
     *
     * @return 返回当前系统语言。例如：当前设置的是“中文-中国”，则返回“zh-CN”
     */
    public static String getSystemLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * 获取当前系统上的语言列表(Locale列表)
     *
     * @return 语言列表
     */
    public static Locale[] getSystemLanguageList() {
        return Locale.getAvailableLocales();
    }

    /**
     * 获取当前手机系统版本号
     *
     * @return 系统版本号
     */
    public static String getSystemVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * 获取手机型号
     *
     * @return 手机型号
     */
    public static String getSystemModel() {
        return android.os.Build.MODEL;
    }

    /**
     * 获取手机厂商
     *
     * @return 手机厂商
     */
    public static String getDeviceBrand() {


        return android.os.Build.BRAND;
    }

    private static String sID = null;
    private static final String INSTALLATION = "INSTALLATION";

    /**
     * 获取设备id
     *
     * @param context
     * @return
     */
    public synchronized static String getIMEI(Context context) {
        if (sID == null) {
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                if (!installation.exists())
                    writeInstallationFile(installation);
                sID = readInstallationFile(installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }

    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }

    /**
     * 手机型号
     */
    public static String getModel() {
        return android.os.Build.MODEL;
    }


    /**
     * 手机系统类型
     */
    public static int getPlatform() {
        return 1;
    }

    /**
     * APP版本
     */
    public static String getAppVersionName() {
        try {
            PackageInfo info = MyApplication.getInstance().getPackageManager().getPackageInfo(MyApplication.getInstance().getPackageName(), 0);
            String version = info.versionName;
            return version.replaceAll("-debug", "");
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    /**
     * APP版本号
     */
    public static int getAppVersionCode() {
        try {
            PackageInfo info = MyApplication.getInstance().getPackageManager().getPackageInfo(MyApplication.getInstance().getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    /**
     * get App versionCode
     *
     * @param context
     * @return
     */
    public String getVersionCode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        String versionCode = "";
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionCode = packageInfo.versionCode + "";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * get App versionName
     *
     * @param context
     * @return
     */
    public static String getVersionName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        String versionName = "";
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }


    /**
     * 是否存在 sdcard
     *
     * @return
     */
    public static boolean hasSdcard() {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    ////////////////////////////////////////////////////////////////////软键盘////////////////////////////////////////////////////////////////////////
    /**
     * 获取软键盘的高度 * *
     *
     * @param rootView *
     * @param listener
     */
    private static boolean isFirst = true;

//    public static void getSoftKeyboardHeight(final View rootView, final OnGetSoftHeightListener listener) {
//        final ViewTreeObserver.OnGlobalLayoutListener layoutListener
//                = new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                if (isFirst) {
//                    final Rect rect = new Rect();
//                    rootView.getWindowVisibleDisplayFrame(rect);
//                    final int screenHeight = rootView.getRootView().getHeight();
//                    final int heightDifference = screenHeight - rect.bottom;
//                    //设置一个阀值来判断软键盘是否弹出
//                    boolean visible = heightDifference > screenHeight / 3;
//                    if (visible) {
//                        isFirst = false;
//                        if (listener != null) {
//                            listener.onShowed(heightDifference);
//                        }
//                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                    }
//                }
//            }
//        };
//        rootView.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
//    }

//    /**
//     * 判断软键盘是否弹出
//     * * @param rootView
//     *
//     * @param listener 备注：在不用的时候记得移除OnGlobalLayoutListener
//     */
//    public static ViewTreeObserver.OnGlobalLayoutListener doMonitorSoftKeyWord(final View rootView, final OnSoftKeyWordShowListener listener) {
//        final ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                final Rect rect = new Rect();
//                rootView.getWindowVisibleDisplayFrame(rect);
//                final int screenHeight = rootView.getRootView().getHeight();
//                Log.e("TAG", rect.bottom + "#" + screenHeight);
//                final int heightDifference = screenHeight - rect.bottom;
//                boolean visible = heightDifference > screenHeight / 3;
//                if (listener != null)
//                    listener.hasShow(visible);
//            }
//        };
//        rootView.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
//        return layoutListener;
//    }

    public static void copyFilesFromAssets(Context context, String assetsName, String newPath, String fileName) {
        File file = new File(newPath);
        if (!file.exists()) {
            Logging.d(TAG, newPath + " is not exist");
            file.mkdir();
        }
        file = new File(newPath + fileName);
        if (!file.exists()) {
            Logging.d(TAG, newPath + fileName
                    + " is not exist");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            InputStream is = context.getAssets().open(assetsName);
            FileOutputStream os = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int byteCount = 0;
            while ((byteCount = is.read(buffer)) != -1) {
                os.write(buffer, 0, byteCount);
            }
            os.flush();
            is.close();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(float dpValue) {
        final float scale = 160;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int sp2px(Context context, float spVal)

    {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spVal, context.getResources().getDisplayMetrics());
    }

    public static Bitmap getVideoFirstFrame(String filePath) {
        //实例化MediaMetadataRetriever对象
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        File file = new File(filePath);
        if (file.exists()) {
            mmr.setDataSource(file.getAbsolutePath());//设置数据源为该文件对象指定的绝对路径
            Bitmap bitmap = mmr.getFrameAtTime();//获得视频第一帧的Bitmap对象
            return bitmap;
        } else {
            return null;
        }
    }


    // 1、将Bitmap对象读到字节数组中
    public static byte[] bitmapToByte(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    //    时间显示
//    public static String showTime(long time) {
//
//        long currentTimeMillis = System.currentTimeMillis();
//
//        long totalMilliSeconds = currentTimeMillis - time;
//
//        long totalSeconds = totalMilliSeconds / 1000; //总共多少秒
//
////        long currentSecond = totalSeconds % 60; //求出现在的秒
//
//        long totalMinutes = totalSeconds / 60;//总共多少分钟
//
////        long currentMinute = totalMinutes % 60;//求出现在的分
//
//        long totalHour = totalMinutes / 60;//总共多少小时
//
////        long currentHour = totalHour % 24;//求出现在的小时
//
//        long totalDay = totalHour / 24;//总共多少天
//
//        long yesterday_y = currentTimeMillis / (1000 * 3600 * 24) * (1000 * 3600 * 24) - TimeZone.getDefault().getRawOffset() - 24 * 60 * 60 * 1000;//昨天0点0分0秒的毫秒数  也就是前天24点0分0秒的毫秒数
//
//        long yesterday = currentTimeMillis / (1000 * 3600 * 24) * (1000 * 3600 * 24) - TimeZone.getDefault().getRawOffset();//今天0点0分0秒的毫秒数  也就是昨天24点0分0秒的毫秒数
//
//        long today = currentTimeMillis / (1000 * 3600 * 24) * (1000 * 3600 * 24) - TimeZone.getDefault().getRawOffset() + 24 * 60 * 60 * 1000;//今天24点0分0秒的毫秒数
//
//        Date yearFirstDay = getYearFirst(getCurrentYear());//获取某年的第一天
//
//        long yearFirstDayTime = yearFirstDay.getTime();//获取某年的最后一天的时间戳
//
//        if (totalSeconds < 60) {
//            return "刚刚";
//        } else if (totalMinutes > 1 && totalMinutes < 60) {
//            return totalMinutes + "分钟前";
//        } else if (time < today && time > yesterday) {
//            return CalculationUtils.getStringDateBySecond(time, CalculationUtils.COLON_HOUR_MIN);
//        } else if (time < yesterday && time > yesterday_y) {
//            String stringDateBySecond = CalculationUtils.getStringDateBySecond(time, CalculationUtils.COLON_HOUR_MIN);
//            return "昨天 " + stringDateBySecond;
//        } else if (time < yesterday_y && time > yearFirstDayTime) {
//            return CalculationUtils.getStringDateBySecond(time, CalculationUtils.CHARACTER_YEAR_MONTH_DAY_HOUR_MIN);
//        } else if (time < yearFirstDayTime) {
//            //上一年
//            return CalculationUtils.getStringDateBySecond(time, CalculationUtils.MINUS_YEAR_MONTH_DAY_HOUR_MIN);
//        } else {
//            return "未知时间";
//        }
//
//    }


    public static Date getYesterday() {
        Calendar yesterday = Calendar.getInstance();
        yesterday.set(Calendar.DAY_OF_MONTH, yesterday.get(Calendar.DAY_OF_MONTH) - 1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        yesterday.set(Calendar.MILLISECOND, 0);
        return yesterday.getTime();
    }

    //获取时间的零点
    public static Date getDayZero(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date start = calendar.getTime();
        return start;
    }

    //获取时间的零点
    public static Date getTomorrowZero(Date date) {
        Calendar tomorrow = Calendar.getInstance();
        if (date != null) {
            tomorrow.setTime(date);
        }
        tomorrow.set(Calendar.DAY_OF_MONTH, tomorrow.get(Calendar.DAY_OF_MONTH) + 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        return tomorrow.getTime();
    }

    public static Date getFormatDateToDayStart(Date date) {
        Calendar today = Calendar.getInstance();
        today.setTime(date);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today.getTime();
    }

    public static int daysOfTwo(Date fDate, Date oDate) {

        Calendar aCalendar = Calendar.getInstance();

        aCalendar.setTime(fDate);

        int day1 = aCalendar.get(Calendar.DAY_OF_YEAR);

        aCalendar.setTime(oDate);

        int day2 = aCalendar.get(Calendar.DAY_OF_YEAR);

        return day2 - day1;

    }

    /**
     * 获取指定日期间内所有日期
     *
     * @param startTime
     * @param num
     * @return
     */
    public static List<Date> getTimesByDayNum(Date startTime, int num) {
        List<Date> times = new ArrayList();
        Calendar day = Calendar.getInstance();
        day.setTime(startTime);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        times.add(day.getTime());
        for (int i = 1; i <= num - 1; i++) {
            day.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH) - 1);
            times.add(day.getTime());
        }
        return times;
    }

    /**
     * 获取指定日期加指定小时后的日期
     *
     * @param date
     * @param num
     * @return
     */
    public static Date getDateAndAddHours(Date date, int num) {
        Calendar day = Calendar.getInstance();
        day.setTime(date);
        day.set(Calendar.HOUR_OF_DAY, day.get(Calendar.HOUR_OF_DAY) + num);
        return day.getTime();
    }

    /**
     * 获取指定日期之后几天的日期
     *
     * @param startTime
     * @param num
     * @return
     */
    public static Date getDayByAfter(Date startTime, int num) {
        Calendar day = Calendar.getInstance();
        day.setTime(startTime);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        day.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH) + num);
        return day.getTime();
    }

    /**
     * 获取指定日期之前几天的日期
     *
     * @param startTime
     * @param num
     * @return
     */
    public static Date getDayByBefore(Date startTime, int num) {
        Calendar day = Calendar.getInstance();
        day.setTime(startTime);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        day.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH) - num);
        return day.getTime();
    }

    public static Date initDateToZero(Date date) {
        Calendar day = Calendar.getInstance();
        day.setTime(date);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day.getTime();
    }

    /**
     * 获取某年第一天日期
     *
     * @param year 年份
     * @return Date
     */
    public static Date getYearFirst(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        Date currYearFirst = calendar.getTime();
        return currYearFirst;
    }

    /**
     * 获取某年最后一天日期
     *
     * @param year 年份
     * @return Date
     */
    public static Date getYearLast(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.roll(Calendar.DAY_OF_YEAR, -1);
        Date currYearLast = calendar.getTime();
        return currYearLast;
    }

    /**
     * 得到UTC时间，类型为字符串，格式为"yyyy-MM-dd HH:mm"<br />
     * 如果获取失败，返回null
     *
     * @return
     */
    public static Date getUTCTime() {
        // 1、取得本地时间：
        Calendar cal = Calendar.getInstance();
        // 2、取得时间偏移量：
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
        // 3、取得夏令时差：
        int dstOffset = cal.get(Calendar.DST_OFFSET);
        // 4、从本地时间里扣除这些差量，即可以取得UTC时间：
        cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        return cal.getTime();
    }


    public static Date transformDateByTimeAndHour(Date date, int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Integer getSecondByDays(Date date1, Date date2) {
        return (int) (date1.getTime() - date2.getTime()) / 1000;
    }

    /**
     * 获取当前系统年份
     *
     * @return
     */

    public static int getCurrentYear() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        Date date = new Date();
        return Integer.parseInt(sdf.format(date));
    }


    /**
     * 获取图片名称获取图片的资源id的方法
     *
     * @param
     * @return
     */
//    public static int getResourceIdByName(String imageName) {
//        Context ctx = MyApplication.getContext();
//        int resId = ctx.getResources().getIdentifier(imageName, "drawable", ctx.getPackageName());
//        return resId;
//    }

//    public static Bitmap getRes(String name) {
//        ApplicationInfo appInfo = MyApplication.getContext().getApplicationInfo();
//        int resID = MyApplication.getContext().getResources().getIdentifier(name, "drawable", appInfo.packageName);
//        if (resID == 0) {
//            return BitmapFactory.decodeResource(MyApplication.getContext().getResources(), R.drawable.icon_change_camera);
//        }
//        return BitmapFactory.decodeResource(MyApplication.getContext().getResources(), resID);
//    }

    public static String formatString(double data) {
        DecimalFormat df = new DecimalFormat("#,###.00");
        return df.format(data);
    }
}


