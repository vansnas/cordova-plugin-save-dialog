package io.github.amphiluke;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log; // Import Log for logging
import androidx.documentfile.provider.DocumentFile;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaArgs;

import org.json.JSONException;

import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

public class SaveDialog extends CordovaPlugin {
    private static final int LOCATE_FILE = 1;

    private CallbackContext callbackContext;
    private final ByteArrayOutputStream fileByteStream = new ByteArrayOutputStream();

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        switch (action) {
            case "locateFile":
                this.locateFile(args.getString(0), args.getString(1));
                this.fileByteStream.reset();
                break;
            case "addChunk":
                this.addChunk(args.getArrayBuffer(0));
                break;
            case "saveFile":
                Uri uri = Uri.parse(args.getString(0));
                byte[] rawData = this.fileByteStream.toByteArray();
                
                // Use cordova.getThreadPool().execute(new Runnable()) for asynchronous tasks
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        saveFileAsync(uri, rawData);
                    }
                });

                this.fileByteStream.reset();
                break;
            default:
                return false;
        }
        return true;
    }

    private void locateFile(final String type, final String name) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(type);
                intent.putExtra(Intent.EXTRA_TITLE, name);
                cordova.startActivityForResult(SaveDialog.this, intent, SaveDialog.LOCATE_FILE);
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == SaveDialog.LOCATE_FILE && this.callbackContext != null) {
            if (resultCode == Activity.RESULT_CANCELED) {
                this.callbackContext.error("The dialog has been cancelled");
            } else if (resultCode == Activity.RESULT_OK && resultData != null) {
                Uri uri = resultData.getData();
                
                // Use cordova.getThreadPool().execute(new Runnable()) for asynchronous tasks
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        String filePath = getFilePathFromUri(uri);
                        if (filePath != null) {
                            callbackContext.success(filePath);
                        } else {
                            callbackContext.error("Failed to retrieve file path");
                        }
                    }
                });
            } else {
                this.callbackContext.error("Unknown error");
            }
        }
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }

    private void addChunk(byte[] chunk) {
        // Use cordova.getThreadPool().execute(new Runnable()) for asynchronous tasks
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    fileByteStream.write(chunk);
                    callbackContext.success();
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // Use cordova.getThreadPool().execute(new Runnable()) for asynchronous tasks
    private void saveFileAsync(Uri uri, byte[] rawData) {
        try {
            ParcelFileDescriptor pfd = cordova.getActivity().getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
            try {
                fileOutputStream.write(rawData);
                
                // Use cordova.getThreadPool().execute(new Runnable()) for asynchronous tasks
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        String filePath = getFilePathFromUri(uri);
                        callbackContext.success(filePath);
                    }
                });
            } catch (Exception e) {
                callbackContext.error(e.getMessage());
                e.printStackTrace();
            } finally {
                fileOutputStream.close();
                pfd.close();
            }
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
            e.printStackTrace();
        }
    }


    private String getFilePathFromUri(Uri uri) {
        if (uri == null) {
            return null;
        }

        String path = uri.toString();
        Log.d("FilePathFromUri", "URI used directly: " + path);

        return path;
    }



}
