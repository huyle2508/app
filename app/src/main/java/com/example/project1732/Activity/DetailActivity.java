package com.example.project1732.Activity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.project1732.Domain.Foods;
import com.example.project1732.Helper.ManagmentCart;
import com.example.project1732.R;
import com.example.project1732.databinding.ActivityDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import eightbitlab.com.blurview.RenderScriptBlur;

public class DetailActivity extends BaseActivity { // Kế thừa BaseActivity
    private ActivityDetailBinding binding;
    private Foods object;
    private int num = 1;
    private ManagmentCart managmentCart;
    private FirebaseAuth mAuth; // Thêm FirebaseAuth
    private DatabaseReference favoritesRef; // Tham chiếu đến nút Favorites của user
    private FirebaseUser currentUser;
    private boolean isFavorite = false; // Cờ trạng thái yêu thích

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        managmentCart = new ManagmentCart(this);
        mAuth = FirebaseAuth.getInstance(); // Khởi tạo Auth
        currentUser = mAuth.getCurrentUser(); // Lấy user hiện tại

        getBundleExtra(); // Lấy object Foods trước

        // Chỉ thiết lập Firebase nếu user đã đăng nhập và object tồn tại
        if (currentUser != null && object != null) {
            // Tham chiếu đến mục yêu thích cụ thể: /Favorites/{userId}/{foodId}
            favoritesRef = database.getReference("Favorites")
                    .child(currentUser.getUid())
                    .child(String.valueOf(object.getId())); // Food ID làm key
            checkFavoriteStatus(); // Kiểm tra trạng thái yêu thích ban đầu
        } else if (object == null) {
            Log.e("DetailActivity", "Food object is null!");
            Toast.makeText(this, "Lỗi tải chi tiết món ăn.", Toast.LENGTH_SHORT).show();
            finish(); // Đóng activity nếu không có dữ liệu món ăn
            return;
        }
        // Nếu user chưa đăng nhập, nút favorite sẽ không hoạt động (hoặc có thể ẩn đi)


        setVariable();
        setBlurEffect();
    }

    // Hàm kiểm tra trạng thái yêu thích từ Firebase
    private void checkFavoriteStatus() {
        if (favoritesRef == null) return; // Thoát nếu chưa có ref

        favoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavorite = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class)); // Kiểm tra key tồn tại và giá trị là true
                updateFavoriteButtonUI(); // Cập nhật icon nút favorite
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("DetailActivity", "Lỗi kiểm tra trạng thái yêu thích: ", error.toException());
            }
        });
    }

    // Hàm cập nhật giao diện nút yêu thích
    private void updateFavoriteButtonUI() {
        if (isFavorite) {
            binding.imageView10.setImageResource(R.drawable.favorite_red); // Icon màu đỏ
        } else {
            binding.imageView10.setImageResource(R.drawable.favorite_white); // Icon màu trắng
        }
    }

    // Hàm xử lý sự kiện nhấn nút yêu thích
    private void handleFavoriteClick() {
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để yêu thích", Toast.LENGTH_SHORT).show();
            // Có thể chuyển đến màn hình Login
            // startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (favoritesRef == null) {
            Log.e("DetailActivity", "favoritesRef is null in handleFavoriteClick");
            return;
        }


        isFavorite = !isFavorite; // Đảo trạng thái

        if (isFavorite) {
            // Thêm vào yêu thích
            favoritesRef.setValue(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(DetailActivity.this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DetailActivity.this, "Lỗi: Không thể yêu thích", Toast.LENGTH_SHORT).show();
                    isFavorite = false; // Quay lại trạng thái cũ nếu lỗi
                }
                updateFavoriteButtonUI(); // Cập nhật UI
            });
        } else {
            // Xóa khỏi yêu thích
            favoritesRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(DetailActivity.this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DetailActivity.this, "Lỗi: Không thể bỏ yêu thích", Toast.LENGTH_SHORT).show();
                    isFavorite = true; // Quay lại trạng thái cũ nếu lỗi
                }
                updateFavoriteButtonUI(); // Cập nhật UI
            });
        }
        // Cập nhật UI ngay lập tức để tạo cảm giác phản hồi nhanh
        updateFavoriteButtonUI();
    }


    private void setBlurEffect() {
        float radius = 10f;
        View decorView = getWindow().getDecorView();
        ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
        Drawable windowBackground = decorView.getBackground();

        try {
            binding.blurView.setupWith(rootView, new RenderScriptBlur(this))
                    .setFrameClearDrawable(windowBackground)
                    .setBlurRadius(radius);
            binding.blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            binding.blurView.setClipToOutline(true);

            binding.blurView2.setupWith(rootView, new RenderScriptBlur(this))
                    .setFrameClearDrawable(windowBackground)
                    .setBlurRadius(radius);
            binding.blurView2.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            binding.blurView2.setClipToOutline(true);
        } catch (Exception e) {
            Log.e("DetailActivity", "Error setting up BlurView", e);
        }
    }

    private void setVariable() {
        binding.backBtn.setOnClickListener(v -> finish());

        if (object != null) {
            Glide.with(DetailActivity.this)
                    .load(object.getImagePath())
                    .into(binding.pic);

            binding.priceTxt.setText(String.format(java.util.Locale.US, "$%.2f", object.getPrice()));
            binding.titleTxt.setText(object.getTitle());
            binding.descriptionTxt.setText(object.getDescription());
            binding.ratingTxt.setText(String.format(java.util.Locale.US, "%.1f Rating", object.getStar()));
            binding.ratingBar.setRating((float) object.getStar());
            binding.timeTxt.setText(String.format("%d min", object.getTimeValue())); // Hiển thị thời gian
            binding.totalTxt.setText(String.format(java.util.Locale.US, "$%.2f", (num * object.getPrice())));

            binding.plusBtn.setOnClickListener(v -> {
                num = num + 1;
                binding.numEdt.setText(String.valueOf(num));
                binding.totalTxt.setText(String.format(java.util.Locale.US, "$%.2f", (num * object.getPrice())));
            });

            binding.minusBtn.setOnClickListener(v -> {
                if (num > 1) {
                    num = num - 1;
                    binding.numEdt.setText(String.valueOf(num));
                    binding.totalTxt.setText(String.format(java.util.Locale.US, "$%.2f", (num * object.getPrice())));
                }
            });
            // Listener cho EditText
            binding.numEdt.setOnEditorActionListener((v, actionId, event) -> { // Thay numTxt thành numEdt
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_NULL) {
                    try {
                        int newQuantity = Integer.parseInt(binding.numEdt.getText().toString()); // Thay numTxt thành numEdt
                        if (newQuantity > 0) {
                            num = newQuantity; // Cập nhật biến num
                            updateQuantityUI(); // Cập nhật lại toàn bộ UI liên quan đến số lượng
                        } else {
                            // Nếu nhập <= 0, đặt lại giá trị hiện tại
                            binding.numEdt.setText(String.valueOf(num)); // Thay numTxt thành numEdt
                            Toast.makeText(DetailActivity.this, "Số lượng phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        // Nếu nhập không hợp lệ, đặt lại giá trị hiện tại
                        binding.numEdt.setText(String.valueOf(num)); // Thay numTxt thành numEdt
                        Toast.makeText(DetailActivity.this, "Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                    // Ẩn bàn phím và bỏ focus
                    //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    //imm.hideSoftInputFromWindow(binding.numEdt.getWindowToken(), 0); // Thay numTxt thành numEdt
                    //binding.numEdt.clearFocus(); // Thay numTxt thành numEdt
                    return true;
                }
                return false;
            });
            binding.addBtn.setOnClickListener(v -> {
                object.setNumberInCart(num);
                managmentCart.insertFood(object);
            });

            // Thêm listener cho nút favorite
            binding.imageView10.setOnClickListener(v -> handleFavoriteClick());

        } else {
            Log.e("DetailActivity", "Food object is null in setVariable!");
            // Không thể hiển thị chi tiết nếu object null
        }
    }

    private void getBundleExtra() {
        try {
            object = (Foods) getIntent().getSerializableExtra("object");
            if (object == null) {
                Log.e("DetailActivity", "Failed to get Foods object from Intent extras.");
                Toast.makeText(this, "Không thể tải dữ liệu món ăn.", Toast.LENGTH_SHORT).show();
                finish(); // Đóng activity nếu không có dữ liệu
            }
        } catch (Exception e) {
            Log.e("DetailActivity", "Error getting Serializable extra", e);
            Toast.makeText(this, "Lỗi tải dữ liệu.", Toast.LENGTH_SHORT).show();
            finish();
        }

    }
    private void updateQuantityUI() {
        if (object == null) return;
        binding.numEdt.setText(String.valueOf(num)); // Cập nhật EditText
        binding.totalTxt.setText(String.format(java.util.Locale.US, "$%.2f", (num * object.getPrice())));
        // Cần di chuyển con trỏ đến cuối khi cập nhật EditText bằng code
        binding.numEdt.setSelection(binding.numEdt.getText().length());
    }
}