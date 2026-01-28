package com.city_i.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.city_i.models.IssueModel;
import com.city_i.utils.DateUtils;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AIPriorityEngine {
    private static final String TAG = "AIPriorityEngine";

    private Context context;
    private Interpreter tflite;
    private ImageClassifier imageClassifier;
    private TextAnalyzer textAnalyzer;
    private LocationAnalyzer locationAnalyzer;
    private ModelManager modelManager;

    // Priority weights for different factors
    private static final float WEIGHT_IMAGE = 0.35f;
    private static final float WEIGHT_TEXT = 0.25f;
    private static final float WEIGHT_LOCATION = 0.25f;
    private static final float WEIGHT_TIME = 0.15f;

    // Category priority mapping
    private static final Map<String, Integer> CATEGORY_PRIORITY = new HashMap<String, Integer>() {{
        put("Accident", 10);
        put("Fire", 10);
        put("Electric Hazard", 9);
        put("Severe Pothole", 9);
        put("Severe Water Leakage", 8);
        put("Severe Sewage", 8);
        put("Road Collapse", 9);
        put("Bridge Damage", 9);
        put("Building Crack", 7);
        put("Garbage Pileup", 6);
        put("Street Light Out", 5);
        put("Minor Pothole", 4);
        put("Park Maintenance", 3);
        put("Public Toilet Issue", 6);
        put("Drainage Blockage", 7);
        put("Illegal Dumping", 5);
        put("Stray Animals", 4);
        put("Noise Pollution", 3);
        put("Air Pollution", 7);
        put("Water Pollution", 8);
        put("Other", 5);
    }};

    public AIPriorityEngine(Context context) {
        this.context = context;
        initializeComponents();
    }

    private void initializeComponents() {
        try {
            modelManager = new ModelManager(context);
            imageClassifier = new ImageClassifier(context, modelManager);
            textAnalyzer = new TextAnalyzer(context);
            locationAnalyzer = new LocationAnalyzer(context);

            // Load TensorFlow Lite model
            tflite = modelManager.loadPriorityModel();

            Log.d(TAG, "AI Priority Engine initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing AI components: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calculate priority for a civic issue
     * Returns priority from 1 (lowest) to 10 (highest)
     */
    public int calculateIssuePriority(IssueModel issue) {
        try {
            Log.d(TAG, "Calculating priority for issue: " + issue.getTitle());

            // Base priority from category
            int categoryPriority = getCategoryPriority(issue.getCategory());

            // Analyze image if available
            int imagePriority = 5; // Default medium
            if (issue.getImagePath() != null && !issue.getImagePath().isEmpty()) {
                File imageFile = new File(issue.getImagePath());
                if (imageFile.exists()) {
                    imagePriority = imageClassifier.analyzeImageSeverity(issue.getImagePath());
                    Log.d(TAG, "Image priority: " + imagePriority);
                }
            }

            // Analyze text description
            int textPriority = textAnalyzer.analyzeUrgency(issue.getDescription());
            Log.d(TAG, "Text priority: " + textPriority);

            // Analyze location
            int locationPriority = locationAnalyzer.calculateLocationPriority(
                    issue.getLatitude(), issue.getLongitude()
            );
            Log.d(TAG, "Location priority: " + locationPriority);

            // Time-based priority (rush hour, night time, etc.)
            int timePriority = calculateTimePriority(issue.getCreatedAt());
            Log.d(TAG, "Time priority: " + timePriority);

            // Use TensorFlow Lite model for final prediction if available
            int aiPriority = 5;
            if (tflite != null) {
                aiPriority = predictWithModel(categoryPriority, imagePriority,
                        textPriority, locationPriority, timePriority);
                Log.d(TAG, "AI model priority: " + aiPriority);
            }

            // Weighted average calculation
            float weightedPriority =
                    (categoryPriority * 0.2f) +
                            (imagePriority * WEIGHT_IMAGE) +
                            (textPriority * WEIGHT_TEXT) +
                            (locationPriority * WEIGHT_LOCATION) +
                            (timePriority * WEIGHT_TIME);

            if (tflite != null) {
                // Blend AI prediction with weighted average
                weightedPriority = (weightedPriority * 0.6f) + (aiPriority * 0.4f);
            }

            int finalPriority = Math.round(weightedPriority);

            // Ensure priority is between 1 and 10
            finalPriority = Math.max(1, Math.min(10, finalPriority));

            Log.d(TAG, "Final calculated priority: " + finalPriority);

            return finalPriority;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating priority: " + e.getMessage());
            e.printStackTrace();
            return 5; // Return medium priority as fallback
        }
    }

    /**
     * Predict priority using TensorFlow Lite model
     */
    private int predictWithModel(int... features) {
        try {
            if (tflite == null || features.length != 5) {
                return 5;
            }

            // Prepare input tensor (normalized to 0-1)
            float[][] input = new float[1][5];
            for (int i = 0; i < 5; i++) {
                input[0][i] = features[i] / 10.0f;
            }

            // Prepare output tensor
            float[][] output = new float[1][3]; // 3 classes: Low, Medium, High

            // Run inference
            tflite.run(input, output);

            // Interpret output (3-class classification)
            int predictedClass = 0;
            float maxProb = output[0][0];

            for (int i = 1; i < 3; i++) {
                if (output[0][i] > maxProb) {
                    maxProb = output[0][i];
                    predictedClass = i;
                }
            }

            // Map class to priority score
            switch (predictedClass) {
                case 0: return 3; // Low priority
                case 1: return 6; // Medium priority
                case 2: return 9; // High priority
                default: return 5;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in model prediction: " + e.getMessage());
            return 5;
        }
    }

    /**
     * Get base priority from category
     */
    private int getCategoryPriority(String category) {
        Integer priority = CATEGORY_PRIORITY.get(category);
        return priority != null ? priority : 5;
    }

    /**
     * Calculate time-based priority
     */
    private int calculateTimePriority(Date issueTime) {
        if (issueTime == null) {
            issueTime = new Date();
        }

        int hour = DateUtils.getHourOfDay(issueTime);
        int dayOfWeek = DateUtils.getDayOfWeek(issueTime);

        // Higher priority during rush hours (7-10 AM, 4-7 PM)
        boolean isRushHour = (hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 19);

        // Higher priority on weekdays
        boolean isWeekday = dayOfWeek >= 1 && dayOfWeek <= 5;

        // Higher priority at night (8 PM to 6 AM) for safety issues
        boolean isNightTime = hour >= 20 || hour <= 6;

        int timePriority = 5; // Default

        if (isRushHour && isWeekday) {
            timePriority = 8;
        } else if (isNightTime) {
            timePriority = 7;
        } else if (isWeekday) {
            timePriority = 6;
        } else {
            timePriority = 4; // Weekend non-rush hour
        }

        return timePriority;
    }

    /**
     * Analyze image and get immediate feedback
     */
    public String analyzeImageImmediate(Bitmap image) {
        try {
            if (imageClassifier != null) {
                return imageClassifier.quickAnalyze(image);
            }
            return "Unable to analyze image";
        } catch (Exception e) {
            Log.e(TAG, "Error in immediate image analysis: " + e.getMessage());
            return "Analysis failed";
        }
    }

    /**
     * Get priority label from score
     */
    public String getPriorityLabel(int priorityScore) {
        if (priorityScore >= 8) {
            return "High Priority";
        } else if (priorityScore >= 5) {
            return "Medium Priority";
        } else {
            return "Low Priority";
        }
    }

    /**
     * Get priority color from score
     */
    public int getPriorityColor(int priorityScore) {
        if (priorityScore >= 8) {
            return android.graphics.Color.RED;
        } else if (priorityScore >= 5) {
            return android.graphics.Color.parseColor("#FF9800"); // Orange
        } else {
            return android.graphics.Color.parseColor("#4CAF50"); // Green
        }
    }

    /**
     * Get explanation for priority score
     */
    public String getPriorityExplanation(int priorityScore, String category) {
        if (priorityScore >= 8) {
            return String.format(
                    "This %s issue has been marked as HIGH PRIORITY due to potential safety risks or severe impact. Municipal authorities have been notified for immediate action.",
                    category.toLowerCase()
            );
        } else if (priorityScore >= 5) {
            return String.format(
                    "This %s issue requires attention within 24-48 hours. It has been assigned to the appropriate department for resolution.",
                    category.toLowerCase()
            );
        } else {
            return String.format(
                    "This %s issue will be addressed during regular maintenance schedules. Thank you for reporting!",
                    category.toLowerCase()
            );
        }
    }

    /**
     * Clean up resources
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }

        if (imageClassifier != null) {
            imageClassifier.close();
            imageClassifier = null;
        }

        Log.d(TAG, "AI Priority Engine resources released");
    }
}