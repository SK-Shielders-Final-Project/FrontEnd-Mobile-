package com.mobility.hack.ride;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class QrScanActivity extends AppCompatActivity {
    // [6] 큐싱 취약점: QR 코드에서 읽은 URL을 검증 없이 브라우저로 열기
    public void onQrCodeScanned(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}