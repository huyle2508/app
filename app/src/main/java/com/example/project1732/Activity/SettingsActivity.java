package com.example.project1732.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.project1732.R;
import com.example.project1732.databinding.ActivitySettingsBinding;

public class SettingsActivity extends BaseActivity { // Kế thừa BaseActivity

    private ActivitySettingsBinding binding;
    private ListView settingsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settingsListView = binding.settingsListView;

        // Danh sách các tùy chọn cài đặt
        String[] settingsOptions = {"Thông tin tài khoản", "Đổi mật khẩu", "Lịch sử mua hàng","Món ăn yêu thích"};

        // Sử dụng ArrayAdapter đơn giản để hiển thị danh sách
        // (Bạn có thể dùng custom adapter nếu muốn giao diện phức tạp hơn)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, // Layout mặc định của Android cho list item
                android.R.id.text1, // ID của TextView trong layout mặc định
                settingsOptions);

        // Tùy chỉnh màu chữ cho ListView item
         adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, settingsOptions) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(getResources().getColor(R.color.white)); // Đặt màu chữ trắng
                text.setTextSize(18); // Đặt kích thước chữ
                return view;
            }
        };


        settingsListView.setAdapter(adapter);

        // Xử lý sự kiện khi nhấn vào một mục trong ListView
        settingsListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = null;
            switch (position) {
                case 0: // Thông tin tài khoản
                    intent = new Intent(SettingsActivity.this, ProfileActivity.class);
                    break;
                case 1: // Đổi mật khẩu
                    intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
                    break;
                case 2: // Lịch sử mua hàng
                    intent = new Intent(SettingsActivity.this, OrderHistoryActivity.class);
                    break;
                case 3: // Món ăn yêu thích
                    intent = new Intent(SettingsActivity.this, FavoriteActivity.class);
                    break;
            }
            if (intent != null) {
                startActivity(intent);
            }
        });

        // Xử lý nút quay lại
        binding.backBtn.setOnClickListener(v -> finish());
    }
}