package com.finedine.rms.utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;


    public class DialogUtils {
        public static void showProgressDialog(Context context, String message) {
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        public static void hideProgressDialog(ProgressDialog progressDialog) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }

        public static void showErrorDialog(Context context, String title, String message) {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        }

        public static void showConfirmationDialog(Context context, String title, String message,
                                                  DialogInterface.OnClickListener positiveListener) {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Confirm", positiveListener)
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        public static void showListDialog(Context context, String title, String[] items,
                                          DialogInterface.OnClickListener listener) {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setItems(items, listener)
                    .show();
        }
    }

