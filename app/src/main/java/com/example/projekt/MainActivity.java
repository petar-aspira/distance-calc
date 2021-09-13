package com.example.projekt;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {

    private TextView dayDistanceLabel;
    private TextView monthDistanceLabel;

    private LocationListener locationListener;
    private LocationManager locationManager;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();

        dayDistanceLabel = findViewById(R.id.dayDistanceLabel);
        monthDistanceLabel = findViewById(R.id.monthDistanceLabel);
        SharedPreferences sharedPreferences = this.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Button button = findViewById(R.id.resetAll);
        button.setOnClickListener(v -> {
            editor.clear().apply();
            dayDistanceLabel.setText("0");
            monthDistanceLabel.setText("0");
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd");
        DateTimeFormatter mtf = DateTimeFormatter.ofPattern("uuuu-MM");
        LocalDate localDate = LocalDate.now();
        String dayDate = dtf.format(localDate);
        String monthDate = mtf.format(localDate);

        double previousLat = sharedPreferences.getFloat("previous_lat", 0);
        double previousLong = sharedPreferences.getFloat("previous_long", 0);
        double todayDistance = sharedPreferences.getFloat("distance_day_" + dayDate, 0);
        double monthDistance = sharedPreferences.getFloat("distance_month_" + monthDate, 0);

        if (todayDistance != 0) {
            dayDistanceLabel.setText(String.valueOf(todayDistance));
        }
        if (monthDistance != 0) {
            monthDistanceLabel.setText(String.valueOf(monthDistance));
        }

        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                // dont calculate if starting location hasn't been set
                if (previousLat == 0) {
                    editor.putFloat("previous_lat", (float) location.getLatitude());
                    editor.putFloat("previous_long", (float) location.getLongitude());
                } else {
                    double distance = calculateDistance(previousLat, (float) location.getLatitude(), previousLong, (float) location.getLongitude());

                    // save values to shared preferences
                    editor.putFloat("previous_lat", (float) location.getLatitude());
                    editor.putFloat("previous_long", (float) location.getLongitude());
                    editor.putFloat("distance_day_" + dayDate, (float) (todayDistance + distance));
                    editor.putFloat("distance_month_" + monthDate, (float) (monthDistance + distance));

                    dayDistanceLabel.setText(String.valueOf(todayDistance + distance));
                    monthDistanceLabel.setText(String.valueOf(monthDistance + distance));
                }
                editor.apply();

            }
            public double calculateDistance(double lat1, double lat2, double long1, double long2) {
                double theta = long1 - long2;
                double dist = Math.sin(deg2rad(lat1))
                        * Math.sin(deg2rad(lat2))
                        + Math.cos(deg2rad(lat1))
                        * Math.cos(deg2rad(lat2))
                        * Math.cos(deg2rad(theta));
                dist = Math.acos(dist);
                dist = rad2deg(dist);
                dist = dist * 60 * 1.1515;

                return (float) dist;
            }

            private double deg2rad(double deg) {
                return (deg * Math.PI / 180.0);
            }

            private double rad2deg(double rad) {
                return (rad * 180.0 / Math.PI);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        locationManager = (LocationManager) getSystemService (Context.LOCATION_SERVICE);
        String provider = locationManager.getBestProvider(new Criteria(), true);

        try {
            if (provider != null) {
                locationManager.requestLocationUpdates(provider, 1000, 20, locationListener);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Greška u dozvolama", Toast.LENGTH_SHORT).show();
        }
    }

    public void checkAndRequestPermissions() {
        String LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION;
        if (ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            int PERMISSIONS_REQUEST_CODE = 1240;
            ActivityCompat.requestPermissions(this, new String[]{LOCATION_PERMISSION}, PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationManager = null;
            locationListener = null;
        }
    }
}