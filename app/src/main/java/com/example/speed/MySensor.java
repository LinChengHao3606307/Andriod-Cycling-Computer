package com.example.speed;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MySensor implements SensorEventListener {

    private Sensor accelerometer;
    private Sensor magnetometer;
    public float[] orientationValues = new float[3];
    private float[] rotationMatrix = new float[9];
    public float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];
    private String slope = new String();
    public float zeroR=0;

    public String getSlope() {
        return slope;
    }

    public void registerSensor(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterSensor(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerReading = sensorEvent.values;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerReading = sensorEvent.values;
        }

        if (accelerometer != null && magnetometer != null) {
            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
            SensorManager.getOrientation(rotationMatrix, orientationValues);
        }
        slope = String.valueOf(calcuSlope(accelerometerReading[1],zeroR));
        if (slope.indexOf('.')>0) { slope = slope.substring(0,slope.indexOf('.')); }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //R: reading when set 0; cr: current reading
    public float calcuSlope(float cr, float R) {
        float RS = R*R;
        float crs = cr*cr;
        double up = 100*( R * Math.sqrt(100-RS) - cr * Math.sqrt(100-crs) );
        return (float) (up / (RS+crs-100));
    }
}
