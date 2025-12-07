package com.example.upload_image;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.upload_image.DatabaseHelper; // Đảm bảo import đúng
import com.example.upload_image.User;

import java.io.File;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    private ImageView imgAvatar;
    private TextView tvId, tvUsername, tvFullName, tvEmail, tvGender;
    private Button btnLogout;

    private int userId = -1;
    private DatabaseHelper db;
    private String currentUsername;

    private final ActivityResultLauncher<Intent> avatarEditLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String avatarPath = result.getData().getStringExtra("AVATAR_PATH");
                    if (avatarPath != null) {
                        loadAvatar(avatarPath);
                        // Reload lại data từ DB để chắc chắn
                        loadUserData();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = new DatabaseHelper(this);

        imgAvatar = findViewById(R.id.imgAvatar);
        tvId = findViewById(R.id.tvId);
        tvUsername = findViewById(R.id.tvUsername);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvGender = findViewById(R.id.tvGender);
        btnLogout = findViewById(R.id.btnLogout);

        // Lấy thông tin từ Intent
        Intent intent = getIntent();
        if (intent != null) {
            // Trường hợp 1: Có USER_ID
            userId = intent.getIntExtra("USER_ID", -1);

            // Trường hợp 2: Chỉ có USERNAME (từ Login) -> Phải tra DB để lấy ID
            if (userId == -1 && intent.hasExtra("USERNAME")) {
                currentUsername = intent.getStringExtra("USERNAME");
                User user = db.getUser(currentUsername);
                if (user != null) {
                    userId = user.getId();
                }
            }
        }

        // Fallback: Lấy từ SharedPreferences nếu Intent trống
        if (userId == -1) {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            userId = prefs.getInt("USER_ID", -1);
            if(userId == -1) {
                currentUsername = prefs.getString("USERNAME", "");
                if(!currentUsername.isEmpty()){
                    User user = db.getUser(currentUsername);
                    if(user != null) userId = user.getId();
                }
            }
        }

        // Load dữ liệu lên giao diện
        loadUserData();

        // Sự kiện click avatar để upload
        imgAvatar.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, MainActivity.class);
            if (userId != -1) {
                // Truyền ID chuẩn xác sang MainActivity
                i.putExtra("USER_ID", userId);
                i.putExtra("USERNAME", currentUsername); // Truyền thêm username cho chắc
                // Đánh dấu MainActivity được mở từ Profile để xử lý result đúng
                i.putExtra("FROM_PROFILE", true);
                avatarEditLauncher.launch(i);
            } else {
                Toast.makeText(this, "Không tìm thấy User ID. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> {
            finish();
        });
    }

    private void loadUserData() {
        if (userId != -1) {
            // Lấy dữ liệu mới nhất từ DB
            // (Giả sử bạn có hàm getUserById, nếu không dùng getUser(username))
            User user = null;
            if(currentUsername != null && !currentUsername.isEmpty()){
                user = db.getUser(currentUsername);
            }

            if (user != null) {
                tvId.setText(String.valueOf(user.getId()));
                tvUsername.setText(user.getUsername());
                tvFullName.setText(user.getFullName());
                tvEmail.setText(user.getEmail());
                tvGender.setText(user.getGender());

                // Lưu lại session
                currentUsername = user.getUsername();
                userId = user.getId();
                saveSession();

                if (user.getAvatar() != null) {
                    loadAvatar(user.getAvatar());
                }
            }
        }

        // If an AVATAR_PATH was passed via Intent (e.g., MainActivity redirected here after upload), show it
        Intent intent = getIntent();
        if (intent != null) {
            String extraAvatar = intent.getStringExtra("AVATAR_PATH");
            if (extraAvatar != null && !extraAvatar.isEmpty()) {
                loadAvatar(extraAvatar);
            }
        }
    }

    private void saveSession(){
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt("USER_ID", userId);
        ed.putString("USERNAME", currentUsername);
        ed.apply();
    }

    private void loadAvatar(String path) {
        try {
            Glide.with(this)
                    .load(path) // Glide tự xử lý File path hoặc URL
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imgAvatar);
        } catch (Exception e) {
            Log.e(TAG, "Error loading avatar", e);
        }
    }
}