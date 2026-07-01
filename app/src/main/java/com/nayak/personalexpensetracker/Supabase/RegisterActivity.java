package com.nayak.personalexpensetracker.Supabase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.nayak.personalexpensetracker.R;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvToLogin;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        etName = findViewById(R.id.etRegisterName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvToLogin = findViewById(R.id.tvToLogin);

        requestQueue = Volley.newRequestQueue(this);

        tvToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, SigninActivity.class));
            finish();
        });

        btnRegister.setOnClickListener(v -> handleSignUp());
    }

    private void handleSignUp() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("email", email);
            jsonPayload.put("password", password);


            JSONObject metaData = new JSONObject();
            metaData.put("name", name);
            jsonPayload.put("data", metaData);

            JsonObjectRequest signupRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    SupabaseConfig.SIGNUP_URL,
                    jsonPayload,
                    response -> {
                        Toast.makeText(RegisterActivity.this, "Registration Successful! Please Login.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(RegisterActivity.this, SigninActivity.class));
                        finish();
                    },
                    error -> {
                        String errMsg = "Registration failed.";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String body = new String(error.networkResponse.data, "UTF-8");
                                JSONObject errorObj = new JSONObject(body);
                                errMsg = errorObj.optString("msg", errMsg);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Toast.makeText(RegisterActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("apikey", SupabaseConfig.API_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(signupRequest);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}