package com.city_i.ai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class ModelManager {
    private static final String TAG = "ModelManager";

    private Context context;

    // Model file names
    private static final String PRIORITY_MODEL = "priority_model.tflite";
    private static final String IMAGE_CLASSIFIER_MODEL = "image_classifier.tflite";
    private static final String TEXT_ANALYZER_MODEL = "text_analyzer.tflite";

    // Model cache
    private Map<String, Interpreter> modelCache;
    private GpuDelegate gpuDelegate;
    private NnApiDelegate nnApiDelegate;

    // Model metadata
    private static final Map<String, ModelInfo> MODEL_INFO = new HashMap<String, ModelInfo>() {{
        put(PRIORITY_MODEL, new ModelInfo("Priority Predictor", "1.0", 5, 3));
        put(IMAGE_CLASSIFIER_MODEL, new ModelInfo("Image Classifier", "1.0", 224*224*3, 16));
        put(TEXT_ANALYZER_MODEL, new ModelInfo("Text Analyzer", "1.0", 100, 5));
    }};

    static class ModelInfo {
        String name;
        String version;
        int inputSize;
        int outputSize;

        ModelInfo(String name, String version, int inputSize, int outputSize) {
            this.name = name;
            this.version = version;
            this.inputSize = inputSize;
            this.outputSize = outputSize;
        }
    }

    public ModelManager(Context context) {
        this.context = context;
        this.modelCache = new HashMap<>();
        initializeDelegates();
        copyModelsToStorage();
    }

    /**
     * Initialize hardware acceleration delegates
     */
    private void initializeDelegates() {
        try {
            // Initialize GPU delegate for faster inference
            gpuDelegate = new GpuDelegate();
            Log.d(TAG, "GPU delegate initialized");
        } catch (Exception e) {
            Log.w(TAG, "GPU delegate not available: " + e.getMessage());
        }

        try {
            // Initialize NNAPI delegate for neural network acceleration
            nnApiDelegate = new NnApiDelegate();
            Log.d(TAG, "NNAPI delegate initialized");
        } catch (Exception e) {
            Log.w(TAG, "NNAPI delegate not available: " + e.getMessage());
        }
    }

    /**
     * Copy models from assets to internal storage
     */
    private void copyModelsToStorage() {
        String[] models = {PRIORITY_MODEL, IMAGE_CLASSIFIER_MODEL, TEXT_ANALYZER_MODEL};

        for (String modelName : models) {
            try {
                File modelFile = new File(context.getFilesDir(), modelName);

                // Check if model already exists
                if (!modelFile.exists()) {
                    Log.d(TAG, "Copying model to storage: " + modelName);

                    // Copy from assets
                    InputStream inputStream = context.getAssets().open(modelName);
                    FileOutputStream outputStream = new FileOutputStream(modelFile);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }

                    outputStream.close();
                    inputStream.close();

                    Log.d(TAG, "Model copied successfully: " + modelName);
                } else {
                    Log.d(TAG, "Model already exists: " + modelName);
                }

            } catch (IOException e) {
                Log.e(TAG, "Error copying model " + modelName + ": " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error with model " + modelName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Load priority prediction model
     */
    public Interpreter loadPriorityModel() {
        return loadModel(PRIORITY_MODEL);
    }

    /**
     * Load image classifier model
     */
    public Interpreter loadImageClassifierModel() {
        return loadModel(IMAGE_CLASSIFIER_MODEL);
    }

    /**
     * Load text analyzer model
     */
    public Interpreter loadTextAnalyzerModel() {
        return loadModel(TEXT_ANALYZER_MODEL);
    }

    /**
     * Generic model loader
     */
    private Interpreter loadModel(String modelName) {
        // Check cache first
        if (modelCache.containsKey(modelName)) {
            Log.d(TAG, "Loading from cache: " + modelName);
            return modelCache.get(modelName);
        }

        try {
            Log.d(TAG, "Loading model: " + modelName);

            // Load model file
            File modelFile = new File(context.getFilesDir(), modelName);

            if (!modelFile.exists()) {
                Log.w(TAG, "Model file not found: " + modelName);

                // Try to load from assets as fallback
                MappedByteBuffer modelBuffer = loadModelFileFromAssets(modelName);
                if (modelBuffer == null) {
                    Log.e(TAG, "Cannot load model from assets: " + modelName);
                    return null;
                }

                // Create interpreter with hardware acceleration options
                Interpreter.Options options = createInterpreterOptions();
                Interpreter interpreter = new Interpreter(modelBuffer, options);
                modelCache.put(modelName, interpreter);

                Log.d(TAG, "Model loaded from assets: " + modelName);
                return interpreter;
            }

            // Load from storage
            FileInputStream inputStream = new FileInputStream(modelFile);
            FileChannel fileChannel = inputStream.getChannel();

            long startOffset = 0;
            long declaredLength = fileChannel.size();
            MappedByteBuffer modelBuffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY, startOffset, declaredLength
            );

            fileChannel.close();
            inputStream.close();

            // Create interpreter with hardware acceleration options
            Interpreter.Options options = createInterpreterOptions();
            Interpreter interpreter = new Interpreter(modelBuffer, options);
            modelCache.put(modelName, interpreter);

            Log.d(TAG, "Model loaded successfully: " + modelName);
            return interpreter;

        } catch (Exception e) {
            Log.e(TAG, "Error loading model " + modelName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load model directly from assets
     */
    private MappedByteBuffer loadModelFileFromAssets(String modelName) {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();

            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getLength();
            MappedByteBuffer buffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY, startOffset, declaredLength
            );

            fileChannel.close();
            inputStream.close();
            fileDescriptor.close();

            return buffer;

        } catch (IOException e) {
            Log.e(TAG, "Error loading model from assets: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create interpreter options with hardware acceleration
     */
    private Interpreter.Options createInterpreterOptions() {
        Interpreter.Options options = new Interpreter.Options();

        // Set number of threads
        options.setNumThreads(4);

        // Try to use GPU delegate first
        if (gpuDelegate != null) {
            try {
                options.addDelegate(gpuDelegate);
                Log.d(TAG, "Using GPU delegate");
            } catch (Exception e) {
                Log.w(TAG, "GPU delegate failed: " + e.getMessage());
            }
        }

        // Fallback to NNAPI
        if (nnApiDelegate != null) {
            try {
                options.addDelegate(nnApiDelegate);
                Log.d(TAG, "Using NNAPI delegate");
            } catch (Exception e) {
                Log.w(TAG, "NNAPI delegate failed: " + e.getMessage());
            }
        }

        // Enable XNNPACK delegate for CPU acceleration (Android 10+)
        try {
            options.setUseXNNPACK(true);
            Log.d(TAG, "XNNPACK enabled");
        } catch (Exception e) {
            Log.w(TAG, "XNNPACK not available: " + e.getMessage());
        }

        // Allow dynamic batch size
        options.setAllowBufferHandleOutput(true);

        return options;
    }

    /**
     * Check if model is available
     */
    public boolean isModelAvailable(String modelName) {
        File modelFile = new File(context.getFilesDir(), modelName);
        return modelFile.exists();
    }

    /**
     * Get model information
     */
    public ModelInfo getModelInfo(String modelName) {
        return MODEL_INFO.get(modelName);
    }

    /**
     * Get all available models
     */
    public Map<String, Boolean> getAvailableModels() {
        Map<String, Boolean> availableModels = new HashMap<>();

        for (String modelName : MODEL_INFO.keySet()) {
            availableModels.put(modelName, isModelAvailable(modelName));
        }

        return availableModels;
    }

    /**
     * Get model file size
     */
    public long getModelSize(String modelName) {
        File modelFile = new File(context.getFilesDir(), modelName);
        if (modelFile.exists()) {
            return modelFile.length();
        }
        return 0;
    }

    /**
     * Clear model cache
     */
    public void clearCache() {
        Log.d(TAG, "Clearing model cache");

        // Close all interpreters
        for (Interpreter interpreter : modelCache.values()) {
            if (interpreter != null) {
                interpreter.close();
            }
        }

        modelCache.clear();
        Log.d(TAG, "Model cache cleared");
    }

    /**
     * Create dummy model for testing (for hackathon demo)
     */
    public void createDummyPriorityModel() {
        try {
            Log.d(TAG, "Creating dummy priority model for demo");

            // Create a simple text file as placeholder
            String dummyContent = "DUMMY_MODEL_FOR_HACKATHON_DEMO\n";
            dummyContent += "This is a placeholder for the actual TensorFlow Lite model.\n";
            dummyContent += "In production, this would be a trained .tflite file.\n";

            File modelFile = new File(context.getFilesDir(), PRIORITY_MODEL);
            FileOutputStream outputStream = new FileOutputStream(modelFile);
            outputStream.write(dummyContent.getBytes());
            outputStream.close();

            Log.d(TAG, "Dummy model created");

        } catch (Exception e) {
            Log.e(TAG, "Error creating dummy model: " + e.getMessage());
        }
    }

    /**
     * Clean up resources
     */
    public void close() {
        // Clear cache
        clearCache();

        // Close delegates
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
            Log.d(TAG, "GPU delegate closed");
        }

        if (nnApiDelegate != null) {
            nnApiDelegate.close();
            nnApiDelegate = null;
            Log.d(TAG, "NNAPI delegate closed");
        }

        Log.d(TAG, "Model manager resources released");
    }

    /**
     * Get model statistics for debugging
     */
    public String getModelStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== AI Model Statistics ===\n\n");

        Map<String, Boolean> availableModels = getAvailableModels();

        for (Map.Entry<String, Boolean> entry : availableModels.entrySet()) {
            String modelName = entry.getKey();
            boolean isAvailable = entry.getValue();
            ModelInfo info = MODEL_INFO.get(modelName);

            stats.append("Model: ").append(modelName).append("\n");
            stats.append("Status: ").append(isAvailable ? "Available" : "Not Available").append("\n");

            if (isAvailable && info != null) {
                stats.append("Version: ").append(info.version).append("\n");
                stats.append("Input Size: ").append(info.inputSize).append("\n");
                stats.append("Output Size: ").append(info.outputSize).append("\n");
                stats.append("File Size: ").append(getModelSize(modelName) / 1024).append(" KB\n");
            }

            stats.append("\n");
        }

        stats.append("Cache Size: ").append(modelCache.size()).append(" models loaded\n");

        return stats.toString();
    }
}