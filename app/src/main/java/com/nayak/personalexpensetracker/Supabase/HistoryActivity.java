package com.nayak.personalexpensetracker.Supabase;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.nayak.personalexpensetracker.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    private EditText etSearch;
    private Spinner spSort, spDateFilter;
    private RecyclerView rvHistory;

    private RequestQueue requestQueue;
    private String userId, accessToken;

    private List<JSONObject> masterList;
    private List<JSONObject> filteredList;
    private TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        etSearch = findViewById(R.id.etSearch);
        spSort = findViewById(R.id.spSort);
        spDateFilter = findViewById(R.id.spDateFilter);
        rvHistory = findViewById(R.id.rvHistory);

        requestQueue = Volley.newRequestQueue(this);
        masterList = new ArrayList<>();
        filteredList = new ArrayList<>();

        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = sharedPref.getString("USER_ID", null);
        accessToken = sharedPref.getString("ACCESS_TOKEN", null);

        if (userId == null || accessToken == null) {
            Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupSpinners();
        fetchHistoryFromCloud();


        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFiltersAndSorting();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSpinners() {

        String[] sortOptions = {"Latest First", "Oldest First", "Highest Amount", "Lowest Amount"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSort.setAdapter(sortAdapter);


        String[] dateOptions = {"All Time", "Today", "Last 7 Days", "This Month"};
        ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dateOptions);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDateFilter.setAdapter(dateAdapter);


        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                applyFiltersAndSorting();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spSort.setOnItemSelectedListener(filterListener);
        spDateFilter.setOnItemSelectedListener(filterListener);
    }

    private void fetchHistoryFromCloud() {

        String url = SupabaseConfig.TRANSACTIONS_URL + "?user_id=eq." + userId + "&order=created_at.desc";

        JsonArrayRequest getRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        masterList.clear();
                        for (int i = 0; i < response.length(); i++) {
                            masterList.add(response.getJSONObject(i));
                        }
                        applyFiltersAndSorting();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(HistoryActivity.this, "Network read failed", Toast.LENGTH_SHORT).show()
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

    private void applyFiltersAndSorting() {
        String searchQuery = etSearch.getText().toString().toLowerCase().trim();
        String dateFilter = spDateFilter.getSelectedItem().toString();
        String sortOrder = spSort.getSelectedItem().toString();

        filteredList.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


        Calendar calToday = Calendar.getInstance();
        String todayString = sdf.format(calToday.getTime());

        Calendar cal7DaysAgo = Calendar.getInstance();
        cal7DaysAgo.add(Calendar.DAY_OF_YEAR, -7);
        Date sevenDaysAgoDate = cal7DaysAgo.getTime();

        Calendar calMonthStart = Calendar.getInstance();
        calMonthStart.set(Calendar.DAY_OF_MONTH, 1);
        Date monthStartDate = calMonthStart.getTime();


        for (JSONObject obj : masterList) {
            try {
                String category = obj.getString("category").toLowerCase();
                String note = obj.optString("note", "").toLowerCase();
                String dateStr = obj.optString("created_at", "");


                boolean textMatches = category.contains(searchQuery) || note.contains(searchQuery);
                if (!textMatches) continue;


                if (!dateFilter.equals("All Time") && !dateStr.isEmpty()) {
                    Date itemDate = sdf.parse(dateStr.substring(0, 10));
                    String itemDateStr = dateStr.substring(0, 10);

                    if (dateFilter.equals("Today") && !itemDateStr.equals(todayString)) {
                        continue;
                    }
                    if (dateFilter.equals("Last 7 Days") && itemDate.before(sevenDaysAgoDate)) {
                        continue;
                    }
                    if (dateFilter.equals("This Month") && itemDate.before(monthStartDate)) {
                        continue;
                    }
                }

                filteredList.add(obj);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        Collections.sort(filteredList, (o1, o2) -> {
            try {
                if (sortOrder.equals("Highest Amount")) {
                    return Double.compare(o2.getDouble("amount"), o1.getDouble("amount"));
                } else if (sortOrder.equals("Lowest Amount")) {
                    return Double.compare(o1.getDouble("amount"), o2.getDouble("amount"));
                } else if (sortOrder.equals("Oldest First")) {
                    return o1.optString("created_at").compareTo(o2.optString("created_at"));
                } else {
                    return o2.optString("created_at").compareTo(o1.optString("created_at"));
                }
            } catch (JSONException e) {
                return 0;
            }
        });


        JSONArray jsonArray = new JSONArray(filteredList);
        adapter = new TransactionAdapter(jsonArray);
        rvHistory.setAdapter(adapter);
    }
}