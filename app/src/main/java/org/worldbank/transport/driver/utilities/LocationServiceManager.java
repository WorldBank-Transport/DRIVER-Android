package org.worldbank.transport.driver.utilities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import org.worldbank.transport.driver.services.DriverLocationService;
import org.worldbank.transport.driver.staticmodels.DriverApp;

import java.lang.ref.WeakReference;


/**
 * Handles starting, stopping, and checking the status of the DriverLocationService.
 *
 * Created by kathrynkillebrew on 1/20/16.
 */
public class LocationServiceManager implements DriverLocationService.DriverLocationUpdateListener {

    private static final String LOG_LABEL = "LocationSvcMgr";

    private static LocationServiceManager locationServiceManager = new LocationServiceManager();

    private WeakReference<Activity> caller;
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
    public void startService(Activity caller) {
        Log.d(LOG_LABEL, "Starting location service");

        if (isBound) {
            Log.e(LOG_LABEL, "Attempting to start already-running location service!");
            return;
        }

        Context appContext = DriverApp.getContext();
        appContext.bindService(new Intent(appContext, DriverLocationService.class), serviceConnection, Context.BIND_NOT_FOREGROUND);
        isBound = true;

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
    public boolean isRunning() {
        return isBound;
    }

    private void setLocation(DriverLocationService.EstimatedLocation estimatedLocation) {
        // TODO: update data on currently editing record
    }

    /**
     * Location service has done its best to estimate the current location, and has called
     * this listener to let it know to finish up.
     *
     * @param estimatedLocation location found
     */
    @Override
    public void bestLocationFound(DriverLocationService.EstimatedLocation estimatedLocation) {
        setLocation(estimatedLocation);
        unbindService();
        // TODO: update UI
    }

    @Override
    public void gotGpsFix() {
        // TODO: update UI
    }

    @Override
    public void foundFirstLocation() {
        // TODO: update UI
    }

    private void unbindService() {
        Log.d(LOG_LABEL, "Stopping location service");
        Context appContext = DriverApp.getContext();
        appContext.unbindService(serviceConnection);
        isBound = false;
    }
}
