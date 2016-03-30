package org.worldbank.transport.driver.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.staticmodels.DriverUserInfo;
import org.worldbank.transport.driver.utilities.CheckSchemaUrlBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * Check if there is a new schema available.
 *
 * Created by kathrynkillebrew on 1/26/16.
 */
public class CheckSchemaTask extends AsyncTask<DriverUserInfo, String, String> {

    /* The name of the record type in use by the app; should match default in web app */
    private static final String RECORD_TYPE_LABEL = "Incident";

    private static final String LOG_LABEL = "CheckSchemaTask";

    public interface CheckSchemaCallbackListener {
        void foundSchema(String currentSchema);
        void schemaCheckCancelled();
        void schemaCheckError(String errorMessage);
        void haveInvalidCredentials();
    }

    public interface CurrentSchemaUrl {
        // Backend endpoints. Note that it is necessary to keep the trailing slash here.
        String RECORDTYPE_ENDPOINT = "api/recordtypes/";

        URL currentSchemaUrl(String serverUrl, String recordTypeLabel);
    }

    private String serverUrl;
    private final Context context = DriverAppContext.getContext();
    private final WeakReference<CheckSchemaCallbackListener> listener;
    private final CurrentSchemaUrl currentSchemaUrl;

    public CheckSchemaTask(CheckSchemaCallbackListener listener) {
        this(listener, new CheckSchemaUrlBuilder());
    }

    /**
     * Constructor that may be used directly int testing, with a mocked URL builder
     *
     * @param listener callback listener
     * @param currentSchemaUrl URL builder that returns endpoint to query for current schema
     */
    public CheckSchemaTask(CheckSchemaCallbackListener listener, CurrentSchemaUrl currentSchemaUrl) {
        this.listener = new WeakReference<>(listener);
        serverUrl = context.getString(R.string.api_server_url);
        this.currentSchemaUrl = currentSchemaUrl;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected String doInBackground(DriverUserInfo... params) {
        if(!DriverApp.getIsNetworkAvailable()) {
            // no network available. don't bother logging in
            publishProgress(context.getString(R.string.error_no_network));
            Log.d(LOG_LABEL, "No network");
            cancel(true);
            return null;
        }

        DriverUserInfo userInfo = params[0];

       if (userInfo == null) {
           publishProgress(context.getString(R.string.error_schema_check));
           Log.e(LOG_LABEL, "missing user info!");
           cancel(true);
           return null;
       }

        String token = userInfo.getUserToken();

        // should not happen, but check anyways
        if (token == null || token.isEmpty()) {
            publishProgress(context.getString(R.string.error_schema_check));
            Log.e(LOG_LABEL, "missing user token!");
            cancel(true);
            return null;
        }

        try {
            URL url = currentSchemaUrl.currentSchemaUrl(serverUrl, RECORD_TYPE_LABEL);

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Authorization", "Token " + token);

            // if get a 403 back, tell activity to go login again (shouldn't happen)
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                Log.w(LOG_LABEL, "User token must be invalid.");
                CheckSchemaCallbackListener caller = listener.get();
                if (caller != null) {
                    caller.haveInvalidCredentials();
                } else {
                    Log.w(LOG_LABEL, "Cannot notify of 403 because listener has gone");
                }
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

            // parse the JSON to find the schema UUID
            JSONObject json = new JSONObject(responseStr);
            JSONObject resultJson = json.getJSONArray("results").getJSONObject(0);
            String foundCurrentSchema = resultJson.getString("current_schema");

            // sanity check; make sure that the string found is actually a UUID
            try {
                UUID.fromString(foundCurrentSchema);
            } catch (IllegalArgumentException e) {
                // does not conform to UUID String representation
                publishProgress(context.getString(R.string.error_schema_check));
                cancel(true);
                Log.e(LOG_LABEL, "Schema UUID string found, but it doesn't look like a UUID: " + foundCurrentSchema);
                return null;
            }

            // found a good current schema; return it
            return foundCurrentSchema;

        } catch (IOException e) {
            Log.e(LOG_LABEL, "Error communicating with server to perform schema check");
            publishProgress(context.getString(R.string.error_schema_check));
            e.printStackTrace();
            cancel(true);
        } catch (JSONException e) {
            Log.e(LOG_LABEL, "Error parsing JSON response to schema check");
            publishProgress(context.getString(R.string.error_schema_check));
            e.printStackTrace();
            cancel(true);
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        CheckSchemaCallbackListener caller = listener.get();
        if (caller != null) {
            caller.schemaCheckError(values[0]);
        } else {
            Log.w(LOG_LABEL, "Cannot send back schema check error because listener has gone");
        }
    }

    @Override
    protected void onCancelled(String s) {
        CheckSchemaCallbackListener caller = listener.get();
        if (caller != null) {
            caller.schemaCheckCancelled();
        } else {
            Log.w(LOG_LABEL, "Cannot notify of schema check cancellation because listener has gone");
        }
    }

    @Override
    protected void onPostExecute(String s) {

        // sanity check; shouldn't happen
        if (s == null || s.isEmpty()) {
            Log.e(LOG_LABEL, "Schema check task finished with no schema!");
        }

        CheckSchemaCallbackListener caller = listener.get();
        if (caller != null) {
            caller.foundSchema(s);
        } else {
            Log.w(LOG_LABEL, "Cannot send back current schema because listener has gone");
        }
    }
}
