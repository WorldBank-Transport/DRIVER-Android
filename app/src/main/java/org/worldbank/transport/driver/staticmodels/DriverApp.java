package org.worldbank.transport.driver.staticmodels;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Singleton to hold data used across the application.
 *
 * Created by kathrynkillebrew on 12/9/15.
 */
public class DriverApp extends Application {

    private DriverUserInfo userInfo;
    private static Context mContext;
    private static ConnectivityManager connMgr;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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

    public static boolean getIsNetworkAvailable() {
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }
}
