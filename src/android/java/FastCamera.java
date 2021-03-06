package com.cordovapluginfastcam;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class FastCamera extends CordovaPlugin implements GpsDataCallback {
    private static final String TAG = "CameraTestApp";
    private CallbackContext startCameraCallback = null;
    private CallbackContext positionDataCallback = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        
        if (action.equals("startCamera")) {
            this.startCameraCallback  = callbackContext;
            this.startCamera(args);
            return true;
        }

        if (action.equals("initGps")) {
            this.positionDataCallback = callbackContext;
            this.initGps(args);
            return true;
        }

        if (action.equals("simulateGps")) {
            this.positionDataCallback = callbackContext;
            this.simulateGps(args);
            return true;
        }

        return false;
    }

    private void simulateGps(JSONArray args) {
        Double altOffset = 0d;
        try {
            altOffset = args.getDouble(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        GpsCommunication gps = GpsCommunication.getInstance(this.cordova.getActivity());
        gps.configure(0, altOffset, GpsCommunication.GeoidModel.alto_adige);
        // true = simulate gps position
        gps.initialize(true);
    }

    private void startCamera(JSONArray args) {
        String mode = "SINGLE_PHOTO";
        long clockSyncTimestamp = 0l;
        try {
            mode = args.getString(0);
            clockSyncTimestamp = args.getLong(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Context context = this.cordova.getActivity().getApplicationContext();
        Intent intent = new Intent(context, CameraActivity.class);
        Bundle intentData = new Bundle();
        intentData.putString("cameraMode", mode);
        intentData.putLong("clockSyncTimestamp", clockSyncTimestamp);
        intent.putExtras(intentData);
        this.cordova.setActivityResultCallback(this);
        this.cordova.getActivity().startActivityForResult(intent, 0);
    }

    private void initGps(JSONArray args) {
        int baudRate = 0;
        Double altOffset = 0d;
        GpsCommunication.GeoidModel geoidModel = GpsCommunication.GeoidModel.alto_adige;
        try {
            baudRate = args.getInt(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            altOffset = args.getDouble(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            geoidModel = GpsCommunication.GeoidModel.values()[args.getInt(2)];
        } catch (JSONException e) {
            e.printStackTrace();
        }

        GpsCommunication gps = GpsCommunication.getInstance(this.cordova.getActivity());
        gps.configure(baudRate, altOffset, geoidModel);
        gps.addEventListener(this);
        gps.initialize();
    }

    @Override
    public void onData(GPSPosition pos) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, pos.toJson());
        pluginResult.setKeepCallback(true); // keep callback
        this.positionDataCallback.sendPluginResult(pluginResult);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK && this.startCameraCallback != null) {
            String resultJSON = CameraActivity.getResultJson();
            this.startCameraCallback.success(resultJSON);
        }
    }

    private void echo(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}