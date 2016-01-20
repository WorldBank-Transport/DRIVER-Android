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
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.activities.RecordFormConstantsActivity;
import org.worldbank.transport.driver.staticmodels.DriverApp;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class DriverLocationService extends Service implements GpsStatus.Listener {

    private static final String LOG_LABEL = "LocationService";

    // identifier for device location access request, if runtime prompt necessary
    public static final int PERMISSION_REQUEST_ID = Integer.MAX_VALUE / 8;
    public static final int API_AVAILABILITY_REQUEST_ID = Integer.MAX_VALUE / 16;

    /**
     * Location accuracy is a radius in meters of 68% confidence, so updates may come through
     * with high accuracy that actually fall outside the stated radius.
     */

    // minimum location accuracy to accept for a location update (GPS should always be within this)
    private static final int MINIMUM_LOCATION_ACCURACY = 100;

    // after minimum accuracy location found,
    // keep listening for this level of accuracy until timeout reached
    private static final int PREFERRED_LOCATION_ACCURACY = 30;
    private static final int PREFERRED_LOCATION_TIMEOUT_MS = 20000; // milliseconds

    public interface DriverLocationUpdateListener {
        void bestLocationFound(Location estimatedLocation);
        void gotGpsFix();
        void foundFirstLocation();
    }

    public class LocationServiceBinder extends Binder {
        public DriverLocationService getService() {
            return DriverLocationService.this;
        }
    }

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Context context;
    private final IBinder binder = new LocationServiceBinder();
    private DriverLocationUpdateListener driverLocationUpdateListener;

    // will be non-null when at least one location update received with the minimum accuracy
    private Location acceptableLocation;
    private long startedWaitingForBetterLocation;
    private ArrayList<Location> preferredAccuracyLocations;

    /**
     * Return best location estimate found for received updates.
     * Should stop this service after receiving this result (can be accomplished by unbinding)
     *
     * @return best estimate of current location during service run
     */
    @Nullable
    public Location getBestLocationFound() {

        if (preferredAccuracyLocations.size() > 0) {
            // calculate weighted average location from those found

            if (preferredAccuracyLocations.size() == 1) {
                // if only got one, just use it
                return new Location(preferredAccuracyLocations.get(0));
            }

            double sumLat = 0;
            double sumLon = 0;
            float sumAccuracy = 0;
            double sumInvertedAccuracy = 0;

            for (Location location : preferredAccuracyLocations) {
                float accuracy = location.getAccuracy();

                // accuracy for these readings is within range of 0 to PREFERRED_LOCATION_ACCURACY
                // smaller is better; invert range for weighted average
                float invertedAccuracy = PREFERRED_LOCATION_ACCURACY - accuracy;

                sumLat += location.getLatitude() * invertedAccuracy;
                sumLon += location.getLongitude() * invertedAccuracy;
                sumAccuracy += accuracy;
                sumInvertedAccuracy += invertedAccuracy;
            }

            // use weighted average for coordinates
            double bestLat = sumLat / sumInvertedAccuracy;
            double bestLon = sumLon / sumInvertedAccuracy;

            // store simple average for accuracy
            float bestAccuracy = sumAccuracy / preferredAccuracyLocations.size();
            Location estimatedLocation = new Location("");
            estimatedLocation.setLatitude(bestLat);
            estimatedLocation.setLongitude(bestLon);
            estimatedLocation.setAccuracy(bestAccuracy);
            return estimatedLocation;

        } else if (acceptableLocation != null) {
            return acceptableLocation;
        }

        return null;
    }

    /**
     * Call when service has finished attempting to find the best location. Notify listener.
     * Listener should stop this service after result received.
     */
    private void finishWithBestLocation() {
        if (driverLocationUpdateListener != null) {
            driverLocationUpdateListener.bestLocationFound(getBestLocationFound());
        } else {
            Log.w(LOG_LABEL, "Best location estimate found, but there's nobody listening!");
            stopSelf();
        }
    }

    /**
     * Check if time to wait for improved locations has elapsed.
     *
     * @return true if had started waiting for more updates, and time for that has elapsed
     */
    private boolean doneWaitingForUpdates() {
        return acceptableLocation != null &&
                System.currentTimeMillis() > startedWaitingForBetterLocation + PREFERRED_LOCATION_TIMEOUT_MS;
    }

    @Override
    public void onGpsStatusChanged(int event) {

        // check if time for improved location updates has elapsed while waiting
        // (might get here if updates stopped coming through)
        if (doneWaitingForUpdates()) {
            // finish up
            finishWithBestLocation();
        }

        switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                Log.d(LOG_LABEL, "Got GPS fix");
                // notify listener so UI can update status
                if (driverLocationUpdateListener != null) {
                    driverLocationUpdateListener.gotGpsFix();
                }
                break;
            case GpsStatus.GPS_EVENT_STARTED:
                // time to first fix is going to take >30s from now
                // for a device without an Internet connection
                Log.d(LOG_LABEL, "GPS started");
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                Log.w(LOG_LABEL, "GPS stopped!");
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                // gets reported periodically; could be used to check for satellite count/status
                break;
            default:
                Log.w(LOG_LABEL, "Unrecognized GPS status event: " + event);
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // service will stop automatically if app stops
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Android Studio insists we check for permissions again here
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
            locationManager.removeGpsStatusListener(this);
        }

        locationListener = null;
        driverLocationUpdateListener = null;
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(LOG_LABEL, "Location changed: " + location.toString());

                if (doneWaitingForUpdates()) {
                    finishWithBestLocation();
                    return;
                }

                // ignore location updates without accuracy info
                if (!location.hasAccuracy()) {
                    return;
                }

                float accuracy = location.getAccuracy();

                // ignore location updates with worse accuracy than the minimum allowed
                if (accuracy > MINIMUM_LOCATION_ACCURACY) {
                    return;
                }

                if (acceptableLocation == null) {
                    // got first good update; continue to wait for a better location
                    acceptableLocation = location;
                    if (driverLocationUpdateListener != null) {
                        driverLocationUpdateListener.foundFirstLocation();
                    }
                    startedWaitingForBetterLocation = System.currentTimeMillis();
                } else if (accuracy < PREFERRED_LOCATION_ACCURACY) {
                    // found a location with good accuracy; keep it
                    preferredAccuracyLocations.add(location);
                } else if (accuracy < acceptableLocation.getAccuracy()) {
                    // got an update with a non-preferred accuracy, but better than previous
                    acceptableLocation = location;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // TODO: what info comes through here?
            }

            @Override
            public void onProviderEnabled(String provider) {
                if (provider.equals(LocationManager.GPS_PROVIDER)) {
                    Log.w(LOG_LABEL, "GPS enabled while location service already running. How did we get here?");
                }
            }

            @Override
            public void onProviderDisabled(String provider) {
                // GPS got disabled after location updates already started; prompt to re-enable
                if (provider.equals(LocationManager.GPS_PROVIDER)) {
                    Log.d(LOG_LABEL, "GPS disabled!");
                    promptToEnableGps();
                    stopSelf();
                }
            }
        };
    }

    public DriverLocationService() {
        this.context = DriverApp.getContext();
        this.preferredAccuracyLocations = new ArrayList<>(50);
        this.acceptableLocation = null;
        this.startedWaitingForBetterLocation = 0;
    }

    public void addDriverLocationUpdateListener(DriverLocationUpdateListener listener) {
        this.driverLocationUpdateListener = listener;
    }

    /**
     * Check if app has permission and access to device location, and that GPS is present and enabled.
     * If so, start receiving location updates.
     *
     * @return True if location updates have been started
     */
    public boolean requestUpdatesOrPermissions(WeakReference<RecordFormConstantsActivity> caller) {
        // check for location service availability and status

        GoogleApiAvailability gapiAvailability = GoogleApiAvailability.getInstance();
        int availability = gapiAvailability.isGooglePlayServicesAvailable(context);

        if (availability != ConnectionResult.SUCCESS) {
            // possibilities for play service access failure are:
            // SERVICE_MISSING, SERVICE_UPDATING, SERVICE_VERSION_UPDATE_REQUIRED, SERVICE_DISABLED, SERVICE_INVALID

            // have to make a new weak reference to upcast type for error dialog
            Activity callingActivity = caller.get();
            if (callingActivity != null) {
                WeakReference<Activity> activityWeakReference = new WeakReference<>(callingActivity);
                // show system dialog to explain; also finish calling activity, since user cannot enter a record
                showApiErrorDialog(activityWeakReference, gapiAvailability, availability);
            }

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
                return false; // up to the activity to start this service again when permissions granted
            }

            ActivityCompat.requestPermissions(callingActivity, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ID);
            return false;
        } else {
            // check if device has GPS
            if (!locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                // let user know they must use a device with GPS for this app to work
                Toast toast = new Toast(context);
                toast.setText(R.string.location_requires_gps);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.show();
                return false;
            }

            // prompt user to turn on GPS, if needed
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                promptToEnableGps();
                return false;
            }
            // have permission and access to GPS location, and GPS is enabled; request updates

            // Android Studio will complain mightily if requestLocationUpdates is not called
            // after a permissions check that is in the same method
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.addGpsStatusListener(this);

            return true;
        }
    }

    /**
     * Open GPS system dialog and show a message explaining that this app needs GPS enabled.
     * This service should be stopped after calling this method. Calling activity should check
     * if this service needs to be (re-)started in its onResume, which would happen after GPS
     * system dialog gets dismissed.
     */
    private void promptToEnableGps() {
        Intent enableGpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(enableGpsIntent);

        Toast toast = new Toast(context);
        toast.setText(R.string.location_gps_needed_rationale);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }

    private void showApiErrorDialog(WeakReference<Activity> caller, GoogleApiAvailability gapiAvailability, int errorCode) {
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
