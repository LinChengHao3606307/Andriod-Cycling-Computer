package com.example.speed;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class VisitWeatherMapActivity extends AppCompatActivity {

    private WebView webView;
    private String city;
    private static ArrayList<Flage> flags = new ArrayList<>();
    private boolean isPaused;
    private float lastPauseDistance, fromLastPauseTime;
    private int screenWidth, speedTxtCenterY;
    private ArrayList<Float> allFlagsTimes = new ArrayList<>();
    private ArrayList<Float> allFlagsDist  = new ArrayList<>();
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_weather_map);
        webView = findViewById(R.id.webView);
        Intent intent = getIntent();
        String type = intent.getStringExtra("requestType");
        city = intent.getStringExtra("locationCity");
        if (city == null) {
            city = "the city";
        }

        screenWidth = intent.getIntExtra("screenWidth",0);
        speedTxtCenterY = intent.getIntExtra("speedTxtCenterY",0);
        float[] allFlagsDistArray = getIntent().getFloatArrayExtra("allFlagsDist");
        float[] allFlagsTimesArray = getIntent().getFloatArrayExtra("allFlagsTimes");
        if (allFlagsDistArray != null) {
            for (float value : allFlagsDistArray) {
                allFlagsDist.add(value);
            }
        }
        if (allFlagsTimesArray != null) {
            for (float value : allFlagsTimesArray) {
                allFlagsTimes.add(value);
            }
        }
        //intent.putExtra("flags states",flags);
        isPaused = intent.getBooleanExtra("isPaused",true);
        //intent.putExtra("isPaused",!isMeasuring);
        lastPauseDistance = intent.getFloatExtra("pauseDist",0);
        //intent.putExtra("pauseDist",lastPauseDistance);
        fromLastPauseTime = intent.getFloatExtra("pauseTime",0);
        //intent.putExtra("pauseTime",fromLastPauseTime);
        webView.loadUrl("https://www.msn.com/zh-cn/weather/maps/" + type);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
    }
    public void goBack(View view) {
        Intent intent = new Intent(VisitWeatherMapActivity.this, MainActivity.class);
        intent.putExtra("initState",true);
        intent.putExtra("locationInfo", city);

        intent.putExtra("screenWidth",screenWidth);
        intent.putExtra("speedTxtCenterY",speedTxtCenterY);
        float[] allFlagsDistArray = new float[allFlagsDist.size()];
        for (int i = 0; i < allFlagsDist.size(); i++) {
            allFlagsDistArray[i] = allFlagsDist.get(i);
        }
        intent.putExtra("allFlagsDist",allFlagsDistArray);

        float[] allFlagsTimesArray = new float[allFlagsTimes.size()];
        for (int i = 0; i < allFlagsTimes.size(); i++) {
            allFlagsTimesArray[i] = allFlagsTimes.get(i);
        }
        intent.putExtra("allFlagsTimes",allFlagsTimesArray);

        intent.putExtra("isPaused",isPaused);
        intent.putExtra("pauseDist",lastPauseDistance);
        intent.putExtra("pauseTime",fromLastPauseTime);
        startActivity(intent);
    }
}