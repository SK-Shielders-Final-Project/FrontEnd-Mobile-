package com.mobility.hack.auth;

import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.PublicKeyResponse;
import com.mobility.hack.network.UpdateUserRequest;
import com.mobility.hack.network.UserInfoResponse;
import com.mobility.hack.security.TokenManager;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditMyInfoActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private TextView tvPassword;
    private Button btnSave;
    private ApiService apiService;
    private TokenManager tokenManager;
    private UserInfoResponse currentUserInfo;
    private final StringBuilder passwordBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_my_info);

        // MainApplication에서 싱글톤 인스턴스 가져오기
        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        tvPassword = findViewById(R.id.tv_password);
        btnSave = findViewById(R.id.btn_save);

        etName.setEnabled(false);

        loadUserInfo();
        setupVirtualKeypad();

        btnSave.setOnClickListener(v -> fetchPublicKeyAndSave());
    }

    private void loadUserInfo() {
        long userId = tokenManager.fetchUserId();
        apiService.getUserInfo(userId).enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUserInfo = response.body();
                    etName.setText(currentUserInfo.getName()); // 실제 이름으로 설정
                    etEmail.setText(currentUserInfo.getEmail());
                    etPhone.setText(currentUserInfo.getPhone());
                } else {
                    String errorMsg = "사용자 정보를 가져오지 못했습니다. 코드: " + response.code();
                    Toast.makeText(EditMyInfoActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                Toast.makeText(EditMyInfoActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupVirtualKeypad() {
        GridLayout keypadLayout = findViewById(R.id.keypad_layout);
        keypadLayout.setColumnCount(10);
        keypadLayout.setUseDefaultMargins(false);
        keypadLayout.removeAllViews();

        List<String> keyValues = new ArrayList<>();
        // Numbers
        for (int i = 0; i <= 9; i++) {
            keyValues.add(String.valueOf(i));
        }
        // Alphabets
        for (char c = 'a'; c <= 'z'; c++) {
            keyValues.add(String.valueOf(c));
        }
        // Special Characters
        String[] specialChars = {"@", "#", "$", "%", "^", "&", "*", "(", ")", "-", "_", "+", "=", "!", "?"};
        Collections.addAll(keyValues, specialChars);

        Collections.shuffle(keyValues);
        keyValues.add("<-"); // Add backspace key

        for (String value : keyValues) {
            TextView key = new TextView(this, null, 0);
            key.setText(value);
            key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            key.setGravity(Gravity.CENTER);
            key.setPadding(0, 8, 0, 8);
            key.setClickable(true);
            key.setIncludeFontPadding(false);

            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            key.setBackgroundResource(outValue.resourceId);

            if (value.equals("<-")) {
                key.setOnClickListener(view -> {
                    if (passwordBuilder.length() > 0) {
                        passwordBuilder.deleteCharAt(passwordBuilder.length() - 1);
                        updatePasswordView();
                    }
                });
            } else {
                key.setOnClickListener(view -> {
                    passwordBuilder.append(value);
                    updatePasswordView();
                });
            }

            GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.columnSpec = colSpec;
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            key.setLayoutParams(params);
            keypadLayout.addView(key);
        }
    }

    private void updatePasswordView() {
        StringBuilder maskedPassword = new StringBuilder();
        for (int i = 0; i < passwordBuilder.length(); i++) {
            maskedPassword.append('*');
        }
        tvPassword.setText(maskedPassword.toString());
    }

    private void fetchPublicKeyAndSave() {
        String password = passwordBuilder.toString();
        if (password.isEmpty()) {
            Toast.makeText(this, "현재 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getPublicKey().enqueue(new Callback<PublicKeyResponse>() {
            @Override
            public void onResponse(Call<PublicKeyResponse> call, Response<PublicKeyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String publicKey = response.body().getPublicKey();
                    saveUserInfo(publicKey);
                } else {
                    Toast.makeText(EditMyInfoActivity.this, "암호화 키를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PublicKeyResponse> call, Throwable t) {
                Toast.makeText(EditMyInfoActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserInfo(String publicKey) {
        if (currentUserInfo == null) {
            Toast.makeText(this, "기존 사용자 정보를 불러오지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = currentUserInfo.getName();
        String username = currentUserInfo.getUsername();
        String email = etEmail.getText().toString();
        String phone = etPhone.getText().toString();
        String password = passwordBuilder.toString();

        try {
            String encryptedPassword = encrypt(password, publicKey);
            UpdateUserRequest request = new UpdateUserRequest(name, username, encryptedPassword, email, phone, currentUserInfo.getAdminLev());

            apiService.updateUserInfo(request).enqueue(new Callback<UserInfoResponse>() {
                @Override
                public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(EditMyInfoActivity.this, "정보가 성공적으로 수정되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String errorMessage = "정보 수정에 실패했습니다.";
                        if (response.code() == 401) {
                            errorMessage = "비밀번호가 일치하지 않습니다.";
                        }
                        Toast.makeText(EditMyInfoActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                    Toast.makeText(EditMyInfoActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "암호화 실패", Toast.LENGTH_SHORT).show();
        }
    }

    private String encrypt(String plainText, String publicKeyString) throws Exception {
        byte[] publicKeyBytes = Base64.decode(publicKeyString, Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }
}
