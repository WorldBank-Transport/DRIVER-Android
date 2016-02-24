package org.worldbank.transport.driver.services;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
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
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.activities.RecordFormConstantsActivity;
import org.worldbank.transport.driver.staticmodels.DriverApp;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class DriverLocationService extends Service implements GpsStatus.Listener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_LABEL = "LocationService";

    // identifier for device location access request, if runtime prompt necessary
    // request code must be in lower 8 bits
    public static final int PERMISSION_REQUEST_ID = 11;
    public static final int API_AVAILABILITY_REQUEST_ID = 22;

    /**
     * Location accuracy is a radius in meters of 68% confidence, so some updates may come through
     * with seemingly high accuracy that actually fall far outside the stated radius.
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
    }

    public class LocationServiceBinder extends Binder {
        public DriverLocationService getService() {
            return DriverLocationService.this;
        }
    }

    private GoogleApiClient apiClient;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Context context;
    private final IBinder binder = new LocationServiceBinder();
    private DriverLocationUpdateListener driverLocationUpdateListener;

    // will be non-null when at least one location update received with the minimum accuracy
    private Location acceptableLocation;
    private long startedWaitingForBetterLocation;
    private ArrayList<Location> preferredAccuracyLocations;
    private boolean doneWaitingForLocations;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Android Studio will complain mightily if requestLocationUpdates is not called
        // after a permissions check that is in the same method
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_LABEL, "Actually starting location updates now.");

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(50);
            locationRequest.setMaxWaitTime(25);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, locationListener);
        } else {
            Log.w(LOG_LABEL, "Got onConnected callback, but do not have location services permissions now");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(LOG_LABEL, "Location services API connection suspended!");
        // TODO: anything?
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(LOG_LABEL, "Location services API connection failed!");
        // TODO: anything?
    }

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

            Log.d(LOG_LABEL, "Finding weighted average of " + preferredAccuracyLocations.size() + " location readings");

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
        doneWaitingForLocations = true;
        if (driverLocationUpdateListener != null) {
            driverLocationUpdateListener.bestLocationFound(getBestLocationFound());
        } else {
            Log.w(LOG_LABEL, "Best location estimate found, but there's nobody listening!");
            // best to stop service by unbinding, but if listener somehow disappears fist, clean up
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

        // ignore any events that come through during finish-up activities
        if (doneWaitingForLocations) {
            return;
        }

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
            if (apiClient != null && apiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, locationListener);
            }
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
                Log.d(LOG_LABEL, "Location changed: (" + location.getLatitude() + "," +
                        location.getLongitude() + ") accuracy: " + location.getAccuracy());

                // ignore any events that come through during finish-up activities
                if (doneWaitingForLocations) {
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
                    startedWaitingForBetterLocation = System.currentTimeMillis();
                } else {
                    if (accuracy < PREFERRED_LOCATION_ACCURACY) {
                        // found a location with good accuracy; track it for weighted average
                        preferredAccuracyLocations.add(location);
                    }

                    if (accuracy <= acceptableLocation.getAccuracy()) {
                        // got single reading better or more recent than last best; use it instead
                        acceptableLocation = location;
                    }
                }

                if (doneWaitingForUpdates()) {
                    finishWithBestLocation();
                }
            }
        };
    }

    public DriverLocationService() {
        this.context = DriverApp.getContext();
        this.preferredAccuracyLocations = new ArrayList<>(50);
        this.acceptableLocation = null;
        this.startedWaitingForBetterLocation = 0;
        this.doneWaitingForLocations = false;
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

            // in case user has denied location permissions to app previously, tell them why it's needed, then prompt again
            if (ActivityCompat.shouldShowRequestPermissionRationale(callingActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                displayPermissionRequestRationale(context);
                // On subsequent prompts, user will get a "never ask again" option in the dialog.
                // If that option gets checked, attempting to create a record will simply close
                // the record form back out again and display a toast message with the reason.
            }

            ActivityCompat.requestPermissions(callingActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ID);
            return false; // up to the activity to start this service again when permissions granted
        } else {
            // check if device has GPS
            if (!locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                // let user know they must use a device with GPS for this app to work
                Toast toast = Toast.makeText(context, context.getString(R.string.location_requires_gps), Toast.LENGTH_LONG);
                toast.show();
                return false;
            }

            // prompt user to turn on GPS, if needed
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                promptToEnableGps(caller);
                return false;
            }
            // have permission and access to GPS location, and GPS is enabled; request updates

            apiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            apiClient.connect();
            Log.d(LOG_LABEL, "Connecting to GoogleApiClient for GPS...");

            // cannot actually start requesting location updates until API client sends
            // onConnect callback
            // http://developer.android.com/training/location/receive-location-updates.html

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
    private void promptToEnableGps(WeakReference<RecordFormConstantsActivity> caller) {
        Toast toast = Toast.makeText(context, context.getString(R.string.location_gps_needed_rationale), Toast.LENGTH_LONG);
        toast.show();

        Intent enableGpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

        if (caller != null) {
            RecordFormConstantsActivity callingActivity = caller.get();
            if (callingActivity != null) {
                callingActivity.startActivity(enableGpsIntent);
            } else {
                // activity went away; open prompt in new context
                enableGpsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(enableGpsIntent);
            }
        } else {
            // GPS got disabled after location updates already started
            enableGpsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableGpsIntent);
        }
    }

    private void showApiErrorDialog(WeakReference<Activity> caller, GoogleApiAvailability gapiAvailability, int errorCode) {
        final Activity callingActivity = caller.get();
        if (callingActivity == null) {
            return;
        }

        Dialog errorDialog = gapiAvailability.getErrorDialog(callingActivity, errorCode, API_AVAILABILITY_REQUEST_ID);
        errorDialog.show();
        errorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // wait until user has finished reading dialog to close out the form
                // (otherwise dialog will get dismissed immediately)
                callingActivity.finish();
            }
        });
    }

    public static void displayPermissionRequestRationale(Context context) {
        Toast toast = Toast.makeText(context, context.getString(R.string.location_fine_permission_rationale), Toast.LENGTH_LONG);
        toast.show();
    }
}
