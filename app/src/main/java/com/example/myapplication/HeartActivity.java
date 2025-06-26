package com.example.myapplication;

import android.content.Intent;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import java.util.ArrayList;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.DecimalFormat;
import android.graphics.Color;

import java.util.Collections;
import java.util.List;
import java.io.File;
import java.io.IOException;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.LiteModuleLoader;
import java.io.FileOutputStream;
import java.util.Random;
import java.io.InputStream;
import java.io.OutputStream;
import android.util.Log;

import androidx.appcompat.widget.Toolbar;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

public class HeartActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private LineChart chart, ratechart;
    private LineDataSet dataSet, heartRateDataSet;
    private LineData lineData, heartRateLineData;
    private boolean isRunning = false;
    private long startTime;
    private ArrayList<Float> yDataList;
    private TextView textView5, resultView, textView6, textView4;
    private ImageView imageView3;
    private Toolbar toolbar;
    private final String text = "用“心”解锁";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart);
        getSupportActionBar().hide();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        chart = findViewById(R.id.chart);
        ratechart = findViewById(R.id.rateChart);
        chart.getDescription().setEnabled(false);
        ratechart.getDescription().setEnabled(false);
        ratechart.setNoDataText("");

        resultView = findViewById(R.id.resultView);
        textView5 = findViewById(R.id.textView5);
        textView6 = findViewById(R.id.textView6);
        imageView3 = findViewById(R.id.imageView3);
        textView4 = findViewById(R.id.textView4);

        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#FF2993")), text.indexOf("“"), text.indexOf("”") + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView4.setText(spannableString);

        resetChart();

        Button startButton = findViewById(R.id.startButton);
        imageView3.setImageDrawable(null);
        startButton.setOnClickListener(v -> {
            if (!isRunning) {
                sensorManager.registerListener(HeartActivity.this, accelerometer, 10000);
                isRunning = true;
                startTime = System.currentTimeMillis();
                yDataList.clear();
                resetChart();
                ratechart.clear();
                resultView.setText("");
                textView5.setText("");
                imageView3.setImageDrawable(null);
            }
        });

        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(v -> {
            if (isRunning) {
                sensorManager.unregisterListener(HeartActivity.this);
                isRunning = false;

                long elapsedTime = System.currentTimeMillis() - startTime;

                if (elapsedTime < 6400) {
                    Toast.makeText(HeartActivity.this, "测量时间不足，请重新测试", Toast.LENGTH_SHORT).show();
                    return; // 🚫 不再往下执行预测逻辑
                }

                List<Float> normalizedData = normalize(yDataList);
                String prediction = predict_result(normalizedData, HeartActivity.this);
                if (prediction.equals("NO!")) {
                    imageView3.setImageResource(R.drawable.no);
                    Toast.makeText(HeartActivity.this, "身份验证失败，返回登录页", Toast.LENGTH_SHORT).show();

                    imageView3.postDelayed(() -> {
                        Intent intent = new Intent(HeartActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }, 1500);
                } else {
                    imageView3.setImageResource(R.drawable.yes);
                }

                int y = heartrate(normalizedData);
                resetHeartRateChart(y);
            }
        });

    }

    private void resetChart() {
        chart.setNoDataText("");
        chart.clear();
        dataSet = new LineDataSet(new ArrayList<>(), "心跳振幅");
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(1.4f);
        dataSet.setColor(Color.rgb(255, 165, 0));
        lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.getAxisLeft().setEnabled(false);
        yDataList = new ArrayList<>();
        chart.invalidate();
    }

    private void resetHeartRateChart(int y) {
        ratechart.clear();
        heartRateDataSet = new LineDataSet(new ArrayList<>(), "心率");
        heartRateDataSet.setDrawCircles(true);
        heartRateDataSet.setDrawValues(false);
        heartRateDataSet.setCircleColor(Color.BLUE);
        heartRateDataSet.setLineWidth(2f);
        heartRateDataSet.setCircleRadius(3f);
        heartRateDataSet.setColor(Color.RED);
        Random a = new Random();
        for (int i = 1; i <= 7; i++) {
            heartRateDataSet.addEntry(new Entry(i, y - 3 + a.nextInt(6)));
        }
        heartRateLineData = new LineData(heartRateDataSet);
        ratechart.setData(heartRateLineData);
        ratechart.getAxisLeft().setEnabled(false);
        ratechart.invalidate();
    }

    public String predict_result(List<Float> normalizedData, Context context) {
        try {
            Module model = LiteModuleLoader.load(assetFilePath(this, "mobile_DRSN2.ptl"));
            float[] inputArray = new float[512];
            for (int i = 0; i < normalizedData.size(); i++) {
                inputArray[i] = normalizedData.get(i);
            }
            Tensor inputTensor = Tensor.fromBlob(inputArray, new long[]{1, 1, 512});
            float outputValue = model.forward(IValue.from(inputTensor)).toTensor().getDataAsFloatArray()[0];
            textView5.setText(new DecimalFormat("0.00").format(outputValue * 100) + "%");
            return outputValue > 0.8 ? "YES!" : "NO!";
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Prediction", "IOException: " + Log.getStackTraceString(e));
            return "IOException: " + e.getMessage();
        }
    }

    public List<Float> normalize(List<Float> data) {
        List<Float> filtered = data.subList(100, Math.min(500, data.size()));
        float min = Collections.min(filtered);
        float max = Collections.max(filtered);
        List<Float> normalized = new ArrayList<>();
        for (Float value : filtered) normalized.add((value - min) / (max - min));
        return normalized;
    }

    public int heartrate(List<Float> normalizedData) {
        List<Float> segment = normalizedData.subList(200, Math.min(362, normalizedData.size()));
        int maxIdx = segment.indexOf(Collections.max(segment));
        float secondMax = 0; int secondIdx = -1;
        int start = Math.max(0, maxIdx - 80), end = Math.min(segment.size(), maxIdx + 80);
        for (int i = start; i < end; i++) {
            if (segment.get(i) > secondMax && i != maxIdx) {
                secondMax = segment.get(i);
                secondIdx = i;
            }
        }
        int interval = Math.abs(maxIdx - secondIdx);
        return (interval < 60 || interval > 100) ? 70 : 6000 / interval;
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) return file.getAbsolutePath();
        try (InputStream is = context.getAssets().open(assetName);
             OutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
            os.flush();
            return file.getAbsolutePath();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRunning) {
            float y = event.values[1];
            yDataList.add(y);
            float t = (System.currentTimeMillis() - startTime) / 1000f;
            dataSet.addEntry(new Entry(t, y));
            lineData.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
            if (dataSet.getEntryCount() > 200) {
                chart.getXAxis().setAxisMinimum(dataSet.getEntryForIndex(dataSet.getEntryCount() - 200).getX());
                chart.getXAxis().setAxisMaximum(dataSet.getEntryForIndex(dataSet.getEntryCount() - 1).getX());
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (isRunning) sensorManager.registerListener(this, accelerometer, 10000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}









