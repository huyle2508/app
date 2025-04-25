package com.example.project1732.Activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project1732.Adapter.CartAdapter;
import com.example.project1732.Domain.Foods;
import com.example.project1732.Domain.Order;
import com.example.project1732.Domain.User;
import com.example.project1732.Helper.ManagmentCart;
import com.example.project1732.databinding.ActivityCartBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

import eightbitlab.com.blurview.RenderScriptBlur;

public class CartActivity extends BaseActivity {
    private ActivityCartBinding binding;
    private CartAdapter adapter;
    private ManagmentCart managmentCart;
    private double tax;
    private FirebaseAuth mAuth;
    private String userAddress = "";
    private String userPhone = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        managmentCart = new ManagmentCart(this);

        fetchUserInfo();
        setVariable();
        initList();
        calculateCart();
        setBlurEffect();
    }

    // Hàm lấy thông tin địa chỉ, SĐT từ Firebase
    private void fetchUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = database.getReference("Users").child(currentUser.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            userAddress = (user.getAddress() != null && !user.getAddress().isEmpty()) ? user.getAddress() : "";
                            userPhone = (user.getPhone() != null && !user.getPhone().isEmpty()) ? user.getPhone() : "";
                            Log.d("CartActivity", "User info fetched: Address=" + userAddress + ", Phone=" + userPhone);
                        }
                    } else {
                        Log.w("CartActivity", "User data not found for UID: " + currentUser.getUid());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("CartActivity", "Failed to fetch user info: ", error.toException());
                }
            });
        }
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
            Log.e("CartActivity", "Error setting up BlurView", e);
        }
    }

    private void initList() {
        binding.cartView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new CartAdapter(managmentCart.getListCart(), CartActivity.this, this::calculateCart);
        binding.cartView.setAdapter(adapter);

    }

    private void setVariable() {
        binding.backBtn.setOnClickListener(v -> finish());
        binding.button2.setOnClickListener(v -> placeOrder()); // Gọi hàm đặt hàng
        binding.button.setOnClickListener(v -> applyCoupon()); // Gọi hàm áp dụng coupon
    }

    private void applyCoupon() {
        String couponCode = binding.editTextText.getText().toString().trim();
        if (!couponCode.isEmpty()) {
            Toast.makeText(CartActivity.this, "Đã áp dụng mã: " + couponCode, Toast.LENGTH_SHORT).show();
            // TODO: Thêm logic xử lý mã giảm giá thực tế và gọi lại calculateCart()
            calculateCart(); // Tính lại tổng tiền sau khi áp dụng coupon (nếu có)
        } else {
            Toast.makeText(CartActivity.this, "Vui lòng nhập mã giảm giá", Toast.LENGTH_SHORT).show();
        }
    }

    private void placeOrder() {
        // 1. Kiểm tra đăng nhập
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        // 2. Kiểm tra giỏ hàng
        ArrayList<Foods> cartItems = managmentCart.getListCart();
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng của bạn đang trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Kiểm tra địa chỉ và SĐT
        if (userAddress.isEmpty() || userPhone.isEmpty()) {
            Toast.makeText(this, "Chưa có thông tin địa chỉ/SĐT. Vui lòng cập nhật hồ sơ.", Toast.LENGTH_LONG).show();
            fetchUserInfo();
            return;
        }

        // 4. Lấy thông tin cần thiết
        String userId = currentUser.getUid();
        long timestamp = System.currentTimeMillis();
        double finalTotalAmount = calculateFinalTotal();
        String paymentMethod = "COD"; // <-- Đặt phương thức thanh toán là COD
        String initialStatus = "Pending"; // Trạng thái ban đầu

        // 5. Tạo đối tượng Order
        DatabaseReference ordersRef = database.getReference("Orders").child(userId);
        String orderId = ordersRef.push().getKey();

        if (orderId == null) {
            Toast.makeText(this, "Không thể tạo mã đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo đối tượng Order với phương thức thanh toán
        Order newOrder = new Order(orderId, userId, timestamp, finalTotalAmount, initialStatus,
                cartItems, userAddress, userPhone, paymentMethod); // <-- Thêm paymentMethod

        // 6. Lưu vào Firebase
        binding.button2.setEnabled(false);
        binding.button2.setText("Đang xử lý...");

        ordersRef.child(orderId).setValue(newOrder)
                .addOnSuccessListener(aVoid -> {
                    Log.d("CartActivity", "Đặt hàng thành công (COD)! Order ID: " + orderId);
                    Toast.makeText(CartActivity.this, "Đặt hàng thành công! (Thanh toán khi nhận hàng)", Toast.LENGTH_LONG).show(); // Thông báo rõ COD
                    managmentCart.clearCart();
                    // Cập nhật UI sau khi xóa giỏ hàng
                    runOnUiThread(() -> { // Đảm bảo cập nhật UI trên Main thread
                        initList();
                        calculateCart();
                    });
                    // Chuyển đến màn hình Lịch sử đơn hàng
                    Intent intent = new Intent(CartActivity.this, OrderHistoryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("CartActivity", "Đặt hàng thất bại: ", e);
                    Toast.makeText(CartActivity.this, "Đặt hàng thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Kích hoạt lại nút trên Main thread
                    runOnUiThread(() -> {
                        binding.button2.setEnabled(true);
                        binding.button2.setText("Place Order");
                    });
                });
    }


    private double calculateFinalTotal() {
        double percentTax = 0.02;
        double delivery = 10;
        double subTotal = managmentCart.getTotalFee();
        tax = Math.round(subTotal * percentTax * 100.0) / 100.0;
        // TODO: Áp dụng giảm giá coupon vào subTotal hoặc total nếu cần
        return Math.round((subTotal + tax + delivery) * 100.0) / 100.0;
    }

    private void calculateCart() {
        double percentTax = 0.02;
        double delivery = 10;
        double subTotal = managmentCart.getTotalFee();
        tax = Math.round(subTotal * percentTax * 100.0) / 100.0;

        double total = calculateFinalTotal();
        double itemTotal = Math.round(subTotal * 100.0) / 100.0;

        // Cập nhật UI với định dạng tiền tệ, sử dụng Locale.US để đảm bảo dấu "." là thập phân
        binding.totalFeeTxt.setText(String.format(Locale.US, "%.3fđ", itemTotal));
        binding.taxTxt.setText(String.format(Locale.US, "%.3fđ", tax));
        binding.deliveryTxt.setText(String.format(Locale.US, "%.3fđ", delivery));
        binding.totalTxt.setText(String.format(Locale.US, "%.3fđ", total));

        // Cập nhật trạng thái nút đặt hàng
        binding.button2.setEnabled(!managmentCart.getListCart().isEmpty());
    }
}