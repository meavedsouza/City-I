package com.city_i.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final long UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_INTERVAL = 5000; // 5 seconds
    private static final float MIN_DISTANCE = 10; // 10 meters

    private final IBinder binder = new LocalBinder();
    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;

    // Listeners for location updates
    private List<LocationUpdateListener> listeners = new ArrayList<>();

    public interface LocationUpdateListener {
        void onLocationUpdated(Location location);
        void onLocationError(String error);
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService created");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationService started");
        startLocationUpdates();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        Log.d(TAG, "LocationService destroyed");
    }

    private void setupLocationUpdates() {
        // Create location request
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(MIN_DISTANCE);

        // Create location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = location;
                        notifyLocationUpdate(location);
                        Log.d(TAG, "New location: " + location.getLatitude() + ", " + location.getLongitude());
                    }
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            notifyLocationError("Location permission not granted");
            return;
        }

        try {
            // Try FusedLocationProvider first (more accurate)
            fusedLocationClient.requestLocationUpdates(
                    createLocationRequest(),
                    locationCallback,
                    null
            );

            Log.d(TAG, "Fused location updates started");

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());

            // Fallback to Android LocationManager
            try {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        UPDATE_INTERVAL,
                        MIN_DISTANCE,
                        locationListener
                );

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        UPDATE_INTERVAL,
                        MIN_DISTANCE,
                        locationListener
                );

                Log.d(TAG, "LocationManager updates started");

            } catch (SecurityException se) {
                Log.e(TAG, "LocationManager SecurityException: " + se.getMessage());
                notifyLocationError("Location permission denied");
            }
        }
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(MIN_DISTANCE);
        return locationRequest;
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }

        Log.d(TAG, "Location updates stopped");
    }

    // Traditional LocationListener (fallback)
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            currentLocation = location;
            notifyLocationUpdate(location);
            Log.d(TAG, "Location changed: " + location.getLatitude() + ", " + location.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "Provider " + provider + " status changed: " + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "Provider " + provider + " enabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "Provider " + provider + " disabled");
            notifyLocationError("Location provider disabled");
        }
    };

    // Public methods
    public Location getCurrentLocation() {
        return currentLocation;
    }

    public GeoPoint getCurrentGeoPoint() {
        if (currentLocation != null) {
            return new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
        }
        return null;
    }

    public double getLatitude() {
        return currentLocation != null ? currentLocation.getLatitude() : 0.0;
    }

    public double getLongitude() {
        return currentLocation != null ? currentLocation.getLongitude() : 0.0;
    }

    public boolean hasLocation() {
        return currentLocation != null;
    }

    public float calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0]; // Distance in meters
    }

    public float distanceToIssue(double issueLat, double issueLon) {
        if (currentLocation == null) return -1;

        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                issueLat,
                issueLon,
                results
        );
        return results[0];
    }

    // Listener management
    public void addLocationListener(LocationUpdateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeLocationListener(LocationUpdateListener listener) {
        listeners.remove(listener);
    }

    private void notifyLocationUpdate(Location location) {
        for (LocationUpdateListener listener : listeners) {
            listener.onLocationUpdated(location);
        }
    }

    private void notifyLocationError(String error) {
        for (LocationUpdateListener listener : listeners) {
            listener.onLocationError(error);
        }
    }

    // Static helper methods
    public static String formatDistance(float meters) {
        if (meters < 1000) {
            return String.format("%.0f m", meters);
        } else {
            return String.format("%.1f km", meters / 1000);
        }
    }

    public static boolean isLocationPermissionGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}