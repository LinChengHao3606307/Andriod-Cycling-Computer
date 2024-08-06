package com.example.speed;

import android.location.Location;
import android.location.LocationListener;

import androidx.annotation.NonNull;

public class LoListener implements LocationListener {
    public boolean loChanged;
    public Long prevTime, now;
    public double preLon, preLat;
    public double distanceTotal;
    public String speedTxt;
    private final double adjustSpeedConst = 1;
    private final double calcuConst = 4003017.359204114544449100198974742575044;
    private final double accumuDistConst = 111.282193107158453824654523398;

    public LoListener(Long prevTime) {
        this.prevTime = prevTime;
        this.now = prevTime;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        now = System.currentTimeMillis();
        double nowLon=location.getLongitude();
        double nowLat=location.getLatitude();
        loChanged = true;
        Long lastFiveMeterTake = now - prevTime;
        prevTime=now;

        double dLatM = (nowLat - preLat);
        double dLonM = (nowLon - preLon);
        double distanceOnXBall = Math.sqrt(dLatM*dLatM+dLonM*dLonM);
        double distanceInKm = accumuDistConst*distanceOnXBall;
        double distanceN = distanceOnXBall*calcuConst;
        if (distanceInKm>999) {distanceInKm=0;}
        distanceInKm = Math.sqrt(distanceInKm*distanceInKm - Math.min(0.000001,distanceInKm*distanceInKm));
        distanceTotal += distanceInKm;
        double speedValue = 100*distanceN / lastFiveMeterTake;

        speedValue = Math.sqrt(speedValue*speedValue - Math.min(speedValue,adjustSpeedConst));
        if (speedValue>999) {speedValue=999;}

        speedTxt = String.valueOf(speedValue);
        if (speedTxt.indexOf('.') == -1) {speedTxt = speedTxt + ".0";}
        while (!inRightFormat(speedTxt)) {
            speedTxt = "0" + speedTxt;
        }
        speedTxt = speedTxt.substring(0,5);
        preLat=nowLat;
        preLon=nowLon;

    }


    private double calculateSpeed(double preLat, double preLon, double lat, double lon, Long timeInterval) {
        double dLatM = (lat - preLat)*calcuConst;
        double dLonM = (lon - preLon)*calcuConst;
        double distanceM = Math.sqrt(dLatM*dLatM+dLonM*dLonM);
        double speed = 100*distanceM / timeInterval;
        speed = Math.sqrt(speed*speed - Math.min(speed*speed,adjustSpeedConst));
        if (speed>999) {speed=999;}
        return speed;
    }

    private boolean inRightFormat(String txt) {
        if (txt.length() < 4) {return false;}
        if (txt.charAt(3) != '.') {return false;}
        return true;
    }


}
