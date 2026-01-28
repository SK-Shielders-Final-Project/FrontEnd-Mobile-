package com.mobility.hack.ride;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.mobility.hack.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// Map에서 자전거 목록을 불러올 때 사용
public class FileCacher {

    private String bikeApiUrl;
    private Context context; // SharedPreferences 접근을 위해 멤버 변수로 저장

    // 1. 생성자를 통해 Context를 전달받습니다.
    public FileCacher(android.content.Context context) {
        // 2. 전달받은 context를 사용하여 strings.xml의 값을 가져옵니다.
        String baseUrl = context.getString(R.string.server_url);
        this.bikeApiUrl = baseUrl + "/api/bikes";
    }

    // 데이터 로드 결과를 돌려주기 위한 콜백 인터페이스
    public interface BikeCallback {
        void onSuccess(List<BikeDTO> bikeList);
        void onFailure(String errorMessage);
    }

    public void getBikeList(BikeCallback callback) {
        new Thread(() -> {
            try {
// 1. SharedPreferences에서 JWT 토큰 읽기
                SharedPreferences sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                String token = sharedPreferences.getString("jwt_token", null);

                URL url = new URL(bikeApiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // 2. Authorization 헤더에 Bearer 토큰 추가
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                } else {
                    // 보안 관점: 인증 없이 목록 조회를 시도할 경우 예외 처리
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onFailure("인증 토큰이 없습니다."));
                    return;
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    List<BikeDTO> bikeList = parseBikeJson(response.toString());
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(bikeList));
                } else if (responseCode == 401) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onFailure("인증이 만료되었습니다. 다시 로그인해주세요."));
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onFailure("Server Error: " + responseCode));
                }

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onFailure("Network Error: " + e.getMessage()));
            }
        }).start();
    }

    // JSON 문자열을 객체 리스트로 변환
    private List<BikeDTO> parseBikeJson(String jsonString) {
        List<BikeDTO> list = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String id = jsonObject.getString("bike_id");
                String serial = jsonObject.getString("serial_number");
                String model = jsonObject.getString("model_name");
                String status = jsonObject.getString("status");
                double lat = jsonObject.getDouble("latitude");

                // [주의] 요청하신 명세서에 "longtitude" (오타)로 되어 있어 그대로 파싱합니다.
                // 실제 개발 시 API 명세서의 오타까지 정확히 맞춰야 파싱 에러가 안 납니다.
                double lng = jsonObject.getDouble("longtitude");

                list.add(new BikeDTO(id, serial, model, status, lat, lng));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}