package com.nayak.personalexpensetracker.Supabase;

import android.app.AlertDialog;
import android.content.Intent; // ✅ Added Intent Import
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nayak.personalexpensetracker.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class CategoryActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private FloatingActionButton fabAddCategory;
    private Button btnGoToBudget;

    private RequestQueue requestQueue;
    private String userId, accessToken;
    private JSONArray categoriesArray;
    private CategoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category);

        rvCategories = findViewById(R.id.rvCategories);
        fabAddCategory = findViewById(R.id.fabAddCategory);
        btnGoToBudget = findViewById(R.id.btnGoToBudget);

        requestQueue = Volley.newRequestQueue(this);
        categoriesArray = new JSONArray();

        rvCategories.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = sharedPref.getString("USER_ID", null);
        accessToken = sharedPref.getString("ACCESS_TOKEN", null);

        if (userId == null || accessToken == null) {
            Toast.makeText(this, "Session invalid. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchCategories();

        fabAddCategory.setOnClickListener(v -> showAddCategoryDialog());


        btnGoToBudget.setOnClickListener(v -> {
            Intent intent = new Intent(CategoryActivity.this, BudgetActivity.class);
            startActivity(intent);
        });
    }

    private void fetchCategories() {
        String url = SupabaseConfig.CATEGORY_URL + "?user_id=eq." + userId + "&order=name.asc";

        JsonArrayRequest getRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    categoriesArray = response;
                    adapter = new CategoryAdapter(categoriesArray, this::deleteCategoryFromCloud);
                    rvCategories.setAdapter(adapter);
                },
                error -> Toast.makeText(CategoryActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", SupabaseConfig.API_KEY);
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        requestQueue.add(getRequest);
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.popup_category, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText etDialogCategoryName = dialogView.findViewById(R.id.etDialogCategoryName);
        RadioGroup rgDialogCategoryType = dialogView.findViewById(R.id.rgDialogCategoryType);
        Button btnDialogSaveCategory = dialogView.findViewById(R.id.btnDialogSaveCategory);

        btnDialogSaveCategory.setOnClickListener(v -> {
            String catName = etDialogCategoryName.getText().toString().trim();
            int selectedRadioId = rgDialogCategoryType.getCheckedRadioButtonId();

            String catType = (selectedRadioId == R.id.rbDialogIncome) ? "Income" : "Expense";

            if (catName.isEmpty()) {
                Toast.makeText(CategoryActivity.this, "Please enter a category name", Toast.LENGTH_SHORT).show();
                return;
            }

            addCategoryToCloud(catName, catType, dialog);
        });

        dialog.show();
    }

    private void addCategoryToCloud(String name, String type, AlertDialog dialog) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("user_id", userId);
            payload.put("name", name);
            payload.put("type", type);

            StringRequest postRequest = new StringRequest(Request.Method.POST, SupabaseConfig.CATEGORY_URL,
                    response -> {
                        dialog.dismiss();
                        Toast.makeText(CategoryActivity.this, "Category Added!", Toast.LENGTH_SHORT).show();
                        fetchCategories();
                    },
                    error -> {
                        if (error.networkResponse != null && (error.networkResponse.statusCode == 201 || error.networkResponse.statusCode == 204 || error.networkResponse.statusCode == 200)) {
                            dialog.dismiss();
                            Toast.makeText(CategoryActivity.this, "Category Saved Successfully!", Toast.LENGTH_SHORT).show();
                            fetchCategories();
                            return;
                        }
                        Toast.makeText(CategoryActivity.this, "Failed to insert row metric values", Toast.LENGTH_SHORT).show();
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

    private void deleteCategoryFromCloud(String id) {
        String url = SupabaseConfig.CATEGORY_URL + "?id=eq." + id;

        StringRequest deleteRequest = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    Toast.makeText(CategoryActivity.this, "Category Deleted!", Toast.LENGTH_SHORT).show();
                    fetchCategories();
                },
                error -> {
                    if (error.networkResponse != null && (error.networkResponse.statusCode == 204 || error.networkResponse.statusCode == 200)) {
                        Toast.makeText(CategoryActivity.this, "Category Deleted Successfully!", Toast.LENGTH_SHORT).show();
                        fetchCategories();
                        return;
                    }
                    Toast.makeText(CategoryActivity.this, "Deletion failed", Toast.LENGTH_SHORT).show();
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