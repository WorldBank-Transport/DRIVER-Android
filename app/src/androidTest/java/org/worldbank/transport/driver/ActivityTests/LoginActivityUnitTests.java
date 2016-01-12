package org.worldbank.transport.driver.ActivityTests;

import android.test.ActivityUnitTestCase;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import org.worldbank.transport.driver.MockDriverContext;
import org.worldbank.transport.driver.activities.LoginActivity;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverUserAuth;
import org.worldbank.transport.driver.staticmodels.DriverUserInfo;

import java.util.ArrayList;

/**
 * Unit tests for login activity methods.
 * For testing interactivity of components, use functional tests instead.
 *
 * Created by kathrynkillebrew on 12/18/15.
 */
public class LoginActivityUnitTests extends ActivityUnitTestCase<LoginActivity> {

    public LoginActivityUnitTests() {
        super(LoginActivity.class);
    }

    public void testSavedUserFoundAtLaunch() {

        MockDriverContext driverContext = new MockDriverContext();
        DriverApp app = driverContext.getDriverApp();
        setApplication(app);

        // this fails due to activity being in sub-package
        //Intent intent = new Intent(getInstrumentation().getTargetContext(), LoginActivity.class);
        //startActivity(intent, null, null);

        // launch activity this way as workaround for ComponentName bug in ActivityUnitCase:
        // https://code.google.com/p/android/issues/detail?id=22737&q=activityunittestcase&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars
        setActivity(launchActivity("org.worldbank.transport.driver", LoginActivity.class, null));
        LoginActivity activity = getActivity();

        assertNotNull("Failed to launch login activity", activity);

        // set a user
        DriverUserAuth userAuth = new DriverUserAuth();
        userAuth.token = "111111111111111111111111111";
        userAuth.user = 111;
        DriverUserInfo userInfo = new DriverUserInfo();
        userInfo.username = "foo";
        userInfo.id = 111;
        userInfo.email = "foo@example.com";
        userInfo.groups = new ArrayList<>();
        userInfo.groups.add("analyst");
        userInfo.setUserToken(userAuth);

        DriverApp usingApp = (DriverApp)activity.getApplication();
        usingApp.setUserInfo(userInfo);

        assertTrue("Saved user info not found on app launch", activity.haveSavedUserInfo());

        activity.finish();
    }

    public void testNoUserFoundAtLaunch() {

        MockDriverContext driverContext = new MockDriverContext();
        DriverApp app = driverContext.getDriverApp();
        setApplication(app);

        LoginActivity activity = launchActivity("org.worldbank.transport.driver", LoginActivity.class, null);
        setActivity(activity);

        assertNotNull("Failed to launch login activity", activity);

        DriverApp usingApp = (DriverApp)activity.getApplication();
        usingApp.setUserInfo(null);

        assertFalse("User info found on app launch when none expected", activity.haveSavedUserInfo());

        activity.finish();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
}
