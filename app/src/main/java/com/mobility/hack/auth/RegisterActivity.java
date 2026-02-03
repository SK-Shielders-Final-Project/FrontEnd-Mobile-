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
import com.mobility.hack.network.RegisterRequest;
import com.mobility.hack.security.TokenManager;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // MainApplication에서 ApiService 및 TokenManager 인스턴스 가져오기
        apiService = ((MainApplication) getApplication()).getApiService();
        tokenManager = ((MainApplication) getApplication()).getTokenManager();

        TextInputLayout usernameLayout = findViewById(R.id.textInputLayoutUsername);
        TextInputLayout nameLayout = findViewById(R.id.textInputLayoutName);
        TextInputLayout passwordLayout = findViewById(R.id.textInputLayoutPassword);
        TextInputLayout emailLayout = findViewById(R.id.textInputLayoutEmail);
        TextInputLayout phoneLayout = findViewById(R.id.textInputLayoutPhone);
        TextInputEditText usernameEditText = findViewById(R.id.editTextUsername);
        TextInputEditText nameEditText = findViewById(R.id.editTextName);
        TextInputEditText passwordEditText = findViewById(R.id.editTextPassword);
        TextInputEditText emailEditText = findViewById(R.id.editTextEmail);
        TextInputEditText phoneEditText = findViewById(R.id.editTextPhone);
        Button registerButton = findViewById(R.id.buttonRegister);

        addTextWatcher(usernameEditText, usernameLayout);
        addTextWatcher(nameEditText, nameLayout);
        addTextWatcher(passwordEditText, passwordLayout);
        addTextWatcher(emailEditText, emailLayout);
        addTextWatcher(phoneEditText, phoneLayout);

        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String name = nameEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String phone = phoneEditText.getText().toString();
            boolean hasError = false;

            if (username.isEmpty()) {
                usernameLayout.setError("아이디를 입력해주세요.");
                hasError = true;
            }
            if (name.isEmpty()) {
                nameLayout.setError("이름을 입력해주세요.");
                hasError = true;
            }
            if (password.isEmpty()) {
                passwordLayout.setError("비밀번호를 입력해주세요.");
                hasError = true;
            }
            if (email.isEmpty()) {
                emailLayout.setError("이메일을 입력해주세요.");
                hasError = true;
            }
            if (phone.isEmpty()) {
                phoneLayout.setError("휴대폰 번호를 입력해주세요.");
                hasError = true;
            }
            if (hasError) return;

            register(new RegisterRequest(username, name, password, email, phone), usernameLayout, emailLayout);
        });
    }

    private void register(RegisterRequest request, TextInputLayout usernameLayout, TextInputLayout emailLayout) {
        apiService.register(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NotNull Call<Void> call, @NotNull Response<Void> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "회원가입에 성공했습니다!", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (response.code() == 409) { // Conflict
                    usernameLayout.setError("이미 사용 중인 아이디입니다.");
                    emailLayout.setError("이미 사용 중인 이메일입니다.");
                } else {
                    Toast.makeText(RegisterActivity.this, "회원가입에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NotNull Call<Void> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(RegisterActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTextWatcher(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
