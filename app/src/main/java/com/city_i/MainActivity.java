package com.city_i;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import com.city_i.auth.LoginActivity;
import com.city_i.dashboard.UserDashboardActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin, btnRegister, btnGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGuest = findViewById(R.id.btnGuest);

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this,
                    com.city_i.auth.RegisterActivity.class));
        });

        btnGuest.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this,
                    UserDashboardActivity.class));
        });
    }
}