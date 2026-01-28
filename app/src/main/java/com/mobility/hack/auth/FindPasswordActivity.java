package com.mobility.hack.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RetrofitClient;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FindPasswordActivity extends AppCompatActivity {

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_password);

        // [1] RetrofitClient 호출부 수정 (인자 없음)
        apiService = RetrofitClient.getInstance().getApiService();

        TextInputLayout usernameInputLayout = findViewById(R.id.textInputLayoutUsername);
        TextInputLayout emailInputLayout = findViewById(R.id.textInputLayoutEmail);
        TextInputEditText usernameEditText = findViewById(R.id.editTextUsername);
        TextInputEditText emailEditText = findViewById(R.id.editTextEmail);
        Button requestResetButton = findViewById(R.id.buttonRequestReset);

        addTextWatcher(usernameEditText, usernameInputLayout);
        addTextWatcher(emailEditText, emailInputLayout);

        requestResetButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String email = emailEditText.getText().toString();

            boolean hasError = false;
            if (username.isEmpty()) {
                usernameInputLayout.setError("아이디를 입력해주세요.");
                hasError = true;
            }
            if (email.isEmpty()) {
                emailInputLayout.setError("이메일을 입력해주세요.");
                hasError = true;
            }

            if (hasError) return;

            String forgedHost = "attacker.com";
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("email", email);

            requestPasswordReset(forgedHost, payload);
        });
    }

    private void requestPasswordReset(String host, Map<String, String> payload) {
        // [3] Callback 타입을 ResponseBody로 맞춰 호환성 확보
        apiService.requestPasswordReset(host, payload).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(FindPasswordActivity.this, "재설정 링크가 발송되었습니다.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(FindPasswordActivity.this, "요청에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(FindPasswordActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTextWatcher(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
