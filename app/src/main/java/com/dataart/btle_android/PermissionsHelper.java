package com.dataart.btle_android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import timber.log.Timber;

/**
 * Created by Constantine Mars on 12/6/15.
 * Android M with Google Play Services requires Location enabled before starting BLE devices discovery
 * This helper implements turning on Location services programmatically
 */
public class PermissionsHelper implements ResultCallback<LocationSettingsResult>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final int REQUEST_CHECK_SETTINGS = 100;
    protected LocationSettingsRequest mLocationSettingsRequest;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    private LocationEnabledListener listener;
    private Activity activity;
    private boolean waitForResume = false;

    public PermissionsHelper(LocationEnabledListener listener, Activity activity) {
        this.listener = listener;
        this.activity = activity;
        buildGoogleApiClient();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    public void start() {
        mGoogleApiClient.connect();
    }

    protected void stop() {
        mGoogleApiClient.disconnect();
    }

    /**
     * Build request to enable Location
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Location request is necessary for defining which exactly permissions must be enabled
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Timber.i("All location settings are satisfied.");
                listener.onLocationEnabled();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Timber.i("Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings ");

                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result
                    // in onActivityResult().
                    status.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Timber.i("PendingIntent unable to execute request.");
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Timber.i("Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Timber.i("User agreed to make required location settings changes.");
                        listener.onLocationEnabled();
                        break;
                    case Activity.RESULT_CANCELED:
                        Timber.i("User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }

        return gps_enabled || network_enabled;
    }

    protected void checkLocationEnabled() {
        if (isLocationEnabled()) {
            listener.onLocationEnabled();
            return;
        }

//        If location isn't enabled - check whether we can call Google Play Services or user to manually
//        switch Location
        final int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity.getApplicationContext());
        if (status == ConnectionResult.SUCCESS) {
            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(
                            mGoogleApiClient,
                            mLocationSettingsRequest
                    );
            result.setResultCallback(this);
        } else {
//            If no services available - the only thing we can do is to
//            ask user to switch Location manually
            activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            waitForResume = true;
        }
    }

    public void resume() {
        if (waitForResume) {
            checkLocationEnabled();
        }
    }

    public interface LocationEnabledListener {
        void onLocationEnabled();
    }

}
//    private AppCompatActivity activity;
//    private static Integer requestCodeCounter = 0;
//
//    public static Integer requestPermission(Activity activity, String permission) {
//
//        // Here, thisActivity is the current activity
//        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
//            return null;
//        }
//
//        Integer requestCode = requestCodeCounter++;
//        // Should we show an explanation?
//        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
//
//            // Show an expanation to the user *asynchronously* -- don't block
//            // this thread waiting for the user's response! After the user
//            // sees the explanation, try again to request the permission.
//
//        } else {
//
//            // No explanation needed, we can request the permission.
//
//            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
//
//            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
//            // app-defined int constant. The callback method gets the
//            // result of the request.
//        }
//
//        return requestCode;
//    }
//
//    public static void onRequestPermissionsResult(OnPermissionGranted onPermissionGranted, final Integer expectedCode, int requestCode, String permissions[], int[] grantResults) {
//
//        if(expectedCode != null && expectedCode == requestCode) {
//            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
//            onPermissionGranted.call(granted);
//        }
//    }
//
//    public interface OnPermissionGranted{
//        void call(boolean granted);
//    }
