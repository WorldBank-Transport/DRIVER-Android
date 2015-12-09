package org.worldbank.transport.driver.tasks;

/**
 * Created by kathrynkillebrew on 12/8/15.
 */

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;
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
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 *
 * TODO: Use AccountAuthenticator instead:
 * http://developer.android.com/reference/android/accounts/AbstractAccountAuthenticator.html
 */
public class LoginTask extends AsyncTask<Void, Void, DriverUserInfo> {

    public interface LoginCallbackListener {
        void loginCompleted(DriverUserInfo userInfo);
        void loginCancelled();
    }

    // TODO: put URLs in templated config file
    private static final String PASSWORD_LOGIN_URL = "http://prs.azavea.com/api-token-auth/";

    private final String mUsername;
    private final String mPassword;
    private final LoginCallbackListener mListener;

    public LoginTask(String username, String password, LoginCallbackListener listener) {
        mUsername = username;
        mPassword = password;
        mListener = listener;
    }

    @Override
    protected DriverUserInfo doInBackground(Void... params) {
        HttpURLConnection urlConnection = null;

        // will contain fetched credentials if login successful
        DriverUserAuth auth;
        DriverUserInfo userInfo = null;

        try {
            URL url = new URL(PASSWORD_LOGIN_URL);

            urlConnection = (HttpURLConnection) url.openConnection();

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

            // will get a 400 response if username/password invalid
            if (urlConnection.getResponseCode() != 200) {
                Log.e("LoginTask", "Failed to login. Got response: " +
                        urlConnection.getResponseCode() + ": " + urlConnection.getResponseMessage());
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

            Log.d("LoginTask", "Got server response: " + responseStr);

            Gson gson = new GsonBuilder().create();
            auth = gson.fromJson(responseStr, DriverUserAuth.class);

            Log.d("LoginTask", "Parsed user ID: " + String.valueOf(auth.user));
            Log.d("LoginTask", "Parsed user token: " + auth.token);

            if (auth.token != null && auth.token.length() > 0) {
                // great, try fetching something now
                urlConnection.disconnect();

                // TODO: proper URL building
                URL userInfoUrl = new URL("http://prs.azavea.com/api/users/" + String.valueOf(auth.user));

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

                Log.d("LoginTask", "Got user info response: " + responseStr);

                userInfo = gson.fromJson(responseStr, DriverUserInfo.class);
                Log.d("LoginTask", "Found user groups: " + userInfo.groups.toString());

                // TODO: store user auth and info somewhere

                if (!userInfo.hasWritePermission()) {
                    Log.d("LoginTask", "User does not have privileges to add records!");
                    // TODO: return appropriate message instead of logging in user
                } else {
                    // set user token on user info object, for convenient storage and retrieval
                    if (!userInfo.setUserToken(auth)) {
                        // nullify user info if it didn't work
                        userInfo = null;
                    }
                }
            }

        } catch (MalformedURLException e) {
            Log.e("LoginTask", "Bad login URL");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("LoginTask", "Network error logging in");
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e("LoginTask", "Error parsing JSON for login request");
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return userInfo;
        //return auth != null && auth.token != null && auth.token.length() > 0;
    }

    @Override
    protected void onPostExecute(final DriverUserInfo userInfo) {
        mListener.loginCompleted(userInfo);
    }

    @Override
    protected void onCancelled() {
        mListener.loginCancelled();
    }
}
