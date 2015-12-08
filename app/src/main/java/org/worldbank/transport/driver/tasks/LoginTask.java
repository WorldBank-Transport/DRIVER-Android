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
 */
public class LoginTask extends AsyncTask<Void, Void, Boolean> {

    public interface LoginCallbackListener {
        void loginCompleted(boolean successful);
        void loginCancelled();
    }

    // TODO: put in templated config file
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
    protected Boolean doInBackground(Void... params) {
        // TODO: attempt authentication against a network service.

        // http://developer.android.com/reference/java/net/HttpURLConnection.html
        HttpURLConnection urlConnection = null;

        // will contain fetched credentials if login successful
        DriverUserAuth auth = null;

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
                return false;
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

        if (auth != null && auth.token.length() > 0) {
            return true;
        }

        return false;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        mListener.loginCompleted(success);
    }

    @Override
    protected void onCancelled() {
        mListener.loginCancelled();
    }
}
