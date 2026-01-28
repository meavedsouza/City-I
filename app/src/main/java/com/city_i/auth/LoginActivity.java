package com.city_i.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.city_i.dashboard.UserDashboardActivity;
import com.city_i.services.FirebaseService;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase authentication
        FirebaseService.loginUser(email, password, new FirebaseService.AuthCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(LoginActivity.this, "Login successful!",
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this,
                        UserDashboardActivity.class));
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(LoginActivity.this, "Login failed: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}