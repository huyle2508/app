package com.example.project1732.Activity; // Hoặc package phù hợp

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.project1732.Adapter.OrderAdapter; // Import OrderAdapter
import com.example.project1732.Domain.Order; // Import Order
import com.example.project1732.databinding.ActivityOrderHistoryBinding; // Import Binding
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query; // Import Query
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections; // Import Collections


public class OrderHistoryActivity extends BaseActivity {

    private ActivityOrderHistoryBinding binding;
    private FirebaseAuth mAuth;
    private OrderAdapter orderAdapter;
    private ArrayList<Order> orderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        orderList = new ArrayList<>();

        setupRecyclerView();
        loadOrderHistory();

        binding.backBtn.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        binding.orderHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(orderList, this);
        binding.orderHistoryRecyclerView.setAdapter(orderAdapter);
    }

    private void loadOrderHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem lịch sử", Toast.LENGTH_SHORT).show();
            // Có thể chuyển về màn hình Login
            finish();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference ordersRef = database.getReference("Orders").child(userId);

        // Hiển thị ProgressBar
        binding.progressBarOrders.setVisibility(View.VISIBLE);
        binding.emptyOrdersTxt.setVisibility(View.GONE);

        // Sắp xếp theo timestamp giảm dần để đơn mới nhất lên đầu
        Query query = ordersRef.orderByChild("timestamp");

        query.addValueEventListener(new ValueEventListener() { // Dùng addValueEventListener để tự cập nhật nếu trạng thái thay đổi
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                        Order order = orderSnapshot.getValue(Order.class);
                        if (order != null) {
                            // Firebase trả về key, gán vào orderId nếu chưa có khi lưu
                            if (order.getOrderId() == null) {
                                order.setOrderId(orderSnapshot.getKey());
                            }
                            orderList.add(order);
                        }
                    }
                    // Đảo ngược danh sách để đơn mới nhất lên đầu
                    Collections.reverse(orderList);
                    binding.emptyOrdersTxt.setVisibility(View.GONE);
                } else {
                    binding.emptyOrdersTxt.setVisibility(View.VISIBLE);
                    Log.d("OrderHistory", "No orders found for user: " + userId);
                }
                orderAdapter.notifyDataSetChanged();
                binding.progressBarOrders.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBarOrders.setVisibility(View.GONE);
                binding.emptyOrdersTxt.setText("Lỗi tải lịch sử đơn hàng");
                binding.emptyOrdersTxt.setVisibility(View.VISIBLE);
                Log.e("OrderHistory", "Failed to load order history", error.toException());
                Toast.makeText(OrderHistoryActivity.this, "Lỗi tải lịch sử đơn hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}