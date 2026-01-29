package com.city_i.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageClassifier {
    private static final String TAG = "ImageClassifier";

    private Context context;
    private ModelManager modelManager;
    private Interpreter imageInterpreter;

    // Image dimensions for the model
    private static final int IMG_WIDTH = 224;
    private static final int IMG_HEIGHT = 224;
    private static final int IMG_CHANNELS = 3;

    // Categories for civic issues
    private static final String[] CATEGORIES = {
            "pothole", "garbage", "street_light", "water_leakage",
            "sewage", "public_toilet", "park", "road_damage",
            "drainage", "electrical", "traffic_sign", "building",
            "tree", "stray_animal", "graffiti", "other"
    };

    // Severity indicators for different categories
    private static final Map<String, Integer> CATEGORY_SEVERITY = new HashMap<String, Integer>() {{
        put("pothole", 8);
        put("sewage", 9);
        put("water_leakage", 7);
        put("electrical", 10);
        put("road_damage", 8);
        put("garbage", 6);
        put("street_light", 5);
        put("public_toilet", 6);
        put("drainage", 7);
        put("traffic_sign", 7);
        put("building", 8);
        put("tree", 4);
        put("stray_animal", 5);
        put("graffiti", 3);
        put("park", 4);
        put("other", 5);
    }};

    public ImageClassifier(Context context, ModelManager modelManager) {
        this.context = context;
        this.modelManager = modelManager;
        initializeImageClassifier();
    }

    private void initializeImageClassifier() {
        try {
            imageInterpreter = modelManager.loadImageClassifierModel();
            if (imageInterpreter != null) {
                Log.d(TAG, "Image classifier initialized successfully");
            } else {
                Log.w(TAG, "Image classifier model not available, using fallback methods");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing image classifier: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Analyze image and return severity score (1-10)
     */
    public int analyzeImageSeverity(String imagePath) {
        try {
            Log.d(TAG, "Analyzing image: " + imagePath);

            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: " + imagePath);
                return 5; // Default medium severity
            }

            // Load and preprocess image
            Bitmap bitmap = loadAndPreprocessImage(imagePath);
            if (bitmap == null) {
                return analyzeImageFeaturesFallback(imagePath);
            }

            // Use TensorFlow Lite model if available
            if (imageInterpreter != null) {
                return analyzeWithModel(bitmap);
            } else {
                // Fallback to feature-based analysis
                return analyzeImageFeatures(bitmap);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error analyzing image: " + e.getMessage());
            e.printStackTrace();
            return 5; // Default medium severity
        }
    }

    /**
     * Analyze image using TensorFlow Lite model
     */
    private int analyzeWithModel(Bitmap bitmap) {
        try {
            // Convert bitmap to TensorImage
            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(bitmap);

            // Create image processor
            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(IMG_HEIGHT, IMG_WIDTH, ResizeOp.ResizeMethod.BILINEAR))
                    .add(new NormalizeOp(0.0f, 255.0f)) // Normalize to [0,1]
                    .build();

            // Preprocess the image
            tensorImage = imageProcessor.process(tensorImage);

            // Get the ByteBuffer from TensorImage
            ByteBuffer inputBuffer = tensorImage.getBuffer();

            // Prepare output tensor
            float[][] output = new float[1][CATEGORIES.length];

            // Run inference
            imageInterpreter.run(inputBuffer, output);

            // Get top category
            int topCategoryIndex = getMaxIndex(output[0]);
            String topCategory = CATEGORIES[topCategoryIndex];
            float confidence = output[0][topCategoryIndex];

            Log.d(TAG, "Detected category: " + topCategory + " with confidence: " + confidence);

            // Calculate severity based on category and confidence
            int baseSeverity = CATEGORY_SEVERITY.getOrDefault(topCategory, 5);
            int severity = adjustSeverityByConfidence(baseSeverity, confidence);

            Log.d(TAG, "Calculated severity: " + severity);

            return severity;

        } catch (Exception e) {
            Log.e(TAG, "Error in model analysis: " + e.getMessage());
            return analyzeImageFeatures(bitmap);
        }
    }

    /**
     * Fallback method: Analyze image features
     */
    private int analyzeImageFeatures(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Calculate brightness
        float brightness = calculateBrightness(bitmap);

        // Calculate contrast
        float contrast = calculateContrast(bitmap);

        // Detect edges (simplified)
        int edgeDensity = detectEdgeDensity(bitmap);

        // Detect color patterns (looking for hazardous colors)
        int hazardousColorScore = detectHazardousColors(bitmap);

        // Calculate overall severity
        int severity = 5; // Base

        // Adjust based on features
        if (edgeDensity > 1000) severity += 2; // Lots of edges = potential damage
        if (hazardousColorScore > 30) severity += 2; // Hazardous colors present
        if (brightness < 0.3) severity += 1; // Dark image might be dangerous area
        if (contrast > 0.5) severity += 1; // High contrast might indicate damage

        // Ensure within bounds
        severity = Math.max(1, Math.min(10, severity));

        Log.d(TAG, String.format(
                "Feature analysis - Brightness: %.2f, Contrast: %.2f, Edges: %d, Hazard: %d, Severity: %d",
                brightness, contrast, edgeDensity, hazardousColorScore, severity
        ));

        return severity;
    }

    /**
     * Quick analysis for immediate feedback
     */
    public String quickAnalyze(Bitmap image) {
        try {
            if (image == null) {
                return "No image to analyze";
            }

            // Simple analysis for immediate feedback
            float brightness = calculateBrightness(image);
            int edgeDensity = detectEdgeDensity(image);
            int colorScore = detectHazardousColors(image);

            StringBuilder result = new StringBuilder();

            if (brightness < 0.3) {
                result.append("• Low light detected\n");
            }

            if (edgeDensity > 800) {
                result.append("• Possible structural damage\n");
            }

            if (colorScore > 25) {
                result.append("• Hazardous conditions detected\n");
            }

            if (result.length() == 0) {
                result.append("• Image quality is good\n");
                result.append("• Ready for submission\n");
            } else {
                result.append("• Consider retaking photo\n");
            }

            return result.toString();

        } catch (Exception e) {
            return "Quick analysis unavailable";
        }
    }

    /**
     * Fallback when bitmap can't be loaded
     */
    private int analyzeImageFeaturesFallback(String imagePath) {
        try {
            // Try to get basic file info
            File imageFile = new File(imagePath);
            long fileSize = imageFile.length();

            // Very basic heuristic based on file size
            int severity = 5;

            if (fileSize < 50000) { // Less than 50KB - likely poor quality
                severity = 3;
            } else if (fileSize > 1000000) { // More than 1MB - likely high quality
                severity = 7;
            }

            Log.d(TAG, "Fallback analysis - File size: " + fileSize + ", Severity: " + severity);

            return severity;

        } catch (Exception e) {
            return 5; // Default
        }
    }

    /**
     * Helper methods for image analysis
     */
    private float calculateBrightness(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        long totalBrightness = 0;
        int sampleSize = 1000; // Sample points for performance

        for (int i = 0; i < sampleSize; i++) {
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            int pixel = bitmap.getPixel(x, y);

            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);

            // Calculate brightness using standard formula
            totalBrightness += (int) (0.299 * r + 0.587 * g + 0.114 * b);
        }

        return totalBrightness / (float) (sampleSize * 255);
    }

    private float calculateContrast(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int sampleSize = 500;

        List<Integer> brightnessValues = new ArrayList<>();

        for (int i = 0; i < sampleSize; i++) {
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            int pixel = bitmap.getPixel(x, y);

            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);

            int brightness = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            brightnessValues.add(brightness);
        }

        // Simple contrast calculation (range of brightness)
        Collections.sort(brightnessValues);
        int min = brightnessValues.get(0);
        int max = brightnessValues.get(brightnessValues.size() - 1);

        return (max - min) / 255.0f;
    }

    private int detectEdgeDensity(Bitmap bitmap) {
        // Simplified edge detection by sampling pixel differences
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int edgeCount = 0;
        int samples = 200;

        for (int i = 0; i < samples; i++) {
            int x = (int) (Math.random() * (width - 2));
            int y = (int) (Math.random() * (height - 2));

            int pixel1 = bitmap.getPixel(x, y);
            int pixel2 = bitmap.getPixel(x + 1, y);
            int pixel3 = bitmap.getPixel(x, y + 1);

            // Calculate color differences
            int diff1 = Math.abs(Color.red(pixel1) - Color.red(pixel2)) +
                    Math.abs(Color.green(pixel1) - Color.green(pixel2)) +
                    Math.abs(Color.blue(pixel1) - Color.blue(pixel2));

            int diff2 = Math.abs(Color.red(pixel1) - Color.red(pixel3)) +
                    Math.abs(Color.green(pixel1) - Color.green(pixel3)) +
                    Math.abs(Color.blue(pixel1) - Color.blue(pixel3));

            if (diff1 > 50 || diff2 > 50) { // Threshold for edge
                edgeCount++;
            }
        }

        return edgeCount * (width * height / samples); // Scale up to estimate total
    }

    private int detectHazardousColors(Bitmap bitmap) {
        // Detect colors that might indicate hazardous conditions
        // Red/Yellow for danger/warning, Brown for waste/sewage
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int hazardousCount = 0;
        int samples = 300;

        for (int i = 0; i < samples; i++) {
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            int pixel = bitmap.getPixel(x, y);

            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);

            // Detect red/orange/yellow (danger/warning)
            if (r > 150 && g < 100 && b < 100) { // Red
                hazardousCount++;
            } else if (r > 150 && g > 100 && b < 50) { // Orange/Yellow
                hazardousCount++;
            }
            // Detect brown (waste/sewage)
            else if (r > 100 && r < 150 && g > 50 && g < 100 && b < 50) {
                hazardousCount++;
            }
        }

        return hazardousCount;
    }

    private Bitmap loadAndPreprocessImage(String imagePath) {
        try {
            // First, decode with bounds to check memory
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            // Calculate sampling to reduce memory usage
            int scale = 1;
            while (options.outWidth / scale > 1024 || options.outHeight / scale > 1024) {
                scale *= 2;
            }

            // Decode actual bitmap with sampling
            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from: " + imagePath);
                return null;
            }

            // Resize to standard size if needed
            if (bitmap.getWidth() != IMG_WIDTH || bitmap.getHeight() != IMG_HEIGHT) {
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, IMG_WIDTH, IMG_HEIGHT, true);
                bitmap.recycle();
                return resized;
            }

            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage());
            return null;
        }
    }

    private int getMaxIndex(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];

        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    private int adjustSeverityByConfidence(int baseSeverity, float confidence) {
        // Adjust severity based on model confidence
        if (confidence > 0.8) {
            return Math.min(10, baseSeverity + 1);
        } else if (confidence > 0.6) {
            return baseSeverity;
        } else {
            return Math.max(1, baseSeverity - 1);
        }
    }

    /**
     * Clean up resources
     */
    public void close() {
        if (imageInterpreter != null) {
            imageInterpreter.close();
            imageInterpreter = null;
        }
        Log.d(TAG, "Image classifier resources released");
    }
}