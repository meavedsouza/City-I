package com.city_i.dashboard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import com.city_i.R;
import com.city_i.fragments.HomeFragment;
import com.city_i.fragments.ReportFragment;
import com.city_i.fragments.TrackFragment;
import com.city_i.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class UserDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Load default fragment
        loadFragment(new HomeFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;

            if (item.getItemId() == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_report) {
                fragment = new ReportFragment();
            } else if (item.getItemId() == R.id.nav_track) {
                fragment = new TrackFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
            }

            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}