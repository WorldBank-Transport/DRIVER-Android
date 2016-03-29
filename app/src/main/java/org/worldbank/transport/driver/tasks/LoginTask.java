package org.worldbank.transport.driver.tasks;

/**
 * Handle user login in background. Gets user information and stores it to shared preferences.
 *
 * Created by kathrynkillebrew on 12/8/15.
 */

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.staticmodels.DriverUserAuth;
import org.worldbank.transport.driver.staticmodels.DriverUserInfo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Represents an asynchronous login/registration task used to authenticate the user.
 * Only one login task should be executed at a time, as the URI builder used here is not threadsafe.
 *
 * TODO: Use system-wide Account Manager instead by subclassing AbstractAccountAuthenticator:
 * http://developer.android.com/reference/android/accounts/AbstractAccountAuthenticator.html
 */
public class LoginTask extends AsyncTask<String, String, DriverUserInfo> {
    
    private static final String LOG_LABEL = "LoginTask";

    public interface LoginCallbackListener {
        void loginCompleted(DriverUserInfo userInfo);
        void loginCancelled();
        void loginError(String errorMessage);
    }

    public interface LoginUrls {
        // Backend endpoints. Note that it is necessary to keep the trailing slash here.
        // Publicly accessible for testing convenience.
        String TOKEN_ENDPOINT = "api-token-auth/";
        String SSO_TOKEN_ENDPOINT = "api/sso-token-auth/";
        String USER_ENDPOINT = "api/users/";

        URL userTokenUrl(String serverUrl, boolean isSso);
        URL userInfoUrl(String serverUrl, int userId);
    }

    private String serverUrl;
    private final Context context = DriverAppContext.getContext();

    private String mUsername;
    private String mPassword;
    private String mSsoToken;
    private final WeakReference<LoginCallbackListener> mListener;
    private final LoginUrls mLoginUrls;

    /**
     * Create login task for username/password login.
     *
     * @param username Username for login, entered in login form.
     * @param password Password for user, entered in login form.
     * @param listener Listening activity that will receive callbacks.
     * @param loginUrls Endpoints used to log in and get user info.
     */
    public LoginTask(String username, String password, LoginCallbackListener listener, LoginUrls loginUrls) {
        this(listener, loginUrls);
        mUsername = username;
        mPassword = password;
        mSsoToken = null;
    }

    /**
     * Create login task for SSO login.
     *
     * @param ssoToken ID token returned by Google sign-in authentication request
     * @param listener Listening activity that will receive callbacks.
     * @param loginUrls Endpoints used to log in and get user info.
     */
    public LoginTask(String ssoToken, LoginCallbackListener listener, LoginUrls loginUrls) {
        this(listener, loginUrls);
        mSsoToken = ssoToken;
        mUsername = null;
        mPassword = null;
    }

    public LoginTask(LoginCallbackListener listener, LoginUrls loginUrls) {
        mListener = new WeakReference<>(listener);
        mLoginUrls = loginUrls;
        serverUrl = context.getString(R.string.api_server_url);
    }

    @Override
    protected DriverUserInfo doInBackground(String... params) {
        if(!DriverApp.getIsNetworkAvailable()) {
            // no network available. don't bother logging in
            publishProgress(context.getString(R.string.error_no_network));
            Log.d(LOG_LABEL, "No network");
            cancel(true);
            return null;
        }

        HttpURLConnection urlConnection = null;

        // will contain fetched credentials if login successful
        DriverUserInfo userInfo = null;

        JSONObject authJson = new JSONObject();

        try {
            if (mUsername != null && mPassword != null) {
                // username/password login
                URL tokenUrl = mLoginUrls.userTokenUrl(serverUrl, false);
                Log.d(LOG_LABEL, "Going to attempt username/password login with token endpoint: " + tokenUrl);
                urlConnection = (HttpURLConnection) tokenUrl.openConnection();
                authJson.put("username", mUsername);
                authJson.put("password", mPassword);
            } else {
                // SSO login
                URL tokenUrl = mLoginUrls.userTokenUrl(serverUrl, true);
                Log.d(LOG_LABEL, "Going to attempt SSO login with token endpoint: " + tokenUrl);
                urlConnection = (HttpURLConnection) tokenUrl.openConnection();
                authJson.put("token", mSsoToken);
            }

            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);
            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));

            Log.d(LOG_LABEL, "Sending authJson: " + authJson.toString());

            writer.write(authJson.toString());
            writer.flush();
            writer.close();
            out.close();

            // check response
            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                Log.e(LOG_LABEL, "Failed to login. Got response: " +
                        urlConnection.getResponseCode() + ": " + urlConnection.getResponseMessage());
                if (responseCode == 400 && mUsername != null) {
                    // will get a 400 response if username/password invalid
                    publishProgress(context.getString(R.string.error_incorrect_username_or_password));
                } else {
                    // send general "server error" message
                    publishProgress(context.getString(R.string.error_server_login));
                }

                // bail now
                urlConnection.disconnect();
                cancel(true);
                return null;
            }

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader ir = new BufferedReader(new InputStreamReader(in));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = ir.readLine()) != null) {
                stringBuilder.append(line);
            }
            ir.close();
            in.close();
            String responseStr = stringBuilder.toString();

            Log.d(LOG_LABEL, "Token request response:");
            Log.d(LOG_LABEL, responseStr);

            Gson gson = new GsonBuilder().create();
            DriverUserAuth auth = gson.fromJson(responseStr, DriverUserAuth.class);

            if (auth.token != null && auth.token.length() > 0) {
                // get user info, reusing some objects for the connection
                urlConnection.disconnect();

                URL userInfoUrl = mLoginUrls.userInfoUrl(serverUrl, auth.user);
                Log.d(LOG_LABEL, "Going to attempt fetching user info from endpoint: " + userInfoUrl);

                urlConnection = (HttpURLConnection) userInfoUrl.openConnection();
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                urlConnection.setDoOutput(false);
                urlConnection.setDoInput(true);
                urlConnection.setRequestProperty("Authorization", "Token " + auth.token);

                in = new BufferedInputStream(urlConnection.getInputStream());
                ir = new BufferedReader(new InputStreamReader(in));
                stringBuilder = new StringBuilder();
                while ((line = ir.readLine()) != null) {
                    stringBuilder.append(line);
                }
                ir.close();
                in.close();
                responseStr = stringBuilder.toString();

                Log.d(LOG_LABEL, "User info request response:");
                Log.d(LOG_LABEL, responseStr);

                userInfo = gson.fromJson(responseStr, DriverUserInfo.class);

                if (!userInfo.hasWritePermission()) {
                    Log.d(LOG_LABEL, "User does not have privileges to add records!");
                    publishProgress(context.getString(R.string.error_user_cannot_write_records));
                    userInfo = null;
                } else {
                    // set user token on user info object, for convenient storage and retrieval
                    if (!userInfo.setUserToken(auth)) {
                        // Setting user token failed (shouldn't happen)
                        userInfo = null;
                        publishProgress(context.getString(R.string.error_login_unknown));
                    }
                }
            } else {
                // parsed auth response without error, but missing data (shouldn't happen)
                publishProgress(context.getString(R.string.error_login_unknown));
            }
        } catch (IOException e) {
            Log.e(LOG_LABEL, "Network error logging in");
            e.printStackTrace();
            publishProgress(context.getString(R.string.error_login_network));
            userInfo = null;
        } catch (JSONException e) {
            Log.e(LOG_LABEL, "Error parsing JSON for login request");
            e.printStackTrace();
            publishProgress(context.getString(R.string.error_login_unknown));
            userInfo = null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        // use null userInfo object as a flag to cancel, so cleanup can happen after try/catch
        if (userInfo == null) {
            cancel(true);
        }

        return userInfo;
    }

    @Override
    protected void onPostExecute(final DriverUserInfo userInfo) {
        LoginCallbackListener caller = mListener.get();
        if (caller != null) {
            caller.loginCompleted(userInfo);
        } else {
            Log.w(LOG_LABEL, "Cannot send back user info because listener has gone");
        }
    }

    @Override
    protected void onCancelled() {
        LoginCallbackListener caller = mListener.get();
        if (caller != null) {
            caller.loginCancelled();
        } else {
            Log.w(LOG_LABEL, "Cannot notify of login cancellation because listener has gone");
        }
    }

    /**
     * When an error response is received, invoke error handler callback with error message,
     * and cancel action.
     * (There are no true progress updates.  This is being done to return the error.)
     *
     * @param errors Contains error object with descriptive message
     */
    @Override
    protected void onProgressUpdate(String... errors) {
        LoginCallbackListener caller = mListener.get();
        if (caller != null) {
            caller.loginError(errors[0]);
        } else {
            Log.w(LOG_LABEL, "Cannot send back user info because listener has gone");
        }
    }

}