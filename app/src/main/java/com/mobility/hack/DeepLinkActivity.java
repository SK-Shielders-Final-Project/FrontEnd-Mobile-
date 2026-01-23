package com.mobility.hack;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class DeepLinkActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // [3] 검증 없는 URL 리다이렉션 취약점
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            String targetUrl = data.getQueryParameter("url");
            if (targetUrl != null) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)));
            }
        }
        finish();
    }
}