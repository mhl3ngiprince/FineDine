package com.finedine.rms;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.R;
import com.finedine.rms.MenuItemAdapter;
import com.finedine.rms.AppDatabase;
import com.finedine.rms.MenuItem;
import java.util.List;



public class MenuManagementActivity extends AppCompatActivity {
    private RecyclerView rvMenuItems;
    private MenuAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_management);

        rvMenuItems = findViewById(R.id.rvMenuItems);
        rvMenuItems.setLayoutManager(new LinearLayoutManager(this));

        loadMenuItems();
    }

    private void loadMenuItems() {
        AppDatabase db = AppDatabase.getDatabase(this);
        new Thread(() -> {
            List<MenuItem> items = db.menuItemDao().getAllAvailable();
            runOnUiThread(() -> {
                MenuItemAdapter.OnMenuItemClickListener OnItemClickListener = (MenuItemAdapter.OnMenuItemClickListener) db.menuItemDao().getAllAvailable();
                adapter = new MenuAdapter(items, (MenuAdapter.OnItemClickListener) OnItemClickListener);
                rvMenuItems.setAdapter(adapter);
            });
        }).start();
    }
}