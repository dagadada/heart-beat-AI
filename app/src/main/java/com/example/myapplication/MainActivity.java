package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;

import org.json.JSONObject;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity {

    private EditText usernameEditText;
    private Button loginButton;
    private OkHttpClient client = new OkHttpClient();

    private static final String SERVER_URL = "http://1.14.199.19:5000"; // 替换为你的服务器IP

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameEditText = findViewById(R.id.usernameEditText);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(MainActivity.this, "请输入用户名", Toast.LENGTH_SHORT).show();
                return;
            }

            checkUserFromServer(username);
        });
    }

    private void checkUserFromServer(String username) {
        String url = SERVER_URL + "/check_user?username=" + username;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "连接服务器失败", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    try {
                        JSONObject json = new JSONObject(body);
                        String status = json.getString("status");

                        runOnUiThread(() -> {
                            Intent intent;
                            switch (status) {
                                case "has_model":
                                    intent = new Intent(MainActivity.this, HeartActivity.class);
                                    break;
                                case "no_model":
                                    intent = new Intent(MainActivity.this, DataCollectActivity.class);
                                    break;
                                default:
                                    intent = new Intent(MainActivity.this, RegisterActivity.class);
                                    break;
                            }
                            intent.putExtra("username", username);
                            startActivity(intent);
                            finish();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "解析服务器响应失败", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
        });
    }
}


