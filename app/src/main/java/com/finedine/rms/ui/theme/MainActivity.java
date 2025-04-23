package com.finedine.rms.ui.theme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.finedine.rms.R;
import com.finedine.rms.AppDatabase;
import com.finedine.rms.KitchenActivity;
import com.finedine.rms.ManagerDashboardActivity;
import com.finedine.rms.OrderActivity;
import com.finedine.rms.RegisterActivity;
import com.finedine.rms.Reservation;
import com.finedine.rms.User;

public class MainActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
   // private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
    }

    public void attemptLogin(View view) {
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();


        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }


        new Thread(() -> {
            User user = AppDatabase.getDatabase(this).userDao().login(email, hashPassword(password));
            runOnUiThread(() -> {
                if (user != null) {
                    saveUserSession(user);
                    redirectToRoleHome(user.role);
                } else {
                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private String hashPassword(String plain) {
        // In production, use proper hashing like BCrypt
        return String.valueOf(plain.hashCode());
    }

    private void saveUserSession(User user) {
        SharedPreferences prefs = getSharedPreferences("FineDinePrefs", MODE_PRIVATE);

        prefs.edit()
                .putInt("userId", user.user_id)
                .putString("userRole", user.role)
                .putString("userName", user.name)
                .apply();

    }

   private void redirectToRoleHome(String role) {
        Intent intent;
        switch (role){
            case "waiter":
                intent = new Intent(this, OrderActivity.class);
                break;
            case "chef":
                intent = new Intent(this, KitchenActivity.class);
                break;
            case "manager":
                intent = new Intent(this, ManagerDashboardActivity.class);
                break;
            default: // customer
                intent = new Intent(this, Reservation.class);
        }
        startActivity(intent);
        finish();
    }

    public void goToRegister(View view) {
        startActivity(new Intent(this, RegisterActivity.class));
    }
}