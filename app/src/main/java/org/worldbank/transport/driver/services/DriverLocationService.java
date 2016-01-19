package org.worldbank.transport.driver.services;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.worldbank.transport.driver.R;

import java.lang.ref.WeakReference;


public class DriverLocationService extends Service implements GpsStatus.Listener {

    // identifier for device location access request, if runtime prompt necessary
    public static final int PERMISSION_REQUEST_ID = Integer.MAX_VALUE / 8;
    public static final int API_AVAILABILITY_REQUEST_ID = Integer.MAX_VALUE / 16;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private WeakReference<Activity> caller;
    private Context context;


    @Override
    public void onGpsStatusChanged(int event) {

        switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                break;
            case GpsStatus.GPS_EVENT_STARTED:
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                break;
            default:
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);

        // TODO: should be explicitly stopped when done during normal operation, but what if there are no clients left?
        // If not sticky, need to figure out how to start service again if needed after app next launch

        // return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
            locationManager.removeGpsStatusListener(this);
        }

        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if (!requestUpdatesOrPermissions()) {
            this.stopSelf();
        }
    }

    public DriverLocationService(Activity caller) {
        this.context = caller.getApplicationContext();
        this.caller = new WeakReference<>(caller);
    }

    /**
     * Check if app has access and permission to get location updates, and if so, start them.
     *
     * @return True if location updates have been started right now
     */
    private boolean requestUpdatesOrPermissions() {
        // check for location service availability and status

        GoogleApiAvailability gapiAvailability = GoogleApiAvailability.getInstance();
        int availability = gapiAvailability.isGooglePlayServicesAvailable(context);

        if (availability != ConnectionResult.SUCCESS) {
            // possibilities for play service access failure are:
            // SERVICE_MISSING, SERVICE_UPDATING, SERVICE_VERSION_UPDATE_REQUIRED, SERVICE_DISABLED, SERVICE_INVALID

            // show system dialog to explain; also finish calling activity, since user cannot enter a record
            showApiErrorDialog(gapiAvailability, availability);
            return false;
        }

        // in API 23+, permission granting happens at runtime
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Activity callingActivity = caller.get();
            if (callingActivity == null) {
                // calling activity is already gone, so don't bother attempting to prompt for permissions now
                return false;
            }
            // in case user has denied location permissions to app previously, tell them why it's needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(callingActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                displayPermissionRequestRationale(context);
                return false; // up to the app to start up this service again when permissions granted
            }

            ActivityCompat.requestPermissions(callingActivity, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ID);
            return false;
        } else {

            // prompt user to turn on GPS, if needed
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Intent enableGpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(enableGpsIntent);

                Toast toast = new Toast(context);
                toast.setText(R.string.location_gps_needed_rationale);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.show();

                return false;
            }

            // have permission and access to GPS location; request updates

            // Android Studio will complain mightily if requestLocationUpdates is not called
            // after a permissions check in the same method
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.addGpsStatusListener(this);

            return true;
        }
    }

    private void showApiErrorDialog(GoogleApiAvailability gapiAvailability, int errorCode) {
        Activity callingActivity = caller.get();
        if (callingActivity == null) {
            return;
        }

        Dialog errorDialog = gapiAvailability.getErrorDialog(callingActivity, errorCode, API_AVAILABILITY_REQUEST_ID);
        errorDialog.show();
        callingActivity.finish();
    }

    public static void displayPermissionRequestRationale(Context context) {
        Toast toast = new Toast(context);
        toast.setText(R.string.location_fine_permission_rationale);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }
}
