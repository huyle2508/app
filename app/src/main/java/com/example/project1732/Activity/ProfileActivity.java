package com.example.project1732.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.project1732.Domain.User;
import com.example.project1732.databinding.ActivityProfileBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends BaseActivity { // Kế thừa BaseActivity

    private ActivityProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private User currentUserData; // Lưu trữ dữ liệu user hiện tại

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Nếu người dùng chưa đăng nhập, có thể quay lại màn hình Login
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Lấy tham chiếu đến node của user trong Firebase
        userRef = database.getReference("Users").child(currentUser.getUid());

        loadUserProfile();
        setupButtonClickListeners();
    }

    private void loadUserProfile() {
        binding.progressBarProfile.setVisibility(View.VISIBLE);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBarProfile.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    currentUserData = snapshot.getValue(User.class);
                    if (currentUserData != null) {
                        populateUI(currentUserData);
                    } else {
                        Toast.makeText(ProfileActivity.this, "Không thể tải dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Nếu chưa có dữ liệu, tạo mới với email và hiển thị
                    if (currentUser != null && currentUser.getEmail() != null) {
                        currentUserData = new User(currentUser.getEmail(), currentUser.getEmail().split("@")[0], System.currentTimeMillis(), "", "");
                        populateUI(currentUserData);
                        // Lưu dữ liệu cơ bản này vào DB nếu chưa có
                        userRef.setValue(currentUserData);
                    } else {
                        Toast.makeText(ProfileActivity.this, "Không tìm thấy dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBarProfile.setVisibility(View.GONE);
                Log.e("ProfileActivity", "Lỗi tải dữ liệu: ", error.toException());
                Toast.makeText(ProfileActivity.this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(User user) {
        binding.profileEmailEdt.setText(user.getEmail() != null ? user.getEmail() : "");
        binding.profileNameEdt.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
        binding.profileAddressEdt.setText(user.getAddress() != null ? user.getAddress() : "");
        binding.profilePhoneEdt.setText(user.getPhone() != null ? user.getPhone() : "");
    }

    private void setupButtonClickListeners() {
        binding.backBtnProfile.setOnClickListener(v -> finish());

        binding.saveProfileBtn.setOnClickListener(v -> saveUserProfile());
    }

    private void saveUserProfile() {
        String displayName = binding.profileNameEdt.getText().toString().trim();
        String address = binding.profileAddressEdt.getText().toString().trim();
        String phone = binding.profilePhoneEdt.getText().toString().trim();

        // Kiểm tra dữ liệu nhập (tùy chọn)
        if (displayName.isEmpty()) {
            binding.profileNameEdt.setError("Tên không được để trống");
            binding.profileNameEdt.requestFocus();
            return;
        }
        // Thêm kiểm tra cho address, phone nếu cần

        binding.progressBarProfile.setVisibility(View.VISIBLE);
        binding.saveProfileBtn.setEnabled(false);

        // Tạo Map để cập nhật các trường cụ thể, tránh ghi đè email, createdAt
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", displayName);
        updates.put("address", address);
        updates.put("phone", phone);

        userRef.updateChildren(updates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                binding.progressBarProfile.setVisibility(View.GONE);
                binding.saveProfileBtn.setEnabled(true);
                if (task.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                    // Cập nhật lại currentUserData nếu cần
                    if (currentUserData != null) {
                        currentUserData.setDisplayName(displayName);
                        currentUserData.setAddress(address);
                        currentUserData.setPhone(phone);
                    }
                    // Có thể đóng activity sau khi lưu hoặc không
                    // finish();
                } else {
                    Log.e("ProfileActivity", "Lỗi cập nhật: ", task.getException());
                    Toast.makeText(ProfileActivity.this, "Cập nhật thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}