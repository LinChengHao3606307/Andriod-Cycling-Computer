package com.example.speed;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    //6c84761d147a4bbfabf84702242507
    private TextView speedTxt, windSpeedTxt, precipeTxt, cloudTxt, uvTxt, tempTxt, slopeTxt, infoTxt, totalDistTxt, timeTakenTxt, avgSpeedTxt;
    private ImageView windDirImg, ringImg, cyclingPathImg, homeImg, currentPosImg, averageSpeedImg, timeTakenImg, totalDistImg;
    private ProgressBar progressBarImg;
    private Button setZeroBtn, moreInfoBtn;
    private ImageButton setFlageBtn, startStopBtn;

    private MySensor mySensors;
    private LoListener locationListener;
    private Handler handler = new Handler();
    private Runnable getAndShowWeatherInfo, updateUI;

    private float windDirv;
    private String city;
    private String requestInfo="precipitation";
    private boolean isInit, isMeasuring=true, showAverageData =true;
    private float fromLastPauseTime, tillLastPauseTime, lastPauseDistance, distanceTotal;
    private int screenWidth, speedTxtCenterY;
    private long startMeasuringTime;

    private ConstraintLayout parentLayout, mainPaddleLayout;
    private static ArrayList<Flag> flags = new ArrayList<>();
    private static Flag prevSelectedFlag, justSelectedFlag;
    private DrawArcView highlightedArc;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initViews();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.black));

        //location
        locationListener = new LoListener(System.currentTimeMillis());
        LocationManager locationManager = (LocationManager) getSystemService(this.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 5, locationListener);

        //intent
        restorePreviousStates();


        //sensor
        mySensors = new MySensor();
        mySensors.registerSensor(this);


        //constantly running process
        updateUI = new Runnable() {
            private final CharSequence windSpeedInitialTxt = windSpeedTxt.getText();
            @Override
            public void run() {

                if (!isInit) {
                    if (windSpeedTxt.getText() != windSpeedInitialTxt) {
                        initViewsLate();
                        isInit = true;
                        locationListener.distanceTotal = 0;
                        changeMeasuringState();
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                setHomeAndDestFlag();
                            }
                        };
                        handler.postDelayed(r,2000);

                        Runnable r1 = new Runnable() {
                            @Override
                            public void run() {
                                updatePathBetweenFlags();
                                handler.postDelayed(this,100);
                            }
                        };
                        handler.postDelayed(r1,2000);

                        Runnable r2 = new Runnable() {
                            private float time=0;
                            @Override
                            public void run() {
                                progressBarAnimation(time);
                                time += 0.01;
                                if (time < 3) {
                                    handler.postDelayed(this, 10);
                                }
                            }
                        };
                        handler.post(r2);
                    }
                    //Toast.makeText(MainActivity.this, String.valueOf(speedTxtTop[1]), Toast.LENGTH_LONG).show();
                }

                updateDestFlag();
                updateMainPaddleUI();
                updateOverallDataUI();
                handler.postDelayed(this, 100);
            }
        };
        handler.post(updateUI);


        getAndShowWeatherInfo = new Runnable() {
            private int count = 0;

            @Override
            public void run() {

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("https://api.weatherapi.com/v1/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                WeatherApiService service = retrofit.create(WeatherApiService.class);
                String pOfCall;
                if (city != "the city" && count < 1) {
                    pOfCall = city;
                } else {
                    pOfCall = locationListener.preLat + "," + locationListener.preLon;
                }
                Call<WeatherResponse> call = service.getCurrentWeather("6c84761d147a4bbfabf84702242507", pOfCall);
                call.enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        WeatherResponse weatherResponse = response.body();
                        if (weatherResponse != null) {
                            updateWeatherRelatedElements(weatherResponse);
                            count++;
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        //Toast.makeText(MainActivity.this, "Failed to fetch weather data", Toast.LENGTH_LONG).show();
                    }
                });
                handler.postDelayed(this, Math.min(480, 10 + 10 * count) * 1000);
            }
        };
        if (!isInit) {
            handler.postDelayed(getAndShowWeatherInfo, 5 * 1000);
        } else {
            handler.post(getAndShowWeatherInfo);
        }


    }


    private void updateDestFlag() {
        if (isMeasuring) {
            distanceTotal = (float) locationListener.distanceTotal;
            if (flags.size() > 1) {
                flags.get(1).timeTaken = fromLastPauseTime + tillLastPauseTime;
                flags.get(1).distanceFromStart = distanceTotal;
            }
        } else {
            distanceTotal = lastPauseDistance;
            if (flags.size() > 1) {
                flags.get(1).timeTaken = fromLastPauseTime;
                flags.get(1).distanceFromStart = lastPauseDistance;
            }
        }
        tillLastPauseTime = (float) (System.currentTimeMillis() - startMeasuringTime) / 1000;
    }

    public static void isClicked(View itemView) {
        for (Flag f:flags) {
            if (f.itemView == itemView) {
                prevSelectedFlag.setToUnselect();
                prevSelectedFlag = justSelectedFlag;
                justSelectedFlag = f;
                return;
            }
        }
    }

    private Flag putFlagAt(float ang, boolean isVisible) {
        Flag newFlag = new Flag(ang,screenWidth,speedTxtCenterY, MainActivity.this,parentLayout, fromLastPauseTime+tillLastPauseTime, distanceTotal, isVisible);
        return newFlag;
    }


    private void restorePreviousStates() {
        Intent intent = getIntent();
        isInit = intent.getBooleanExtra("initState",false);
        city = intent.getStringExtra("locationInfo");
        if (city == null) {
            city="the city";
        }
        screenWidth = intent.getIntExtra("screenWidth",0);
        speedTxtCenterY = intent.getIntExtra("speedTxtCenterY",0);
        float[] allFlagsDistArray = getIntent().getFloatArrayExtra("allFlagsDist");
        float[] allFlagsTimesArray = getIntent().getFloatArrayExtra("allFlagsTimes");
        if ((allFlagsDistArray!=null && allFlagsTimesArray!=null) && (screenWidth!=0 && speedTxtCenterY!=0)) {
            restoreFlags(allFlagsDistArray, allFlagsTimesArray);
            showOrHideAverageData();
            setCorrectViewSize();
            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    updatePathBetweenFlags();
                    handler.postDelayed(this,100);
                }
            };
            handler.post(r1);
            progressBarAnimation(10);
        }
        restorePause(intent);
    }
    private void restorePause(Intent intent) {
        lastPauseDistance = intent.getFloatExtra("pauseDist",0);
        fromLastPauseTime = intent.getFloatExtra("pauseTime",0);
        distanceTotal = lastPauseDistance;
        locationListener.distanceTotal = distanceTotal;
        if (!intent.getBooleanExtra("isPaused",false)) {
            changeMeasuringState();
        }
        changeMeasuringState();
    }
    private void restoreFlags(float[] allFlagsDistArray, float[] allFlagsTimesArray) {
        for (Flag f:flags) {
            parentLayout.removeView(f.itemView);
        }
        flags.clear();
        int count=0;
        for (float value : allFlagsDistArray) {
            float ang=0;
            if (count==0) {ang=-26;}
            if (count==1) {ang=22;}
            Flag newFlag = new Flag(ang,screenWidth,speedTxtCenterY, MainActivity.this,parentLayout, allFlagsTimesArray[count], value, count>1);
            flags.add(newFlag);
            count++;
        }
        flags.get(0).isSelected=true;
        flags.get(1).isSelected=true;
        prevSelectedFlag = flags.get(1);
        justSelectedFlag = flags.get(0);
        showAverageData =false;
    }


    private void initViews() {
        parentLayout = findViewById(R.id.main);
        mainPaddleLayout = findViewById(R.id.mainPaddleLayout);
        speedTxt = findViewById(R.id.speedTxt);
        slopeTxt = findViewById(R.id.slopeTxt);
        windSpeedTxt = findViewById(R.id.windSpeed);
        precipeTxt = findViewById(R.id.precipeTxt);
        cloudTxt = findViewById(R.id.cloudTxt);
        uvTxt = findViewById(R.id.uvTxt);
        tempTxt = findViewById(R.id.tempTxt);
        windDirImg = findViewById(R.id.windComming);
        infoTxt = findViewById(R.id.infoTxt);
        setZeroBtn = findViewById(R.id.setZeroBtn);
        moreInfoBtn = findViewById(R.id.moreInfoBtn);
        ringImg = findViewById(R.id.ringImgView);
        progressBarImg = findViewById(R.id.progressBar);

        cyclingPathImg = findViewById(R.id.cyclingPathImg);
        cyclingPathImg.setVisibility(View.INVISIBLE);

        totalDistTxt = findViewById(R.id.distanceTxt);
        timeTakenTxt = findViewById(R.id.timeTakenTxt);
        avgSpeedTxt = findViewById(R.id.averageSpeedTxt);
        totalDistImg = findViewById(R.id.distanceImg);
        timeTakenImg = findViewById(R.id.timeTakenImg);
        averageSpeedImg = findViewById(R.id.averageSpeedImg);

        startStopBtn = findViewById(R.id.startAndStopBtn);
        startStopBtn.setVisibility(View.GONE);
        setFlageBtn = findViewById(R.id.setFlagBtn);
        setFlageBtn.setVisibility(View.GONE);

        homeImg = findViewById(R.id.homeImg);
        homeImg.setVisibility(View.INVISIBLE);
        currentPosImg = findViewById(R.id.currentLocationImg);
        currentPosImg.setVisibility(View.INVISIBLE);
    }
    private void setCorrectViewSize() {
        ViewGroup.LayoutParams mainPaddleLayoutParams = mainPaddleLayout.getLayoutParams();
        mainPaddleLayoutParams.height = screenWidth;
        mainPaddleLayout.setLayoutParams(mainPaddleLayoutParams);
        ViewGroup.LayoutParams homeImgParams = homeImg.getLayoutParams();
        homeImgParams.height = screenWidth/2;
        homeImg.setLayoutParams(homeImgParams);
        ViewGroup.LayoutParams currentPosImgParams =currentPosImg.getLayoutParams();
        currentPosImgParams.height = screenWidth/2;
        currentPosImg.setLayoutParams(currentPosImgParams);
        ViewGroup.LayoutParams cyclingPathImgParams = cyclingPathImg.getLayoutParams();
        cyclingPathImgParams.height = screenWidth/2;
        cyclingPathImg.setLayoutParams(cyclingPathImgParams);
    }
    private void initViewsLate() {
        screenWidth = mainPaddleLayout.getWidth();
        int[] speedTxtTop = new int[2];
        speedTxt.getLocationOnScreen(speedTxtTop);
        speedTxtCenterY = speedTxt.getHeight()/2 + speedTxtTop[1];
        setCorrectViewSize();

        cyclingPathImg.setAlpha(255);
        cyclingPathImg.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
        cyclingPathImg.setVisibility(View.VISIBLE);
        AnimatedVectorDrawable avd = (AnimatedVectorDrawable) cyclingPathImg.getDrawable();
        avd.start();
        startStopBtn.setVisibility(View.VISIBLE);
        setFlageBtn.setVisibility(View.VISIBLE);
    }
    private void progressBarAnimation(float time) {
        if (time > 1) {
            progressBarImg.setVisibility(View.GONE);
            if (time < 3) {
                int v = (int) Math.max(160-160*(time-1), 20);
                int color = Color.rgb(v, v, v);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ringImg.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }
            } else {
                int color = Color.rgb(20,20,20);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ringImg.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }
            }
        } else {
            int v = (int) Math.min(120*time + 80, 160);
            int color = Color.rgb(v, v, v);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ringImg.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
    }


    private void updateMainPaddleUI(){
        if (locationListener.loChanged) {
            locationListener.loChanged = false;
            speedTxt.setText(locationListener.speedTxt);
        }
        if ((System.currentTimeMillis() - locationListener.prevTime) / 1000 > 10) {
            speedTxt.setText("000.0");
        }


        float north = (float) Math.toDegrees(-mySensors.orientationValues[0]);

        slopeTxt.setText(mySensors.getSlope());
        //TransitionManager.beginDelayedTransition((ViewGroup) slopeTxt.getRootView());
        windDirImg.setRotation(180 + windDirv + north);
    }
    private void updateOverallDataUI() {
        if (flags.size()>1) {
            if (flags.get(0).isSelected) {
                if (showAverageData) {
                    homeImg.setVisibility(View.VISIBLE);
                }
            } else {
                homeImg.setVisibility(View.INVISIBLE);
            }
            if (flags.get(1).isSelected) {
                if (showAverageData) {
                    currentPosImg.setVisibility(View.VISIBLE);
                }
            } else {
                currentPosImg.setVisibility(View.INVISIBLE);
            }
        }

        float distanceBetween=0;
        float timeBetween=1;
        if (justSelectedFlag!= null && prevSelectedFlag!=null) {
            distanceBetween = Math.max(justSelectedFlag.distanceFromStart, prevSelectedFlag.distanceFromStart) - Math.min(justSelectedFlag.distanceFromStart, prevSelectedFlag.distanceFromStart);
            timeBetween = Math.max(justSelectedFlag.timeTaken, prevSelectedFlag.timeTaken) - Math.min(justSelectedFlag.timeTaken, prevSelectedFlag.timeTaken);
        }
        totalDistTxt.setText(convertDistanceToString(distanceBetween));
        timeTakenTxt.setText(convertSecondsToTimeString(timeBetween));
        avgSpeedTxt.setText(convertSpeedToString(3600*distanceBetween/timeBetween));

        if (flags.size()>2) {
            float upperBound=18;
            for (int i=flags.size()-1; i>1; i--) {
                //Math.min(Math.max(0,(flags.get(i).distanceFromStart)/distanceTotal),1)
                float shoudBe = 25;
                if (distanceTotal !=0) {
                    shoudBe = -25 + 50 * ((flags.get(i).distanceFromStart) / distanceTotal);
                }
                //float upperBound=18-4.5f*(flags.size()-i-1);
                float lowerBound=-19+4.5f*(i-2);
                shoudBe = Math.max(shoudBe,lowerBound);
                shoudBe = Math.min(shoudBe,upperBound);
                upperBound = shoudBe-4.5f;
                flags.get(i).moveTo(MainActivity.this,shoudBe,screenWidth,speedTxtCenterY);
            }
        }
    }
    private void setHomeAndDestFlag() {
        fromLastPauseTime = 0;
        tillLastPauseTime = 0;
        flags.add(putFlagAt(-26, false));
        flags.add(putFlagAt(22, false));
        flags.get(0).isSelected = true;
        flags.get(1).isSelected = true;
        prevSelectedFlag = flags.get(1);
        justSelectedFlag = flags.get(0);
        cyclingPathImg.setAlpha(153);
        cyclingPathImg.setImageTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
        startMeasuringTime = System.currentTimeMillis();
        changeMeasuringState();
    }
    private void updatePathBetweenFlags() {
        if (showAverageData) {
            if (justSelectedFlag != null && prevSelectedFlag != null) {
                parentLayout.removeView(highlightedArc);
                float leftEnd = Math.min(justSelectedFlag.positionAng, prevSelectedFlag.positionAng);
                float rightEnd = Math.max(justSelectedFlag.positionAng, prevSelectedFlag.positionAng);
                leftEnd += 0.3;
                rightEnd += 0.3;
                if (leftEnd < -21) {
                    leftEnd = -25;
                }
                if (rightEnd > 21) {
                    rightEnd = 25;
                }
                DrawArcView arcView = new DrawArcView(this, screenWidth / 2, speedTxtCenterY, screenWidth, leftEnd, rightEnd);
                arcView.setId(View.generateViewId());
                parentLayout.addView(arcView);
                highlightedArc = arcView;
            }
        }
        else {
            parentLayout.removeView(highlightedArc);
        }
    }
    private void updateWeatherRelatedElements(WeatherResponse weatherResponse) {
        windSpeedTxt.setText(String.valueOf(weatherResponse.getWindSpeed()));
        precipeTxt.setText(String.valueOf(weatherResponse.getPrecip_mm()));
        cloudTxt.setText(String.valueOf(weatherResponse.getCloud()));
        uvTxt.setText(String.valueOf(weatherResponse.getUv()));
        tempTxt.setText(String.valueOf(weatherResponse.getTemp()));
        windDirv = (float) weatherResponse.getWindDir();
        city = weatherResponse.getCity();
    }


    public void changeMeasuringState() {
        if (isMeasuring) {
            isMeasuring = false;
            fromLastPauseTime += tillLastPauseTime;
            startStopBtn.setImageResource(R.drawable.ic_play);
            lastPauseDistance = distanceTotal;
        }
        else {
            isMeasuring = true;
            startMeasuringTime = System.currentTimeMillis();
            startStopBtn.setImageResource(R.drawable.ic_pause);
            distanceTotal = lastPauseDistance;
            locationListener.distanceTotal = distanceTotal;
        }
    }
    public void startMeasuringTotal(View view) {
        changeMeasuringState();
    }
    public void setFlage(View view) {
        if (!isMeasuring) {
            Toast.makeText(this,"Cannot set flag while stop",Toast.LENGTH_LONG).show();
        }
        else {
            flags.add(putFlagAt(18,true));
        }
    }
    public void setZero(View view) {
        if (mySensors.accelerometerReading[1]<8) {
            if (mySensors.zeroR == 0) {
                setZeroBtn.setText("USE DEFULT ZERO");
                mySensors.zeroR = mySensors.accelerometerReading[1];
            } else {
                setZeroBtn.setText("SET CURRENT TILT TO ZERO");
                mySensors.zeroR = 0;
            }
        }
        else {
            setZeroBtn.setText("PLEACE LAY DOWN THE DIVICE");
        }
    }
    public void visitWeb(View view) {
        Intent intent = new Intent(MainActivity.this, VisitWeatherMapActivity.class);
        intent.putExtra("requestType",requestInfo);
        intent.putExtra("currentState",isInit);
        intent.putExtra("locationCity",city);

        //Flage newFlage = new Flage(ang,screenWidth,speedTxtCenterY, MainActivity.this,parentLayout, fromLastPauseTime+tillLastPauseTime, distanceTotal, isVisible);
        intent.putExtra("screenWidth",screenWidth);
        intent.putExtra("speedTxtCenterY",speedTxtCenterY);
        float[] allFlagsTimes = new float[flags.size()];
        //ArrayList<Float> allFlagsTimes = new ArrayList<>();
        float[] allFlagsDist = new float[flags.size()];
        for (int i=0;i<flags.size();i++) {
            allFlagsTimes[i] = flags.get(i).timeTaken;
            allFlagsDist[i] = flags.get(i).distanceFromStart;
        }
        intent.putExtra("allFlagsTimes",allFlagsTimes);
        intent.putExtra("allFlagsDist",allFlagsDist);


        intent.putExtra("isPaused",!isMeasuring);
        if (isMeasuring) {
            changeMeasuringState();
        }
        intent.putExtra("pauseDist",lastPauseDistance);
        intent.putExtra("pauseTime",fromLastPauseTime);
        handler.removeCallbacks(updateUI);
        handler.removeCallbacks(getAndShowWeatherInfo);
        startActivity(intent);
    }
    public void showOrHideAverageData() {
        if (!showAverageData) {
            cyclingPathImg.setVisibility(View.VISIBLE);

            totalDistTxt.setVisibility(View.VISIBLE);
            timeTakenTxt.setVisibility(View.VISIBLE);
            avgSpeedTxt.setVisibility(View.VISIBLE);
            totalDistImg.setVisibility(View.VISIBLE);
            timeTakenImg.setVisibility(View.VISIBLE);
            averageSpeedImg.setVisibility(View.VISIBLE);

            startStopBtn.setVisibility(View.VISIBLE);
            setFlageBtn.setVisibility(View.VISIBLE);

            homeImg.setVisibility(View.VISIBLE);
            currentPosImg.setVisibility(View.VISIBLE);
            showAverageData =true;
            for (Flag f: flags) {
                f.itemView.setVisibility(View.VISIBLE);
            }
        }
        else {
            cyclingPathImg.setVisibility(View.GONE);

            totalDistTxt.setVisibility(View.GONE);
            timeTakenTxt.setVisibility(View.GONE);
            avgSpeedTxt.setVisibility(View.GONE);
            totalDistImg.setVisibility(View.GONE);
            timeTakenImg.setVisibility(View.GONE);
            averageSpeedImg.setVisibility(View.GONE);

            startStopBtn.setVisibility(View.GONE);
            setFlageBtn.setVisibility(View.GONE);

            homeImg.setVisibility(View.GONE);
            currentPosImg.setVisibility(View.GONE);
            showAverageData =false;
            for (Flag f: flags) {
                f.itemView.setVisibility(View.GONE);
            }
        }
    }



    public String convertSecondsToTimeString(float seconds) {
        int totalSeconds = (int) seconds;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        String formattedTime = String.format("%02dh%02dm", hours, minutes);
        return formattedTime;
    }
    public String convertDistanceToString(float dis) {
        float roundedDistance = Math.round(dis * 10.0f) / 10.0f;
        return String.format("%.1fkm", roundedDistance);
    }
    public String convertSpeedToString(float speed) {
        float roundedDistance = Math.round(speed * 10.0f) / 10.0f;
        return String.format("%.1fkm/h", roundedDistance);
    }


    public void introduceSlopeInfo(View view) {
        if (infoTxt.getVisibility() == View.GONE) {
            infoTxt.setText("Slope(%) that are accurete when static. Click this button to set zero/defult");
            infoTxt.setVisibility(View.VISIBLE);
            setZeroBtn.setVisibility(View.VISIBLE);
        }
        else {
            infoTxt.setText("");
            infoTxt.setVisibility(View.GONE);
            setZeroBtn.setVisibility(View.GONE);
            moreInfoBtn.setVisibility(View.GONE);
        }
        showOrHideAverageData();
    }
    public void introducePrecipeInfo(View view) {
        if (infoTxt.getVisibility() == View.GONE) {
            infoTxt.setText("Precipitation of "+city+"(mm). 0-1 recommended");
            infoTxt.setVisibility(View.VISIBLE);
            moreInfoBtn.setVisibility(View.VISIBLE);
            requestInfo="precipitation";
        }
        else {
            infoTxt.setText("");
            infoTxt.setVisibility(View.GONE);
            setZeroBtn.setVisibility(View.GONE);
            moreInfoBtn.setVisibility(View.GONE);
        }
        showOrHideAverageData();
    }
    public void introduceCloudInfo(View view) {
        if (infoTxt.getVisibility() == View.GONE) {
            infoTxt.setText("Cloud cover percentage of "+city+"(%)");
            infoTxt.setVisibility(View.VISIBLE);
            moreInfoBtn.setVisibility(View.VISIBLE);
            requestInfo="cloud";
        }
        else {
            infoTxt.setText("");
            infoTxt.setVisibility(View.GONE);
            setZeroBtn.setVisibility(View.GONE);
            moreInfoBtn.setVisibility(View.GONE);
        }
        showOrHideAverageData();
    }
    public void introduceWindInfo(View view) {
        if (infoTxt.getVisibility() == View.GONE) {
            infoTxt.setText("Wind speed of "+city+"(km/h) with direction indicated by the above arrow");
            infoTxt.setVisibility(View.VISIBLE);
            moreInfoBtn.setVisibility(View.VISIBLE);
            requestInfo="wind";
        }
        else {
            infoTxt.setText("");
            infoTxt.setVisibility(View.GONE);
            setZeroBtn.setVisibility(View.GONE);
            moreInfoBtn.setVisibility(View.GONE);
        }
        showOrHideAverageData();
    }
    public void introduceUvInfo(View view) {
        if (infoTxt.getVisibility() == View.GONE) {
            infoTxt.setText("Ultraviolet(UV) of "+city+". 0-6 recommended");
            infoTxt.setVisibility(View.VISIBLE);
            //moreInfoBtn.setVisibility(View.VISIBLE);
        }
        else {
            infoTxt.setText("");
            infoTxt.setVisibility(View.GONE);
            setZeroBtn.setVisibility(View.GONE);
            moreInfoBtn.setVisibility(View.GONE);
        }
        showOrHideAverageData();
    }
    public void introduceTempInfo(View view) {
        if (infoTxt.getVisibility() == View.GONE) {
            infoTxt.setText("Temperature of "+city+"(celsius)");
            infoTxt.setVisibility(View.VISIBLE);
            moreInfoBtn.setVisibility(View.VISIBLE);
            requestInfo="temperature";
        }
        else {
            infoTxt.setText("");
            infoTxt.setVisibility(View.GONE);
            setZeroBtn.setVisibility(View.GONE);
            moreInfoBtn.setVisibility(View.GONE);
        }
        showOrHideAverageData();
    }


    @Override
    protected void onResume() {
        super.onResume();
        mySensors.registerSensor(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        mySensors.unregisterSensor(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mySensors.unregisterSensor(this);
    }
}