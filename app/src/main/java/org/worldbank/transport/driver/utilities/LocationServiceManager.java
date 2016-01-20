package org.worldbank.transport.driver.utilities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import org.worldbank.transport.driver.activities.RecordFormConstantsActivity;
import org.worldbank.transport.driver.services.DriverLocationService;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverConstantFields;

import java.lang.ref.WeakReference;


/**
 * Handles starting, stopping, and checking the status of the DriverLocationService.
 *
 * Created by kathrynkillebrew on 1/20/16.
 */
public class LocationServiceManager implements DriverLocationService.DriverLocationUpdateListener {

    private static final String LOG_LABEL = "LocationSvcMgr";

    private static LocationServiceManager locationServiceManager = new LocationServiceManager();

    private WeakReference<RecordFormConstantsActivity> caller;
    private DriverApp app;
    private DriverLocationService driverLocationService;
    private boolean isBound;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            driverLocationService = ((DriverLocationService.LocationServiceBinder)service).getService();
            driverLocationService.addDriverLocationUpdateListener(LocationServiceManager.this);

            // tell service to start location updates; if it cannot do so, stop service
            boolean didStart = driverLocationService.requestUpdatesOrPermissions(caller);
            if (!didStart) {
                Log.w(LOG_LABEL, "Failed to start location updates!");
                stopService();
            }
            Log.d(LOG_LABEL, "Location service connection established.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            driverLocationService = null;
            Log.d(LOG_LABEL, "Location service disconnected.");
        }
    };

    private LocationServiceManager() {} // singleton; do not instantiate directly

    public static LocationServiceManager getInstance() {
        return locationServiceManager;
    }

    /**
     * Start location service in app context (not activity context), so it may continue to run
     * as long as app is running. Should check first if it's already running with {@link #isRunning()}.
     *
     * @param caller Activity starting this service; will get closed if location updates aren't possible
     */
    public void startService(RecordFormConstantsActivity caller) {
        Log.d(LOG_LABEL, "Starting location service");

        if (isBound) {
            Log.e(LOG_LABEL, "Attempting to start already-running location service!");
            return;
        }

        Context appContext = DriverApp.getContext();
        appContext.bindService(new Intent(appContext, DriverLocationService.class), serviceConnection, Context.BIND_NOT_FOREGROUND);
        isBound = true;

        app = (DriverApp)caller.getApplication();
        this.caller = new WeakReference<>(caller);
    }

    /**
     * To be called when user exits record form, in case that happens before the service
     * finishes attempting to find the best location estimate. Will set location of currently
     * editing record to the best found so far before stopping service.
     */
    public void stopService() {
        if (isBound && driverLocationService != null) {
            setLocation(driverLocationService.getBestLocationFound());
        } else {
            Log.w(LOG_LABEL, "Location service not available to get result");
        }
        unbindService();
    }

    /**
     * Check if location service is running
     *
     * @return true if currently bound to a location service
     */
    public static boolean isRunning() {
        return getInstance().isBound;
    }

    /**
     * Set the location on the currently editing record.
     *
     * @param estimatedLocation location to set (may be average of multiple readings)
     */
    private void setLocation(Location estimatedLocation) {
        DriverConstantFields constants = app.getEditConstants();
        if (constants != null) {
            constants.location = estimatedLocation;
        } else {
            // might happen if user left the record form; should have warned them before they
            // exited a record without a location set yet
            Log.w(LOG_LABEL, "No constants found to update!");
        }
    }

    /**
     * Location service has done its best to estimate the current location, and has called
     * this listener to let it know to finish up.
     *
     * @param estimatedLocation location found
     */
    @Override
    public void bestLocationFound(Location estimatedLocation) {
        setLocation(estimatedLocation);
        unbindService();
        // update UI
        RecordFormConstantsActivity activity = caller.get();
        if (activity != null) {
            activity.onBestLocationFound();
        }
    }

    @Override
    public void gotGpsFix() {
        // update UI
        RecordFormConstantsActivity activity = caller.get();
        if (activity != null) {
            activity.onGotGpxFix();
        }
    }

    @Override
    public void foundFirstLocation() {
        // update UI
        RecordFormConstantsActivity activity = caller.get();
        if (activity != null) {
            activity.onFoundFirstLocation();
        }
    }

    private void unbindService() {
        Log.d(LOG_LABEL, "Stopping location service");
        Context appContext = DriverApp.getContext();
        appContext.unbindService(serviceConnection);
        isBound = false;
    }
}
