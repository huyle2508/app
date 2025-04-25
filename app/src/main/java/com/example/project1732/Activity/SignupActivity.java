package com.example.project1732.Activity; // Thay đổi package nếu cần

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.project1732.Domain.User;
import com.example.project1732.databinding.ActivitySignupBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;


public class SignupActivity extends BaseActivity {

    private ActivitySignupBinding binding; // Sử dụng View Binding
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();


        setButtonClickListeners();
    }

    private void setButtonClickListeners() {
        binding.signupBtn.setOnClickListener(v -> registerUser());

        binding.gotoLoginTxt.setOnClickListener(v -> {
            // Kết thúc activity hiện tại để quay về LoginActivity (nếu nó còn trong stack)
            // Hoặc startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String email = binding.signupEmailEdt.getText().toString().trim();
        String password = binding.signupPasswordEdt.getText().toString().trim();
        String confirmPassword = binding.signupConfirmPasswordEdt.getText().toString().trim();

        if (email.isEmpty()) {
            binding.signupEmailEdt.setError("Vui lòng nhập email");
            binding.signupEmailEdt.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.signupEmailEdt.setError("Vui lòng nhập email hợp lệ");
            binding.signupEmailEdt.requestFocus();
            return;
        }


        if (password.isEmpty()) {
            binding.signupPasswordEdt.setError("Vui lòng nhập mật khẩu");
            binding.signupPasswordEdt.requestFocus();
            return;
        }
        // Optional: Add password complexity checks (e.g., minimum length)
        if (password.length() < 6) {
            binding.signupPasswordEdt.setError("Mật khẩu phải có ít nhất 6 ký tự");
            binding.signupPasswordEdt.requestFocus();
            return;
        }


        if (confirmPassword.isEmpty()) {
            binding.signupConfirmPasswordEdt.setError("Vui lòng xác nhận mật khẩu");
            binding.signupConfirmPasswordEdt.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.signupConfirmPasswordEdt.setError("Mật khẩu xác nhận không khớp");
            binding.signupConfirmPasswordEdt.requestFocus();
            // Clear the confirm password field maybe
            binding.signupConfirmPasswordEdt.setText("");
            return;
        }

        // Hiển thị ProgressBar và vô hiệu hóa nút
        // Ví dụ: binding.progressBarSignup.setVisibility(View.VISIBLE);
        binding.signupBtn.setEnabled(false);
        binding.signupBtn.setText("Đang đăng ký...");


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> { // Lambda cho gọn
                    if (task.isSuccessful()) {
                        Log.d("SignupActivity", "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Lưu thông tin người dùng vào Realtime Database
                            saveUserInfoToDatabase(firebaseUser, email); // Gọi hàm lưu
                        } else {
                            // Hiếm khi xảy ra, nhưng nên xử lý
                            // Ví dụ: binding.progressBarSignup.setVisibility(View.GONE);
                            binding.signupBtn.setEnabled(true);
                            binding.signupBtn.setText("Đăng Ký");
                            Toast.makeText(SignupActivity.this, "Không thể lấy thông tin người dùng.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Đăng ký thất bại
                        Log.w("SignupActivity", "createUserWithEmail:failure", task.getException());
                        // Ví dụ: binding.progressBarSignup.setVisibility(View.GONE);
                        binding.signupBtn.setEnabled(true);
                        binding.signupBtn.setText("Đăng Ký");
                        Toast.makeText(SignupActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserInfoToDatabase(FirebaseUser firebaseUser, String email) {
        String userId = firebaseUser.getUid();
        // Sử dụng database instance từ BaseActivity
        DatabaseReference userRef = database.getReference("Users").child(userId);

        // Lấy tên hiển thị ban đầu từ email hoặc để trống
        String initialDisplayName = email.split("@")[0]; // Ví dụ

        // *** KHỞI TẠO USER VỚI ADDRESS VÀ PHONE RỖNG ***
        User user = new User(email, initialDisplayName, System.currentTimeMillis(), "", ""); // Address="", Phone=""

        userRef.setValue(user).addOnCompleteListener(task -> {
            // Ví dụ: binding.progressBarSignup.setVisibility(View.GONE);
            // Kích hoạt lại nút đăng ký chỉ khi lưu data thất bại, vì nếu thành công sẽ chuyển màn hình
            if (!task.isSuccessful()) {
                binding.signupBtn.setEnabled(true);
                binding.signupBtn.setText("Đăng Ký");
            }

            if (task.isSuccessful()) {
                Log.d("SignupActivity", "User data saved successfully.");
                Toast.makeText(SignupActivity.this, "Đăng ký thành công.", Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
            } else {
                Log.w("SignupActivity", "Failed to save user data.", task.getException());
                // Thông báo lỗi lưu data, nhưng vẫn có thể coi là đăng ký thành công
                // Quyết định xem có nên đăng nhập luôn không hay yêu cầu thử lại
                Toast.makeText(SignupActivity.this, "Đăng ký thành công nhưng lưu dữ liệu thất bại.", Toast.LENGTH_LONG).show();
                navigateToMainActivity(); // Vẫn cho vào Main sau khi Auth thành công
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Đóng SignupActivity
    }
}