package org.worldbank.transport.driver.UtilityTests;

import android.content.Context;
import android.test.AndroidTestCase;

import org.worldbank.transport.driver.activities.RecordFormConstantsActivity;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.utilities.LocationServiceManager;

/**
 * Created by kathrynkillebrew on 1/25/16.
 */
public class LocationServiceManagerTests extends AndroidTestCase {

    LocationServiceManager manager;
    RecordFormConstantsActivity activity;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        manager = LocationServiceManager.getInstance();
        activity = new RecordFormConstantsActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        LocationServiceManager.stopService();
        activity.finish();
    }

    public void testStatuses() {
        assertEquals("location service should not report running yet",
                 LocationServiceManager.Status.OFF, LocationServiceManager.getCurrentStatus());

        manager.startService(activity);

        assertEquals("location service should have started",
                LocationServiceManager.Status.AWAITING_GPS, LocationServiceManager.getCurrentStatus());

        LocationServiceManager.stopService();

        assertEquals("location service should have stopped", false, LocationServiceManager.isRunning());

        assertEquals("location service status should show it has stopped",
                LocationServiceManager.Status.OFF, LocationServiceManager.getCurrentStatus());

    }
}
