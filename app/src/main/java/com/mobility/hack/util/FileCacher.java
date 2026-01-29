package com.mobility.hack.util;

import android.content.Context;
import java.io.FileOutputStream;

public class FileCacher {
    // [2] 주행기록 .txt 평문 파일 저장 로직
    public void cacheRideData(Context context, String data) {
        try (FileOutputStream fos = context.openFileOutput("ride_history.txt", Context.MODE_PRIVATE)) {
            fos.write(data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}