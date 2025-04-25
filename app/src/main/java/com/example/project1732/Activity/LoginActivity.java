package com.example.project1732.Activity; // Thay đổi package nếu cần

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.project1732.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Kế thừa từ BaseActivity nếu bạn muốn dùng chung FirebaseDatabase instance
public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding; // Sử dụng View Binding
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        setButtonClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Kiểm tra nếu người dùng đã đăng nhập, chuyển đến MainActivity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity();
        }
    }

    private void setButtonClickListeners() {
        binding.loginBtn.setOnClickListener(v -> loginUser());

        binding.gotoSignupTxt.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            // Không cần finish() ở đây nếu muốn người dùng có thể quay lại
        });
    }

    private void loginUser() {
        String email = binding.loginEmailEdt.getText().toString().trim();
        String password = binding.loginPasswordEdt.getText().toString().trim();

        if (email.isEmpty()) {
            binding.loginEmailEdt.setError("Vui lòng nhập email");
            binding.loginEmailEdt.requestFocus();
            return;
        }
        // Optional: Basic email format validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.loginEmailEdt.setError("Vui lòng nhập email hợp lệ");
            binding.loginEmailEdt.requestFocus();
            return;
        }


        if (password.isEmpty()) {
            binding.loginPasswordEdt.setError("Vui lòng nhập mật khẩu");
            binding.loginPasswordEdt.requestFocus();
            return;
        }

        // Hiển thị ProgressBar (nếu có) và vô hiệu hóa nút
        // Ví dụ: binding.progressBarLogin.setVisibility(View.VISIBLE);
        binding.loginBtn.setEnabled(false);
        binding.loginBtn.setText("Đang đăng nhập..."); // Thay đổi text nút (tùy chọn)


        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Ẩn ProgressBar và kích hoạt lại nút
                        // Ví dụ: binding.progressBarLogin.setVisibility(View.GONE);
                        binding.loginBtn.setEnabled(true);
                        binding.loginBtn.setText("Đăng Nhập"); // Khôi phục text nút

                        if (task.isSuccessful()) {
                            // Đăng nhập thành công
                            Log.d("LoginActivity", "signInWithEmail:success");
                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công.", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        } else {
                            // Đăng nhập thất bại
                            Log.w("LoginActivity", "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Xóa các activity trước đó
        startActivity(intent);
        finish(); // Đóng LoginActivity
    }
}