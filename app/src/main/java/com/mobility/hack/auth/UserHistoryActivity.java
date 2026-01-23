package com.mobility.hack.auth;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class UserHistoryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // [10] IDOR (Insecure Direct Object Reference) 실습용 (타인 이용내역 열람)
    }
}