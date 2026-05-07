package com.example.placekeeper;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editPassword;
    private Button buttonLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        buttonLogin = findViewById(R.id.button_login);
        TextView textRegister = findViewById(R.id.text_register);

        buttonLogin.setOnClickListener(v -> loginUser());
        textRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = Objects.requireNonNull(editEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(editPassword.getText()).toString().trim();

        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editPassword.setError("Password is required");
            return;
        }

        buttonLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        buttonLogin.setEnabled(true);
                    }
                });
    }
}