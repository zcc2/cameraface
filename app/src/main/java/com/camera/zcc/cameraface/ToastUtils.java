package com.camera.zcc.cameraface;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by TR on 2017/2/28.
 */

public class ToastUtils {

    /**
     * 单例的设计模式
     * 1.私有的构造方法
     * 2.私有的静态变量
     * 3.公有的静态的访问方法
     */

    private ToastUtils() {
    }

    private static Toast toast = null;

    private static TextView tvMessage = null;

    public static void getToast(Context context, String msg){

        if(toast == null){
            synchronized (ToastUtils.class){
                if(toast == null){
                     toast = new Toast(context);
                    //设置Toast显示位置，居中，向 X、Y轴偏移量均为0
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    //设置显示时长
                    toast.setDuration(Toast.LENGTH_SHORT);
                    //获取自定义视图
                    View view = LayoutInflater.from(context).inflate(R.layout.toast_view, null);
                     tvMessage = (TextView) view.findViewById(R.id.tv_message_toast);
                    //设置视图
                    toast.setView(view);
                }
            }
        }

        //设置文本
        tvMessage.setText(msg);
        toast.show();
    }

}


