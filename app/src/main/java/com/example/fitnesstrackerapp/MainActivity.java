package com.example.fitnesstrackerapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_ACTIVITY_RECOGNITION = 1001;

    TextView tvSteps, tvDistance, tvCalories;
    SensorManager sensorManager;
    Sensor stepSensor;
    int initialStepCount = -1;
    int currentSteps = 0;

    LineChart lineChart;
    ArrayList<Entry> stepEntries = new ArrayList<>();
    int timeIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSteps = findViewById(R.id.tvSteps);
        tvDistance = findViewById(R.id.tvDistance);
        tvCalories = findViewById(R.id.tvCalories);
        lineChart = findViewById(R.id.lineChart);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (hasActivityRecognitionPermission()) {
            initStepSensor();
        } else {
            requestActivityRecognitionPermission();
        }

        setupChart();
    }

    private void initStepSensor() {
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount < 0) {
                initialStepCount = (int) event.values[0];
            }

            currentSteps = (int) event.values[0] - initialStepCount;
            float distance = currentSteps * 0.7f;   // 0.7 meters per step
            float calories = currentSteps * 0.04f;  // 0.04 kcal per step

            tvSteps.setText("Steps: " + currentSteps);
            tvDistance.setText(String.format("Distance: %.2f m", distance));
            tvCalories.setText(String.format("Calories: %.2f kcal", calories));

            stepEntries.add(new Entry(timeIndex++, currentSteps));
            LineDataSet dataSet = new LineDataSet(stepEntries, "Steps Over Time");
            dataSet.setColor(ContextCompat.getColor(this, R.color.purple_700));
            dataSet.setLineWidth(2f);

            LineData data = new LineData(dataSet);
            lineChart.setData(data);
            lineChart.invalidate();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed
    }

    private void setupChart() {
        lineChart.getDescription().setText("Live Step Count");
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.setTouchEnabled(false);
        lineChart.setPinchZoom(false);
    }

    private boolean hasActivityRecognitionPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    REQUEST_ACTIVITY_RECOGNITION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initStepSensor();
            } else {
                tvSteps.setText("Permission denied. Cannot track steps.");
            }
        }
    }
}
