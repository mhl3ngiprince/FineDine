package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.finedine.rms.KitchenActivity;
import com.finedine.rms.LoginActivity;
import com.finedine.rms.ManagerDashboardActivity;

import com.finedine.rms.OrderActivity;
import com.finedine.rms.R;

import com.finedine.rms.ui.theme.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.finedine.rms.utils.RoleManager;
import com.finedine.rms.utils.SharedPrefsManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Delay for splash screen (optional)
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthAndNavigate, 1500);
    }
    
    private void checkAuthAndNavigate() {
        // Check if user is already logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser != null) {
            // User is logged in, navigate to main activity
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            // No user logged in, go to login screen
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        finish(); // Close splash activity
    }
}