package com.fox.facedecter.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.jaeger.ninegridimgdemo.R;

public class MainActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        TextView centerTitle = (TextView) findViewById(R.id.title) ;
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("人脸识别系统");

        findViewById(R.id.btn_verify).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VerifyActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_detect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DetectActivity.class);
                startActivity(intent);
            }
        });
    }
}
