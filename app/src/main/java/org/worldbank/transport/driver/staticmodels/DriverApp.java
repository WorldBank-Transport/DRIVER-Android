package org.worldbank.transport.driver.staticmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import org.worldbank.transport.driver.R;

/**
 * Singleton to hold data used across the application.
 *
 * Created by kathrynkillebrew on 12/9/15.
 */
public class DriverApp extends Application {

    private DriverUserInfo userInfo;
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
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
}
