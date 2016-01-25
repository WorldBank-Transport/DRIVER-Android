package org.worldbank.transport.driver.UtilityTests;

import android.test.AndroidTestCase;

import org.worldbank.transport.driver.activities.RecordFormConstantsActivity;
import org.worldbank.transport.driver.utilities.LocationServiceManager;

/**
 * Created by kathrynkillebrew on 1/25/16.
 */
public class LocationServiceManagerTests extends AndroidTestCase {

    LocationServiceManager manager;
    RecordFormConstantsActivity dummyActivity;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        manager = LocationServiceManager.getInstance();
        dummyActivity = new RecordFormConstantsActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        LocationServiceManager.stopService();
        dummyActivity.finish();
    }

    public void testStatuses() {
        assertEquals("location service should not report running yet",
                 LocationServiceManager.Status.OFF, LocationServiceManager.getCurrentStatus());

        manager.startService(dummyActivity);

        assertEquals("location service should have started",
                LocationServiceManager.Status.AWAITING_GPS, LocationServiceManager.getCurrentStatus());


    }
}
