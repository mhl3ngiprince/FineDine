package com.finedine.rms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class StaffActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FirebaseFirestore db;
    private List<StaffMember> staffList;
    private StaffAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        
        // Initialize UI components
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        staffList = new ArrayList<>();
        adapter = new StaffAdapter(this, staffList);
        recyclerView.setAdapter(adapter);
        
        // Setup SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::loadStaffData);
        
        // Setup FAB for adding new staff
        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(view -> {
            // Launch EditStaffActivity to add a new staff member
            // Intent intent = new Intent(StaffActivity.this, EditStaffActivity.class);
            // startActivity(intent);
        });
        
        // Load initial data
        loadStaffData();
    }
    
    private void loadStaffData() {
        swipeRefreshLayout.setRefreshing(true);
        
        db.collection("users")
            .whereEqualTo("role", "staff")
            .get()
            .addOnCompleteListener(task -> {
                swipeRefreshLayout.setRefreshing(false);
                
                if (task.isSuccessful()) {
                    staffList.clear();
                    QuerySnapshot result = task.getResult();
                    if (result != null) {
                        for (QueryDocumentSnapshot document : result) {
                            StaffMember staff = document.toObject(StaffMember.class);
                            staff.setId(document.getId());
                            staffList.add(staff);
                        }
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    // Handle error
                }
            });
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation item clicks
        int id = item.getItemId();
        
        // Add navigation handling code here
        
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
    
    // Inner class for staff member data model
    public static class StaffMember {
        private String id;
        private String name;
        private String email;
        private String phone;
        private String role;
        private String hireDate;
        
        public StaffMember() {
            // Required empty constructor for Firestore
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getHireDate() { return hireDate; }
        public void setHireDate(String hireDate) { this.hireDate = hireDate; }
    }
    
    // Inner class for RecyclerView adapter
    private static class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.StaffViewHolder> {
        private final List<StaffMember> staffList;
        private final AppCompatActivity activity;
        
        public StaffAdapter(AppCompatActivity activity, List<StaffMember> staffList) {
            this.activity = activity;
            this.staffList = staffList;
        }
        
        @NonNull
        @Override
        public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff, parent, false);
            return new StaffViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
            StaffMember staff = staffList.get(position);
            holder.nameTextView.setText(staff.getName());
            holder.emailTextView.setText(staff.getEmail());
            holder.phoneTextView.setText(staff.getPhone());
            holder.roleTextView.setText(staff.getRole());
            
            holder.itemView.setOnClickListener(v -> {
                // Open staff details or edit activity
                // Intent intent = new Intent(activity, EditStaffActivity.class);
                // intent.putExtra("STAFF_ID", staff.getId());
                // activity.startActivity(intent);
            });
        }
        
        @Override
        public int getItemCount() {
            return staffList.size();
        }
        
        static class StaffViewHolder extends RecyclerView.ViewHolder {
            TextView nameTextView, emailTextView, phoneTextView, roleTextView;
            
            StaffViewHolder(View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.tvStaffName);
                emailTextView = itemView.findViewById(R.id.tvStaffEmail);
                phoneTextView = itemView.findViewById(R.id.text_phone);
                roleTextView = itemView.findViewById(R.id.tvStaffRole);
            }
        }
    }
}