package com.city_i.ai;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.city_i.services.LocationService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LocationAnalyzer {
    private static final String TAG = "LocationAnalyzer";

    private Context context;
    private LocationService locationService;

    // Coordinates for important locations in Goa (approximate)
    private static final double[][] HOSPITALS = {
            {15.4909, 73.8278},  // Goa Medical College, Bambolim
            {15.5546, 73.7411},  // Victor Hospital, Margao
            {15.4986, 73.8255},  // Healthway Hospital
            {15.3926, 73.8789}   // Manipal Hospital
    };

    private static final double[][] SCHOOLS = {
            {15.5546, 73.7411},  // Margao area schools
            {15.4909, 73.8278},  // Panaji area schools
            {15.3926, 73.8789}   // Vasco area schools
    };

    private static final double[][] GOVERNMENT_OFFICES = {
            {15.4986, 73.8255},  // Secretariat, Panaji
            {15.5546, 73.7411},  // South Goa Collector
            {15.3926, 73.8789}   // Mormugao Port Trust
    };

    private static final double[][] TOURIST_SPOTS = {
            {15.5515, 73.7555},  // Colva Beach
            {15.5939, 73.7427},  // Benaulim Beach
            {15.4094, 73.7882},  // Sinquerim Beach
            {15.4165, 73.7705},  // Fort Aguada
            {15.5036, 73.7653},  // Old Goa Churches
            {15.4022, 74.0154}   // Dudhsagar Falls
    };

    // Residential area boundaries (simplified)
    private static final double[][] RESIDENTIAL_ZONES = {
            {15.45, 73.80},  // Panaji residential
            {15.55, 73.74},  // Margao residential
            {15.40, 73.85}   // Vasco residential
    };

    // Commercial area boundaries
    private static final double[][] COMMERCIAL_ZONES = {
            {15.4986, 73.8255},  // Panaji commercial
            {15.5546, 73.7411},  // Margao commercial
            {15.3926, 73.8789}   // Vasco commercial
    };

    // Distance thresholds in meters
    private static final int CRITICAL_DISTANCE = 500;    // 500m from critical infrastructure
    private static final int HIGH_PRIORITY_DISTANCE = 1000; // 1km from important locations
    private static final int MEDIUM_PRIORITY_DISTANCE = 2000; // 2km

    public LocationAnalyzer(Context context) {
        this.context = context;
        Log.d(TAG, "Location analyzer initialized");
    }

    public void setLocationService(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * Calculate location-based priority (1-10)
     */
    public int calculateLocationPriority(double latitude, double longitude) {
        try {
            Log.d(TAG, String.format("Calculating location priority for: %.6f, %.6f", latitude, longitude));

            int priorityScore = 5; // Default medium priority

            // Check proximity to critical infrastructure
            if (isNearCriticalInfrastructure(latitude, longitude)) {
                priorityScore += 3;
                Log.d(TAG, "Near critical infrastructure: +3");
            }

            // Check if in residential area
            if (isInResidentialArea(latitude, longitude)) {
                priorityScore += 2;
                Log.d(TAG, "In residential area: +2");
            }

            // Check if in commercial area
            if (isInCommercialArea(latitude, longitude)) {
                priorityScore += 1;
                Log.d(TAG, "In commercial area: +1");
            }

            // Check if near tourist spot
            if (isNearTouristSpot(latitude, longitude)) {
                priorityScore += 2;
                Log.d(TAG, "Near tourist spot: +2");
            }

            // Check if near schools (higher priority during school hours)
            if (isNearSchool(latitude, longitude)) {
                priorityScore += 2;
                Log.d(TAG, "Near school: +2");
            }

            // Check population density (simplified)
            int densityScore = estimatePopulationDensity(latitude, longitude);
            priorityScore += densityScore;
            Log.d(TAG, "Population density score: +" + densityScore);

            // Adjust based on distance from city center
            priorityScore = adjustByDistanceFromCenter(priorityScore, latitude, longitude);

            // Ensure within bounds
            priorityScore = Math.max(1, Math.min(10, priorityScore));

            Log.d(TAG, "Final location priority: " + priorityScore);

            return priorityScore;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating location priority: " + e.getMessage());
            return 5; // Default
        }
    }

    /**
     * Check if location is near critical infrastructure
     */
    private boolean isNearCriticalInfrastructure(double lat, double lon) {
        // Check hospitals
        for (double[] hospital : HOSPITALS) {
            double distance = calculateDistance(lat, lon, hospital[0], hospital[1]);
            if (distance <= CRITICAL_DISTANCE) {
                Log.d(TAG, String.format("Near hospital (%.2fm)", distance));
                return true;
            }
        }

        // Check government offices
        for (double[] office : GOVERNMENT_OFFICES) {
            double distance = calculateDistance(lat, lon, office[0], office[1]);
            if (distance <= CRITICAL_DISTANCE) {
                Log.d(TAG, String.format("Near government office (%.2fm)", distance));
                return true;
            }
        }

        return false;
    }

    /**
     * Check if location is in residential area
     */
    private boolean isInResidentialArea(double lat, double lon) {
        // Simplified check - in real app, use GIS data
        for (double[] zone : RESIDENTIAL_ZONES) {
            double distance = calculateDistance(lat, lon, zone[0], zone[1]);
            if (distance <= 2000) { // Within 2km of residential zone center
                return true;
            }
        }
        return false;
    }

    /**
     * Check if location is in commercial area
     */
    private boolean isInCommercialArea(double lat, double lon) {
        for (double[] zone : COMMERCIAL_ZONES) {
            double distance = calculateDistance(lat, lon, zone[0], zone[1]);
            if (distance <= 1500) { // Within 1.5km of commercial zone center
                return true;
            }
        }
        return false;
    }

    /**
     * Check if location is near tourist spot
     */
    private boolean isNearTouristSpot(double lat, double lon) {
        for (double[] spot : TOURIST_SPOTS) {
            double distance = calculateDistance(lat, lon, spot[0], spot[1]);
            if (distance <= 1000) { // Within 1km of tourist spot
                Log.d(TAG, String.format("Near tourist spot (%.2fm)", distance));
                return true;
            }
        }
        return false;
    }

    /**
     * Check if location is near school
     */
    private boolean isNearSchool(double lat, double lon) {
        for (double[] school : SCHOOLS) {
            double distance = calculateDistance(lat, lon, school[0], school[1]);
            if (distance <= 500) { // Within 500m of school
                return true;
            }
        }
        return false;
    }

    /**
     * Estimate population density (simplified)
     */
    private int estimatePopulationDensity(double lat, double lon) {
        // Simplified density estimation based on proximity to urban centers

        // Panaji area (high density)
        double distanceToPanaji = calculateDistance(lat, lon, 15.4986, 73.8255);
        if (distanceToPanaji <= 3000) {
            return 2; // High density
        }

        // Margao area (high density)
        double distanceToMargao = calculateDistance(lat, lon, 15.5546, 73.7411);
        if (distanceToMargao <= 3000) {
            return 2; // High density
        }

        // Vasco area (medium density)
        double distanceToVasco = calculateDistance(lat, lon, 15.3926, 73.8789);
        if (distanceToVasco <= 3000) {
            return 1; // Medium density
        }

        return 0; // Low density (rural area)
    }

    /**
     * Adjust priority based on distance from city center
     */
    private int adjustByDistanceFromCenter(int priority, double lat, double lon) {
        // Distance from Panaji (main city center)
        double distance = calculateDistance(lat, lon, 15.4986, 73.8255);

        if (distance <= 5000) { // Within 5km of city center
            return priority + 1; // Higher priority in city center
        } else if (distance >= 20000) { // More than 20km away
            return priority - 1; // Lower priority in remote areas
        }

        return priority;
    }

    /**
     * Calculate distance between two points in meters
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Get location description for UI
     */
    public String getLocationDescription(double latitude, double longitude) {
        StringBuilder description = new StringBuilder();

        if (isNearCriticalInfrastructure(latitude, longitude)) {
            description.append("• Near critical infrastructure\n");
        }

        if (isInResidentialArea(latitude, longitude)) {
            description.append("• Residential area\n");
        }

        if (isInCommercialArea(latitude, longitude)) {
            description.append("• Commercial area\n");
        }

        if (isNearTouristSpot(latitude, longitude)) {
            description.append("• Tourist area\n");
        }

        if (isNearSchool(latitude, longitude)) {
            description.append("• Near educational institution\n");
        }

        if (description.length() == 0) {
            description.append("• General area\n");
        }

        return description.toString();
    }

    /**
     * Get nearest landmark
     */
    public String getNearestLandmark(double latitude, double longitude) {
        String nearest = "Unknown area";
        double minDistance = Double.MAX_VALUE;

        // Check hospitals
        for (double[] hospital : HOSPITALS) {
            double distance = calculateDistance(latitude, longitude, hospital[0], hospital[1]);
            if (distance < minDistance && distance < 2000) {
                minDistance = distance;
                nearest = "Near medical facility";
            }
        }

        // Check tourist spots
        for (double[] spot : TOURIST_SPOTS) {
            double distance = calculateDistance(latitude, longitude, spot[0], spot[1]);
            if (distance < minDistance && distance < 1500) {
                minDistance = distance;
                nearest = "Tourist area";
            }
        }

        // Check if in city center
        double distanceToPanaji = calculateDistance(latitude, longitude, 15.4986, 73.8255);
        if (distanceToPanaji < 3000 && distanceToPanaji < minDistance) {
            nearest = "City center area";
        }

        return nearest;
    }

    /**
     * Check if location is valid (within Goa boundaries)
     */
    public boolean isValidGoaLocation(double latitude, double longitude) {
        // Rough boundaries of Goa
        double minLat = 14.9;
        double maxLat = 15.8;
        double minLon = 73.7;
        double maxLon = 74.3;

        return latitude >= minLat && latitude <= maxLat &&
                longitude >= minLon && longitude <= maxLon;
    }

    /**
     * Get formatted distance string
     */
    public String getFormattedDistance(double lat1, double lon1, double lat2, double lon2) {
        double distance = calculateDistance(lat1, lon1, lat2, lon2);

        if (distance < 1000) {
            return String.format("%.0f m", distance);
        } else {
            return String.format("%.1f km", distance / 1000);
        }
    }
}