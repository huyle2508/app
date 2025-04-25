package com.example.project1732.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project1732.Adapter.BestFoodAdapter;
import com.example.project1732.Adapter.CategoryAdapter;
import com.example.project1732.Adapter.ListFoodAdapter;
import com.example.project1732.Domain.Category;
import com.example.project1732.Domain.Foods;
import com.example.project1732.Domain.Location;
import com.example.project1732.Domain.Price;
import com.example.project1732.Domain.Time;
import com.example.project1732.Domain.User;
import com.example.project1732.R;
import com.example.project1732.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.jakewharton.rxbinding4.widget.RxTextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    // --- Biến cho Live Search ---
    private RecyclerView searchResultsRecyclerView;
    private ListFoodAdapter searchAdapter;
    private ArrayList<Foods> searchResultList = new ArrayList<>();
    private ArrayList<Foods> allFoodsList = new ArrayList<>(); // Lưu trữ toàn bộ danh sách Foods để lọc
    private ValueEventListener allFoodsListener; // Listener để lấy toàn bộ foods
    private DatabaseReference foodsRef; // Tham chiếu đến nút Foods
    private final CompositeDisposable compositeDisposable = new CompositeDisposable(); // Để quản lý RxJava
    // --- Hết biến cho Live Search ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        foodsRef = database.getReference("Foods"); // Khởi tạo tham chiếu Foods

        if (mAuth.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        // --- Khởi tạo cho Live Search ---
        // Giả sử bạn đã thêm RecyclerView với id searchResultsRecyclerView vào activity_main.xml
        searchResultsRecyclerView = binding.searchResultsRecyclerView; // Thay R.id.searchResultsRecyclerView nếu dùng findViewById
        setupSearchRecyclerView();
        preloadAllFoods(); // Tải trước toàn bộ dữ liệu Foods
        setupLiveSearch(); // Thiết lập lắng nghe EditText
        // --- Hết Khởi tạo cho Live Search ---


        loadUserInfo();
        initLocation();
        initTime();
        initPrice();
        initBestFood();
        initCategory();
        setVariable();
        setupSpinnerListeners();
    }

    // --- Các hàm cho Live Search ---

    private void setupSearchRecyclerView() {
        searchAdapter = new ListFoodAdapter(searchResultList);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(searchAdapter);
        // Ban đầu ẩn đi
        searchResultsRecyclerView.setVisibility(View.GONE);
    }

    // Hàm tải trước toàn bộ dữ liệu Foods một lần (CẢNH BÁO HIỆU NĂNG!)
    private void preloadAllFoods() {
        Log.d("MainActivity", "Preloading all food data...");
        binding.progressBarBestFood.setVisibility(View.VISIBLE); // Hiện loading tạm

        // Sử dụng ValueEventListener để nó tự cập nhật nếu dữ liệu thay đổi
        // Nếu chỉ cần tải 1 lần, dùng addListenerForSingleValueEvent
        allFoodsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allFoodsList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        Foods food = issue.getValue(Foods.class);
                        if (food != null) {
                            allFoodsList.add(food);
                        }
                    }
                    Log.d("MainActivity", "Preloaded " + allFoodsList.size() + " food items.");
                } else {
                    Log.d("MainActivity", "No food data found during preload.");
                }
                // Chỉ ẩn ProgressBar chính khi preload xong
                // binding.progressBarBestFood.setVisibility(View.GONE); // Không ẩn ở đây nữa
                // Nếu đang có query thì thực hiện lại tìm kiếm với dữ liệu mới
                String currentQuery = binding.searchEdt.getText().toString().trim();
                if (!currentQuery.isEmpty()) {
                    performLiveSearch(currentQuery);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to preload food data: ", error.toException());
                binding.progressBarBestFood.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Lỗi tải dữ liệu món ăn", Toast.LENGTH_SHORT).show();
            }
        };
        foodsRef.addValueEventListener(allFoodsListener); // Lắng nghe liên tục
    }


    private void setupLiveSearch() {
        compositeDisposable.add(
                RxTextView.textChanges(binding.searchEdt)
                        .skipInitialValue()
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .map(CharSequence::toString)
                        .map(String::trim)
                        .distinctUntilChanged()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(query -> {
                            if (query.isEmpty()) {
                                searchResultsRecyclerView.setVisibility(View.GONE);
                                searchResultList.clear();
                                searchAdapter.notifyDataSetChanged();
                                // Hiện lại các view khác nếu cần
                                binding.bestFoodView.setVisibility(View.VISIBLE);
                                binding.categoryView.setVisibility(View.VISIBLE);
                                binding.textView3.setVisibility(View.VISIBLE); // Title Best Foods
                                binding.textView4.setVisibility(View.VISIBLE); // VIEW ALL Best Foods
                                binding.textView23.setVisibility(View.VISIBLE); // Title Category
                                // Ẩn loading (nếu có)
                                binding.progressBarBestFood.setVisibility(View.GONE);
                            } else {
                                // Ẩn các view không liên quan đến kết quả search
                                binding.bestFoodView.setVisibility(View.GONE);
                                binding.categoryView.setVisibility(View.GONE);
                                binding.textView3.setVisibility(View.GONE);
                                binding.textView4.setVisibility(View.GONE);
                                binding.textView23.setVisibility(View.GONE);
                                performLiveSearch(query);
                            }
                        }, throwable -> {
                            Log.e("MainActivity", "Lỗi RxTextView: ", throwable);
                        })
        );
    }

    // Hàm thực hiện lọc trên danh sách allFoodsList đã tải trước
    private void performLiveSearch(String query) {
        Log.d("MainActivity", "Performing client-side filter for: " + query);
        searchResultList.clear(); // Xóa kết quả cũ

        if (allFoodsList.isEmpty()) {
            Log.w("MainActivity", "allFoodsList is empty, cannot perform search yet.");
            binding.progressBarBestFood.setVisibility(View.VISIBLE); // Có thể hiện loading nếu chưa preload xong
            searchResultsRecyclerView.setVisibility(View.GONE);
            searchAdapter.notifyDataSetChanged();
            return; // Thoát nếu chưa có dữ liệu để lọc
        }

        binding.progressBarBestFood.setVisibility(View.GONE); // Đã có dữ liệu, ẩn loading

        String lowerCaseQuery = query.toLowerCase();

        for (Foods food : allFoodsList) {
            if (food.getTitle() != null && food.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                searchResultList.add(food);
            }
        }

        Log.d("MainActivity", "Found " + searchResultList.size() + " results containing '" + query + "'");

        if (searchResultList.isEmpty()) {
            searchResultsRecyclerView.setVisibility(View.GONE);
            // Hiển thị thông báo "Không tìm thấy" nếu cần
        } else {
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        }
        searchAdapter.notifyDataSetChanged(); // Cập nhật RecyclerView kết quả tìm kiếm
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear(); // Hủy tất cả Subscriptions của RxJava
        // Gỡ bỏ listener của Firebase nếu dùng addValueEventListener
        if (foodsRef != null && allFoodsListener != null) {
            foodsRef.removeEventListener(allFoodsListener);
        }
    }

    // --- Hết Các hàm cho Live Search ---


    private void loadUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference userRef = database.getReference("Users").child(uid);

            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                            binding.texUser.setText(user.getDisplayName());
                        } else if (currentUser.getEmail() != null) {
                            binding.texUser.setText(currentUser.getEmail().split("@")[0]);
                        } else {
                            binding.texUser.setText("User");
                        }
                    } else {
                        Log.w("MainActivity", "User data not found in Database for UID: " + uid);
                        if (currentUser.getEmail() != null) {
                            binding.texUser.setText(currentUser.getEmail().split("@")[0]);
                        } else {
                            binding.texUser.setText("User");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("MainActivity", "Failed to load user data.", error.toException());
                    if (currentUser.getEmail() != null) {
                        binding.texUser.setText(currentUser.getEmail().split("@")[0]);
                    } else {
                        binding.texUser.setText("User");
                    }
                }
            });
        } else {
            navigateToLogin();
        }
    }


    private void setVariable() {
        binding.cartBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CartActivity.class)));

        // Bỏ OnClickListener của searchBtn vì dùng live search
        // binding.searchBtn.setOnClickListener(v -> { ... });

        binding.imageLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(MainActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        });

        binding.imageView2.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        binding.textView4.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ListFoodActivity.class);
            intent.putExtra("isSearch", false);
            intent.putExtra("CategoryName", "Tất cả món ăn");
            startActivity(intent);
        });
    }

    private void setupSpinnerListeners() {
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Tạm thời chưa xử lý lọc bằng Spinner khi đang dùng live search
                // Bạn có thể kết hợp logic nếu muốn, nhưng sẽ phức tạp hơn
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        binding.locationSp.setOnItemSelectedListener(spinnerListener);
        binding.timeSp.setOnItemSelectedListener(spinnerListener);
        binding.priceSp.setOnItemSelectedListener(spinnerListener);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // --- Giữ nguyên các hàm init... ---
    private void initCategory() {
        DatabaseReference myref = database.getReference("Category");
        binding.progressBarCategory.setVisibility(View.VISIBLE);
        ArrayList<Category> list = new ArrayList<>();

        myref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot issu : snapshot.getChildren()) {
                        list.add(issu.getValue(Category.class));
                    }
                    if (!list.isEmpty()) {
                        binding.categoryView.setLayoutManager(new GridLayoutManager(MainActivity.this, 4));
                        CategoryAdapter adapterCategory = new CategoryAdapter(list);
                        binding.categoryView.setAdapter(adapterCategory);
                    }
                } else {
                    binding.categoryView.setAdapter(null); // Hoặc hiển thị thông báo rỗng
                }
                binding.progressBarCategory.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to load categories.", error.toException());
                binding.progressBarCategory.setVisibility(View.GONE);
            }
        });
    }

    private void initBestFood() {
        DatabaseReference myref = database.getReference("Foods");
        binding.progressBarBestFood.setVisibility(View.VISIBLE);
        ArrayList<Foods> list = new ArrayList<>();
        Query query = myref.orderByChild("BestFood").equalTo(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot issu : snapshot.getChildren()) {
                        list.add(issu.getValue(Foods.class));
                    }
                    if (!list.isEmpty()) {
                        binding.bestFoodView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        BestFoodAdapter adapterBestFood = new BestFoodAdapter(list);
                        binding.bestFoodView.setAdapter(adapterBestFood);
                    } else {
                        binding.bestFoodView.setAdapter(null); // Hoặc hiển thị thông báo rỗng
                    }
                } else {
                    binding.bestFoodView.setAdapter(null); // Hoặc hiển thị thông báo rỗng
                }
                // Chỉ ẩn progress bar chính nếu không có tìm kiếm nào đang diễn ra
                if (binding.searchEdt.getText().toString().trim().isEmpty()) {
                    binding.progressBarBestFood.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to load best foods.", error.toException());
                binding.progressBarBestFood.setVisibility(View.GONE);
            }
        });
    }

    private void initLocation() {
        DatabaseReference myRef = database.getReference("Location");
        ArrayList<Location> list = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot issue : dataSnapshot.getChildren()) {
                        list.add(issue.getValue(Location.class));
                    }
                    ArrayAdapter<Location> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, list);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.locationSp.setAdapter(adapter);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to load locations.", databaseError.toException());
            }
        });
    }

    private void initTime() {
        DatabaseReference myRef = database.getReference("Time");
        ArrayList<Time> list = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot issue : dataSnapshot.getChildren()) {
                        list.add(issue.getValue(Time.class));
                    }
                    ArrayAdapter<Time> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, list);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.timeSp.setAdapter(adapter);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to load times.", databaseError.toException());
            }
        });
    }

    private void initPrice() {
        DatabaseReference myRef = database.getReference("Price");
        ArrayList<Price> list = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot issue : dataSnapshot.getChildren()) {
                        list.add(issue.getValue(Price.class));
                    }
                    ArrayAdapter<Price> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, list);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.priceSp.setAdapter(adapter);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to load prices.", databaseError.toException());
            }
        });
    }
}