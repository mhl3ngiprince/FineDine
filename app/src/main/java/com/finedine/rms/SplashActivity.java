package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.finedine.rms.KitchenActivity;
import com.finedine.rms.LoginActivity;
import com.finedine.rms.ManagerDashboardActivity;

import com.finedine.rms.OrderActivity;
import com.finedine.rms.R;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.finedine.rms.utils.RoleManager;
import com.finedine.rms.utils.SharedPrefsManager;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        prefsManager = new SharedPrefsManager(this);

        // Delay for splash screen (optional)
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthAndNavigate, 1500);
    }
    
    private void checkAuthAndNavigate() {
        // Check if user is already logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isLoggedIn = prefsManager.isUserLoggedIn();

        if (currentUser != null && isLoggedIn) {
            // User is logged in, navigate based on role
            String userRole = prefsManager.getUserRole();
            Log.d(TAG, "User is logged in with role: " + userRole);
            navigateBasedOnRole(userRole);
        } else if (currentUser != null) {
            // User is authenticated with Firebase but not in our SharedPreferences
            Log.d(TAG, "User authenticated with Firebase but not in SharedPrefs, fetching data");
            fetchUserDataFromFirestore(currentUser);
        } else {
            // No user logged in, go to login screen
            Log.d(TAG, "User not logged in, redirecting to login");
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish(); // Close splash activity
        }
    }

    private void fetchUserDataFromFirestore(FirebaseUser user) {
        FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        String name = documentSnapshot.getString("name");

                        if (role == null || role.isEmpty()) {
                            role = "customer"; // Default role
                        }

                        // Save to SharedPreferences
                        prefsManager.saveUserSession(0, role, name != null ? name : "");
                        prefsManager.setUserLoggedIn(true);

                        // Navigate based on role
                        navigateBasedOnRole(role);
                    } else {
                        // No user data in Firestore, go to login
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data", e);
                    // Error fetching user data, go to login
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                });
    }

    private void navigateBasedOnRole(String role) {
        Intent intent;

        switch (role) {
            case "manager":
                intent = new Intent(this, ManagerDashboardActivity.class);
                break;
            case "chef":
                intent = new Intent(this, KitchenActivity.class);
                break;
            case "waiter":
                intent = new Intent(this, OrderActivity.class);
                break;
            case "admin":
                intent = new Intent(this, AdminActivity.class);
                break;
            default:
                // Default to ReservationActivity for customers
                intent = new Intent(this, ReservationActivity.class);
                break;
        }

        // Add role info to intent
        intent.putExtra("user_role", role);
        startActivity(intent);
    }
}