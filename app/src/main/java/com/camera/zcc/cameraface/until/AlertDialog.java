package com.camera.zcc.cameraface.until;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.camera.zcc.cameraface.R;


public class AlertDialog {

    private Context context;
    private String title;
    private String message;
    private String bottomMsg,leftButtonText,rightButtonText;
    private Display display;
    private TextView tvTitle;
    private TextView tvMessage;
    private Button btn_bottom;
    private RelativeLayout btnPositive;
    private Dialog dialog;
    private LinearLayout ll_content;
    private Button btnAlertDialogNegative,btnAlertDialogPositive;
    private boolean isShowTitle = false;
    //默认值
    private boolean isCanceledOnTouchOutside = true;
    private boolean isCancelable = true;
    private boolean isShowPositiveDouble = false;  //默认底部一个按钮
    private LinearLayout btnPositiveDouble;

    //一个底部按钮
    public AlertDialog(@NonNull Context context,
                       String title,
                       String message,
                       String bottomMsg,
                       boolean isShowTitle,
                       boolean isCanceledOnTouchOutside
                       , boolean isCancelable) {
        this.context = context;
        this.title = title;
        this.message = message;
        this.bottomMsg = bottomMsg;
        this.isShowTitle = isShowTitle;
        this.isCanceledOnTouchOutside = isCanceledOnTouchOutside;
        this.isCancelable = isCancelable;
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
    }

    //两个底部按钮
    public AlertDialog(@NonNull Context context,
                       String title,
                       String message,
                       String leftButtonText,
                       String rightButtonText,
                       boolean isShowTitle,
                       boolean isCanceledOnTouchOutside
                       , boolean isCancelable,
                       boolean isShowPositiveDouble) {
        this.context = context;
        this.title = title;
        this.message = message;
        this.isShowTitle = isShowTitle;
        this.leftButtonText = leftButtonText;
        this.rightButtonText = rightButtonText;
        this.isCanceledOnTouchOutside = isCanceledOnTouchOutside;
        this.isCancelable = isCancelable;
        this.isShowPositiveDouble = isShowPositiveDouble;
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
    }

    public AlertDialog builder() {
        // 获取Dialog布局
        View view = LayoutInflater.from(context).inflate(
                R.layout.view_alertdialog, null);

        ll_content = (LinearLayout)view. findViewById(R.id.ll_content);
        tvTitle = (TextView)view. findViewById(R.id.tvAlertDialogTitle);
        btn_bottom = (Button) view. findViewById(R.id.btn_sure);
        if(isShowTitle){
            tvTitle.setVisibility(View.VISIBLE);
            tvTitle.setText(title);
        }else{
            tvTitle.setVisibility(View.GONE);
        }
        tvMessage = (TextView)view. findViewById(R.id.tvAlertDialogMessage);
        tvMessage.setText(message);
        btnPositive = (RelativeLayout)view. findViewById(R.id.btnAlertDialogPositive_single);
        btnPositiveDouble = (LinearLayout)view.findViewById(R.id.btnAlertDialogPositive_double);
        btnAlertDialogNegative = (Button)view.findViewById(R.id.btnAlertDialogNegative);
        btnAlertDialogPositive = (Button)view.findViewById(R.id.btnAlertDialogPositive);
        if(isShowPositiveDouble){
            btnPositiveDouble.setVisibility(View.VISIBLE);
            btnPositive.setVisibility(View.GONE);
            btnAlertDialogNegative.setText(leftButtonText);
            btnAlertDialogPositive.setText(rightButtonText);
        }else{
            btnPositiveDouble.setVisibility(View.GONE);
            btnPositive.setVisibility(View.VISIBLE);
            btn_bottom.setText(bottomMsg);
        }
        // 定义Dialog布局和参数
        dialog = new Dialog(context, R.style.MyDialog);
        dialog.setContentView(view);
        dialog.setCanceledOnTouchOutside(isCanceledOnTouchOutside);
        dialog.setCancelable(isCancelable);
        // 调整dialog背景大小
        ll_content.setLayoutParams(new FrameLayout.LayoutParams((int) (display
                .getWidth() * 0.75), LinearLayout.LayoutParams.WRAP_CONTENT));
        return this;
    }

    /**
     * 一个底部Button
     * @param listener
     * @return
     */
    public AlertDialog setPositiveButton(boolean isDismiss,final View.OnClickListener listener) {
        btn_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btn_sure) {
                    if(isDismiss){
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                    if(listener != null)
                        listener.onClick(v);
                }
            }
        });
        return this;
    }

    /**
     * 两个底部Button
     * @param listener
     * @return
     */
    public AlertDialog setPositiveButtonDouble_left(final View.OnClickListener listener) {

        //左边按钮
        btnAlertDialogNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnAlertDialogNegative) {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    listener.onClick(v);
                }
            }
        });

        return this;
    }

        //右边按钮
        public AlertDialog setPositiveButtonDouble_right(final View.OnClickListener listener) {
        btnAlertDialogPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnAlertDialogPositive) {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    listener.onClick(v);
                }
            }
        });

        return this;
    }

    public void show() {
        if(!dialog.isShowing()){
            dialog.show();
        }
    }
}
