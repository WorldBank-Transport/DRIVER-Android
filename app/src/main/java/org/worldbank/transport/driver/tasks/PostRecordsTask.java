package org.worldbank.transport.driver.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.datastore.DriverRecordContract;
import org.worldbank.transport.driver.datastore.RecordDatabaseManager;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.staticmodels.DriverUserInfo;
import org.worldbank.transport.driver.utilities.UploadRecordUrlBuilder;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Upload records to server, then delete them from the local database.
 *
 * Created by kathrynkillebrew on 1/28/16.
 */
public class PostRecordsTask extends AsyncTask<Integer, Integer, Integer> {

    public static final String LOG_LABEL = "PostRecordsTask";

    public interface PostRecordsListener {
        void recordUploadFinished(int failed);
        void recordUploadCancelled(String errorMessage);
        void uploadedOneRecord();
        void haveInvalidCredentials();
    }

    public interface UploadRecordUrl {
        // Backend endpoints. Note that it is necessary to keep the trailing slash here.
        String RECORD_ENDPOINT = "api/records/";

        URL recordUrl(String serverUrl);
    }

    private String serverUrl;
    private final Context context = DriverAppContext.getContext();
    private final WeakReference<PostRecordsListener> listener;
    private final UploadRecordUrl uploadRecordUrl;
    private final DriverUserInfo userInfo;
    private final RecordDatabaseManager databaseManager;
    private String errorMessage;

    public PostRecordsTask(PostRecordsListener listener, DriverUserInfo userInfo) {
        this(listener, userInfo, new UploadRecordUrlBuilder());
    }

     // Invoke this constructor directly in test.
    public PostRecordsTask(PostRecordsListener listener, DriverUserInfo userInfo, UploadRecordUrl uploadRecordUrl) {

        this.listener = new WeakReference<>(listener);
        this.userInfo = userInfo;
        this.uploadRecordUrl = uploadRecordUrl;
        this.databaseManager = DriverApp.getDatabaseManager();
        serverUrl = context.getString(R.string.api_server_url);
    }

    @Override
    protected Integer doInBackground(Integer... params) {

        Cursor cursor = databaseManager.readAllRecords();
        int failed = cursor.getCount(); // decrement failure count as records are uploaded successfully

        if(!DriverApp.getIsNetworkAvailable()) {
            // no network available. don't bother logging in
            errorMessage = context.getString(R.string.error_no_network);
            Log.d(LOG_LABEL, "No network");
            cancel(true);
            return null;
        }

        if (userInfo == null) {
            errorMessage = context.getString(R.string.error_record_upload);
            Log.e(LOG_LABEL, "missing user info!");
            PostRecordsListener caller = listener.get();
            if (caller != null) {
                caller.haveInvalidCredentials();
            }
            cancel(true);
            return null;
        }

        String token = userInfo.getUserToken();

        // should not happen, but check anyways
        if (token == null || token.isEmpty()) {
            errorMessage = context.getString(R.string.error_record_upload);
            Log.e(LOG_LABEL, "missing user token!");
            PostRecordsListener caller = listener.get();
            if (caller != null) {
                caller.haveInvalidCredentials();
            }
            cancel(true);
            return null;
        }

        Log.d(LOG_LABEL, "Going to upload " + cursor.getCount() + " records...");

        if (!cursor.moveToFirst()) {
            Log.w(LOG_LABEL, "No records in cursor to upload!");
            errorMessage = context.getString(R.string.records_nothing_to_upload);
            cancel(true);
            return null;
        }

        try {
            int idCol = cursor.getColumnIndexOrThrow(DriverRecordContract.RecordEntry._ID);
            int schemaCol = cursor.getColumnIndexOrThrow(DriverRecordContract.RecordEntry.COLUMN_SCHEMA_VERSION);
            int dataCol = cursor.getColumnIndexOrThrow(DriverRecordContract.RecordEntry.COLUMN_DATA);
            int weatherCol = cursor.getColumnIndexOrThrow(DriverRecordContract.RecordEntry.COLUMN_WEATHER);
            int lightCol = cursor.getColumnIndexOrThrow(DriverRecordContract.RecordEntry.COLUMN_LIGHT);
            int colOccurredFrom = cursor.getColumnIndexOrThrow(DriverRecordContract.RecordEntry.COLUMN_OCCURRED_FROM);
            int colOccurredTo = cursor.getColumnIndexOrThrow(DriverRecordContract.RecordEntry.COLUMN_OCCURRED_TO);
            int colLat = cursor.getColumnIndexOrThrow(DriverRecordContract.RecordEntry.COLUMN_LATITUDE);
            int colLon = cursor.getColumnIndexOrThrow(DriverRecordContract.RecordEntry.COLUMN_LONGITUDE);
            int colEnteredAt = cursor.getColumnIndexOrThrow(DriverRecordContract.RecordEntry.COLUMN_ENTERED_AT);
            int colUpdatedAt = cursor.getColumnIndexOrThrow(DriverRecordContract.RecordEntry.COLUMN_UPDATED_AT);

            do {
                int recordId = cursor.getInt(idCol);
                Log.d(LOG_LABEL, "Reading record to upload: " + recordId);

                // this try block is for attempting to read/upload a single record;
                // it will continue to the next record in the loop on failure
                try {
                    String schemaVersion = cursor.getString(schemaCol);
                    String data = cursor.getString(dataCol);
                    String weather = cursor.getString(weatherCol);
                    String light = cursor.getString(lightCol);
                    String occurredFrom = cursor.getString(colOccurredFrom);
                    String occurredTo = cursor.getString(colOccurredTo);
                    Double latitude = cursor.getDouble(colLat);
                    Double longitude = cursor.getDouble(colLon);
                    String enteredAt = cursor.getString(colEnteredAt);
                    String updatedAt = cursor.getString(colUpdatedAt);

                    // go build the JSON to POST
                    JSONObject postJson = new JSONObject();
                    postJson.put("schema", schemaVersion);

                    // the non-constant data section
                    JSONObject dataJson = new JSONObject(data);
                    postJson.put("data", dataJson);

                    // constant fields
                    if (weather != null && !weather.isEmpty()) {
                        postJson.put("weather", weather);
                    }

                    if (light != null && !light.isEmpty()) {
                        postJson.put("light", light);
                    }

                    // build geometry object

                    // user allowed to save record without a location, in case they cannot get a
                    // GPS fix somewhere, but it cannot be uploaded until set
                    if (latitude == 0 && longitude == 0) {
                        Log.d(LOG_LABEL, "Record without coordinates cannot be uploaded");
                        continue;
                    }

                    JSONObject geomJson = new JSONObject();
                    JSONArray coordArray = new JSONArray();
                    coordArray.put(longitude);
                    coordArray.put(latitude);
                    geomJson.put("type", "Point");
                    geomJson.put("coordinates", coordArray);
                    postJson.put("geom", geomJson);

                    postJson.put("occurred_from", occurredFrom);
                    postJson.put("occurred_to", occurredTo);
                    postJson.put("created", enteredAt);
                    postJson.put("modified", updatedAt);

                    // now go upload it
                    URL uploadUrl = uploadRecordUrl.recordUrl(serverUrl);
                    HttpURLConnection urlConnection = (HttpURLConnection) uploadUrl.openConnection();

                    urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    urlConnection.setRequestProperty("Authorization", "Token " + token);
                    urlConnection.setDoOutput(true);
                    urlConnection.setChunkedStreamingMode(0);
                    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                    writer.write(postJson.toString());
                    writer.flush();
                    writer.close();
                    out.close();

                    // check response
                    int responseCode = urlConnection.getResponseCode();
                    if (responseCode != 201) {
                        Log.e(LOG_LABEL, "Failed to upload record. Got response: " +
                                urlConnection.getResponseCode() + ": " + urlConnection.getResponseMessage());

                        if (responseCode == 403) {
                            // credentials must be bad. stop trying to upload records and log out.
                            PostRecordsListener caller = listener.get();
                            if (caller != null) {
                                caller.haveInvalidCredentials();
                            }
                        }

                        // send general "server error" message
                        errorMessage = context.getString(R.string.error_record_upload);
                        urlConnection.disconnect();
                    } else {
                        Log.d(LOG_LABEL, "Record uploaded successfully!");

                        // delete uploaded record from DB now
                        if (!databaseManager.deleteRecord(recordId)) {
                            Log.e(LOG_LABEL, "Failed to delete record " + recordId);
                        }
                        failed--;

                        publishProgress(1);

                    }
                } catch (JSONException e) {
                    Log.e(LOG_LABEL, "Error building JSON upload output");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e(LOG_LABEL, "Error communicating with server to upload record");
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
            return failed;

        } catch (IllegalArgumentException e) {
            Log.e(LOG_LABEL, "Did record post task fail to find a database column?");
            e.printStackTrace();
            errorMessage = context.getString(R.string.error_record_upload);
            cancel(true);
        } finally {
            cursor.close();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        PostRecordsListener caller = listener.get();
        if (caller != null) {
            caller.uploadedOneRecord();
        }
    }

    @Override
    protected void onPostExecute(Integer failed) {
        PostRecordsListener caller = listener.get();
        if (caller != null) {
            caller.recordUploadFinished(failed);
        }
    }

    @Override
    protected void onCancelled() {
        PostRecordsListener caller = listener.get();
        if (caller != null) {
            caller.recordUploadCancelled(errorMessage);
        }
    }
}
