package com.nayak.personalexpensetracker.Supabase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nayak.personalexpensetracker.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvUserName, tvTotalBalance, tvTotalIncome, tvTotalExpenses;
    private TextView tvTodaysExpense, tvMonthlyExpense;
    private FloatingActionButton fabAddTransaction;
    private RecyclerView rvTransactions;
    private RequestQueue requestQueue;

    private ImageView btnSettingsCategory;
    private ImageView btnLogout;



    private View layoutTotalTransactions;

    private String userId;
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        tvUserName = findViewById(R.id.tvUserName);
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvTodaysExpense = findViewById(R.id.tvTodaysExpense);
        tvMonthlyExpense = findViewById(R.id.tvMonthlyExpense);
        fabAddTransaction = findViewById(R.id.fabAddTransaction);



        btnSettingsCategory = findViewById(R.id.btnSettingsCategory);
        btnLogout = findViewById(R.id.btnLogout);


        layoutTotalTransactions = findViewById(R.id.layoutTotalTransactions);

        rvTransactions = findViewById(R.id.rvTransactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        requestQueue = Volley.newRequestQueue(this);

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedName = sharedPref.getString("USER_NAME", "User");

        String tempUserId = sharedPref.getString("USER_ID", null);
        if (tempUserId == null) tempUserId = sharedPref.getString("user_id", null);
        if (tempUserId == null) tempUserId = sharedPref.getString("uid", null);
        if (tempUserId == null) tempUserId = sharedPref.getString("ID", null);

        String tempAccessToken = sharedPref.getString("ACCESS_TOKEN", null);
        if (tempAccessToken == null) tempAccessToken = sharedPref.getString("access_token", null);

        userId = tempUserId;
        accessToken = tempAccessToken;



        if (userId == null || accessToken == null) {
            Toast.makeText(this, "Session missing! Please log in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(DashboardActivity.this, SigninActivity.class));
            finish();
            return;
        }

        tvUserName.setText(savedName);



        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, TransactionActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("ACCESS_TOKEN", accessToken);
            startActivity(intent);
        });



        btnSettingsCategory.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CategoryActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("ACCESS_TOKEN", accessToken);
            startActivity(intent);
        });



        if (layoutTotalTransactions != null) {
            layoutTotalTransactions.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, HistoryActivity.class);
                startActivity(intent);
            });
        }



        btnLogout.setOnClickListener(v -> {
            SharedPreferences preferences = getSharedPreferences("UserSession", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();

            Toast.makeText(DashboardActivity.this, "Logged out successfully!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(DashboardActivity.this, SigninActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedName = sharedPref.getString("USER_NAME", "User");
        tvUserName.setText(savedName);

        String tempUserId = sharedPref.getString("USER_ID", null);
        if (tempUserId == null) tempUserId = sharedPref.getString("user_id", null);
        if (tempUserId == null) tempUserId = sharedPref.getString("uid", null);

        userId = tempUserId;

        String tempAccessToken = sharedPref.getString("ACCESS_TOKEN", null);
        if (tempAccessToken == null) tempAccessToken = sharedPref.getString("access_token", null);
        accessToken = tempAccessToken;

        if (accessToken != null && userId != null) {
            fetchTransactionsFromCloud(accessToken);
        }
    }

    private void fetchTransactionsFromCloud(String accessToken) {
        String url = SupabaseConfig.TRANSACTIONS_URL + "?select=*&user_id=eq." + userId + "&order=created_at.desc";

        JsonArrayRequest getRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> calculateAndPopulateUI(response),
                error -> Toast.makeText(DashboardActivity.this, "Failed to load financials.", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.API_KEY);
                headers.put("Authorization", "Bearer " + accessToken);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(getRequest);
    }

    private void calculateAndPopulateUI(JSONArray transactions) {
        double totalIncome = 0.0;
        double totalExpense = 0.0;
        double todayExpense = 0.0;
        double monthlyExpense = 0.0;

        String todayDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentMonthStr = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        try {
            for (int i = 0; i < transactions.length(); i++) {
                JSONObject obj = transactions.getJSONObject(i);
                double amount = obj.getDouble("amount");
                String type = obj.getString("type");
                String createdAt = obj.optString("created_at", "");

                if (type.equalsIgnoreCase("Income")) {
                    totalIncome += amount;
                } else if (type.equalsIgnoreCase("Expense")) {
                    totalExpense += amount;

                    if (createdAt.startsWith(todayDateStr)) {
                        todayExpense += amount;
                    }
                    if (createdAt.startsWith(currentMonthStr)) {
                        monthlyExpense += amount;
                    }
                }
            }

            double availableBalance = totalIncome - totalExpense;

            tvTotalIncome.setText(String.format("₹%.2f", totalIncome));
            tvTotalExpenses.setText(String.format("₹%.2f", totalExpense));
            tvTotalBalance.setText(String.format("₹%.2f", availableBalance));
            tvTodaysExpense.setText(String.format("₹%.2f", todayExpense));
            tvMonthlyExpense.setText(String.format("₹%.2f", monthlyExpense));

            TransactionAdapter adapter = new TransactionAdapter(transactions);
            rvTransactions.setAdapter(adapter);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}