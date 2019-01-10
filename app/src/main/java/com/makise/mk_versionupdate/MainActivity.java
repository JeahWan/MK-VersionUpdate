package com.makise.mk_versionupdate;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.versionUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //测试版本升级库
                UpdateVersionUtil.beginToDownload(MainActivity.this, getResources().getString(R.string.app_name),
                        R.mipmap.ic_launcher, "apk的下载地址");
            }
        });
    }
}
