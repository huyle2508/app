package com.example.project1732.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.project1732.Adapter.ListFoodAdapter;
import com.example.project1732.Domain.Foods;
import com.example.project1732.databinding.ActivityFavoriteBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FavoriteActivity extends BaseActivity {

    private ActivityFavoriteBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference favRef; // Ref đến /Favorites/{userId}
    private DatabaseReference foodsRef; // Ref đến /Foods
    private ListFoodAdapter favoriteAdapter;
    private ArrayList<Foods> favoriteFoodList = new ArrayList<>();
    private ArrayList<String> favoriteFoodIds = new ArrayList<>(); // Lưu ID món ăn yêu thích

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoriteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        foodsRef = database.getReference("Foods"); // Tham chiếu đến nút Foods

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem mục yêu thích", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        favRef = database.getReference("Favorites").child(currentUser.getUid());

        setupRecyclerView();
        loadFavoriteFoodIds(); // Bắt đầu tải ID yêu thích

        binding.backBtnFavorite.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        binding.favoriteRecyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // Dạng lưới 2 cột
        favoriteAdapter = new ListFoodAdapter(favoriteFoodList); // Dùng ListFoodAdapter
        binding.favoriteRecyclerView.setAdapter(favoriteAdapter);
    }

    // Bước 1: Tải danh sách ID các món ăn yêu thích
    private void loadFavoriteFoodIds() {
        binding.progressBarFavorite.setVisibility(View.VISIBLE);
        binding.emptyFavoriteTxt.setVisibility(View.GONE);

        favRef.addValueEventListener(new ValueEventListener() { // Dùng add để tự cập nhật
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoriteFoodIds.clear(); // Xóa ID cũ
                if (snapshot.exists()) {
                    for (DataSnapshot foodIdSnapshot : snapshot.getChildren()) {
                        // Kiểm tra xem giá trị có phải là true không (nếu bạn lưu boolean)
                        if (Boolean.TRUE.equals(foodIdSnapshot.getValue(Boolean.class))) {
                            String foodId = foodIdSnapshot.getKey();
                            if (foodId != null) {
                                favoriteFoodIds.add(foodId);
                            }
                        }
                        // Nếu bạn chỉ lưu key mà không có value true:
                        // String foodId = foodIdSnapshot.getKey();
                        // if (foodId != null) { favoriteFoodIds.add(foodId); }
                    }
                }

                if (favoriteFoodIds.isEmpty()) {
                    Log.d("FavoriteActivity", "No favorite food IDs found.");
                    favoriteFoodList.clear(); // Xóa danh sách món ăn nếu không có ID
                    favoriteAdapter.notifyDataSetChanged();
                    binding.progressBarFavorite.setVisibility(View.GONE);
                    binding.emptyFavoriteTxt.setVisibility(View.VISIBLE);
                } else {
                    Log.d("FavoriteActivity", "Found " + favoriteFoodIds.size() + " favorite IDs. Fetching details...");
                    loadFavoriteFoodDetails(); // Tải chi tiết món ăn
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FavoriteActivity", "Lỗi tải ID yêu thích: ", error.toException());
                binding.progressBarFavorite.setVisibility(View.GONE);
                binding.emptyFavoriteTxt.setText("Lỗi tải dữ liệu");
                binding.emptyFavoriteTxt.setVisibility(View.VISIBLE);
                Toast.makeText(FavoriteActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Bước 2: Tải chi tiết các món ăn dựa trên danh sách ID
    private void loadFavoriteFoodDetails() {
        favoriteFoodList.clear(); // Xóa danh sách cũ
        // Nếu không còn ID nào thì ẩn progress bar và hiện empty text
        if (favoriteFoodIds.isEmpty()) {
            binding.progressBarFavorite.setVisibility(View.GONE);
            binding.emptyFavoriteTxt.setVisibility(View.VISIBLE);
            favoriteAdapter.notifyDataSetChanged(); // Cập nhật adapter với list rỗng
            return;
        }

        final int[] loadedCount = {0}; // Biến đếm số món đã tải xong

        for (String foodId : favoriteFoodIds) {
            try {
                // Tìm món ăn trong nút Foods dựa trên foodId (là key dạng String)
                // Giả sử ID trong Foods là số nguyên, bạn cần query hoặc lấy toàn bộ rồi lọc
                foodsRef.child(foodId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Foods food = snapshot.getValue(Foods.class);
                            if (food != null) {
                                favoriteFoodList.add(food);
                            } else {
                                Log.w("FavoriteActivity", "Food data is null for ID: " + foodId);
                            }
                        } else {
                            Log.w("FavoriteActivity", "Food details not found for ID: " + foodId);
                        }
                        // Kiểm tra xem đã tải xong tất cả chưa
                        loadedCount[0]++;
                        if (loadedCount[0] == favoriteFoodIds.size()) {
                            finishLoading();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FavoriteActivity", "Lỗi tải chi tiết món ăn ID " + foodId + ": ", error.toException());
                        // Kiểm tra xem đã tải xong tất cả chưa (kể cả lỗi)
                        loadedCount[0]++;
                        if (loadedCount[0] == favoriteFoodIds.size()) {
                            finishLoading();
                        }
                    }
                });

                // --- Cách khác nếu ID trong Foods là số nguyên và foodId lấy từ Favorites là số ---
                 /*
                 int id = Integer.parseInt(foodId); // Chuyển String ID thành int
                 Query foodQuery = foodsRef.orderByChild("Id").equalTo(id);
                 foodQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                     @Override
                     public void onDataChange(@NonNull DataSnapshot snapshot) {
                         if (snapshot.exists()) {
                              // Vì Id là duy nhất, nên chỉ có 1 kết quả (hoặc không có)
                             for(DataSnapshot foodSnap : snapshot.getChildren()) {
                                Foods food = foodSnap.getValue(Foods.class);
                                if (food != null) {
                                    favoriteFoodList.add(food);
                                }
                                break; // Chỉ cần lấy 1
                             }
                         } else {
                            Log.w("FavoriteActivity", "Food details not found for ID: " + foodId);
                         }
                         loadedCount[0]++;
                         if (loadedCount[0] == favoriteFoodIds.size()) {
                             finishLoading();
                         }
                     }
                     @Override
                     public void onCancelled(@NonNull DatabaseError error) { ... }
                 });
                 */

            } catch (NumberFormatException e) {
                Log.e("FavoriteActivity", "Invalid food ID format: " + foodId);
                loadedCount[0]++; // Vẫn tăng biến đếm để kết thúc vòng lặp
                if (loadedCount[0] == favoriteFoodIds.size()) {
                    finishLoading();
                }
            }
        }
    }

    // Hàm gọi khi đã tải xong tất cả chi tiết món ăn
    private void finishLoading() {
        binding.progressBarFavorite.setVisibility(View.GONE);
        if (favoriteFoodList.isEmpty()) {
            binding.emptyFavoriteTxt.setVisibility(View.VISIBLE);
        } else {
            binding.emptyFavoriteTxt.setVisibility(View.GONE);
        }
        favoriteAdapter.notifyDataSetChanged(); // Cập nhật RecyclerView
        Log.d("FavoriteActivity", "Finished loading " + favoriteFoodList.size() + " favorite food details.");
    }
}