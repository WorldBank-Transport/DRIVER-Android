package org.worldbank.transport.driver.utilities;

import android.net.Uri;
import android.util.Log;

import org.worldbank.transport.driver.tasks.LoginTask;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Provides URLs for server interactions used at login.
 *
 * Created by kat on 12/19/15.
 */
public class LoginUrlBuilder implements LoginTask.LoginUrls {
    @Override
    public URL userTokenUrl(String serverUrl, boolean isSso) {

        String endpoint = isSso ? SSO_TOKEN_ENDPOINT : TOKEN_ENDPOINT;

        try {
            return new URL(Uri.parse(serverUrl)
                    .buildUpon()
                    .appendEncodedPath(endpoint)
                    .build()
                    .toString());
        } catch (MalformedURLException e) {
            Log.e("LoginUrlBuilder", "Bad login URL! Check if api_server_url set properly in configurables.xml.");
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public URL userInfoUrl(String serverUrl, int userId) {
        try {
            return new URL(Uri.parse(serverUrl)
                    .buildUpon()
                    .appendEncodedPath(USER_ENDPOINT)
                    .appendPath(String.valueOf(userId))
                    .build()
                    .toString());
        } catch (MalformedURLException e) {
            Log.e("LoginTask", "Bad user info URL! Check if api_server_url set properly in configurables.xml.");
            e.printStackTrace();
        }
        return null;
    }
}
