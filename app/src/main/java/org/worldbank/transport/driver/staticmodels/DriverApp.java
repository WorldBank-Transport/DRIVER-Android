package org.worldbank.transport.driver.staticmodels;

import android.app.Application;
import android.content.Context;

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

    public void setUserInfo(DriverUserInfo userInfo) {
        this.userInfo = userInfo;
        userInfo.writeToSharedPreferences(mContext);
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
