package com.example.healthcompanion.utils;

import com.example.healthcompanion.model.HealthData;

import org.json.JSONObject;

public class DataParser {

    private DataParser() {
        // Utility class
    }

    public static HealthData parse(String rawData) throws Exception {

        if (rawData == null) {
            throw new IllegalArgumentException("Data is null");
        }

        String cleaned = rawData.trim();

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Data is empty");
        }

        HealthData healthData = new HealthData();

        // 1. Try JSON parsing
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {

            try {

                JSONObject object = new JSONObject(cleaned);

                // Heart Rate
                if (object.has("heartRate")) healthData.setHeartRate(object.optInt("heartRate"));
                else if (object.has("heart_rate")) healthData.setHeartRate(object.optInt("heart_rate"));
                else if (object.has("hr")) healthData.setHeartRate(object.optInt("hr"));
                else if (object.has("bpm")) healthData.setHeartRate(object.optInt("bpm"));

                // SpO2
                if (object.has("spo2")) healthData.setSpo2(object.optInt("spo2"));
                else if (object.has("spO2")) healthData.setSpo2(object.optInt("spO2"));
                else if (object.has("SPO2")) healthData.setSpo2(object.optInt("SPO2"));
                else if (object.has("oximeter")) healthData.setSpo2(object.optInt("oximeter"));

                // Temperature
                if (object.has("temperature")) healthData.setTemperature((float) object.optDouble("temperature"));
                else if (object.has("temp")) healthData.setTemperature((float) object.optDouble("temp"));

                // Humidity
                if (object.has("humidity")) healthData.setHumidity((float) object.optDouble("humidity"));
                else if (object.has("hum")) healthData.setHumidity((float) object.optDouble("hum"));

                // Activity
                if (object.has("activity")) healthData.setActivity(object.optInt("activity"));
                else if (object.has("act")) healthData.setActivity(object.optInt("act"));

                // Fall
                if (object.has("fall")) healthData.setFall(object.optInt("fall"));

                return healthData;

            } catch (Exception ignored) {
                // Fallthrough to other parsers
            }
        }

        // 2. Try Key=Value format (e.g., HR=72, SPO2=98, TEMP=36.5, HUM=50, ACT=0, FALL=0)
        if (cleaned.contains("=")) {

            String[] pairs = cleaned.split("[,;\\n]");

            boolean foundKey = false;

            for (String pair : pairs) {

                String[] kv = pair.split("=");

                if (kv.length == 2) {

                    String key = kv[0].trim().toLowerCase();
                    String valStr = kv[1].trim();

                    try {

                        if (key.contains("hr") || key.contains("heart") || key.contains("bpm")) {

                            healthData.setHeartRate(Integer.parseInt(valStr));

                            foundKey = true;

                        } else if (key.contains("spo2") || key.contains("ox")) {

                            healthData.setSpo2(Integer.parseInt(valStr));

                            foundKey = true;

                        } else if (key.contains("temp")) {

                            healthData.setTemperature(Float.parseFloat(valStr));

                            foundKey = true;

                        } else if (key.contains("hum")) {

                            healthData.setHumidity(Float.parseFloat(valStr));

                            foundKey = true;

                        } else if (key.contains("act")) {

                            healthData.setActivity(Integer.parseInt(valStr));

                            foundKey = true;

                        } else if (key.contains("fall")) {

                            healthData.setFall(Integer.parseInt(valStr));

                            foundKey = true;
                        }

                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (foundKey) {
                return healthData;
            }
        }

        // 3. Try CSV format (e.g. "72,98,36.5,50,0,0")
        String[] tokens = cleaned.split("[,;]");

        if (tokens.length >= 2) {

            try {

                if (tokens.length >= 1) healthData.setHeartRate((int) Float.parseFloat(tokens[0].trim()));
                if (tokens.length >= 2) healthData.setSpo2((int) Float.parseFloat(tokens[1].trim()));
                if (tokens.length >= 3) healthData.setTemperature(Float.parseFloat(tokens[2].trim()));
                if (tokens.length >= 4) healthData.setHumidity(Float.parseFloat(tokens[3].trim()));
                if (tokens.length >= 5) healthData.setActivity((int) Float.parseFloat(tokens[4].trim()));
                if (tokens.length >= 6) healthData.setFall((int) Float.parseFloat(tokens[5].trim()));

                return healthData;

            } catch (NumberFormatException ignored) {
            }
        }

        // 4. Try single numeric value (assume heart rate)
        try {

            int singleValue = Integer.parseInt(cleaned);

            healthData.setHeartRate(singleValue);

            return healthData;

        } catch (NumberFormatException ignored) {
        }

        throw new IllegalArgumentException("Unable to parse data format: " + rawData);
    }
}
