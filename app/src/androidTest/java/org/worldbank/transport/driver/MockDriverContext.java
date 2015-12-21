package org.worldbank.transport.driver;

import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;

/**
 * Created by kathrynkillebrew on 12/17/15.
 */
public class MockDriverContext extends DriverAppContext {

    private DriverApp app;

    @Override
    public DriverApp getDriverApp() {
        return app;
    }

    public MockDriverContext() {
        app = new DriverApp();
    }
    /**
     * @param driver_app Initialize from within the app with (DriverApp) getApplicationContext()
     */
    public MockDriverContext(DriverApp driver_app) {
        super(driver_app);
    }
}
