package com.mobility.hack.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mobility.hack.R;
import com.mobility.hack.network.RegisterRequest;
import com.mobility.hack.network.RegisterResponse;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * BaseActivity를 상속받아 컴파일 에러 해결 및 코드 간소화
 */
public class RegisterActivity extends BaseActivity {

    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // [에러 해결] BaseActivity에서 이미 초기화된 apiService 사용

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

            if (!password.equals(passwordConfirm)) {
                passwordInputLayout.setError("비밀번호가 일치하지 않습니다.");
                passwordConfirmInputLayout.setError("비밀번호가 일치하지 않습니다.");
                return;
            }

            RegisterRequest request = new RegisterRequest(username, name, password, email, phone);
            signup(request);
        });
    }

    private void signup(RegisterRequest request) {
        if (apiService == null) return;
        apiService.signup(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NotNull Call<RegisterResponse> call, @NotNull Response<RegisterResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegisterActivity.this, "회원가입을 환영합니다!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e(TAG, "회원가입 실패 코드: " + response.code());
                    Toast.makeText(RegisterActivity.this, "회원가입 실패 (코드: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(@NotNull Call<RegisterResponse> call, @NotNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "네트워크 오류 발생: " + t.getMessage());
                Toast.makeText(RegisterActivity.this, "서버 통신 오류", Toast.LENGTH_LONG).show();
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
