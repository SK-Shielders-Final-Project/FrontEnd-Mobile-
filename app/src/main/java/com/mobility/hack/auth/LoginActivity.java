package com.mobility.hack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.ExchangeRequest;
import com.mobility.hack.network.LoginRequest;
import com.mobility.hack.network.LoginResponse;
import com.mobility.hack.network.PublicKeyResponse;
import com.mobility.hack.ride.MainActivity;
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ApiService apiService;
    private TokenManager tokenManager;
    private CheckBox autoLoginCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        // 무결성 검사는 이미 Splash에서 완료되었으므로 제거됨

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

    private void login(LoginRequest loginRequest) {
        // ===== 무결성 토큰 확인 =====
        String integrityToken = tokenManager.getIntegrityToken();

        if (integrityToken == null) {
            Toast.makeText(this, "보안 검증이 필요합니다. 앱을 재시작해주세요.", Toast.LENGTH_LONG).show();
            return;
        }

        // Interceptor가 자동으로 X-Device-Id, X-Integrity-Token 헤더 추가
        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NotNull Call<LoginResponse> call, @NotNull Response<LoginResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                LoginResponse loginResponse = response.body();

                if (response.isSuccessful() && loginResponse != null && loginResponse.getAccessToken() != null) {
                    Log.d("LOGIN_DEBUG", "Login success, User ID: " + loginResponse.getUserId());

                    tokenManager.saveAuthToken(loginResponse.getAccessToken());
                    tokenManager.saveUserId(loginResponse.getUserId());

                    if (autoLoginCheckBox.isChecked()) {
                        tokenManager.saveRefreshToken(loginResponse.getRefreshToken());
                        tokenManager.saveAutoLogin(true);
                    } else {
                        tokenManager.saveRefreshToken(null);
                        tokenManager.saveAutoLogin(false);
                    }

                    // ===== 토큰 사용 완료, 즉시 삭제 =====
                    tokenManager.clearIntegrityToken();

                    Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                    executeWeakKeyExchange();
                } else {
                    Toast.makeText(LoginActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
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

    // -------------------------------------------------------------
    // Weak Key Exchange Logic (로그인 이후 세션 암호화용)
    // -------------------------------------------------------------
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
                    try {
                        String publicKeyString = response.body().getPublicKey();
                        String cleanedPublicKey = publicKeyString
                                .replace("-----BEGIN PUBLIC KEY-----", "")
                                .replace("-----END PUBLIC KEY-----", "")
                                .replaceAll("\\s", "");

                        byte[] keyBytes = Base64.decode(cleanedPublicKey, Base64.DEFAULT);
                        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        PublicKey publicKey = keyFactory.generatePublic(spec);

                        String weakKey = generateWeakKey();
                        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                        byte[] encryptedKey = cipher.doFinal(weakKey.getBytes());
                        String encryptedKeyString = Base64.encodeToString(encryptedKey, Base64.NO_WRAP);

                        apiService.exchangeKeys(new ExchangeRequest(encryptedKeyString)).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    tokenManager.saveWeakKey(weakKey);
                                    Log.d("KEY_EXCHANGE", "Weak key exchanged successfully");
                                    goToMainActivity();
                                } else {
                                    Log.e("KEY_EXCHANGE", "Failed to exchange weak key.");
                                    goToMainActivity();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Log.e("KEY_EXCHANGE", "Error during key exchange", t);
                                goToMainActivity();
                            }
                        });

                    } catch (Exception e) {
                        Log.e("KEY_EXCHANGE", "Error during key processing", e);
                        goToMainActivity();
                    }
                }
            }

            @Override
            public void onFailure(Call<PublicKeyResponse> call, Throwable t) {
                Log.e("KEY_EXCHANGE", "Error getting public key", t);
                goToMainActivity();
            }
        });
    }
}