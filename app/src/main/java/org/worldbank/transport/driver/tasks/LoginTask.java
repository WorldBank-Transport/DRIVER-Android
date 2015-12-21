package org.worldbank.transport.driver.tasks;

/**
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

    public interface LoginCallbackListener {
        void loginCompleted(DriverUserInfo userInfo);
        void loginCancelled();
        void loginError(String errorMessage);
    }

    public interface LoginUrls {
        // Backend endpoints. Note that it is necessary to keep the trailing slash here.
        // Publicly accessible for testing convenience.
        String TOKEN_ENDPOINT = "api-token-auth/";
        String USER_ENDPOINT = "api/users/";

        URL userTokenUrl(String serverUrl);
        URL userInfoUrl(String serverUrl, int userId);
    }

    private String serverUrl;
    private final Context context = DriverAppContext.getContext();

    private final String mUsername;
    private final String mPassword;
    private final LoginCallbackListener mListener;
    private final LoginUrls mLoginUrls;

    public LoginTask(String username, String password, LoginCallbackListener listener, LoginUrls loginUrls) {
        mUsername = username;
        mPassword = password;
        mListener = listener;
        mLoginUrls = loginUrls;

        serverUrl = context.getString(R.string.api_server_url);
    }

    @Override
    protected DriverUserInfo doInBackground(String... params) {
        if(!DriverApp.getIsNetworkAvailable()) {
            // no network available. don't bother logging in
            publishProgress(context.getString(R.string.error_no_network));
            Log.d("LoginTask", "No network");
            return null;
        }

        HttpURLConnection urlConnection = null;

        // will contain fetched credentials if login successful
        DriverUserInfo userInfo = null;

        try {
            URL tokenUrl = mLoginUrls.userTokenUrl(serverUrl);
            Log.d("LoginTask", "Going to attempt login with token endpoint: " + tokenUrl);

            urlConnection = (HttpURLConnection) tokenUrl.openConnection();

            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);
            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

            JSONObject authJson = new JSONObject();
            authJson.put("username", mUsername);
            authJson.put("password", mPassword);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.write(authJson.toString());
            writer.flush();
            writer.close();
            out.close();

            // check response
            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                Log.e("LoginTask", "Failed to login. Got response: " +
                        urlConnection.getResponseCode() + ": " + urlConnection.getResponseMessage());
                if (responseCode == 400) {
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

            Log.d("LoginTask", "Token request response:");
            Log.d("LoginTask", responseStr);

            Gson gson = new GsonBuilder().create();
            DriverUserAuth auth = gson.fromJson(responseStr, DriverUserAuth.class);

            if (auth.token != null && auth.token.length() > 0) {
                // get user info, reusing some objects for the connection
                urlConnection.disconnect();

                URL userInfoUrl = mLoginUrls.userInfoUrl(serverUrl, auth.user);
                Log.d("LoginTask", "Going to attempt fetching user info from endpoint: " + userInfoUrl);

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

                Log.d("LoginTask", "User info request response:");
                Log.d("LoginTask", responseStr);

                userInfo = gson.fromJson(responseStr, DriverUserInfo.class);

                if (!userInfo.hasWritePermission()) {
                    Log.d("LoginTask", "User does not have privileges to add records!");
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
            Log.e("LoginTask", "Network error logging in");
            // TODO: This is the error if there is no Internet connection; handle this specially
            e.printStackTrace();
            publishProgress(context.getString(R.string.error_login_network));
            userInfo = null;
        } catch (JSONException e) {
            Log.e("LoginTask", "Error parsing JSON for login request");
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
        mListener.loginCompleted(userInfo);
    }

    @Override
    protected void onCancelled() {
        mListener.loginCancelled();
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
        mListener.loginError(errors[0]);
    }

}