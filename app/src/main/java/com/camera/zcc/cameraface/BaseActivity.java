package com.camera.zcc.cameraface;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;
import com.yanzhenjie.permission.SettingService;

import java.util.List;

/**
 * Created by szjy on 2018/3/22.
 */

public class BaseActivity extends AppCompatActivity {
    private final static String TAG = BaseActivity.class.getName();

    private boolean mMainButtonShowed = true;
    private String permissionInfo = "";
    private long exitTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        ButterKnife.bind(this);
        //开启广播去监听 网络 改变事件
//        NetWorkStateReceiver.addObserver(mNetChangeObserver);
//        BaseAppManager.getInstance().addActivity(this);
        //不休眠
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void finish() {
        super.finish();
    }

    /**
     * startActivity with bundle
     *
     * @param clazz
     * @param bundle
     */
    public void readyGo(Class<?> clazz, Bundle bundle) {
        Intent intent = new Intent(this, clazz);
        if (null != bundle) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    /**
     * startActivityForResult
     *
     * @param clazz
     * @param requestCode
     */
    public void readyGoForResult(Class<?> clazz, int requestCode) {
        Intent intent = new Intent(this, clazz);
        startActivityForResult(intent, requestCode);
    }

    /**
     * startActivityForResult with bundle
     *
     * @param clazz
     * @param requestCode
     * @param bundle
     */
    public void readyGoForResult(Class<?> clazz, int requestCode, Bundle bundle) {
        Intent intent = new Intent(this, clazz);
        if (null != bundle) {
            intent.putExtras(bundle);
        }
        startActivityForResult(intent, requestCode);
    }



    public void startActivity(Class clazz, boolean isFinish) {
        startActivity(new Intent(this, clazz));
        if (isFinish) {
            finish();
        }
    }

    public void startActivity(Intent intent, boolean isFinish) {
        startActivity(intent);
        if (isFinish) {
            finish();
        }
    }


    public void showToast(String msg) {
        ToastUtils.getToast(this, msg);
    }

    /**
     * 双击退出App
     */
    public boolean checkExit() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            exitTime = System.currentTimeMillis();
            showToast("再按一次退出");
            return true;
        } else {
            return false;
        }
    }

    /**
     *  3.沉浸式状态栏和导航栏：在 onCreate() 方法中：
     * @param isApplyNav
     */
    public void setStatueBar(boolean isApplyNav){
//            UltimateBar.newImmersionBuilder()
//                    .applyNav(false)    // 是否应用到导航栏
//                    .build(this)
//                    .apply();
    }





    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    /**
     * 请求权限
     * @param requestResult 处理权限
     */
    protected RequestResult mRequestResult;
    protected void requestPremission(RequestResult requestResult, String... permissions){
        this.mRequestResult = requestResult;
        AndPermission.with(this)
                .permission(permissions)
                //被拒绝时
                .rationale(mRationale)
                //允许通过
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        mRequestResult.successResult();
                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        //当总是被拒绝
                        mRequestResult.failuerResult();
                        if (AndPermission.hasAlwaysDeniedPermission(BaseActivity.this, permissions)) {
                            final SettingService settingService = AndPermission.permissionSetting(BaseActivity.this);
                            new AlertDialog.Builder(BaseActivity.this)
                                    .setTitle("温馨提示")
                                    .setMessage("您已拒绝手机权限，没有权限，无法流畅的体验所用功能，点击\"去设置\"按钮授权给我吧！")
                                    .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                            // 如果用户同意去设置：
                                            settingService.execute();
                                        }
                                    })
                                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                            // 如果用户不同意去设置：
                                            settingService.cancel();
                                        }
                                    }).show();
                        }
                    }
                })
                .start();
    }
    private Rationale mRationale = new Rationale() {
        @Override
        public void showRationale(Context context, List<String> permissions,
                                  final RequestExecutor executor) {
            // 这里使用一个Dialog询问用户是否继续授权。
            // 提示用户再次给予授权。自定义对话框。
            new AlertDialog.Builder(BaseActivity.this)
                    .setTitle("温馨提示")
                    .setMessage("您已拒绝获取权限，点击\"确认\"按钮授权给我吧！")
                    .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            // 如果用户继续：
                            executor.execute();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            // 如果用户中断：
                            executor.cancel();
                        }
                    }).show();
        }
    };



    /**
     * 网络断开的时候调用
     */
    protected void onNetworkDisConnected(){
        ToastUtils.getToast(this, "已断开网络");
    }
}
