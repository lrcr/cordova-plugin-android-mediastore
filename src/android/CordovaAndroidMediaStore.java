package com.heartade;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;

import android.provider.MediaStore;
import android.os.Build;
import android.content.ContentValues;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class downloads image(png, jpg, gif, webp) using AndroidMediaStore.
 * Adheres to device storage permission policy on Google play store for Android OS 10 and 11 / API level > 29 devices.
 */
public class CordovaAndroidMediaStore extends CordovaPlugin {

    public CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.callbackContext = callbackContext;

        if (action.equals("store")) {
            boolean result = this.store(args.getString(0), args.getString(1), args.getString(2), args.getString(3), callbackContext);
            if (result) {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                callbackContext.sendPluginResult(pluginResult);
                callbackContext.success("true");
                return true;
            }
        }
        return false;
    }

    private boolean store(String byteString, String fileDir, String fileName, String mimeType, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        final CordovaInterface _cordova = cordova;

        try {

            cordova.getThreadPool().execute(new Runnable() {
                CordovaInterface cordova = _cordova;

                @Override
                public void run() {
                    try {
                        byte[] byteArray = Base64.decode(byteString, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                        Context context = this.cordova.getActivity();
                        ContentResolver contentResolver = context.getContentResolver();
                        if (Build.VERSION.SDK_INT >= 29) {
                            final ContentValues contentValues = new ContentValues();
                            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, fileDir);
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);

                            Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                            OutputStream out = contentResolver.openOutputStream(imageUri);
                            if (mimeType.equals("image/gif")) {
                                out.write(byteArray);
                                out.flush();
                                out.close();
                            }
                            else if (mimeType.equals("image/png")) bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            else if (mimeType.equals("image/webp")) bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out);
                            else bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);


                            contentValues.clear();
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0);
                            contentResolver.update(imageUri, contentValues, null, null);
                        } else {
                            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                            File dir = new File(path + "/" + fileDir);
                            dir.mkdirs();
                            File file = new File(dir, fileName);
                            FileOutputStream out = new FileOutputStream(file);
                            if (mimeType.equals("image/gif")) {
                                out.write(byteArray);
                                out.flush();
                                out.close();
                            }
                            else if (mimeType.equals("image/png")) bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            else if (mimeType.equals("image/webp")) bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out);
                            else bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);

                            Uri contentUri = Uri.fromFile(file);
                            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri));
                        }
                        callbackContext.success();
                    } catch (RuntimeException | IOException e) {
                        e.printStackTrace();
                        callbackContext.error(e.getMessage());
                    }
                }
            });
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
            return false;
        }
    }

}
