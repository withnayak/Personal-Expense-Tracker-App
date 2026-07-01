package com.nayak.personalexpensetracker.Supabase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nayak.personalexpensetracker.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final JSONArray categoryList;
    private final OnCategoryDeleteListener deleteListener;
    public interface OnCategoryDeleteListener {
        void onDeleteClick(String categoryId);
    }

    public CategoryAdapter(JSONArray categoryList, OnCategoryDeleteListener deleteListener) {
        this.categoryList = categoryList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        try {
            JSONObject obj = categoryList.getJSONObject(position);
            String id = obj.getString("id");
            String name = obj.getString("name");
            String type = obj.optString("type", "Expense");

            holder.tvCategoryName.setText(name);
            holder.tvCategoryType.setText(type);


            holder.ivDeleteCategory.setOnClickListener(v -> deleteListener.onDeleteClick(id));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return categoryList.length();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvCategoryType;
        ImageView ivDeleteCategory;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvCategoryType = itemView.findViewById(R.id.tvCategoryType);
            ivDeleteCategory = itemView.findViewById(R.id.ivDeleteCategory);
        }
    }
}