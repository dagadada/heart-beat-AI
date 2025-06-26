package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends Activity {

    private EditText usernameEditText;
    private Button registerButton;
    private OkHttpClient client = new OkHttpClient();

    private static final String SERVER_URL = "http://192.168.36.12:5000/register_user"; // 修改为你的 Flask 地址

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameEditText = findViewById(R.id.usernameEditText);
        registerButton = findViewById(R.id.registerButton);

        // 若 MainActivity 传入用户名，预填充
        String username = getIntent().getStringExtra("username");
        if (username != null) {
            usernameEditText.setText(username);
        }

        registerButton.setOnClickListener(v -> {
            String name = usernameEditText.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "用户名不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            registerUser(name);
        });
    }

    private void registerUser(String username) {
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(RegisterActivity.this, "注册失败: 无法连接服务器", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    try {
                        JSONObject json = new JSONObject(body);
                        String status = json.getString("status");
                        runOnUiThread(() -> {
                            if ("success".equals(status)) {
                                Toast.makeText(RegisterActivity.this, "注册成功！", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RegisterActivity.this, DataCollectActivity.class);
                                intent.putExtra("username", username);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(RegisterActivity.this, "注册失败：用户可能已存在", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}


