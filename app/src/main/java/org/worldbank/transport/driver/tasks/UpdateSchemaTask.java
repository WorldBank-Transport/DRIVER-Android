package org.worldbank.transport.driver.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


import org.apache.commons.io.IOUtils;
import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.staticmodels.DriverUserInfo;
import org.worldbank.transport.driver.utilities.UpdateSchemaUrlBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * Background task for downloading a jar file with updated models for a schema and loading it.
 *
 * Created by kathrynkillebrew on 2/15/16.
 */
public class UpdateSchemaTask extends AsyncTask<String, String, String> {

    private static final String LOG_LABEL = "UpdateSchema";

    public interface UpdateSchemaCallbackListener {
        void schemaUpdated();
        void schemaUpdateCancelled();
        void schemaUpdateError(String errorMessage);
        void haveInvalidCredentials();
    }

    public interface RecordSchemaUrl {
        // Backend endpoints. Note that it is necessary to keep the trailing slash here.
        String RECORDSCHEMA_ENDPOINT = "api/jars/";

        URL schemaUrl(String serverUrl, String recordSchemaUuid);
    }

    private String serverUrl;
    private final Context context = DriverAppContext.getContext();
    private final WeakReference<UpdateSchemaCallbackListener> listener;
    private final RecordSchemaUrl schemaUrl;
    private final DriverUserInfo userInfo;

    public UpdateSchemaTask(UpdateSchemaCallbackListener listener, DriverUserInfo userInfo) {
        this(listener, new UpdateSchemaUrlBuilder(), userInfo);
    }

    /**
     * Constructor that may be used directly int testing, with a mocked URL builder
     *
     * @param listener callback listener
     * @param schemaUrl URL builder that returns endpoint to query for a schema
     */
    public UpdateSchemaTask(UpdateSchemaCallbackListener listener, RecordSchemaUrl schemaUrl, DriverUserInfo userInfo) {
        this.listener = new WeakReference<>(listener);
        serverUrl = context.getString(R.string.api_server_url);
        this.schemaUrl = schemaUrl;
        this.userInfo = userInfo;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected String doInBackground(String... params) {
        if(!DriverApp.getIsNetworkAvailable()) {
            // no network available. don't bother logging in
            publishProgress(context.getString(R.string.error_no_network));
            Log.d(LOG_LABEL, "No network");
            cancel(true);
            return null;
        }

        if (userInfo == null) {
            publishProgress(context.getString(R.string.error_schema_update));
            Log.e(LOG_LABEL, "missing user info!");
            cancel(true);
            return null;
        }

        String token = userInfo.getUserToken();

        // should not happen, but check anyways
        if (token == null || token.isEmpty()) {
            publishProgress(context.getString(R.string.error_schema_update));
            Log.e(LOG_LABEL, "missing user token!");
            cancel(true);
            return null;
        }

        // sanity check; make sure that the string found is actually a UUID
        String recordSchemaUuid = params[0];
        try {
            UUID.fromString(recordSchemaUuid);
        } catch (IllegalArgumentException e) {
            // does not conform to UUID String representation
            publishProgress(context.getString(R.string.error_schema_update));
            cancel(true);
            Log.e(LOG_LABEL, "Schema UUID string found, but it doesn't look like a UUID: " + recordSchemaUuid);
            return null;
        }

        try {
            URL url = schemaUrl.schemaUrl(serverUrl, recordSchemaUuid);

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Authorization", "Token " + token);

            // if get a 403 back, tell activity to go login again (shouldn't happen)
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                Log.w(LOG_LABEL, "User token must be invalid.");
                UpdateSchemaCallbackListener caller = listener.get();
                if (caller != null) {
                    caller.haveInvalidCredentials();
                } else {
                    Log.w(LOG_LABEL, "Cannot notify of 403 because listener has gone");
                }
                cancel(true);
                return null;
            }

            // if get a 201 back, jar doesn't exist just yet (unlikely to happen)
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                Log.d(LOG_LABEL, "Updated model jar is not ready; it is now being created");
                UpdateSchemaCallbackListener caller = listener.get();
                if (caller != null) {
                    caller.schemaUpdateError(context.getString(R.string.schema_update_not_ready));
                } else {
                    Log.w(LOG_LABEL, "Cannot notify of 201 because listener has gone");
                }
                cancel(true);
                return null;
            }

            if (urlConnection.getResponseCode() == 200) {
                // download jar file
                File file = new File(context.getDir("dex", Context.MODE_PRIVATE), DriverApp.UPDATED_JAR_NAME);

                // delete any previously downloaded update
                if (file.exists()) {
                    file.delete();
                }

                InputStream inputStream = urlConnection.getInputStream();
                OutputStream outputStream = new FileOutputStream(file);

                boolean fileDownloadedOk = false;
                try {
                    IOUtils.copy(inputStream, outputStream);
                    fileDownloadedOk = true;
                } catch (IOException e) {
                    Log.e(LOG_LABEL, "Failed to download jar file");
                    // cancel task after streams closed
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(inputStream);
                    IOUtils.closeQuietly(outputStream);
                }

                if (!fileDownloadedOk) {
                    publishProgress(context.getString(R.string.error_schema_update));
                    cancel(true);
                    return null;
                }

                // go load the downloaded schema
                DriverApp driverApp = (DriverApp) DriverApp.getContext();
                if (driverApp.loadSchemaClasses(DriverApp.UPDATED_JAR_NAME, recordSchemaUuid)) {
                    Log.d(LOG_LABEL, "New schema jar loaded successfully");
                    return recordSchemaUuid;
                } else {
                    Log.e(LOG_LABEL, "Could not load updated schema jar file!");
                    // delete the downloaded file; hopefully trying again later will work
                    file.delete();

                    // load backup model jar file
                    driverApp.loadBackupSchema();
                    return ((DriverApp) DriverApp.getContext()).getBackupJarSchemaVersion();
                }

            } else {
                Log.e(LOG_LABEL, "Schema update download request got response " + urlConnection.getResponseCode()
                        + ": " + urlConnection.getResponseMessage());
            }
        } catch (IOException e) {
            Log.e(LOG_LABEL, "Error communicating with server to perform schema check");
            publishProgress(context.getString(R.string.error_schema_update));
            e.printStackTrace();
            cancel(true);
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        UpdateSchemaCallbackListener caller = listener.get();
        if (caller != null) {
            caller.schemaUpdateError(values[0]);
        } else {
            Log.w(LOG_LABEL, "Cannot notify of schema update error because listener has gone");
        }
    }

    @Override
    protected void onCancelled(String s) {
        UpdateSchemaCallbackListener caller = listener.get();
        if (caller != null) {
            caller.schemaUpdateCancelled();
        } else {
            Log.w(LOG_LABEL, "Cannot notify of schema update cancellation because listener has gone");
        }
    }

    @Override
    protected void onPostExecute(String s) {
        // sanity check; shouldn't happen
        if (s == null || s.isEmpty()) {
            Log.e(LOG_LABEL, "Schema check task finished with no schema!");
        }

        UpdateSchemaCallbackListener caller = listener.get();
        if (caller != null) {
            caller.schemaUpdated();
        } else {
            Log.w(LOG_LABEL, "Cannot send back current schema because listener has gone");
        }
    }
}
