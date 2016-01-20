package org.worldbank.transport.driver.utilities;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.worldbank.transport.driver.services.DriverLocationService;

/**
 * Handles starting, stopping, and checking the status of the DriverLocationService.
 *
 * Created by kathrynkillebrew on 1/20/16.
 */
public class LocationServiceManager {
    private DriverLocationService driverLocationService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }
}
