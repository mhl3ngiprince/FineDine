#!/bin/bash
# This script will help you update all activity layouts to include the navigation panel
# and update all the activity Java files to extend BaseActivity

# List of activities to update
activities=(
  "AdminActivity"
  "CreateReservationActivity"
  "EditStaffActivity"
  "InventoryActivity"
  "KitchenActivity"
  "LoginActivity"
  "ManagerDashboardActivity"
  "MenuManagementActivity"
  "ReservationActivity"
  "RegisterActivity"
  "StaffActivity"
  "StaffManagementActivity"
)

# For each activity, update the layout
echo "Updating layout files..."
for activity in "${activities[@]}"; do
  # Convert to lowercase for the layout name
  lower_activity=$(echo "$activity" | sed 's/\([A-Z]\)/\L\1/g')
  layout_file="app/src/main/res/layout/activity_${lower_activity#activity_}.xml"
  
  echo "Updating $layout_file"
  if [ -f "$layout_file" ]; then
    # The commands below are just guidelines. You'll need to manually edit each file.
    # 1. Add the include for navigation_panel at the top of the layout
    # 2. Wrap the existing content in a LinearLayout with padding
    echo "Please update $layout_file manually."
  else
    echo "Warning: $layout_file does not exist."
  fi
done

# For each activity, update the Java file
echo "Updating Java files..."
for activity in "${activities[@]}"; do
  java_file="app/src/main/java/com/finedine/rms/${activity}.java"
  
  echo "Updating $java_file"
  if [ -f "$java_file" ]; then
    # The commands below are just guidelines. You'll need to manually edit each file.
    # 1. Change extends AppCompatActivity to extends BaseActivity
    # 2. Add setupNavigationPanel call after setContentView
    echo "Please update $java_file manually."
  else
    echo "Warning: $java_file does not exist."
  fi
done

echo "All tasks completed. Please review the changes."