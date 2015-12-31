package org.worldbank.transport.driver.staticmodels;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.SharedPreferences;
import android.util.Log;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.models.DriverSchema;
import org.worldbank.transport.driver.utilities.DriverUtilities;

/**
 * Singleton to hold data used across the application.
 *
 * Created by kathrynkillebrew on 12/9/15.
 */
public class DriverApp extends Application {

    /**
     * Current user.
     */
    private DriverUserInfo userInfo;

    /**
     * Object currently being edited (if any).
     */
    private DriverSchema editObject;
    private String[] schemaSectionOrder;

    private static Context mContext;
    private static ConnectivityManager connMgr;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        editObject = null;
    }

    public static Context getContext() {
        return mContext;
    }

    /**
     * Sets current user for app and sets user info in shared preferences.
     * Clears shared preferences for app if null user set (can be used on logout.)
     * @param userInfo DriverUserInfo built from API user response in LoginTask
     */
    public void setUserInfo(DriverUserInfo userInfo) {
        this.userInfo = userInfo;

        if (userInfo != null) {
            userInfo.writeToSharedPreferences(mContext);
        } else {
            // clear shared preferences if user info is reset
            SharedPreferences preferences = mContext.getSharedPreferences(
                    mContext.getString(R.string.shared_preferences_file), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear(); // clears last saved user, if there is one
            editor.apply();
        }
    }

    public DriverUserInfo getUserInfo() {
        // try reading in saved user info if app has none set yet
        if (userInfo == null) {
            userInfo = new DriverUserInfo();
            userInfo.readFromSharedPreferences(mContext);
        }
        return userInfo;
    }

    public DriverSchema getEditObject() {
        if (editObject == null) {
            editObject = new DriverSchema();
            Log.d("DriverApp", "Created new object to edit");
        }

        return editObject;
    }

    public void setEditObject(DriverSchema obj) {
        editObject = obj;
        Log.d("DriverApp", "Have set currently editing object");
    }

    /**
     * Get the order in which the form sections should appear
     * @return Array of ordered field names
     */
    public String[] getSchemaSectionOrder() {
        // lazily fetch section ordering on first reference
        if (schemaSectionOrder == null) {
            schemaSectionOrder = DriverUtilities.getFieldOrder(DriverSchema.class);
        }

        return schemaSectionOrder;
    }

    public static boolean getIsNetworkAvailable() {
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }
}
