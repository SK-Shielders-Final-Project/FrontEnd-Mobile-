package com.mobility.hack.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mobility.hack.MainApplication;
import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.RegisterResponse;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = MainApplication.getRetrofit().create(ApiService.class);

        TextInputLayout usernameInputLayout = findViewById(R.id.textInputLayoutUsername);
        TextInputLayout nameInputLayout = findViewById(R.id.textInputLayoutName);
        TextInputLayout passwordInputLayout = findViewById(R.id.textInputLayoutPassword);
        TextInputLayout passwordConfirmInputLayout = findViewById(R.id.textInputLayoutPasswordConfirm);
        TextInputLayout emailInputLayout = findViewById(R.id.textInputLayoutEmail);
        TextInputLayout phoneInputLayout = findViewById(R.id.textInputLayoutPhone);

        TextInputEditText usernameEditText = findViewById(R.id.editTextUsername);
        TextInputEditText nameEditText = findViewById(R.id.editTextName);
        TextInputEditText passwordEditText = findViewById(R.id.editTextPassword);
        TextInputEditText passwordConfirmEditText = findViewById(R.id.editTextPasswordConfirm);
        TextInputEditText emailEditText = findViewById(R.id.editTextEmail);
        TextInputEditText phoneEditText = findViewById(R.id.editTextPhone);

        Button registerButton = findViewById(R.id.buttonRegister);

        addTextWatcher(usernameEditText, usernameInputLayout);
        addTextWatcher(nameEditText, nameInputLayout);
        addTextWatcher(passwordEditText, passwordInputLayout);
        addTextWatcher(passwordConfirmEditText, passwordConfirmInputLayout);
        addTextWatcher(emailEditText, emailInputLayout);
        addTextWatcher(phoneEditText, phoneInputLayout);

        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String name = nameEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String passwordConfirm = passwordConfirmEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String phone = phoneEditText.getText().toString();

            // (입력값 검증 로직은 동일)
            if (!password.equals(passwordConfirm)) {
                passwordInputLayout.setError("비밀번호가 일치하지 않습니다.");
                passwordConfirmInputLayout.setError("비밀번호가 일치하지 않습니다.");
                return;
            }

            // [취약점 적용] RegisterRequest 대신 HashMap 사용
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("username", username);
            requestData.put("name", name);
            requestData.put("password", password);
            requestData.put("email", email);
            requestData.put("phone", phone);
            // 모의해킹 시, 여기에 requestData.put("admin_lev", 1); 등을 추가하여 테스트 가능

            signup(requestData);
        });
    }

    private void signup(Map<String, Object> request) {
        apiService.signup(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NotNull Call<RegisterResponse> call, @NotNull Response<RegisterResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    String welcomeName = response.body().getName();
                    Toast.makeText(RegisterActivity.this, welcomeName + "님, 회원가입을 환영합니다!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "회원가입에 실패했습니다.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<RegisterResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(RegisterActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
