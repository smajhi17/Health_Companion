package com.example.healthcompanion.model;

public class HealthData {

    private int heartRate;
    private int spo2;
    private float temperature;
    private float humidity;
    private int activity;
    private int fall;

    public HealthData() {
        heartRate = 0;
        spo2 = 0;
        temperature = 0;
        humidity = 0;
        activity = 0;
        fall = 0;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public int getSpo2() {
        return spo2;
    }

    public void setSpo2(int spo2) {
        this.spo2 = spo2;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public int getActivity() {
        return activity;
    }

    public void setActivity(int activity) {
        this.activity = activity;
    }

    public int getFall() {
        return fall;
    }

    public void setFall(int fall) {
        this.fall = fall;
    }
}
