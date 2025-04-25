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

    // --- Adapters ---
    private RecyclerView searchResultsRecyclerView;
    private ListFoodAdapter searchAdapter;          // Adapter cho kết quả tìm kiếm + lọc
    private BestFoodAdapter bestFoodAdapter;        // Adapter gốc cho Best Foods
    private CategoryAdapter categoryAdapter;        // Adapter cho Category

    // --- Data Lists ---
    private ArrayList<Foods> searchResultList = new ArrayList<>();      // Danh sách kết quả cuối cùng (search + filter)
    private ArrayList<Foods> allFoodsList = new ArrayList<>();          // Lưu trữ toàn bộ danh sách Foods (QUAN TRỌNG)
    private ArrayList<Foods> bestFoodOriginalList = new ArrayList<>();  // Lưu trữ danh sách Best Foods gốc
    private ArrayList<Category> categoryList = new ArrayList<>();       // Danh sách Category

    // --- Firebase & RxJava ---
    private ValueEventListener allFoodsListener; // Listener để lấy toàn bộ foods
    private DatabaseReference foodsRef;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // --- Filter State ---
    private Time selectedTime = null;
    private Price selectedPrice = null;
    private Location selectedLocation = null;
    // --- End Variables ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        foodsRef = database.getReference("Foods");

        if (mAuth.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        // --- Initialization ---
        setupRecyclerViews();       // Thiết lập RecyclerViews
        setupLiveSearch();          // Thiết lập lắng nghe EditText (QUAN TRỌNG: đặt trước preload)

        preloadAllFoods();          // Tải trước TOÀN BỘ dữ liệu Foods

        loadUserInfo();
        initLocation();             // Tải dữ liệu cho Spinner Location
        initTime();                 // Tải dữ liệu cho Spinner Time
        initPrice();                // Tải dữ liệu cho Spinner Price
        // initBestFood(); // Không cần gọi riêng vì đã lọc trong preloadAllFoods
        initCategory();             // Tải dữ liệu Category
        setVariable();              // Thiết lập các nút bấm khác
        setupSpinnerListeners();    // Thiết lập listener cho Spinner
    }

    // --- Setup RecyclerViews ---
    private void setupRecyclerViews() {
        // Search Results RecyclerView (dùng ListFoodAdapter, layout lưới)
        searchResultsRecyclerView = binding.searchResultsRecyclerView;
        // Khởi tạo adapter với list rỗng, dùng hàm updateData sau
        searchAdapter = new ListFoodAdapter(new ArrayList<>());
        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        searchResultsRecyclerView.setAdapter(searchAdapter);
        searchResultsRecyclerView.setVisibility(View.GONE); // Ban đầu ẩn

        // Best Food RecyclerView (dùng BestFoodAdapter)
        // Khởi tạo adapter với list rỗng, dùng hàm updateData sau
        bestFoodAdapter = new BestFoodAdapter(new ArrayList<>());
        binding.bestFoodView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        binding.bestFoodView.setAdapter(bestFoodAdapter);
        binding.bestFoodView.setVisibility(View.VISIBLE); // Ban đầu hiện

        // Category RecyclerView (dùng CategoryAdapter)
        // Khởi tạo adapter với list rỗng, dùng hàm updateData sau
        categoryAdapter = new CategoryAdapter(new ArrayList<>());
        binding.categoryView.setLayoutManager(new GridLayoutManager(MainActivity.this, 4));
        binding.categoryView.setAdapter(categoryAdapter);
        binding.categoryView.setVisibility(View.VISIBLE); // Ban đầu hiện
    }

    // Hàm tải trước toàn bộ dữ liệu Foods (Giữ lại vì dùng tìm kiếm client-side)
    private void preloadAllFoods() {
        Log.d("MainActivity", "Đang tải trước toàn bộ dữ liệu món ăn...");
        showLoading(true);

        allFoodsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allFoodsList.clear();
                bestFoodOriginalList.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        Foods food = issue.getValue(Foods.class);
                        if (food != null) {
                            allFoodsList.add(food);
                            if (food.isBestFood()) {
                                bestFoodOriginalList.add(food);
                            }
                        }
                    }
                    Log.d("MainActivity", "Đã tải trước " + allFoodsList.size() + " món ăn.");
                    // Cập nhật Best Foods ban đầu NẾU ô tìm kiếm đang trống
                    if (binding.searchEdt.getText().toString().isEmpty()) {
                        updateBestFoodDisplay(bestFoodOriginalList);
                    }
                } else {
                    Log.d("MainActivity", "Không tìm thấy dữ liệu món ăn khi tải trước.");
                    updateBestFoodDisplay(new ArrayList<>()); // Cập nhật với list rỗng
                }
                showLoading(false);

                // Nếu đang có query -> chạy lại tìm kiếm VÀ lọc
                String currentQuery = binding.searchEdt.getText().toString().trim();
                if (!currentQuery.isEmpty()) {
                    performLiveSearchAndFilter(currentQuery);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Lỗi tải trước dữ liệu món ăn: ", error.toException());
                showLoading(false);
                updateBestFoodDisplay(new ArrayList<>()); // Cập nhật với list rỗng
                Toast.makeText(MainActivity.this, "Lỗi tải dữ liệu món ăn", Toast.LENGTH_SHORT).show();
            }
        };
        foodsRef.addValueEventListener(allFoodsListener); // Dùng addValueEventListener
    }

    // Thiết lập lắng nghe EditText dùng RxBinding
    private void setupLiveSearch() {
        compositeDisposable.add(
                RxTextView.textChanges(binding.searchEdt)
                        .skipInitialValue()
                        .debounce(400, TimeUnit.MILLISECONDS)
                        .map(CharSequence::toString)
                        .map(String::trim)
                        .distinctUntilChanged()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(query -> {
                            if (query.isEmpty()) {
                                showOriginalViews(); // Hiển thị lại view gốc
                                updateBestFoodDisplay(bestFoodOriginalList); // Hiển thị lại best foods gốc
                            } else {
                                hideOriginalViews(); // Ẩn các view gốc
                                performLiveSearchAndFilter(query); // Thực hiện tìm kiếm VÀ lọc
                            }
                        }, throwable -> {
                            Log.e("MainActivity", "Lỗi RxTextView: ", throwable);
                        })
        );
    }

    // Hàm thực hiện TÌM KIẾM client-side VÀ LỌC theo Spinner
    private void performLiveSearchAndFilter(String query) {
        Log.d("SearchFilter", "Bắt đầu tìm kiếm client-side và lọc cho: '" + query + "'");
        searchResultList.clear(); // Xóa kết quả cũ
        showLoading(true);

        // 1. Tìm kiếm theo từ khóa (client-side trên allFoodsList)
        ArrayList<Foods> textSearchResults = new ArrayList<>();
        if (!allFoodsList.isEmpty()) {
            String lowerCaseQuery = query.toLowerCase();
            for (Foods food : allFoodsList) {
                if (food.getTitle() != null && food.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                    textSearchResults.add(food);
                }
            }
            Log.d("SearchFilter", "Tìm thấy " + textSearchResults.size() + " kết quả theo từ khóa (client-side).");
        } else {
            Log.w("SearchFilter", "allFoodsList trống, không thể tìm kiếm client-side.");
            // Không cần return, vì applyClientSideFilters sẽ xử lý list rỗng
        }

        // 2. Áp dụng bộ lọc Spinner cho kết quả tìm kiếm (textSearchResults)
        applyClientSideFilters(textSearchResults); // Lọc trên textSearchResults, kết quả cuối cùng vào searchResultList

        showLoading(false); // Ẩn loading

        // 3. Cập nhật UI cho phần tìm kiếm
        updateSearchDisplay(searchResultList);
    }

    // Hàm áp dụng bộ lọc Spinner client-side
    private void applyClientSideFilters(ArrayList<Foods> inputList) {
        searchResultList.clear(); // Xóa kết quả cuối cùng trước khi lọc lại

        // Lấy ID bộ lọc, mặc định là -1 nếu không chọn hoặc chọn "Tất cả"
        int timeFilterId = (selectedTime != null && selectedTime.getId() != 3) ? selectedTime.getId() : -1;
        int priceFilterId = (selectedPrice != null && selectedPrice.getId() != 3) ? selectedPrice.getId() : -1;
        int locationFilterId = (selectedLocation != null && selectedLocation.getId() != -1) ? selectedLocation.getId() : -1; // ID -1 cho "Tất cả vị trí"

        Log.d("ClientFilter", "Áp dụng bộ lọc client-side: Time=" + timeFilterId + ", Price=" + priceFilterId + ", Location=" + locationFilterId);

        if (timeFilterId == -1 && priceFilterId == -1 && locationFilterId == -1) {
            // Nếu không có bộ lọc nào -> lấy tất cả kết quả từ bước tìm kiếm text
            searchResultList.addAll(inputList);
        } else {
            // Nếu có bộ lọc -> lọc thêm
            for (Foods food : inputList) {
                boolean timeMatch = (timeFilterId == -1) || (food.getTimeId() == timeFilterId);
                boolean priceMatch = (priceFilterId == -1) || (food.getPriceId() == priceFilterId);
                boolean locationMatch = (locationFilterId == -1) || (food.getLocationId() == locationFilterId);

                if (timeMatch && priceMatch && locationMatch) {
                    searchResultList.add(food); // Thêm món ăn nếu khớp tất cả bộ lọc đang chọn
                }
            }
        }
        Log.d("ClientFilter", "Kết quả sau khi lọc client-side: " + searchResultList.size());
    }


    // Thiết lập Listener cho Spinner
    private void setupSpinnerListeners() {
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean filterChanged = false;
                int parentId = parent.getId();

                // Cập nhật trạng thái bộ lọc đã chọn
                if (parentId == R.id.timeSp) {
                    Time newlySelectedTime = (Time) parent.getItemAtPosition(position);
                    if (selectedTime == null || selectedTime.getId() != newlySelectedTime.getId()) {
                        selectedTime = newlySelectedTime; filterChanged = true;
                    }
                } else if (parentId == R.id.priceSp) {
                    Price newlySelectedPrice = (Price) parent.getItemAtPosition(position);
                    if (selectedPrice == null || selectedPrice.getId() != newlySelectedPrice.getId()){
                        selectedPrice = newlySelectedPrice; filterChanged = true;
                    }
                } else if (parentId == R.id.locationSp) {
                    Location newlySelectedLocation = (Location) parent.getItemAtPosition(position);
                    if (selectedLocation == null || selectedLocation.getId() != newlySelectedLocation.getId()) {
                        selectedLocation = newlySelectedLocation; filterChanged = true;
                    }
                }

                // QUAN TRỌNG: Chỉ chạy lại tìm kiếm VÀ lọc nếu bộ lọc thay đổi VÀ đang có tìm kiếm
                if (filterChanged && !binding.searchEdt.getText().toString().trim().isEmpty()) {
                    Log.d("SpinnerListener", "Bộ lọc thay đổi, đang có tìm kiếm. Chạy lại performLiveSearchAndFilter.");
                    performLiveSearchAndFilter(binding.searchEdt.getText().toString().trim());
                } else {
                    Log.d("SpinnerListener", "Bộ lọc thay đổi nhưng ô tìm kiếm trống hoặc bộ lọc không đổi.");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { /* Không cần xử lý */ }
        };

        binding.locationSp.setOnItemSelectedListener(spinnerListener);
        binding.timeSp.setOnItemSelectedListener(spinnerListener);
        binding.priceSp.setOnItemSelectedListener(spinnerListener);
    }


    // --- Các hàm tiện ích và hàm init còn lại ---

    // Cập nhật RecyclerView Best Food
    private void updateBestFoodDisplay(ArrayList<Foods> foodsToShow) {
        if (bestFoodAdapter != null) {
            // Kiểm tra xem adapter có phương thức updateData không
            // Nếu không, bạn phải làm như bên dưới hoặc thêm phương thức đó vào adapter
            // bestFoodAdapter.items.clear(); // Cần đảm bảo 'items' là public hoặc có setter
            // bestFoodAdapter.items.addAll(foodsToShow);
            // bestFoodAdapter.notifyDataSetChanged();
            bestFoodAdapter.updateData(foodsToShow); // Giả sử đã thêm hàm này vào BestFoodAdapter
        } else {
            // Khởi tạo nếu chưa có (ít xảy ra nếu setupRecyclerViews đúng)
            bestFoodAdapter = new BestFoodAdapter(foodsToShow);
            binding.bestFoodView.setAdapter(bestFoodAdapter);
        }
        binding.bestFoodView.setVisibility(foodsToShow.isEmpty() ? View.GONE : View.VISIBLE);
        Log.d("UIUpdate", "updateBestFoodDisplay - Visible: " + (foodsToShow.isEmpty() ? "GONE" : "VISIBLE"));

    }

    // Cập nhật RecyclerView Category
    private void updateCategoryDisplay(ArrayList<Category> categoriesToShow) {
        if (categoryAdapter != null) {
            // Tương tự BestFoodAdapter
            categoryAdapter.updateData(categoriesToShow); // Giả sử đã thêm hàm này vào CategoryAdapter
        } else {
            categoryAdapter = new CategoryAdapter(categoriesToShow);
            binding.categoryView.setAdapter(categoryAdapter);
        }
        binding.categoryView.setVisibility(categoriesToShow.isEmpty() ? View.GONE : View.VISIBLE);
        binding.textView23.setVisibility(categoriesToShow.isEmpty() ? View.GONE : View.VISIBLE); // Cập nhật cả tiêu đề
        Log.d("UIUpdate", "updateCategoryDisplay - Visible: " + (categoriesToShow.isEmpty() ? "GONE" : "VISIBLE"));
    }

    // Cập nhật RecyclerView kết quả tìm kiếm
    private void updateSearchDisplay(ArrayList<Foods> foodsToShow) {
        if (searchAdapter != null) {
            searchAdapter.updateData(foodsToShow); // Giả sử đã thêm hàm này vào ListFoodAdapter
        } else {
            searchAdapter = new ListFoodAdapter(foodsToShow);
            searchResultsRecyclerView.setAdapter(searchAdapter);
        }

        if (foodsToShow.isEmpty()) {
            binding.emptyResultText.setText("Không có kết quả phù hợp.");
            binding.emptyResultText.setVisibility(View.VISIBLE);
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyResultText.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        }
        Log.d("UIUpdate", "updateSearchDisplay - Visible: " + (foodsToShow.isEmpty() ? "GONE" : "VISIBLE"));
    }


    // Hiển thị lại giao diện gốc
    private void showOriginalViews() {
        Log.d("UIUpdate", "showOriginalViews called");
        searchResultsRecyclerView.setVisibility(View.GONE);
        binding.emptyResultText.setVisibility(View.GONE);

        // Hiển thị lại Best Food và Category (visibility dựa trên data đã load)
        updateBestFoodDisplay(bestFoodOriginalList);
        updateCategoryDisplay(categoryList);

        // Hiển thị lại các tiêu đề nếu RecyclerView tương ứng hiển thị
        binding.textView3.setVisibility(binding.bestFoodView.getVisibility());
        binding.textView4.setVisibility(binding.bestFoodView.getVisibility());
        binding.textView23.setVisibility(binding.categoryView.getVisibility());
    }

    // Ẩn các view gốc khi tìm kiếm
    private void hideOriginalViews() {
        Log.d("UIUpdate", "hideOriginalViews called");
        binding.bestFoodView.setVisibility(View.GONE);
        binding.categoryView.setVisibility(View.GONE);
        binding.textView3.setVisibility(View.GONE);
        binding.textView4.setVisibility(View.GONE);
        binding.textView23.setVisibility(View.GONE);
        // Không ẩn emptyResultText ở đây, để performLiveSearchAndFilter quyết định
    }

    // Hiển thị/Ẩn loading
    private void showLoading(boolean show) {
        // Nên dùng ProgressBar riêng cho từng phần hoặc 1 cái chung ở giữa
        binding.progressBarBestFood.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.progressBarCategory.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            searchResultsRecyclerView.setVisibility(View.GONE); // Ẩn kết quả khi load
            binding.bestFoodView.setVisibility(View.GONE);      // Ẩn best food khi load
            binding.categoryView.setVisibility(View.GONE);      // Ẩn category khi load
            binding.emptyResultText.setVisibility(View.GONE);   // Ẩn thông báo lỗi khi load
        }
    }

    // loadUserInfo, setVariable, navigateToLogin giữ nguyên như trước
    private void loadUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference userRef = database.getReference("Users").child(uid);
            userRef.addValueEventListener(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) { /* ... như cũ ... */
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                            binding.texUser.setText(user.getDisplayName());
                        } else if (currentUser.getEmail() != null) {
                            binding.texUser.setText(currentUser.getEmail().split("@")[0]);
                        } else { binding.texUser.setText("User"); }
                    } else {
                        if (currentUser.getEmail() != null) {
                            binding.texUser.setText(currentUser.getEmail().split("@")[0]);
                        } else { binding.texUser.setText("User"); }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { /* ... như cũ ... */
                    Log.e("MainActivity", "Failed to load user data.", error.toException());
                    if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) { // Re-check null
                        binding.texUser.setText(mAuth.getCurrentUser().getEmail().split("@")[0]);
                    } else { binding.texUser.setText("User"); }
                }
            });
        } else { navigateToLogin(); }
    }

    private void setVariable() {
        binding.cartBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CartActivity.class)));
        binding.imageLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(MainActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        });
        binding.imageView2.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
        binding.textView4.setOnClickListener(v -> { // VIEW ALL (Best Foods)
            Intent intent = new Intent(MainActivity.this, ListFoodActivity.class);
            intent.putExtra("isBestFood", true);
            intent.putExtra("CategoryName", "Món ngon nhất");
            startActivity(intent);
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // initBestFood giờ không cần thiết vì đã xử lý trong preloadAllFoods
    private void initBestFood() {
        // Dữ liệu bestFoodOriginalList được lọc trong preloadAllFoods
        // Hàm updateBestFoodDisplay sẽ được gọi từ preloadAllFoods hoặc showOriginalViews
        Log.d("MainActivity", "initBestFood (chỉ đảm bảo UI được cập nhật nếu cần)");
        if (binding.searchEdt.getText().toString().isEmpty()) {
            updateBestFoodDisplay(bestFoodOriginalList); // Cập nhật lạiเผื่อ preload chạy trước khi view sẵn sàng
        }
    }

    // initCategory chỉ tải data và gọi update display
    private void initCategory() {
        DatabaseReference myref = database.getReference("Category");
        binding.progressBarCategory.setVisibility(View.VISIBLE);
        myref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot issu : snapshot.getChildren()) {
                        Category cat = issu.getValue(Category.class);
                        if (cat != null) categoryList.add(cat);
                    }
                }
                updateCategoryDisplay(categoryList); // Cập nhật adapter và visibility
                binding.progressBarCategory.setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to load categories.", error.toException());
                binding.progressBarCategory.setVisibility(View.GONE);
                updateCategoryDisplay(new ArrayList<>()); // Cập nhật với list rỗng
            }
        });
    }

    // initLocation, initTime, initPrice giữ nguyên như trước, thêm mục "Tất cả"
    private void initLocation() {
        DatabaseReference myRef = database.getReference("Location");
        ArrayList<Location> list = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list.clear();
                Location allLoc = new Location(); allLoc.setId(-1); allLoc.setLoc("Tất cả Vị trí"); list.add(allLoc);
                if (dataSnapshot.exists()) {
                    for (DataSnapshot issue : dataSnapshot.getChildren()) {
                        Location loc = issue.getValue(Location.class);
                        if (loc != null && loc.getLoc() != null && !loc.getLoc().trim().isEmpty()) { list.add(loc); }
                    }
                }
                setupSpinnerAdapter(binding.locationSp, list);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { Log.e("SpinnerInit", "Location Error: " + e.getMessage()); setupSpinnerAdapter(binding.locationSp, list); } // Vẫn setup với list (có thể chỉ có "All")
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
                    for (DataSnapshot issue : dataSnapshot.getChildren()) { Time time = issue.getValue(Time.class); if(time != null) list.add(time); }
                    list.sort((t1, t2) -> t1.getId() == 3 ? -1 : (t2.getId() == 3 ? 1 : 0)); // Đẩy ID 3 (All) lên đầu
                }
                // Nếu không có ID 3 từ DB, thêm thủ công
                if (list.isEmpty() || list.get(0).getId() != 3) {
                    Time allTime = new Time(); allTime.setId(3); allTime.setValue("Tất cả Thời gian"); list.add(0, allTime); // Giả sử ID 3 là All
                }
                setupSpinnerAdapter(binding.timeSp, list);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { Log.e("SpinnerInit", "Time Error: " + e.getMessage()); setupSpinnerAdapter(binding.timeSp, list); }
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
                    for (DataSnapshot issue : dataSnapshot.getChildren()) { Price price = issue.getValue(Price.class); if(price != null) list.add(price); }
                    list.sort((p1, p2) -> p1.getId() == 3 ? -1 : (p2.getId() == 3 ? 1 : 0)); // Đẩy ID 3 (All) lên đầu
                }
                // Nếu không có ID 3 từ DB, thêm thủ công
                if (list.isEmpty() || list.get(0).getId() != 3) {
                    Price allPrice = new Price(); allPrice.setId(3); allPrice.setValue("Tất cả Giá"); list.add(0, allPrice); // Giả sử ID 3 là All
                }
                setupSpinnerAdapter(binding.priceSp, list);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { Log.e("SpinnerInit", "Price Error: " + e.getMessage()); setupSpinnerAdapter(binding.priceSp, list); }
        });
    }

    // Hàm tiện ích để setup adapter cho Spinner
    private <T> void setupSpinnerAdapter(android.widget.Spinner spinner, ArrayList<T> list) {
        ArrayAdapter<T> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0, false); // Chọn mục đầu tiên (mong muốn là "All") mà không trigger listener lần đầu
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
        if (foodsRef != null && allFoodsListener != null) {
            foodsRef.removeEventListener(allFoodsListener); // Gỡ listener khi hủy Activity
        }
    }
}