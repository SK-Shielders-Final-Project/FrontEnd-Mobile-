package com.mobility.hack.community;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;

public class InquiryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        // [13] 취약점: 자바스크립트 허용으로 인한 Stored XSS 가능성
        settings.setJavaScriptEnabled(true);
        setContentView(webView);
        
        // 실습용 예시 데이터 (실제로는 서버에서 가져옴)
        String xssPayload = "<html><body><h1>문의 내용</h1><script>alert('XSS Attack!');</script></body></html>";
        webView.loadData(xssPayload, "text/html", "UTF-8");
    }
}