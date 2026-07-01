package com.nayak.personalexpensetracker.Supabase;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class SigninActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvToRegister;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvToRegister = findViewById(R.id.tvToRegister);

        requestQueue = Volley.newRequestQueue(this);

        tvToRegister.setOnClickListener(v -> {
            startActivity(new Intent(SigninActivity.this, RegisterActivity.class));
            finish();
        });

        btnLogin.setOnClickListener(v -> handleSignIn());
    }

    private void handleSignIn() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("email", email);
            jsonPayload.put("password", password);

            JsonObjectRequest loginRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    SupabaseConfig.LOGIN_URL,
                    jsonPayload,
                    response -> {
                        try {
                            String accessToken = response.getString("access_token");
                            JSONObject userObj = response.getJSONObject("user");
                            String userId = userObj.getString("id");


                            String userName = "User";
                            if (userObj.has("raw_user_meta_data")) {
                                JSONObject rawMeta = userObj.getJSONObject("raw_user_meta_data");
                                userName = rawMeta.optString("name", rawMeta.optString("full_name", email.split("@")[0]));
                            } else if (userObj.has("user_metadata")) {
                                JSONObject meta = userObj.getJSONObject("user_metadata");
                                userName = meta.optString("name", meta.optString("full_name", email.split("@")[0]));
                            } else if (response.has("user") && response.getJSONObject("user").has("user_metadata")) {
                                userName = response.getJSONObject("user").getJSONObject("user_metadata").optString("name", email.split("@")[0]);
                            } else {
                                userName = email.split("@")[0];
                            }

                            SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();

                            editor.clear();
                            editor.putString("USER_NAME", userName);
                            editor.putString("USER_ID", userId);
                            editor.putString("ACCESS_TOKEN", accessToken);
                            editor.apply();

                            Toast.makeText(SigninActivity.this, "Welcome back, " + userName + "!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(SigninActivity.this, DashboardActivity.class);
                            startActivity(intent);
                            finish();

                        } catch (JSONException e) {
                            Toast.makeText(SigninActivity.this, "Response processing error.", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> Toast.makeText(SigninActivity.this, "Invalid email or password.", Toast.LENGTH_LONG).show()
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("apikey", SupabaseConfig.API_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(loginRequest);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}