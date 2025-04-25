package com.example.project1732.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project1732.Adapter.ListFoodAdapter;
import com.example.project1732.Domain.Foods;
import com.example.project1732.databinding.ActivityListFoodBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ListFoodActivity extends BaseActivity {
    private ActivityListFoodBinding binding;
    private RecyclerView.Adapter adapterListFood;
    private int categoryId;
    private String categoryName;
    private String searchText;
    private boolean isSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityListFoodBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        getIntentExtra();
        initList();
    }

    private void getIntentExtra() {
        categoryId=getIntent().getIntExtra("CategoryId",0);
        categoryName=getIntent().getStringExtra("CategoryName");
        searchText=getIntent().getStringExtra("text");
        isSearch=getIntent().getBooleanExtra("isSearch",false);

        binding.titleTxt.setText(categoryName);
        binding.backBtn.setOnClickListener(v -> finish());
    }


    private void initList(){
        DatabaseReference myRef=database.getReference("Foods");
        binding.progressBar.setVisibility(View.VISIBLE);
        ArrayList<Foods> list=new ArrayList<>();
        Query query;
        if(isSearch){
            Log.d("SearchDebug", "Searching for text: '" + searchText + "'"); // In ra searchText đang dùng
            query=myRef.orderByChild("Title").startAt(searchText).endAt(searchText+'\uf8ff');
        }else{
            query=myRef.orderByChild("CategoryId").equalTo(categoryId);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("SearchDebug", "onDataChange called. Snapshot exists: " + snapshot.exists()); // Kiểm tra snapshot có dữ liệu không

                if(snapshot.exists()){
                    list.clear();
                    int resultsCount = 0; // Đếm số kết quả
                    for(DataSnapshot issue:snapshot.getChildren()){
                        Foods food = issue.getValue(Foods.class);
                        if (food != null && food.getTitle() != null) { // Kiểm tra null trước khi log
                            Log.d("SearchDebug", "Found item: " + food.getTitle()); // In ra title của item tìm thấy
                        }
                        list.add(food);
                        resultsCount++;
                        list.add(issue.getValue(Foods.class));
                    }
                    if(list.size()>0){
                        binding.foodListView.setLayoutManager(new GridLayoutManager(ListFoodActivity.this,2));
                        adapterListFood=new ListFoodAdapter(list);
                        binding.foodListView.setAdapter(adapterListFood);
                    }
                    binding.progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SearchDebug", "Query cancelled or failed: " + error.getMessage(), error.toException()); // In lỗi chi tiết
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }
}