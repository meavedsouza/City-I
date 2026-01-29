package com.city_i.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseService {
    private static final FirebaseAuth AUTH = FirebaseAuth.getInstance();
    private static final FirebaseFirestore DB = FirebaseFirestore.getInstance();

    /**
     * Callback used by auth screens.
     */
    public interface AuthCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public static void loginUser(String email, String password, AuthCallback callback) {
        AUTH.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    public static void registerUser(String name, String email, String password, AuthCallback callback) {
        AUTH.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && AUTH.getCurrentUser() != null) {
                        String uid = AUTH.getCurrentUser().getUid();
                        saveUserToFirestore(uid, email, name, callback);
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    private static void saveUserToFirestore(String userId, String email, String name, AuthCallback callback) {
        User user = new User(userId, email, name);
        DB.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // User model class (kept here for simplicity)
    public static class User {
        private String id;
        private String email;
        private String name;

        public User() {}  // Required for Firestore

        public User(String id, String email, String name) {
            this.id = id;
            this.email = email;
            this.name = name;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}