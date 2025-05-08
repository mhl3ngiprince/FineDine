package com.finedine.rms.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.view.MenuItem;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupUtils {
    public static String backupDatabase(Context context) {
        File dbFile = context.getDatabasePath("fine_dine_db");
        File backupDir = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            backupDir = new File(context.getExternalFilesDir(null), "backups");
        }

        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File backupFile = new File(backupDir, "fine_dine_backup_" + timestamp + ".db");

        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = new FileOutputStream(backupFile)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            Toast.makeText(context, "Backup created successfully", Toast.LENGTH_SHORT).show();
            return backupFile.getAbsolutePath();
        } catch (IOException e) {
            Toast.makeText(context, "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public static boolean restoreDatabase(Context context, Uri backupUri) {
        File dbFile = context.getDatabasePath("fine_dine_db");

        try (InputStream in = context.getContentResolver().openInputStream(backupUri);
             OutputStream out = new FileOutputStream(dbFile)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Restore the database from the most recent backup file
     * This is a convenience method that doesn't require a URI
     *
     * @param context The application context
     * @return true if restore was successful
     */
    public static boolean restoreDatabase(Context context) {
        try {
            // Get backup directory
            File backupDir = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                backupDir = new File(context.getExternalFilesDir(null), "backups");
            }

            if (!backupDir.exists() || !backupDir.isDirectory()) {
                return false;
            }

            // Find the most recent backup file
            File[] backupFiles = backupDir.listFiles((dir, name) -> name.endsWith(".db"));
            if (backupFiles == null || backupFiles.length == 0) {
                return false;
            }

            // Sort files by last modified date (most recent first)
            java.util.Arrays.sort(backupFiles, (f1, f2) ->
                    Long.compare(f2.lastModified(), f1.lastModified()));

            // Get the most recent backup
            File latestBackup = backupFiles[0];

            // Restore from this backup
            File dbFile = context.getDatabasePath("fine_dine_db");
            try (InputStream in = new FileInputStream(latestBackup);
                 OutputStream out = new FileOutputStream(dbFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                Toast.makeText(context, "Restore successful from " + latestBackup.getName(),
                        Toast.LENGTH_LONG).show();
                return true;
            }

        } catch (Exception e) {
            Toast.makeText(context, "Restore failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static void exportToCSV(Context context, List<com.finedine.rms.MenuItem> data, String fileName) {
        File exportDir = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            exportDir = new File(context.getExternalFilesDir(null), "exports");
        }
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, fileName);
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            // Write header
            writer.writeNext(new String[]{"ID", "Name", "Description", "Price"});

            // Write data
            for (com.finedine.rms.MenuItem menuItem : data) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    writer.writeNext(new String[]{
                            String.valueOf(menuItem.getItem_id()),
                            menuItem.toString(),
                            (String) menuItem.getDescription(),
                            String.valueOf(menuItem.getPrice())
                    });
                }
            }

            Toast.makeText(context, "Exported to " + file.getPath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}