package com.huawei.cordovahmslocationplugin;

import android.Manifest;
import android.content.IntentSender;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import com.huawei.hms.common.ApiException;
import com.huawei.hms.common.ResolvableApiException;
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationCallback;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationResult;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.location.LocationSettingsRequest;
import com.huawei.hms.location.LocationSettingsStatusCodes;
import com.huawei.hms.location.SettingsClient;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

/**
 *
 */
public class CordovaHMSLocationPlugin extends CordovaPlugin {

    private static final String TAG = CordovaHMSLocationPlugin.class.getSimpleName();

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private CallbackContext mCallbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        requestPermission();
        if (fusedLocationProviderClient == null) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(cordova.getContext());
        }
        switch (action) {
            case "requestLocation":
                this.setLocationRequest();
                this.setLocationCallback();
                this.checkLocationSetting(callbackContext);
                return true;
            case "removeLocation":
                this.stopLocation();
                return true;
            case "getLastlocation":
                this.getLastlocation(callbackContext);
                return true;
            default:
                return false;
        }
    }

    private void returnLocation(Location location) {
        if (mCallbackContext != null) {
            Log.d(TAG, "returnLocation");
            String message = location.getLatitude() + "," + location.getLongitude();
            PluginResult result = new PluginResult(PluginResult.Status.OK, message);
            result.setKeepCallback(true);
            mCallbackContext.sendPluginResult(result);
        }
    }

    private void getLastlocation(CallbackContext callbackContext) {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            Log.d(TAG, "getLastlocation success");
            if (location == null) {
                return;
            }
            // Location?????????????????????
            String message = location.getLatitude() + "," + location.getLongitude();
            callbackContext.success(message);
        }).addOnFailureListener(e -> {
            // ??????????????????
            callbackContext.error("getLastlocation fail");
        });
    }

    private void stopLocation() {
        if (fusedLocationProviderClient == null) {
            Log.d(TAG, "fusedLocationProviderClient is null");
            return;
        }
        // ?????????????????????????????????mLocationCallback?????????requestLocationUpdates????????????LocationCallback????????????????????????
        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
            .addOnSuccessListener(aVoid -> {
                // ????????????????????????
                Log.d(TAG, "stop success");
                mCallbackContext = null;
            })
            .addOnFailureListener(e -> {
                // ????????????????????????
                Log.d(TAG, "stop fail");
            });
    }

    private void setLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    //????????????????????????
                    if (mLocationCallback != null) {
                        Log.d(TAG, "onLocationResult");
                        Location location = locationResult.getLocations().get(0);
                        returnLocation(location);
                    }
                }
            }
        };
    }

    private void setLocationRequest() {
        mLocationRequest = new LocationRequest();
        // ???????????????????????????(???????????????)
        mLocationRequest.setInterval(10000);
//        mLocationRequest.setNumUpdates(1);
        // ????????????
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void checkLocationSetting(CallbackContext callbackContext) {
        SettingsClient settingsClient = LocationServices.getSettingsClient(cordova.getContext());
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();
        // ????????????????????????
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(locationSettingsResponse -> {
                // ????????????????????????????????????????????????
                fusedLocationProviderClient
                    .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
                    .addOnSuccessListener(aVoid -> {
                        // ???????????????????????????
                        Log.d(TAG, "request success");
                        mCallbackContext = callbackContext;
                    });
            })
            .addOnFailureListener(e -> {
                // ???????????????????????????
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException rae = (ResolvableApiException) e;
                            // ??????startResolutionForResult??????????????????????????????????????????
                            rae.startResolutionForResult(cordova.getActivity(), 0);
                        } catch (IntentSender.SendIntentException sie) {
                            Log.d(TAG, sie.getMessage());
                        }
                        break;
                }
            });
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Log.i(TAG, "sdk < 29 Q");
            if (!cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                || !cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                String[] strings =
                    {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                cordova.requestPermissions(this, 1, strings);
            }
        } else {
            if (!cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                || !cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                || !cordova.hasPermission("android.permission.ACCESS_BACKGROUND_LOCATION")) {
                String[] strings = {android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    "android.permission.ACCESS_BACKGROUND_LOCATION"};
                cordova.requestPermissions(this, 2, strings);
            }
        }
    }
}
