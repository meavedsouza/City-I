package com.city_i.dashboard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.city_i.R;
import com.city_i.ai.AIPriorityEngine;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.Arrays;
import java.util.List;

public class ReportIssueActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int LOCATION_PERMISSION = 101;

    private EditText etDescription;
    private Spinner spinnerCategory;
    private Button btnCapture, btnSubmit;
    private ImageView ivPreview;
    private TextView tvLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private double latitude, longitude;
    private String imagePath;
    private AIPriorityEngine aiPriorityEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        aiPriorityEngine = new AIPriorityEngine(this);

        initializeUI();
        requestPermissions();
        getCurrentLocation();
    }

    private void initializeUI() {
        etDescription = findViewById(R.id.etDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnCapture = findViewById(R.id.btnCapture);
        btnSubmit = findViewById(R.id.btnSubmit);
        ivPreview = findViewById(R.id.ivPreview);
        tvLocation = findViewById(R.id.tvLocation);

        // Issue categories
        List<String> categories = Arrays.asList(
                "Potholes", "Garbage", "Street Lights",
                "Water Leakage", "Sewage", "Public Toilets",
                "Parks", "Road Damage", "Others"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnCapture.setOnClickListener(v -> captureImage());
        btnSubmit.setOnClickListener(v -> submitIssue());
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            tvLocation.setText(String.format("Location: %.4f, %.4f",
                                    latitude, longitude));
                        }
                    });
        }
    }

    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, CAMERA_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            // Image captured, you can process it here
            Toast.makeText(this, "Image captured!", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitIssue() {
        String category = spinnerCategory.getSelectedItem().toString();
        String description = etDescription.getText().toString();

        if (description.isEmpty()) {
            Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use AI to determine priority
        int priorityLevel = aiPriorityEngine.calculatePriority(
                category, description, imagePath, latitude, longitude
        );

        Toast.makeText(this,
                "Issue reported with priority level: " + priorityLevel,
                Toast.LENGTH_SHORT).show();

        // Save to Firebase (implement in FirebaseService)
        // FirebaseService.saveIssue(...);

        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}