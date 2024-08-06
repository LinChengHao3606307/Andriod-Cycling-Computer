package com.example.speed;
import com.google.gson.annotations.SerializedName;

public class WeatherResponse {
    @SerializedName("location")
    private Location location;

    @SerializedName("current")
    private CurrentWeather currentWeather;

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public CurrentWeather getCurrentWeather() {
        return currentWeather;
    }

    public void setCurrentWeather(CurrentWeather currentWeather) {
        this.currentWeather = currentWeather;
    }

    public double getWindSpeed() {
        return currentWeather.getWind_kph();
    }
    public double getWindDir() {
        return currentWeather.getWind_degree();
    }
    public double getPrecip_mm() {
        return currentWeather.getPrecip_mm();
    }
    public double getCloud() {
        return currentWeather.getCloud();
    }
    public double getUv() {
        return currentWeather.getUv();
    }
    public double getTemp() { return  currentWeather.getTempC();}
    public String getCity() { return location.getName();}
}

class Location {
    private String name;
    private String region;
    private String country;
    private double lat;
    private double lon;
    private String tz_id;
    private long localtime_epoch;
    private String localtime;

    public String getName() {
        return name;
    }
    // Getters and setters
}

class CurrentWeather {
    @SerializedName("last_updated_epoch")
    private long lastUpdatedEpoch;

    @SerializedName("last_updated")
    private String lastUpdated;

    //TODO
    @SerializedName("temp_c")
    private double tempC;

    @SerializedName("temp_f")
    private double tempF;

    @SerializedName("is_day")
    private int isDay;

    private Condition condition;
    private double wind_mph;
    private double wind_kph;
    private int wind_degree;
    private String wind_dir;
    private double pressure_mb;
    private double pressure_in;
    //TODO
    private double precip_mm;
    private double precip_in;
    private int humidity;
    //TODO
    private int cloud;
    private double feelslike_c;
    private double feelslike_f;
    private double windchill_c;
    private double windchill_f;
    private double heatindex_c;
    private double heatindex_f;
    private double dewpoint_c;
    private double dewpoint_f;
    private double vis_km;
    private double vis_miles;
    //TODO
    private double uv;
    private double gust_mph;
    private double gust_kph;

    public double getTempC() {
        return tempC;
    }

    public double getWind_kph() {
        return wind_kph;
    }

    public int getWind_degree() {
        return wind_degree;
    }

    public double getPrecip_mm() {
        return precip_mm;
    }

    public double getCloud() {
        return cloud;
    }

    public double getUv() {
        return uv;
    }


}

class Condition {
    private String text;
    private String icon;
    private int code;

    // Getters and setters
}
