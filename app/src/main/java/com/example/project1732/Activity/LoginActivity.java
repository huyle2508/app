package com.example.project1732.Activity; // Thay đổi package nếu cần

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.project1732.R;
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
        binding.forgotPasswordTxt.setOnClickListener(v -> showForgotPasswordDialog());
    }
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null); // Tạo một layout XML riêng cho dialog
        final EditText emailEditText = dialogView.findViewById(R.id.forgotEmailEdt); // ID của EditText trong dialog layout

        builder.setView(dialogView)
                .setTitle(R.string.forgot_password_title)
                .setPositiveButton(R.string.send_reset_email_button, (dialog, id) -> {
                    String email = emailEditText.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(LoginActivity.this, R.string.enter_email_prompt, Toast.LENGTH_SHORT).show();
                        // Có thể hiển thị lại dialog hoặc làm nổi bật EditText
                        // showForgotPasswordDialog(); // Gọi lại để hiển thị dialog
                        return; // Ngăn không đóng dialog nếu email trống
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(LoginActivity.this, R.string.invalid_email_prompt, Toast.LENGTH_SHORT).show();
                        // showForgotPasswordDialog(); // Gọi lại để hiển thị dialog
                        return; // Ngăn không đóng dialog nếu email không hợp lệ
                    }
                    sendPasswordResetEmail(email);
                })
                .setNegativeButton("Hủy", (dialog, id) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    // *******************************************

    // *** THÊM HÀM NÀY ĐỂ GỬI EMAIL ĐẶT LẠI ***
    private void sendPasswordResetEmail(String email) {
        // Có thể thêm ProgressBar ở đây
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    // Ẩn ProgressBar
                    if (task.isSuccessful()) {
                        Log.d("ForgotPassword", "Email sent.");
                        Toast.makeText(LoginActivity.this,
                                String.format(getString(R.string.reset_email_sent_success), email),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Log.w("ForgotPassword", "Error sending reset email", task.getException());
                        Toast.makeText(LoginActivity.this,
                                String.format(getString(R.string.reset_email_sent_fail), task.getException().getMessage()),
                                Toast.LENGTH_LONG).show();
                    }
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