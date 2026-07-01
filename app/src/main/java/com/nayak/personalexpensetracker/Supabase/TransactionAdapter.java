package com.nayak.personalexpensetracker.Supabase;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nayak.personalexpensetracker.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final JSONArray transactionList;

    public TransactionAdapter(JSONArray transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        try {
            JSONObject transaction = transactionList.getJSONObject(position);
            String category = transaction.getString("category");
            String note = transaction.optString("note", "");
            double amount = transaction.getDouble("amount");
            String type = transaction.getString("type");


            String rawDate = transaction.optString("created_at", "");
            String displayDate = "";

            if (!rawDate.isEmpty()) {
                try {

                    SimpleDateFormat fromSupabase = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = fromSupabase.parse(rawDate.substring(0, 10));


                    SimpleDateFormat toUser = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    displayDate = toUser.format(date);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            holder.tvCategory.setText(category);


            String description = note.isEmpty() ? "No description details" : note;
            if (!displayDate.isEmpty()) {
                description += "  •  " + displayDate;
            }
            holder.tvNote.setText(description);

            if (type.equalsIgnoreCase("Income")) {
                holder.tvAmount.setText(String.format("+ ₹%.2f", amount));
                holder.tvAmount.setTextColor(Color.parseColor("#10B981"));
            } else {
                holder.tvAmount.setText(String.format("- ₹%.2f", amount));
                holder.tvAmount.setTextColor(Color.parseColor("#EF4444"));
            }


            holder.itemView.setOnClickListener(v -> {
                android.content.Context context = v.getContext();
                Intent intent = new Intent(context, TransactionActivity.class);
                try {

                    intent.putExtra("TRANSACTION_ID", transaction.getString("id"));
                    intent.putExtra("AMOUNT", amount);
                    intent.putExtra("NOTE", note);
                    intent.putExtra("TYPE", type);
                    intent.putExtra("CATEGORY", category);

                    context.startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.length();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvNote, tvAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            tvNote = itemView.findViewById(R.id.tvItemNote);
            tvAmount = itemView.findViewById(R.id.tvItemAmount);
        }
    }
}