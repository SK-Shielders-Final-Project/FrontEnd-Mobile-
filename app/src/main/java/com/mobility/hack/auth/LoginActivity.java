package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log; // 로그용 추가
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog; // 다이얼로그용 추가
import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.ExchangeRequest;
import com.mobility.hack.network.LoginRequest;
import com.mobility.hack.network.LoginResponse;
import com.mobility.hack.network.PublicKeyResponse;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.SecurityEngine;
import com.mobility.hack.ride.MainActivity;
import com.mobility.hack.security.TokenManager;

import com.mobility.hack.network.IntegrityRequest;
import com.mobility.hack.network.IntegrityResponse;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity {
    private ApiService apiService;
    private TokenManager tokenManager;
    private CheckBox autoLoginCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // MainApplication에서 ApiService 및 TokenManager 인스턴스 가져오기
        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        // ---------------------------------------------------------
        // [보안 로직 추가] 화면 진입 즉시 무결성 검사 실행
        // ---------------------------------------------------------
        // performIntegrityCheck(); //무결성 끄고싶으면 이 줄만 주석처리!

        EditText usernameEditText = findViewById(R.id.editTextId);
        EditText passwordEditText = findViewById(R.id.editTextPassword);
        Button loginButton = findViewById(R.id.buttonLogin);
        TextView registerTextView = findViewById(R.id.textViewRegister);
        TextView findPasswordTextView = findViewById(R.id.textViewFindPassword);
        autoLoginCheckBox = findViewById(R.id.checkbox_auto_login);

        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (!username.isEmpty() && !password.isEmpty()) {
                login(new LoginRequest(username, password));
            } else {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });

        findPasswordTextView.setOnClickListener(v -> {
            Intent intent = new Intent(this, FindPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void performIntegrityCheck() {
        String sig = "";
        String bin = "";

        try {
            sig = SecurityEngine.getNativeSignature(this);
            bin = SecurityEngine.getNativeBinaryHash(this);

            Log.d("SECURITY", "Signature: " + sig);
            Log.d("SECURITY", "Binary: " + bin);

        } catch (UnsatisfiedLinkError e) {
            Log.e("SECURITY", "CRITICAL ERROR: JNI 연결 실패! C++ 함수 이름을 확인하세요.", e);
            Toast.makeText(this, "보안 모듈 로드 실패 (로그 확인 필요)", Toast.LENGTH_LONG).show();
            return; 
        } catch (Exception e) {
            Log.e("SECURITY", "알 수 없는 오류 발생", e);
            return;
        }

        IntegrityRequest req = new IntegrityRequest(sig, bin);

        apiService.checkIntegrity(req).enqueue(new Callback<IntegrityResponse>() {
            @Override
            public void onResponse(@NotNull Call<IntegrityResponse> call, @NotNull Response<IntegrityResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isValid()) {
                        Log.d("SECURITY", "무결성 검증 통과");
                    } else {
                        showKillAppDialog();
                    }
                } else {
                    Log.e("SECURITY", "서버 응답 오류: " + response.code());
                }
            }

            @Override
            public void onFailure(@NotNull Call<IntegrityResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Log.e("SECURITY", "네트워크 오류", t);
            }
        });
    }

    private void showKillAppDialog() {
        if (isFinishing()) return;

        new AlertDialog.Builder(LoginActivity.this)
                .setTitle("⛔ 보안 경고")
                .setMessage("변조된 앱이 감지되었습니다.\n안전을 위해 앱을 종료합니다.")
                .setCancelable(false) 
                .setPositiveButton("종료", (dialog, which) -> {
                    finishAffinity(); 
                    System.exit(0);   
                })
                .show();
    }

    private void login(LoginRequest loginRequest) {
        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NotNull Call<LoginResponse> call, @NotNull Response<LoginResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                LoginResponse loginResponse = response.body();

                if (response.isSuccessful() && loginResponse != null && loginResponse.getAccessToken() != null && !loginResponse.getAccessToken().isEmpty()) {
                    Log.d("LOGIN_DEBUG", "Received user ID: " + loginResponse.getUserId()); // 로그 추가

                    tokenManager.saveAuthToken(loginResponse.getAccessToken());
                    tokenManager.saveUserId(loginResponse.getUserId());

                    if (autoLoginCheckBox.isChecked()) {
                        tokenManager.saveRefreshToken(loginResponse.getRefreshToken());
                        tokenManager.saveAutoLogin(true);
                    } else {
                        tokenManager.saveRefreshToken(null);
                        tokenManager.saveAutoLogin(false);
                    }

                    Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                    executeWeakKeyExchange();

                    goToMainActivity();
                } else {
                    Toast.makeText(LoginActivity.this, "로그인에 실패했습니다. 아이디 또는 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<LoginResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(LoginActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class); 
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String generateWeakKey() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            result.append(characters.charAt((int) Math.floor(Math.random() * characters.length())));
        }
        return result.toString();
    }

    private void executeWeakKeyExchange() {
        apiService.getPublicKey().enqueue(new Callback<PublicKeyResponse>() {
            @Override
            public void onResponse(Call<PublicKeyResponse> call, Response<PublicKeyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String publicKeyString = response.body().getPublicKey();
                    Log.d("KEY_EXCHANGE", "Raw public key: " + publicKeyString);

                    String cleanedPublicKey = publicKeyString
                            .replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", "")
                            .replaceAll("\\s", "");

                    Log.d("KEY_EXCHANGE", "Cleaned public key: " + cleanedPublicKey);

                    String weakKey = generateWeakKey();
                    try {
                        byte[] keyBytes = Base64.decode(cleanedPublicKey, Base64.DEFAULT);
                        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        PublicKey publicKey = keyFactory.generatePublic(spec);

                        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                        byte[] encryptedKey = cipher.doFinal(weakKey.getBytes());
                        String encryptedKeyString = Base64.encodeToString(encryptedKey, Base64.NO_WRAP);

                        apiService.exchangeKeys(new ExchangeRequest(encryptedKeyString)).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Log.d("KEY_EXCHANGE", "Weak key exchanged successfully");
                                    tokenManager.saveWeakKey(weakKey); // 키 저장
                                } else {
                                    String errorBody = "내용 없음";
                                    if (response.errorBody() != null) {
                                        try {
                                            errorBody = response.errorBody().string();
                                        } catch (IOException e) {
                                            Log.e("KEY_EXCHANGE", "exchangeKeys errorBody 읽기 실패", e);
                                        }
                                    }
                                    Log.e("KEY_EXCHANGE", "Failed to exchange weak key. code: " + response.code() + ", error: " + errorBody);
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Log.e("KEY_EXCHANGE", "Error during key exchange", t);
                            }
                        });

                    } catch (Exception e) {
                        Log.e("KEY_EXCHANGE", "Error during key encryption", e);
                    }
                } else {
                    String errorBody = "내용 없음";
                    if (response.errorBody() != null) {
                        try {
                            errorBody = response.errorBody().string();
                        } catch (IOException e) {
                            Log.e("KEY_EXCHANGE", "getPublicKey errorBody 읽기 실패", e);
                        }
                    }
                    Log.e("KEY_EXCHANGE", "Failed to get public key. code: " + response.code() + ", error: " + errorBody);
                }
            }

            @Override
            public void onFailure(Call<PublicKeyResponse> call, Throwable t) {
                Log.e("KEY_EXCHANGE", "Error getting public key", t);
            }
        });
    }
}
