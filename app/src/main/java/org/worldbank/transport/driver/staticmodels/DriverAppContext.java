package org.worldbank.transport.driver.staticmodels;

import android.content.Context;

/**
 * Abstract fetching the application, so context may be mocked out for testing.
 *
 * Created by kathrynkillebrew on 12/9/15.
 */
public class DriverAppContext {

    private DriverApp app;

    /**
     * Empty default constructor to override in test
     */
    public DriverAppContext() {
    }

    /**
     *
     * @param driver_app Initialize from within the app with (DriverApp) getApplicationContext()
     */
    public DriverAppContext(DriverApp driver_app) {
        app = driver_app;
    }

    public DriverApp getDriverApp() {
        return app;
    }

    public void setDriverApp(DriverApp driver_app) {
        app = driver_app;
    }

    /**
     * For testing, override this method to return a mock context
     * @return this application's context
     */
    public static Context getContext() {
        return DriverApp.getContext();
    }

}
