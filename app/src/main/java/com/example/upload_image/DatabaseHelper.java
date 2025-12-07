package com.example.upload_image;

import android.content.Context;
import android.content.SharedPreferences;

public class DatabaseHelper {
    private static final String PREFS = "app_prefs_db";
    private static final String KEY_PREFIX = "user_";
    private Context context;

    public DatabaseHelper(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    public User getUser(String username) {
        if (username == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_PREFIX + username, null);
        if (raw == null) return null;
        String[] parts = raw.split(";", -1);
        try {
            int id = Integer.parseInt(parts[0]);
            String fullName = parts.length > 1 ? parts[1] : "";
            String email = parts.length > 2 ? parts[2] : "";
            String gender = parts.length > 3 ? parts[3] : "";
            String avatar = parts.length > 4 ? parts[4] : null;
            return new User(id, username, fullName, email, gender, avatar);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void saveUser(User user) {
        if (user == null || user.getUsername() == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = user.getId() + ";" + (user.getFullName() == null ? "" : user.getFullName())
                + ";" + (user.getEmail() == null ? "" : user.getEmail())
                + ";" + (user.getGender() == null ? "" : user.getGender())
                + ";" + (user.getAvatar() == null ? "" : user.getAvatar());
        prefs.edit().putString(KEY_PREFIX + user.getUsername(), raw).apply();
    }

    public void updateUserAvatar(int id, String avatarPath) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(KEY_PREFIX)) continue;
            String raw = prefs.getString(key, null);
            if (raw == null) continue;
            String[] parts = raw.split(";", -1);
            if (parts.length == 0) continue;
            try {
                int storedId = Integer.parseInt(parts[0]);
                if (storedId == id) {
                    // rebuild with new avatar
                    parts = new String[] { parts[0], parts.length>1?parts[1]:"", parts.length>2?parts[2]:"", parts.length>3?parts[3]:"", avatarPath };
                    String newRaw = String.join(";", parts);
                    prefs.edit().putString(key, newRaw).apply();
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}

