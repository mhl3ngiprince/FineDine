# Navigation Panel Implementation Guide

This guide explains how to add the navigation panel to all activities in the FineDine app.

## Files Already Updated

1. Created the following new files:
    - `app/src/main/res/layout/navigation_panel.xml` - The navigation panel layout
    - `app/src/main/java/com/finedine/rms/BaseActivity.java` - Base activity class

2. Updated the following activities:
    - `OrderActivity.java` and its layout
    - `KitchenActivity.java` and its layout
    - `LoginActivity.java` (special case - extends BaseActivity but doesn't show navigation panel)

## Steps to Update Remaining Activities

For each activity, follow these steps:

### 1. Update the Layout XML

For each `activity_xyz.xml` file:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    ... >
    
    <!-- Include Navigation Panel -->
    <include
        android:id="@+id/navigation_panel"
        layout="@layout/navigation_panel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
        
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="24dp">
        
        <!-- Original layout content goes here -->
        
    </LinearLayout>
</LinearLayout>
```

Special notes:

- Make sure the root layout is a LinearLayout with vertical orientation
- Wrap all existing content in a new LinearLayout with padding
- Set the root layout's padding to 0

### 2. Update the Java Activity Class

For each `XyzActivity.java` file:

1. Change the class declaration to extend BaseActivity instead of AppCompatActivity:

```java
public class XyzActivity extends BaseActivity {
```

2. Add the setupNavigationPanel() call after setContentView():

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_xyz);
    
    // Setup navigation panel
    setupNavigationPanel("Activity Title");
    
    // Rest of your onCreate code...
}
```

3. Remove any imports for AppCompatActivity that are no longer needed.

## Activities to Update

The following activities still need to be updated:

- AdminActivity
- CreateReservationActivity
- EditStaffActivity
- InventoryActivity
- ManagerDashboardActivity
- MenuManagementActivity
- ReservationActivity
- RegisterActivity
- StaffActivity
- StaffManagementActivity

## Special Cases

- **LoginActivity** - Extends BaseActivity but doesn't display the navigation panel
- **RegisterActivity** - Similar to LoginActivity, should extend BaseActivity but may not display
  the navigation panel

## Testing

After updating all activities:

1. Test navigation between all screens
2. Verify role-based access control still works
3. Ensure UI is consistent across all activities