package com.nayak.personalexpensetracker.Supabase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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

public class TransactionActivity extends AppCompatActivity {

    private EditText etAmount, etNote;
    private RadioGroup rgType;
    private RadioButton rbIncome, rbExpense;
    private Spinner spCategory;
    private Button btnSaveTransaction, btnDeleteTransaction;

    private RequestQueue requestQueue;
    private String userId, accessToken;

    private ArrayList<String> categoryList;
    private ArrayAdapter<String> categoryAdapter;

    private String transactionId = null;
    private boolean isEditMode = false;
    private String passedCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transaction);

        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        rgType = findViewById(R.id.rgType);
        rbIncome = findViewById(R.id.rbIncome);
        rbExpense = findViewById(R.id.rbExpense);
        spCategory = findViewById(R.id.spCategory);
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction);
        btnDeleteTransaction = findViewById(R.id.btnDeleteTransaction);

        requestQueue = Volley.newRequestQueue(this);
        categoryList = new ArrayList<>();

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = sharedPref.getString("USER_ID", null);
        accessToken = sharedPref.getString("ACCESS_TOKEN", null);

        if (userId == null || accessToken == null) {
            Toast.makeText(this, "Session invalid. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (getIntent().hasExtra("TRANSACTION_ID")) {
            transactionId = getIntent().getStringExtra("TRANSACTION_ID");
            if (transactionId != null) {
                transactionId = transactionId.replace("\"", "").trim();
                isEditMode = true;
                btnSaveTransaction.setText("Update Transaction");
                btnDeleteTransaction.setVisibility(View.VISIBLE);

                etAmount.setText(String.valueOf(getIntent().getDoubleExtra("AMOUNT", 0.0)));
                etNote.setText(getIntent().getStringExtra("NOTE"));
                passedCategory = getIntent().getStringExtra("CATEGORY");

                String type = getIntent().getStringExtra("TYPE");
                if ("Income".equalsIgnoreCase(type)) {
                    rbIncome.setChecked(true);
                } else {
                    rbExpense.setChecked(true);
                }
            }
        }

        loadCategoriesIntoSpinner();


        btnSaveTransaction.setOnClickListener(v -> validateAndCheckBudget());
        btnDeleteTransaction.setOnClickListener(v -> deleteTransactionFromCloud());
    }

    private void loadCategoriesIntoSpinner() {
        String url = SupabaseConfig.CATEGORY_URL + "?user_id=eq." + userId + "&order=name.asc";

        JsonArrayRequest getCategoriesRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    categoryList.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            categoryList.add(obj.getString("name"));
                        }
                        if (categoryList.isEmpty()) {
                            categoryList.add("General");
                        }
                        categoryAdapter = new ArrayAdapter<>(TransactionActivity.this,
                                android.R.layout.simple_spinner_item, categoryList);
                        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spCategory.setAdapter(categoryAdapter);

                        if (isEditMode && passedCategory != null) {
                            for (int i = 0; i < categoryList.size(); i++) {
                                if (categoryList.get(i).equalsIgnoreCase(passedCategory)) {
                                    spCategory.setSelection(i);
                                    break;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    categoryList.clear();
                    categoryList.add("General");
                    categoryAdapter = new ArrayAdapter<>(TransactionActivity.this,
                            android.R.layout.simple_spinner_item, categoryList);
                    spCategory.setAdapter(categoryAdapter);
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
        requestQueue.add(getCategoriesRequest);
    }


    private void validateAndCheckBudget() {
        String amountStr = etAmount.getText().toString().trim();
        String selectedCategory = (spCategory.getSelectedItem() != null) ? spCategory.getSelectedItem().toString() : "General";
        String type = rgType.getCheckedRadioButtonId() == R.id.rbIncome ? "Income" : "Expense";

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please insert an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double inputAmount = Double.parseDouble(amountStr);

        if (type.equals("Income") || isEditMode) {
            saveTransactionToCloud();
            return;
        }


        String budgetUrl = SupabaseConfig.BUDGET_URL + "?user_id=eq." + userId + "&category=eq." + selectedCategory;

        JsonArrayRequest budgetRequest = new JsonArrayRequest(Request.Method.GET, budgetUrl, null,
                budgetResponse -> {
                    if (budgetResponse.length() > 0) {
                        try {
                            JSONObject budgetObj = budgetResponse.getJSONObject(budgetResponse.length() - 1);

                            double allowedBudget = budgetObj.getDouble("budget_amount");

                            checkCurrentMonthSpendingAndSave(selectedCategory, allowedBudget, inputAmount);
                        } catch (JSONException e) {
                            saveTransactionToCloud();
                        }
                    } else {
                        saveTransactionToCloud();
                    }
                },
                error -> saveTransactionToCloud()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.API_KEY);
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        requestQueue.add(budgetRequest);
    }

    private void checkCurrentMonthSpendingAndSave(String category, double maxBudget, double newExpense) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String currentMonthStart = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        String transactionUrl = SupabaseConfig.TRANSACTIONS_URL +
                "?user_id=eq." + userId +
                "&category=eq." + category +
                "&type=eq.Expense" +
                "&created_at=gte." + currentMonthStart;

        JsonArrayRequest spendRequest = new JsonArrayRequest(Request.Method.GET, transactionUrl, null,
                response -> {
                    double totalSpentThisMonth = 0;
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            totalSpentThisMonth += response.getJSONObject(i).getDouble("amount");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    if ((totalSpentThisMonth + newExpense) > maxBudget) {
                        new AlertDialog.Builder(TransactionActivity.this)
                                .setTitle("⚠️ Budget Exceeded!")
                                .setMessage(String.format(Locale.getDefault(),
                                        "Your budget limit for %s is ₹%.2f.\nYou have already spent ₹%.2f.\nAdding this item will breach your target tracking parameters!",
                                        category, maxBudget, totalSpentThisMonth))
                                .setPositiveButton("Save Anyway", (dialog, which) -> saveTransactionToCloud())
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .show();
                    } else {
                        saveTransactionToCloud();
                    }
                },
                error -> saveTransactionToCloud()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.API_KEY);
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        requestQueue.add(spendRequest);
    }

    private void saveTransactionToCloud() {
        String amountStr = etAmount.getText().toString().trim();
        String description = etNote.getText().toString().trim();
        String selectedCategory = (spCategory.getSelectedItem() != null) ? spCategory.getSelectedItem().toString() : "General";
        String type = rgType.getCheckedRadioButtonId() == R.id.rbIncome ? "Income" : "Expense";

        double amount = Double.parseDouble(amountStr);

        try {
            JSONObject transactionPayload = new JSONObject();
            transactionPayload.put("user_id", userId);
            transactionPayload.put("amount", amount);
            transactionPayload.put("type", type);
            transactionPayload.put("category", selectedCategory);
            transactionPayload.put("note", description.isEmpty() ? selectedCategory : description);

            int method;
            String url = SupabaseConfig.TRANSACTIONS_URL;

            if (isEditMode) {
                method = Request.Method.PATCH;
                url += (url.contains("?")) ? "&id=eq." + transactionId : "?id=eq." + transactionId;
            } else {
                method = Request.Method.POST;
            }

            StringRequest saveTransactionRequest = new StringRequest(method, url,
                    response -> {
                        Toast.makeText(TransactionActivity.this, isEditMode ? "Transaction Updated!" : "Transaction Saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    error -> {
                        if (error.networkResponse != null &&
                                (error.networkResponse.statusCode == 204 ||
                                        error.networkResponse.statusCode == 201 ||
                                        error.networkResponse.statusCode == 200)) {

                            Toast.makeText(TransactionActivity.this, isEditMode ? "Transaction Updated Successfully!" : "Transaction Saved Successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        Toast.makeText(TransactionActivity.this, "Sync issue.", Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public byte[] getBody() {
                    return transactionPayload.toString().getBytes();
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

            requestQueue.add(saveTransactionRequest);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void deleteTransactionFromCloud() {
        String url = SupabaseConfig.TRANSACTIONS_URL;
        url += (url.contains("?")) ? "&id=eq." + transactionId : "?id=eq." + transactionId;

        StringRequest deleteRequest = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    Toast.makeText(TransactionActivity.this, "Transaction Deleted Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    if (error.networkResponse != null &&
                            (error.networkResponse.statusCode == 204 ||
                                    error.networkResponse.statusCode == 200)) {
                        Toast.makeText(TransactionActivity.this, "Transaction Deleted!", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    Toast.makeText(TransactionActivity.this, "Delete connection rejected.", Toast.LENGTH_SHORT).show();
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
        requestQueue.add(deleteRequest);
    }
}