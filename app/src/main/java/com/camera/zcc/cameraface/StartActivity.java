package com.camera.zcc.cameraface;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class StartActivity extends BaseActivity {

    private Button bt;
    private String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        bt =(Button)findViewById(R.id.bt);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(StartActivity.this,PublishActivity.class);
                intent.putExtra(Constants.FROM, Constants.IMAGE_REQUEST_CODE2);
                startActivity(intent);
            }
        });
        requestPremission(new RequestResult() {
            @Override
            public void successResult() {

            }

            @Override
            public void failuerResult() {
                Toast.makeText(StartActivity.this,"权限拒绝无法使用",Toast.LENGTH_SHORT);
            }
        },permissions);
    }
}
