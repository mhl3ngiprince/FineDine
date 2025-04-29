package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvStaffCount, tvReservationCount, tvMenuItemCount, tvInventoryCount;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("FINE DINE ADMINISTRATION");

        // Set up navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up Action Bar Toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Initialize TextViews for counts
        tvStaffCount = findViewById(R.id.tvStaffCount);
        tvReservationCount = findViewById(R.id.tvReservationCount);
        tvMenuItemCount = findViewById(R.id.tvMenuItemCount);
        tvInventoryCount = findViewById(R.id.tvInventoryCount);

        // Set up click listeners for dashboard items
        CardView cardStaff = findViewById(R.id.cardStaff);
        CardView cardMenu = findViewById(R.id.cardMenu);
        CardView cardOrders = findViewById(R.id.cardOrders);
        CardView cardReservation = findViewById(R.id.cardReservation);
        CardView cardInventory = findViewById(R.id.cardInventory);
        CardView cardReports = findViewById(R.id.cardReports);

        cardStaff.setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, StaffManagementActivity.class)));
        cardMenu.setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, MenuManagementActivity.class)));
        cardOrders.setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, OrderActivity.class)));
        cardReservation.setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, ReservationActivity.class)));
        cardInventory.setOnClickListener(v -> startActivity(new Intent(AdminActivity.this, InventoryActivity.class)));
        cardReports.setOnClickListener(v -> Toast.makeText(AdminActivity.this, "Reports feature coming soon", Toast.LENGTH_SHORT).show());

        // Load dashboard data
        loadDashboardData();

        // Update navigation header with user info
        updateNavigationHeader();
    }

    private void loadDashboardData() {
        // Load Staff Count
        db.collection("users")
                .whereEqualTo("role", "staff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvStaffCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                })
                .addOnFailureListener(e -> {
                    tvStaffCount.setText("0");
                });

        // Load Reservation Count
        db.collection("reservations")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvReservationCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                })
                .addOnFailureListener(e -> {
                    tvReservationCount.setText("0");
                });

        // Load Menu Items Count
        db.collection("menu_items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvMenuItemCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                })
                .addOnFailureListener(e -> {
                    tvMenuItemCount.setText("0");
                });

        // Load Inventory Items Count
        db.collection("inventory")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvInventoryCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                })
                .addOnFailureListener(e -> {
                    tvInventoryCount.setText("0");
                });
    }

    private void updateNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView tvUsername = headerView.findViewById(R.id.nav_header_username);
        TextView tvEmail = headerView.findViewById(R.id.nav_header_email);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Get user data from Firestore
            db.collection("users")
                    .whereEqualTo("email", currentUser.getEmail())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String name = document.getString("name");
                            tvUsername.setText(name != null ? name : "Admin User");
                            tvEmail.setText(currentUser.getEmail());
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Fallback to Firebase Auth data
                        tvUsername.setText("Admin User");
                        tvEmail.setText(currentUser.getEmail());
                    });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already in dashboard, do nothing
        } else if (id == R.id.nav_staff) {
            startActivity(new Intent(AdminActivity.this, StaffManagementActivity.class));
        } else if (id == R.id.nav_menu) {
            startActivity(new Intent(AdminActivity.this, MenuManagementActivity.class));
        } else if (id == R.id.nav_orders) {
            startActivity(new Intent(AdminActivity.this, OrderActivity.class));
        } else if (id == R.id.nav_reservations) {
            startActivity(new Intent(AdminActivity.this, ReservationActivity.class));
        } else if (id == R.id.nav_inventory) {
            startActivity(new Intent(AdminActivity.this, InventoryActivity.class));
        } else if (id == R.id.nav_logout) {
            // Sign out from Firebase Auth
            mAuth.signOut();
            startActivity(new Intent(AdminActivity.this, LoginActivity.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            loadDashboardData();
            Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
