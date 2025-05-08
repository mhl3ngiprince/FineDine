package com.finedine.rms.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupUtils {
    private static final String TAG = "BackupUtils";

    /**
     * Create a backup of the SQLite database file
     *
     * @param context Application context
     * @return Path to the backup file or null if backup failed
     */
    public static String backupDatabase(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return null;
        }

        File dbFile = context.getDatabasePath("fine_dine_db");
        if (!dbFile.exists()) {
            Log.e(TAG, "Database file does not exist: " + dbFile.getAbsolutePath());
            return null;
        }

        File backupDir = new File(context.getExternalFilesDir(null), "backups");
        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                Log.e(TAG, "Failed to create backup directory");
                Toast.makeText(context, "Failed to create backup directory", Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File backupFile = new File(backupDir, "fine_dine_backup_" + timestamp + ".db");

        try (InputStream in = new FileInputStream(dbFile);
             OutputStream out = new FileOutputStream(backupFile)) {

            byte[] buffer = new byte[8192]; // Larger buffer for better performance
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            Log.d(TAG, "Database backed up to: " + backupFile.getAbsolutePath());
            Toast.makeText(context, "Backup created successfully", Toast.LENGTH_SHORT).show();
            return backupFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Backup failed: " + e.getMessage(), e);
            Toast.makeText(context, "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * Restore database from a specific backup URI
     *
     * @param context   Application context
     * @param backupUri URI of the backup file
     * @return true if restore was successful
     */
    public static boolean restoreDatabase(Context context, Uri backupUri) {
        if (context == null || backupUri == null) {
            Log.e(TAG, "Context or backup URI is null");
            return false;
        }

        File dbFile = context.getDatabasePath("fine_dine_db");

        // Create a temporary file for validation
        File tempFile = new File(context.getCacheDir(), "temp_backup_verification.db");

        // First copy to temp file to validate
        try (InputStream in = context.getContentResolver().openInputStream(backupUri);
             OutputStream out = new FileOutputStream(tempFile)) {

            if (in == null) {
                Log.e(TAG, "Could not open input stream from URI: " + backupUri);
                return false;
            }

            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            // Basic validation - check file size and format
            if (tempFile.length() < 1000) { // Arbitrary small size check
                Log.e(TAG, "Backup file seems too small to be a valid database");
                return false;
            }

            // Now copy to actual database file
            try (InputStream tempIn = new FileInputStream(tempFile);
                 OutputStream dbOut = new FileOutputStream(dbFile)) {

                buffer = new byte[8192];
                while ((length = tempIn.read(buffer)) > 0) {
                    dbOut.write(buffer, 0, length);
                }

                Log.d(TAG, "Database restored successfully");
                return true;
            }

        } catch (IOException e) {
            Log.e(TAG, "Restore failed: " + e.getMessage(), e);
            return false;
        } finally {
            // Clean up temp file
            if (tempFile.exists()) {
                tempFile.delete();
            }
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
            if (context == null) {
                Log.e(TAG, "Context is null");
                return false;
            }

            // Get backup directory
            File backupDir = new File(context.getExternalFilesDir(null), "backups");

            if (!backupDir.exists() || !backupDir.isDirectory()) {
                Log.e(TAG, "Backup directory doesn't exist: " + backupDir.getAbsolutePath());
                Toast.makeText(context, "No backups found", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Find the most recent backup file
            File[] backupFiles = backupDir.listFiles((dir, name) -> name.endsWith(".db"));
            if (backupFiles == null || backupFiles.length == 0) {
                Log.e(TAG, "No backup files found in directory: " + backupDir.getAbsolutePath());
                Toast.makeText(context, "No backup files found", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Sort files by last modified date (most recent first)
            java.util.Arrays.sort(backupFiles, (f1, f2) ->
                    Long.compare(f2.lastModified(), f1.lastModified()));

            // Get the most recent backup
            File latestBackup = backupFiles[0];
            Log.d(TAG, "Restoring from most recent backup: " + latestBackup.getName());

            // Restore from this backup
            File dbFile = context.getDatabasePath("fine_dine_db");
            try (InputStream in = new FileInputStream(latestBackup);
                 OutputStream out = new FileOutputStream(dbFile)) {

                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                Log.d(TAG, "Database restored successfully from " + latestBackup.getName());
                Toast.makeText(context, "Restore successful from " + latestBackup.getName(),
                        Toast.LENGTH_LONG).show();
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "Restore failed: " + e.getMessage(), e);
            Toast.makeText(context, "Restore failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Export menu items to CSV file
     *
     * @param context  Application context
     * @param data     List of menu items to export
     * @param fileName Name of the export file
     */
    public static void exportToCSV(Context context, List<com.finedine.rms.MenuItem> data, String fileName) {
        if (context == null || data == null || data.isEmpty()) {
            Log.e(TAG, "Context or data is null/empty");
            return;
        }

        File exportDir = new File(context.getExternalFilesDir(null), "exports");
        if (!exportDir.exists()) {
            if (!exportDir.mkdirs()) {
                Log.e(TAG, "Failed to create export directory");
                Toast.makeText(context, "Failed to create export directory", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Ensure file has .csv extension
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            fileName += ".csv";
        }

        File file = new File(exportDir, fileName);

        try (Writer writer = new FileWriter(file);
             CSVWriter csvWriter = new CSVWriter(writer,
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.DEFAULT_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END)) {

            // Write header
            csvWriter.writeNext(new String[]{"ID", "Title", "Description", "Category", "Price", "Is Available"});

            // Write data
            for (com.finedine.rms.MenuItem menuItem : data) {
                try {
                    csvWriter.writeNext(new String[]{
                            String.valueOf(menuItem.getItem_id()),
                            menuItem.getName(),
                            menuItem.getDescription() != null ? menuItem.getDescription() : "",
                            menuItem.getCategory() != null ? menuItem.getCategory() : "",
                            String.format(Locale.US, "%.2f", menuItem.getPrice()),
                            String.valueOf(menuItem.isAvailability() ? "Yes" : "No")
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error writing menu item to CSV: " + e.getMessage(), e);
                    // Continue with next item
                }
            }

            Log.d(TAG, "CSV export completed: " + file.getAbsolutePath());
            Toast.makeText(context, "Exported to " + file.getPath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "CSV export failed: " + e.getMessage(), e);
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Export any bean-compatible objects to CSV using OpenCSV's bean utilities
     *
     * @param context  Application context
     * @param data     List of objects to export
     * @param fileName Name of the export file
     * @param <T>      Type of objects in the list
     * @return true if export was successful
     */
    public static <T> boolean exportBeansToCSV(Context context, List<T> data, String fileName) {
        if (context == null || data == null || data.isEmpty()) {
            Log.e(TAG, "Context or data is null/empty");
            return false;
        }

        File exportDir = new File(context.getExternalFilesDir(null), "exports");
        if (!exportDir.exists()) {
            if (!exportDir.mkdirs()) {
                Log.e(TAG, "Failed to create export directory");
                Toast.makeText(context, "Failed to create export directory", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // Ensure file has .csv extension
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            fileName += ".csv";
        }

        File file = new File(exportDir, fileName);

        try (Writer writer = new FileWriter(file)) {
            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                    .withQuotechar(CSVWriter.DEFAULT_QUOTE_CHARACTER)
                    .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                    .withOrderedResults(false)
                    .build();

            beanToCsv.write(data);

            Log.d(TAG, "Bean CSV export completed: " + file.getAbsolutePath());
            Toast.makeText(context, "Exported to " + file.getPath(), Toast.LENGTH_LONG).show();
            return true;
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            Log.e(TAG, "Bean CSV export failed: " + e.getMessage(), e);
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}