package com.example.slotsmart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword, etServerUrl;
    private MaterialButton btnLogin;
    private MaterialCardView errorCard;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (session.isLoggedIn()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etServerUrl = findViewById(R.id.etServerUrl);
        btnLogin = findViewById(R.id.btnLogin);
        errorCard = findViewById(R.id.errorCard);

        etServerUrl.setText(session.getServerUrl());

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = text(etEmail);
        String password = text(etPassword);
        String url = text(etServerUrl);

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password.");
            return;
        }

        if (!email.equals(SessionManager.ADMIN_EMAIL)
                || !password.equals(SessionManager.ADMIN_PASSWORD)) {
            showError("Invalid email or password.");
            return;
        }

        if (!url.isEmpty()) {
            if (!url.endsWith("/")) url += "/";
            session.setServerUrl(url);
        }

        session.setLoggedIn(true);
        goToMain();
    }

    private void showError(String msg) {
        errorCard.setVisibility(View.VISIBLE);
        ((android.widget.TextView) findViewById(R.id.tvError)).setText(msg);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
