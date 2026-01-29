package com.city_i.ai;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextAnalyzer {
    private static final String TAG = "TextAnalyzer";

    // Urgency keywords with weights
    private static final Map<String, Integer> URGENCY_KEYWORDS = new HashMap<String, Integer>() {{
        // High urgency (weight 3)
        put("emergency", 3);
        put("urgent", 3);
        put("danger", 3);
        put("dangerous", 3);
        put("hazard", 3);
        put("hazardous", 3);
        put("accident", 3);
        put("crash", 3);
        put("fire", 3);
        put("flood", 3);
        put("collapse", 3);
        put("explosion", 3);
        put("electrocution", 3);
        put("toxic", 3);
        put("lethal", 3);
        put("fatal", 3);
        put("deadly", 3);
        put("injured", 3);
        put("bleeding", 3);
        put("trapped", 3);

        // Medium urgency (weight 2)
        put("serious", 2);
        put("severe", 2);
        put("critical", 2);
        put("important", 2);
        put("broken", 2);
        put("damage", 2);
        put("damaged", 2);
        put("crack", 2);
        put("leak", 2);
        put("leaking", 2);
        put("overflow", 2);
        put("blocked", 2);
        put("blockage", 2);
        put("obstruction", 2);
        put("stuck", 2);
        put("traffic", 2);
        put("jam", 2);
        put("congestion", 2);
        put("smell", 2);
        put("odor", 2);
        put("stink", 2);
        put("pollution", 2);
        put("contaminated", 2);

        // Low urgency (weight 1)
        put("minor", 1);
        put("small", 1);
        put("slight", 1);
        put("little", 1);
        put("suggestion", 1);
        put("improvement", 1);
        put("maintenance", 1);
        put("clean", 1);
        put("dirty", 1);
        put("mess", 1);
        put("litter", 1);
        put("garbage", 1);
        put("trash", 1);
        put("waste", 1);
        put("dust", 1);
        put("noise", 1);
        put("loud", 1);
    }};

    // Negative words that reduce urgency
    private static final Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
            "not", "no", "none", "never", "nothing", "nowhere", "nobody",
            "neither", "nor", "hardly", "scarcely", "barely"
    ));

    // Intensifiers that increase urgency
    private static final Set<String> INTENSIFIERS = new HashSet<>(Arrays.asList(
            "very", "extremely", "highly", "really", "absolutely", "completely",
            "totally", "utterly", "quite", "rather", "pretty", "fairly"
    ));

    // Exclamation patterns
    private static final Pattern EXCLAMATION_PATTERN = Pattern.compile("!+");
    private static final Pattern QUESTION_PATTERN = Pattern.compile("\\?+");

    public TextAnalyzer(Context context) {
        Log.d(TAG, "Text analyzer initialized");
    }

    /**
     * Analyze text urgency and return score (1-10)
     */
    public int analyzeUrgency(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 5; // Default medium urgency for empty text
        }

        try {
            Log.d(TAG, "Analyzing text urgency: " + (text.length() > 50 ? text.substring(0, 50) + "..." : text));

            String cleanedText = preprocessText(text);
            int urgencyScore = calculateUrgencyScore(cleanedText);

            // Adjust based on text length (longer descriptions might be more serious)
            urgencyScore = adjustByLength(urgencyScore, text.length());

            // Adjust based on punctuation
            urgencyScore = adjustByPunctuation(urgencyScore, text);

            // Adjust based on capital letters (shouting)
            urgencyScore = adjustByCapitalization(urgencyScore, text);

            // Ensure within bounds
            urgencyScore = Math.max(1, Math.min(10, urgencyScore));

            Log.d(TAG, "Text urgency score: " + urgencyScore);

            return urgencyScore;

        } catch (Exception e) {
            Log.e(TAG, "Error analyzing text urgency: " + e.getMessage());
            return 5;
        }
    }

    /**
     * Extract category from text if not provided
     */
    public String extractCategory(String text) {
        if (text == null || text.isEmpty()) {
            return "Other";
        }

        String lowerText = text.toLowerCase();

        // Category detection patterns
        if (containsAny(lowerText, "pothole", "road damage", "crack", "hole in road")) {
            return "Pothole";
        } else if (containsAny(lowerText, "garbage", "trash", "litter", "waste", "dump")) {
            return "Garbage";
        } else if (containsAny(lowerText, "street light", "light pole", "dark", "no light")) {
            return "Street Light";
        } else if (containsAny(lowerText, "water leak", "pipe burst", "water flowing", "flood")) {
            return "Water Leakage";
        } else if (containsAny(lowerText, "sewage", "drain", "sewer", "manhole")) {
            return "Sewage";
        } else if (containsAny(lowerText, "toilet", "restroom", "bathroom", "public toilet")) {
            return "Public Toilet";
        } else if (containsAny(lowerText, "park", "garden", "playground", "bench")) {
            return "Park";
        } else if (containsAny(lowerText, "traffic", "signal", "sign", "road sign")) {
            return "Traffic Sign";
        } else if (containsAny(lowerText, "electrical", "wire", "cable", "spark", "shock")) {
            return "Electrical Hazard";
        } else if (containsAny(lowerText, "building", "wall", "structure", "construction")) {
            return "Building Damage";
        } else if (containsAny(lowerText, "tree", "branch", "fallen tree", "tree branch")) {
            return "Tree";
        } else if (containsAny(lowerText, "stray", "dog", "cat", "animal")) {
            return "Stray Animal";
        } else if (containsAny(lowerText, "graffiti", "vandalism", "spray", "paint")) {
            return "Graffiti";
        } else if (containsAny(lowerText, "noise", "sound", "loud", "music")) {
            return "Noise Pollution";
        } else if (containsAny(lowerText, "air", "smoke", "dust", "pollution")) {
            return "Air Pollution";
        } else if (containsAny(lowerText, "water quality", "dirty water", "contaminated")) {
            return "Water Pollution";
        }

        return "Other";
    }

    /**
     * Get sentiment of the text (positive/neutral/negative)
     */
    public String analyzeSentiment(String text) {
        if (text == null || text.isEmpty()) {
            return "neutral";
        }

        String lowerText = text.toLowerCase();
        int positiveCount = 0;
        int negativeCount = 0;

        // Simple sentiment analysis
        String[] positiveWords = {"good", "great", "excellent", "thanks", "thank", "appreciate",
                "helpful", "quick", "fast", "efficient", "clean", "nice"};
        String[] negativeWords = {"bad", "terrible", "awful", "horrible", "worst", "disgusting",
                "dirty", "smelly", "broken", "damaged", "dangerous", "risky"};

        for (String word : positiveWords) {
            if (lowerText.contains(word)) positiveCount++;
        }

        for (String word : negativeWords) {
            if (lowerText.contains(word)) negativeCount++;
        }

        if (positiveCount > negativeCount) {
            return "positive";
        } else if (negativeCount > positiveCount) {
            return "negative";
        } else {
            return "neutral";
        }
    }

    /**
     * Extract location mentions from text
     */
    public String extractLocationMentions(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Common location patterns
        Pattern pattern = Pattern.compile(
                "\\b(near|at|in front of|opposite|beside|next to|behind|between)\\s+([A-Za-z0-9\\s]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);
        StringBuilder locations = new StringBuilder();

        while (matcher.find()) {
            String location = matcher.group(2).trim();
            if (!location.isEmpty() && location.length() > 2) {
                if (locations.length() > 0) {
                    locations.append(", ");
                }
                locations.append(location);
            }
        }

        return locations.toString();
    }

    /**
     * Calculate urgency score based on keywords
     */
    private int calculateUrgencyScore(String text) {
        String lowerText = text.toLowerCase();
        String[] words = lowerText.split("\\s+");

        int totalScore = 0;
        int keywordCount = 0;
        boolean previousWasNegative = false;
        boolean previousWasIntensifier = false;

        for (int i = 0; i < words.length; i++) {
            String word = cleanWord(words[i]);

            if (word.isEmpty()) continue;

            // Check for negative words
            if (NEGATIVE_WORDS.contains(word)) {
                previousWasNegative = true;
                continue;
            }

            // Check for intensifiers
            if (INTENSIFIERS.contains(word)) {
                previousWasIntensifier = true;
                continue;
            }

            // Check if word is an urgency keyword
            if (URGENCY_KEYWORDS.containsKey(word)) {
                int wordScore = URGENCY_KEYWORDS.get(word);

                // Adjust score based on context
                if (previousWasNegative) {
                    wordScore = Math.max(1, wordScore - 2); // Reduce if negated
                } else if (previousWasIntensifier) {
                    wordScore = Math.min(3, wordScore + 1); // Increase if intensified
                }

                totalScore += wordScore;
                keywordCount++;

                Log.d(TAG, "Found urgency keyword: " + word + " score: " + wordScore);
            }

            // Reset context flags
            previousWasNegative = false;
            previousWasIntensifier = false;
        }

        // Calculate average score
        if (keywordCount > 0) {
            int averageScore = totalScore / keywordCount;

            // Map from 1-3 scale to 1-10 scale
            return mapToPriorityScale(averageScore);
        }

        return 5; // Default if no keywords found
    }

    /**
     * Map from keyword weight scale (1-3) to priority scale (1-10)
     */
    private int mapToPriorityScale(int keywordWeight) {
        switch (keywordWeight) {
            case 1: return 3; // Low priority
            case 2: return 6; // Medium priority
            case 3: return 9; // High priority
            default: return 5;
        }
    }

    /**
     * Adjust score based on text length
     */
    private int adjustByLength(int score, int textLength) {
        if (textLength < 20) {
            return score - 1; // Very short descriptions might be less urgent
        } else if (textLength > 200) {
            return score + 1; // Long descriptions might be more detailed/serious
        }
        return score;
    }

    /**
     * Adjust score based on punctuation
     */
    private int adjustByPunctuation(int score, String text) {
        // Count exclamation marks
        Matcher exclamationMatcher = EXCLAMATION_PATTERN.matcher(text);
        int exclamationCount = 0;
        while (exclamationMatcher.find()) {
            exclamationCount++;
        }

        // Count question marks
        Matcher questionMatcher = QUESTION_PATTERN.matcher(text);
        int questionCount = 0;
        while (questionMatcher.find()) {
            questionCount++;
        }

        // Adjust score
        if (exclamationCount >= 3) {
            score += 2; // Multiple exclamations indicate urgency
        } else if (exclamationCount >= 1) {
            score += 1;
        }

        if (questionCount >= 3) {
            score += 1; // Many questions might indicate confusion/urgency
        }

        return score;
    }

    /**
     * Adjust score based on capitalization (shouting)
     */
    private int adjustByCapitalization(int score, String text) {
        int uppercaseCount = 0;
        int wordCount = 0;

        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.length() > 1) { // Ignore single characters
                wordCount++;
                if (word.equals(word.toUpperCase())) {
                    uppercaseCount++;
                }
            }
        }

        // If more than 30% of words are ALL CAPS, consider it shouting
        if (wordCount > 0 && (uppercaseCount * 100 / wordCount) > 30) {
            score += 1;
        }

        return score;
    }

    /**
     * Preprocess text for analysis
     */
    private String preprocessText(String text) {
        // Convert to lowercase
        String processed = text.toLowerCase();

        // Remove URLs
        processed = processed.replaceAll("https?://\\S+\\s?", "");

        // Remove special characters but keep spaces
        processed = processed.replaceAll("[^a-zA-Z0-9\\s]", " ");

        // Remove extra whitespace
        processed = processed.replaceAll("\\s+", " ").trim();

        return processed;
    }

    /**
     * Clean individual word
     */
    private String cleanWord(String word) {
        return word.replaceAll("[^a-zA-Z]", "").toLowerCase();
    }

    /**
     * Check if text contains any of the given strings
     */
    private boolean containsAny(String text, String... patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get summary of text analysis
     */
    public String getAnalysisSummary(String text) {
        int urgency = analyzeUrgency(text);
        String sentiment = analyzeSentiment(text);
        String category = extractCategory(text);
        String location = extractLocationMentions(text);

        StringBuilder summary = new StringBuilder();
        summary.append("Text Analysis Summary:\n\n");

        summary.append("• Urgency Level: ");
        if (urgency >= 8) {
            summary.append("HIGH\n");
        } else if (urgency >= 5) {
            summary.append("MEDIUM\n");
        } else {
            summary.append("LOW\n");
        }

        summary.append("• Sentiment: ").append(sentiment.toUpperCase()).append("\n");
        summary.append("• Detected Category: ").append(category).append("\n");

        if (!location.isEmpty()) {
            summary.append("• Location Mentions: ").append(location).append("\n");
        }

        return summary.toString();
    }
}