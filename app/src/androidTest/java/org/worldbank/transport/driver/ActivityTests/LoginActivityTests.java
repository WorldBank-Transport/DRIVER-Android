package org.worldbank.transport.driver.ActivityTests;

import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.activities.LoginActivity;

/**
 * Created by kathrynkillebrew on 12/17/15.
 */
public class LoginActivityTests extends ActivityInstrumentationTestCase2<LoginActivity> {

    private LoginActivity activity;
    Instrumentation.ActivityMonitor receiverActivityMonitor;

    public LoginActivityTests() {
        super(LoginActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // clear any saved user info from shared preferences
        clearSharedPreferences();

        activity = getActivity();
    }

    private void clearSharedPreferences() {
        Context targetContext = getInstrumentation().getTargetContext();
        SharedPreferences preferences = targetContext.getSharedPreferences(
                targetContext.getString(R.string.shared_preferences_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear(); // clears last saved user, if there is one
        editor.commit();
    }

    @UiThreadTest
    public void testIsThisThingOn() {
        assertEquals("Dummy test", 4, 2 + 2);
    }

    public void testActivityExists() {
        assertNotNull("Login Activity is null", activity);
    }

    @UiThreadTest
    public void testEmptyUsername() {
        AutoCompleteTextView usernameField = (AutoCompleteTextView) activity.findViewById(R.id.email);
        EditText passwordField = (EditText) activity.findViewById(R.id.password);
        Button loginButton = (Button) activity.findViewById(R.id.email_sign_in_button);
        View progress = activity.findViewById(R.id.login_progress);

        // (TextView) findViewById(R.id.error_message);

        usernameField.setText("");
        passwordField.setText("SOMEJUNKHERE");
        loginButton.performClick();

        assertNotNull("Username error message did not appear", usernameField.getError());
        assertNull("Password error appeared when field is OK", passwordField.getError());

        assertEquals("Username does not have focus on error", true, usernameField.hasFocus());
        assertEquals("Progress indicator showing when login form has error",
                View.GONE, progress.getVisibility());
    }

    @UiThreadTest
    public void testEmptyPassword() {
        AutoCompleteTextView usernameField = (AutoCompleteTextView) activity.findViewById(R.id.email);
        EditText passwordField = (EditText) activity.findViewById(R.id.password);
        Button loginButton = (Button) activity.findViewById(R.id.email_sign_in_button);
        View progress = activity.findViewById(R.id.login_progress);

        usernameField.setText("SOMEJUNKHERE");
        passwordField.setText("");
        loginButton.performClick();

        assertNotNull("Password error message did not appear", passwordField.getError());
        assertNull("Username error appeared when field is OK", usernameField.getError());
        assertEquals("Password does not have focus on error", true, passwordField.hasFocus());
        assertEquals("Progress indicator showing when login form has error",
                View.GONE, progress.getVisibility());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        getActivity().finish();
    }
}
