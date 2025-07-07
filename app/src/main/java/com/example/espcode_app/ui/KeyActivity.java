package com.example.espcode_app.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.espcode_app.R;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class KeyActivity extends AppCompatActivity {

    private SharedPreferences encryptedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key);

        initializeApp();
    }

    private void initializeApp() {
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                    this,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("ESP init failed", e);
        }

        EditText inputKey = findViewById(R.id.editKey);
        setupButtons(inputKey);
    }

    private void setupButtons(EditText inputKey) {
        findViewById(R.id.btnSave).setOnClickListener(v -> saveKey(inputKey));
        findViewById(R.id.btnLoad).setOnClickListener(v -> loadKey());
        findViewById(R.id.btnTest).setOnClickListener(v -> testIpInfo());
    }

    private void saveKey(EditText inputKey) {
        try {
            String key = inputKey.getText().toString();
            encryptedPrefs.edit().putString("api_key", key).apply();
            Toast.makeText(this, "Key 已儲存！", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("KeySave", "Save error", e);
        }
    }

    private void loadKey() {
        try {
            String key = encryptedPrefs.getString("api_key", null);
            if (key != null) {
                Toast.makeText(this, "讀出 Key：" + key, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "尚未儲存金鑰", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("KeyLoad", "Load error", e);
        }
    }

    private void testIpInfo() {
        new Thread(() -> {
            try {
                String token = encryptedPrefs.getString("api_key", null);
                if (token == null) {
                    showToast("尚未儲存金鑰");
                    return;
                }

                String apiUrl = "https://ipinfo.io/json?token=" + token;
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");

                int code = conn.getResponseCode();
                if (code == 200) {
                    handleSuccessfulResponse(conn);
                } else {
                    showToast("HTTP Error: " + code);
                }
            } catch (Exception e) {
                Log.e("IPinfo", "Error", e);
                showToast("Exception: " + e.getMessage());
            }
        }).start();
    }

    private void handleSuccessfulResponse(HttpURLConnection conn) throws Exception {
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);

            String json = bos.toString("UTF-8");
            JSONObject obj = new JSONObject(json);
            String ip = obj.optString("ip", "N/A");

            runOnUiThread(() -> {
                Toast.makeText(this, "IP: " + ip, Toast.LENGTH_LONG).show();
                Log.i("IPinfo", json);
            });
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
