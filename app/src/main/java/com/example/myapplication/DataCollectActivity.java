package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

public class DataCollectActivity extends AppCompatActivity implements SensorEventListener {
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
    private ProgressBar progressBar;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private final String text = "用“心”解锁";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collectdata);
        getSupportActionBar().hide();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        chart = findViewById(R.id.chart);
        ratechart = findViewById(R.id.rateChart);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setProgress(0);

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
                sensorManager.registerListener(DataCollectActivity.this, accelerometer, 10000);
                isRunning = true;
                startTime = System.currentTimeMillis();
                yDataList.clear();
                resetChart();
                ratechart.clear();
                resultView.setText("");
                textView5.setText("");
                imageView3.setImageDrawable(null);
                progressBar.setProgress(0);

                progressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (isRunning) {
                            long elapsedTime = System.currentTimeMillis() - startTime;
                            int percentage = (int) Math.min(100, (elapsedTime * 100) / 2560000);
                            progressBar.setProgress(percentage);
                            progressHandler.postDelayed(this, 200);
                        }
                    }
                };
                progressHandler.post(progressRunnable);
            }
        });

        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(v -> {
            if (isRunning) {
                sensorManager.unregisterListener(DataCollectActivity.this);
                isRunning = false;
                progressHandler.removeCallbacks(progressRunnable);

                long elapsedTime = System.currentTimeMillis() - startTime;

                if (elapsedTime < 6400) {
                    Toast.makeText(DataCollectActivity.this, "测量时间不足，请重新测试", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Float> normalizedData = normalize(yDataList);
                int y = heartrate(normalizedData);
                resetHeartRateChart(y);
                predict_result(normalizedData, DataCollectActivity.this, elapsedTime);
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

    public String predict_result(List<Float> normalizedData, Context context, long elapsedTime) {
        try {
            Module model = LiteModuleLoader.load(assetFilePath(this, "mobile_DRSN2.ptl"));
            float[] inputArray = new float[512];
            for (int i = 0; i < normalizedData.size(); i++) {
                inputArray[i] = normalizedData.get(i);
            }
            Tensor inputTensor = Tensor.fromBlob(inputArray, new long[]{1, 1, 512});
            float outputValue = model.forward(IValue.from(inputTensor)).toTensor().getDataAsFloatArray()[0];

            double percentage = (double) elapsedTime / 2560000.0 * 100.0;
            textView5.setText("完整度：" + new DecimalFormat("0.00").format(percentage) + "%");

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

