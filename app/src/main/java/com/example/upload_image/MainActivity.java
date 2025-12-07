package com.example.upload_image;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.upload_image.ServiceAPI;
import com.example.upload_image.RealPathUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    Button btnChoose, btnUpload;
    ImageView imageViewChoose;

    private Uri mUri;
    private ProgressDialog mProgressDialog;
    public static final int MY_REQUEST_CODE = 100;
    public static final String TAG = MainActivity.class.getName();

    private int currentUserId = -1;

    public static String[] storge_permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    public static String[] storge_permissions_33 = {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO
    };

    private final ActivityResultLauncher<Intent> mActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data == null || data.getData() == null) return;

                        mUri = data.getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mUri);
                            imageViewChoose.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            Log.e(TAG, "Error decoding image", e);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AnhXa();

        // Lấy User ID chuẩn xác
        resolveUserId();

        // Disable upload button initially if id not resolved
        btnUpload.setEnabled(currentUserId != -1);

        // If no ID, prompt the user to enter one (or username)
        if (currentUserId == -1) {
            promptForUserIdOrUsername();
        }

        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setMessage("Đang upload ảnh lên Server...");

        btnChoose.setOnClickListener(v -> CheckPermission());

        btnUpload.setOnClickListener(v -> {
            if (mUri != null) {
                uploadImageToServer();
            } else {
                Toast.makeText(MainActivity.this, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm xử lý Logic lấy ID (Đã làm lại cho đơn giản và hiệu quả)
    private void resolveUserId() {
        Intent intent = getIntent();
        if (intent == null) return;

        // 1. Ưu tiên lấy ID trực tiếp nếu được truyền
        currentUserId = intent.getIntExtra("USER_ID", -1);

        // 2. Nếu không có ID, thử lấy từ SharedPreferences (Session đăng nhập)
        if (currentUserId == -1) {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            // Thử lấy ID đã lưu
            currentUserId = prefs.getInt("USER_ID", -1);
        }

        Log.d(TAG, "Final Resolved User ID: " + currentUserId);
    }

    // Prompt dialog to get USER_ID or USERNAME from user when none available
    private void promptForUserIdOrUsername() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập USER_ID hoặc USERNAME");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nhập id (số) hoặc username");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String value = input.getText() != null ? input.getText().toString().trim() : null;
            if (value == null || value.isEmpty()) {
                Toast.makeText(MainActivity.this, "Bạn chưa nhập gì", Toast.LENGTH_SHORT).show();
                return;
            }

            // Try parse as int id first
            try {
                int parsed = Integer.parseInt(value);
                currentUserId = parsed;
                // save to prefs
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                prefs.edit().putInt("USER_ID", currentUserId).apply();
                btnUpload.setEnabled(true);
                Toast.makeText(MainActivity.this, "Sử dụng USER_ID: " + currentUserId, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "User entered numeric ID: " + currentUserId);
                return;
            } catch (NumberFormatException ignored) {
            }

            // If not number, store the username in prefs (useful for other parts of your app),
            // but we still need a numeric USER_ID to perform the upload.
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putString("USERNAME", value).apply();
            Toast.makeText(MainActivity.this, "Lưu username. Vui lòng nhập USER_ID (số) để upload.", Toast.LENGTH_LONG).show();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.setCancelable(false);
        builder.show();
    }

    private void AnhXa() {
        btnChoose = findViewById(R.id.btnChoose);
        btnUpload = findViewById(R.id.btnUpload);
        imageViewChoose = findViewById(R.id.imgChoose);
    }

    private void CheckPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            openGallery();
            return;
        }
        String permissionToCheck = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permissionToCheck) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestPermissions(permissions(), MY_REQUEST_CODE);
        }
    }

    public static String[] permissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return storge_permissions_33;
        } else {
            return storge_permissions;
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        mActivityResultLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            }
        }
    }

    private void uploadImageToServer() {
        // Kiểm tra chặn nếu ID vẫn lỗi
        if (currentUserId == -1) {
            Toast.makeText(this, "Lỗi User ID (-1). Hãy đăng nhập lại để lấy thông tin.", Toast.LENGTH_LONG).show();
            return;
        }

        mProgressDialog.show();

        // 1. Lấy đường dẫn file
        String strRealPath = RealPathUtil.getRealPath(this, mUri);
        File file;

        try {
            if (strRealPath != null) {
                file = new File(strRealPath);
            } else {
                // Fallback nếu không lấy được đường dẫn thực (Android 10+)
                file = createFileFromUri(this, mUri);
                strRealPath = file.getAbsolutePath();
            }
        } catch (Exception e) {
            mProgressDialog.dismiss();
            Toast.makeText(this, "Lỗi file ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalPath = strRealPath;

        // 2. Tạo RequestBody
        RequestBody idPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(currentUserId));

        // Use proper mime type for the file when possible
        String mime = null;
        try {
            if (mUri != null) mime = getContentResolver().getType(mUri);
        } catch (Exception ignored) {}
        MediaType mediaType = MediaType.parse(mime != null ? mime : "image/*");
        RequestBody requestFile = RequestBody.create(mediaType, file);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("images", file.getName(), requestFile);

        // 3. Gọi API (Sử dụng hàm updateImageWithField vì id là RequestBody text)
        ServiceAPI.serviceApi.updateImageWithField(idPart, imagePart).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                mProgressDialog.dismiss();
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Upload thành công!", Toast.LENGTH_SHORT).show();
                    // Cập nhật SharedPreferences (avatar path) nếu cần

                    // Nếu MainActivity được mở từ Profile (FROM_PROFILE=true) -> trả result và finish
                    Intent caller = getIntent();
                    boolean fromProfile = caller != null && caller.getBooleanExtra("FROM_PROFILE", false);
                    if (fromProfile) {
                        Intent result = new Intent();
                        result.putExtra("AVATAR_PATH", finalPath);
                        setResult(RESULT_OK, result);
                        finish();
                    } else {
                        // Nếu không, mở ProfileActivity và truyền AVATAR_PATH để người dùng thấy ngay
                        Intent i = new Intent(MainActivity.this, ProfileActivity.class);
                        i.putExtra("AVATAR_PATH", finalPath);
                        // nếu chúng ta có user id, truyền tiếp để Profile dùng
                        if (currentUserId != -1) i.putExtra("USER_ID", currentUserId);
                        startActivity(i);
                    }
                 } else {
                    Toast.makeText(MainActivity.this, "Thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                    // Log error body if available (use try-with-resources)
                    try (ResponseBody eb = response.errorBody()) {
                        if (eb != null) Log.e(TAG, "Server error body: " + eb.string());
                    } catch (IOException ioe) {
                        Log.e(TAG, "Error reading errorBody", ioe);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                mProgressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Upload error", t);
            }
        });
    }

    // Helper tạo file tạm từ Uri
    private File createFileFromUri(Context context, Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String fileName = "temp_avatar_" + System.currentTimeMillis() + ".jpg";
        File file = new File(context.getCacheDir(), fileName);

        try (InputStream inputStream = resolver.openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        return file;
    }
}
