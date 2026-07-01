package com.nayak.personalexpensetracker.Supabase;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.nayak.personalexpensetracker.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BudgetActivity extends AppCompatActivity {

    private Spinner spBudgetCategory;
    private EditText etBudgetAmount;
    private Button btnSaveBudget;

    private RequestQueue requestQueue;
    private String userId, accessToken;
    private ArrayList<String> categoryList;
    private ArrayAdapter<String> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.budget);

        spBudgetCategory = findViewById(R.id.spBudgetCategory);
        etBudgetAmount = findViewById(R.id.etBudgetAmount);
        btnSaveBudget = findViewById(R.id.btnSaveBudget);

        requestQueue = Volley.newRequestQueue(this);
        categoryList = new ArrayList<>();

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = sharedPref.getString("USER_ID", null);
        accessToken = sharedPref.getString("ACCESS_TOKEN", null);

        if (userId == null || accessToken == null) {
            Toast.makeText(this, "Session invalid!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCategoriesForBudget();
        btnSaveBudget.setOnClickListener(v -> saveBudgetToCloud());
    }

    private void loadCategoriesForBudget() {
        String url = SupabaseConfig.CATEGORY_URL + "?user_id=eq." + userId + "&order=name.asc";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    categoryList.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            categoryList.add(response.getJSONObject(i).getString("name"));
                        }
                        if (categoryList.isEmpty()) categoryList.add("General");

                        categoryAdapter = new ArrayAdapter<>(BudgetActivity.this, android.R.layout.simple_spinner_item, categoryList);
                        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spBudgetCategory.setAdapter(categoryAdapter);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    categoryList.add("General");
                    categoryAdapter = new ArrayAdapter<>(BudgetActivity.this, android.R.layout.simple_spinner_item, categoryList);
                    spBudgetCategory.setAdapter(categoryAdapter);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.API_KEY);
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        requestQueue.add(request);
    }

    private void saveBudgetToCloud() {
        String amountStr = etBudgetAmount.getText().toString().trim();
        String selectedCategory = (spBudgetCategory.getSelectedItem() != null) ? spBudgetCategory.getSelectedItem().toString() : "General";

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);


        Calendar cal = Calendar.getInstance();
        String currentMonthName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.getTime());
        String currentYearStr = String.valueOf(cal.get(Calendar.YEAR));

        try {
            JSONObject payload = new JSONObject();
            payload.put("user_id", userId);
            payload.put("category", selectedCategory);
            payload.put("budget_amount", amount);
            payload.put("month", currentMonthName);
            payload.put("year", currentYearStr);

            StringRequest postRequest = new StringRequest(Request.Method.POST, SupabaseConfig.BUDGET_URL,
                    response -> {
                        Toast.makeText(BudgetActivity.this, "Budget Saved Successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    error -> {
                        if (error.networkResponse != null && (error.networkResponse.statusCode == 201 || error.networkResponse.statusCode == 204 || error.networkResponse.statusCode == 200)) {
                            Toast.makeText(BudgetActivity.this, "Budget Saved Successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        Toast.makeText(BudgetActivity.this, "Failed to register budget boundary.", Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public byte[] getBody() {
                    return payload.toString().getBytes();
                }

                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("apikey", SupabaseConfig.API_KEY);
                    headers.put("Authorization", "Bearer " + accessToken);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            requestQueue.add(postRequest);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}