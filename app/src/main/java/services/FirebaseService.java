package services;  // This matches the import

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public FirebaseService() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // Login method
    public void loginUser(String email, String password, LoginCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Register method
    public void registerUser(String email, String password, String name, RegisterCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        // Save additional user data to Firestore
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), email, name, callback);
                        }
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    private void saveUserToFirestore(String userId, String email, String name, RegisterCallback callback) {
        User user = new User(userId, email, name);
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Callback interfaces
    public interface LoginCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String error);
    }

    public interface RegisterCallback {
        void onSuccess();
        void onFailure(String error);
    }

    // User model class
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