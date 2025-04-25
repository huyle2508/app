package com.example.project1732.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.project1732.databinding.ActivityChangePasswordBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends BaseActivity { // Kế thừa BaseActivity

    private ActivityChangePasswordBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish(); // Đóng activity nếu người dùng không đăng nhập
            return;
        }

        setupButtonClickListeners();
    }

    private void setupButtonClickListeners() {
        binding.backBtnChangePassword.setOnClickListener(v -> finish());
        binding.changePasswordBtn.setOnClickListener(v -> attemptPasswordChange());
    }

    private void attemptPasswordChange() {
        String currentPassword = binding.currentPasswordEdt.getText().toString().trim();
        String newPassword = binding.newPasswordEdt.getText().toString().trim();
        String confirmNewPassword = binding.confirmNewPasswordEdt.getText().toString().trim();

        // --- Validation ---
        if (currentPassword.isEmpty()) {
            binding.currentPasswordEdt.setError("Vui lòng nhập mật khẩu hiện tại");
            binding.currentPasswordEdt.requestFocus();
            return;
        }
        if (newPassword.isEmpty()) {
            binding.newPasswordEdt.setError("Vui lòng nhập mật khẩu mới");
            binding.newPasswordEdt.requestFocus();
            return;
        }
        if (newPassword.length() < 6) {
            binding.newPasswordEdt.setError("Mật khẩu mới phải có ít nhất 6 ký tự");
            binding.newPasswordEdt.requestFocus();
            return;
        }
        if (confirmNewPassword.isEmpty()) {
            binding.confirmNewPasswordEdt.setError("Vui lòng xác nhận mật khẩu mới");
            binding.confirmNewPasswordEdt.requestFocus();
            return;
        }
        if (!newPassword.equals(confirmNewPassword)) {
            binding.confirmNewPasswordEdt.setError("Mật khẩu xác nhận không khớp");
            binding.confirmNewPasswordEdt.requestFocus();
            return;
        }
        if (newPassword.equals(currentPassword)) {
            binding.newPasswordEdt.setError("Mật khẩu mới phải khác mật khẩu hiện tại");
            binding.newPasswordEdt.requestFocus();
            return;
        }
        // --- End Validation ---

        // Hiện ProgressBar và vô hiệu hóa nút
        binding.progressBarChangePassword.setVisibility(View.VISIBLE);
        binding.changePasswordBtn.setEnabled(false);
        binding.changePasswordBtn.setText("Đang xử lý...");

        // Lấy email người dùng hiện tại để tạo credential
        String email = currentUser.getEmail();
        if (email == null) {
            Toast.makeText(this, "Không thể lấy email người dùng.", Toast.LENGTH_SHORT).show();
            binding.progressBarChangePassword.setVisibility(View.GONE);
            binding.changePasswordBtn.setEnabled(true);
            binding.changePasswordBtn.setText("Xác nhận đổi mật khẩu");
            return;
        }

        // Bước 1: Re-authenticate người dùng với mật khẩu hiện tại
        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        Log.d("ChangePasswordActivity", "User re-authenticated.");

                        // Bước 2: Cập nhật mật khẩu mới
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    // Luôn ẩn progressbar và kích hoạt lại nút sau khi hoàn tất
                                    binding.progressBarChangePassword.setVisibility(View.GONE);
                                    binding.changePasswordBtn.setEnabled(true);
                                    binding.changePasswordBtn.setText("Xác nhận đổi mật khẩu");

                                    if (updateTask.isSuccessful()) {
                                        Log.d("ChangePasswordActivity", "User password updated.");
                                        Toast.makeText(ChangePasswordActivity.this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                                        // Xóa các trường và đóng activity
                                        binding.currentPasswordEdt.setText("");
                                        binding.newPasswordEdt.setText("");
                                        binding.confirmNewPasswordEdt.setText("");
                                        finish(); // Đóng màn hình sau khi thành công
                                    } else {
                                        Log.e("ChangePasswordActivity", "Error updating password", updateTask.getException());
                                        Toast.makeText(ChangePasswordActivity.this, "Lỗi đổi mật khẩu: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        // Re-authentication thất bại
                        Log.w("ChangePasswordActivity", "Re-authentication failed", reauthTask.getException());
                        binding.progressBarChangePassword.setVisibility(View.GONE);
                        binding.changePasswordBtn.setEnabled(true);
                        binding.changePasswordBtn.setText("Xác nhận đổi mật khẩu");
                        binding.currentPasswordEdt.setError("Mật khẩu hiện tại không đúng");
                        binding.currentPasswordEdt.requestFocus();
                        Toast.makeText(ChangePasswordActivity.this, "Mật khẩu hiện tại không đúng.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}