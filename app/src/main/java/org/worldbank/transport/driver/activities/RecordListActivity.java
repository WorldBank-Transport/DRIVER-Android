package org.worldbank.transport.driver.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.datastore.DriverRecordContract;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.tasks.CheckSchemaTask;
import org.worldbank.transport.driver.tasks.PostRecordsTask;
import org.worldbank.transport.driver.tasks.UpdateSchemaTask;
import org.worldbank.transport.driver.utilities.DriverUtilities;
import org.worldbank.transport.driver.utilities.LocationServiceManager;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class RecordListActivity extends AppCompatActivity implements CheckSchemaTask.CheckSchemaCallbackListener,
        PostRecordsTask.PostRecordsListener, UpdateSchemaTask.UpdateSchemaCallbackListener {

    private static final String LOG_LABEL = "RecordListActivity";

    private static final DateFormat sourceDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    SimpleCursorAdapter adapter;
    DriverApp app;
    CheckSchemaTask checkSchemaTask;
    PostRecordsTask postRecordsTask;
    UpdateSchemaTask updateSchemaTask;
    ProgressBar progressBar;
    FloatingActionButton fab;
    ListView recordListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = (ProgressBar) findViewById(R.id.record_list_progress);

        DriverAppContext appContext = new DriverAppContext((DriverApp) getApplicationContext());
        app = appContext.getDriverApp();

        // add record button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.record_list_fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // In case user got back to home view via back button, clear any previously
                // editing record so a new one may be created.
                app.clearCurrentlyEditingRecord();
                loadRecordForm();
            }
        });

        // set up list view
        recordListView = (ListView) findViewById(R.id.record_list_view);
        String[] useColumns = { DriverRecordContract.RecordEntry.COLUMN_ENTERED_AT };
        int[] toViews = { R.id.record_list_item_entered_at };
        adapter = new SimpleCursorAdapter(
                getApplicationContext(),
                R.layout.record_list_item,
                app.getAllRecords(),
                useColumns,
                toViews,
                0);

        // use 24-hour date format if system does so
        final DateFormat displayDateFormatter;
        if (android.text.format.DateFormat.is24HourFormat(this)) {
            displayDateFormatter = new SimpleDateFormat("MMM d, yyyy HH:mm:ss z", Locale.getDefault());
        } else {
            displayDateFormatter = new SimpleDateFormat("MMM d, yyyy hh:mm:ss z", Locale.getDefault());
        }

        sourceDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        displayDateFormatter.setTimeZone(TimeZone.getDefault());
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                // format created at date
                if (columnIndex == 1) {
                    TextView textView = (TextView) view;
                    // stored in SQLite as yyyy-mm-dd hh:mm:ss
                    String createdAt = cursor.getString(1);
                    try {
                        Date date = sourceDateFormat.parse(createdAt);
                        String dateString;
                        if (DriverUtilities.languageIsArabic()) {
                            dateString = DriverUtilities.formatDateAsHijri(date);
                        } else {
                            dateString = displayDateFormatter.format(date);
                        }
                        textView.setText(dateString);
                    } catch (ParseException e) {
                        Log.e(LOG_LABEL, "Failed to parse date string " + createdAt);
                        e.printStackTrace();
                        textView.setText(createdAt);
                    }
                    return true;
                }
                return false;
            }
        });
        recordListView.setAdapter(adapter);
	recordListView.setEmptyView(findViewById(android.R.id.empty));

        recordListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // id here is database _ID
                Log.d(LOG_LABEL, "Going to edit record with ID: " + id);
                app.setCurrentlyEditingRecord(id);
                loadRecordForm();
            }
        });

        recordListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_LABEL, "Long-pressed record with ID: " + id);
                RecordLongPressDialog dialog = new RecordLongPressDialog();
                Bundle dialogBundle = new Bundle();
                dialogBundle.putLong("recordId", id);
                dialog.setArguments(dialogBundle);
                dialog.show(getSupportFragmentManager(), "RecordLongPressDialog");
                return true;
            }
        });
    }

    @Override
    protected void onPostResume() {
        Log.d(LOG_LABEL, "in onPostResume for record list; refresh list");
        super.onPostResume();
        if (adapter != null) {
            Log.d(LOG_LABEL, "Updating cursor");
            adapter.changeCursor(app.getAllRecords());
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_upload) {
            // attempt record upload, which will then check for a new schema when done
            startRecordUpload();
            return true;
        } else if (id == R.id.action_update_schema) {
            startSchemaUpdateCheck();
            return true;
        } else if (id == R.id.action_logout) {
            confirmLogout();
        } else {
            Log.w(LOG_LABEL, "Unrecognized menu action: " + id);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Show confirmation dialog to user before actually logging out
     */
    private void confirmLogout() {
        Log.d(LOG_LABEL, "Logout menu option selected");
        WarnLogoutDialog dialog = new WarnLogoutDialog();
        dialog.show(getSupportFragmentManager(), "warnlogout");
    }

    /**
     * Call after user has confirmed logout action in dialog
     */
    private void logout() {
        Log.d(LOG_LABEL, "Logging out user");

        // clear stored user info from app settings and shared preferences
        app.setUserInfo(null);

        if (DriverApp.getIsNetworkAvailable()) {
            showProgressBar(true);
            // attempt to log out of Google SSO session as well, on background thread
            new LogoutGoogleApiClient().execute();
        } else {
           backToLogin();
        }
    }

    /**
     * Close this activity and go back to login screen after logout.
     */
    public void backToLogin() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_LABEL, "navigating back to login screen");
                Intent intent = new Intent(RecordListActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // clear back history
                startActivity(intent);
                finish();
            }
        });
    }

    private void startRecordUpload() {
        if (postRecordsTask != null) {
            Log.d(LOG_LABEL, "Already uploading records");
            return;
        }

        showProgressBar(true);
        postRecordsTask = new PostRecordsTask(this, app.getUserInfo());
        postRecordsTask.execute();
    }

    /**
     * Helper to launch record editor. Currently editing record should be set first
     * (or cleared, if adding a new record).
     */
    private void loadRecordForm() {
        // shouldn't happen at this point, but check
        if (LocationServiceManager.isRunning()) {
            Log.w(LOG_LABEL, "Location service manager still running outside of form. Stopping it.");
            LocationServiceManager.stopService();
        }

        Log.d(LOG_LABEL, "Going to load form...");
        Intent intent = new Intent(this, RecordFormSectionManager.getActivityClassForSection(-1));
        intent.putExtra(RecordFormActivity.SECTION_ID, -1);
        startActivity(intent);
    }

    private void startSchemaUpdateCheck() {
        if (checkSchemaTask != null) {
            Log.d(LOG_LABEL, "Already checking schema");
            return;
        }

        Log.d(LOG_LABEL, "Going to check schema");
        showProgressBar(true);
        checkSchemaTask = new CheckSchemaTask(this);
        checkSchemaTask.execute(app.getUserInfo());
    }

    @Override
    public void foundSchema(String currentSchema) {
        Log.d(LOG_LABEL, "Found schema " + currentSchema);
        checkSchemaTask = null;

        if (!DriverApp.getCurrentSchema().equals(currentSchema)) {
            // update schema if a new one is available
            if (updateSchemaTask != null) {
                Log.w(LOG_LABEL, "Schema update task already running! Doing nothing.");
            } else {
                // be sure there are no records around before updating
                if (adapter.getCount() == 0) {
                    Log.d(LOG_LABEL, "Starting schema update task");
                    updateSchemaTask = new UpdateSchemaTask(this, app.getUserInfo());
                    updateSchemaTask.execute(currentSchema);
                } else {
                    Log.w(LOG_LABEL, "Have records that have not uploaded yet; not updating schema now");
                    showToast(getString(R.string.schema_not_updated_have_records), true);
                    showProgressBar(false);
                }
            }

        } else {
            Log.d(LOG_LABEL, "This schema version is the latest!");
            showToast(getString(R.string.schema_current), true);
            showProgressBar(false);
        }
    }

    @Override
    public void schemaCheckCancelled() {
        Log.d(LOG_LABEL, "Schema check cancelled");
        checkSchemaTask = null;
        showProgressBar(false);
    }

    @Override
    public void schemaCheckError(String errorMessage) {
        Log.d(LOG_LABEL, "Got schema check error: " + errorMessage);
        showToast(errorMessage, true);
        checkSchemaTask = null;
        showProgressBar(false);
    }

    @Override
    public void schemaUpdated() {
        Log.d(LOG_LABEL, "Schema updated!");
        updateSchemaTask = null;
        showToast(getString(R.string.schema_updated), true);
        showProgressBar(false);
    }

    @Override
    public void schemaUpdateCancelled() {
        Log.w(LOG_LABEL, "Schema update cancelled");
        updateSchemaTask = null;
        showProgressBar(false);
    }

    @Override
    public void schemaUpdateError(final String errorMessage) {
        Log.e(LOG_LABEL, "Schema update error: " + errorMessage);
        showToast(errorMessage, true);
        updateSchemaTask = null;
        showProgressBar(false);
    }

    /**
     * This callback method is shared by the schema check task and record post task.
     */
    @Override
    public void haveInvalidCredentials() {
        Log.e(LOG_LABEL, "Have invalid credentials!");
        // Somehow have bad auth token. Clear user info and go back to launch login activity.
        app.setUserInfo(null);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        checkSchemaTask = null;
        postRecordsTask = null;
        updateSchemaTask = null;
        showProgressBar(false);
        finish();
    }

    @Override
    public void recordUploadFinished(int failed) {
        postRecordsTask = null;
        showProgressBar(false);

        if (failed > 0) {
            Log.e(LOG_LABEL, failed + " records failed to upload");
            showToast(getString(R.string.records_uploaded_some_failed, failed), true);
        } else {
            showToast(getString(R.string.records_uploaded_checking_schema), false);
            // start schema update check
            startSchemaUpdateCheck();
        }

        // clear now-outdated list view of the uploaded records
        adapter.changeCursor(app.getAllRecords());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void recordUploadCancelled(String errorMessage) {
        Log.e(LOG_LABEL, "Record upload failed with error: " + errorMessage);
        showProgressBar(false);
        postRecordsTask = null;
        showToast(errorMessage, true);
    }

    @Override
    public void uploadedOneRecord() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressBar == null) {
                    return; // view is gone already
                }
                Log.d(LOG_LABEL, "uploaded one record, updating progress...");
                progressBar.incrementProgressBy(1);
            }
        });
    }

    public void startSingleRecordUploadTask(long recordId) {
        showProgressBar(true);

        if (postRecordsTask != null) {
            Log.w(LOG_LABEL, "Already uploading records; not going to start single record upload");
            return;
        }
        postRecordsTask = new PostRecordsTask(this, app.getUserInfo());
        postRecordsTask.execute(recordId);
    }

    public static class RecordLongPressDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle bundle = getArguments();
            final long recordId = bundle.getLong("recordId");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.record_select_title)
                    .setMessage(R.string.record_select_message)
                    .setPositiveButton(R.string.confirm_action, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(LOG_LABEL, "User selected to upload a single record");
                            RecordListActivity activity = (RecordListActivity) getActivity();
                            activity.startSingleRecordUploadTask(recordId);
                        }
                    })
                    .setNegativeButton(R.string.stop_action, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(LOG_LABEL, "User cancelled single record upload");
                        }
                    });
            return builder.create();
        }
    }

    /**
     * Show/hide the progress bar. When showing, hide record list and floating action button.
     *
     * @param show Show progress bar if true; hide it if false.
     */
    private void showProgressBar(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressBar == null || fab == null || recordListView == null) {
                    return; // view is gone or disappearing
                }

                try {
                    if (show) {
                        progressBar.setIndeterminate(true);
                        progressBar.requestLayout();
                        progressBar.setVisibility(View.VISIBLE);
                        recordListView.setVisibility(View.INVISIBLE);
                        fab.setVisibility(View.INVISIBLE);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        recordListView.setVisibility(View.VISIBLE);
                        fab.setVisibility(View.VISIBLE);
                    }
                } catch (NullPointerException ex) {
                    Log.e(LOG_LABEL, "Could not show/hide progress; view may have gone away");
                }
            }
        });
    }

    private void showToast(final CharSequence message, final boolean isLong) {
        final int toastLength = isLong ? Toast.LENGTH_LONG: Toast.LENGTH_SHORT;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(RecordListActivity.this, message, toastLength);
                toast.show();
            }
        });
    }

    /**
     * Prompt user to save or lose unsaved changes to currently editing record before exiting.
     * Validation should be run before opening this dialog.
     */
    public static class WarnLogoutDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.logout_dialog_title)
                    .setMessage(R.string.logout_dialog_message)
                    .setPositiveButton(R.string.confirm_action, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Log.d(LOG_LABEL, "User confirmed logout action.");
                            ((RecordListActivity) getActivity()).logout();
                        }
                    })
                    .setNegativeButton(R.string.stop_action, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Log.d(LOG_LABEL, "User cancelled logout.");
                            // (do nothing)
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    /**
     * Create a background thread for making a blocking connection call to the Google API client,
     * then using it to log out. Doing this instead of connecting/disconnecting to API client
     * with activity lifecycle as logging out is uncommon thing to do, and so always maintaining
     * a Google API client connection with the activity would be unnecessary overhead.
     */
    private class LogoutGoogleApiClient extends AsyncTask<String, String, Void> {

        private GoogleApiClient googleApiClient;

        public LogoutGoogleApiClient() {
            googleApiClient = new GoogleApiClient.Builder(RecordListActivity.this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API)
                    .build();
        }

        @Override
        protected Void doInBackground(String... params) {
            googleApiClient.blockingConnect(10, TimeUnit.SECONDS);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (googleApiClient.isConnected()) {
                Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(new ResultCallback<com.google.android.gms.common.api.Status>() {
                    @Override
                    public void onResult(@NonNull com.google.android.gms.common.api.Status status) {
                        googleApiClient.disconnect();
                        Log.d(LOG_LABEL, "background logout task finished");
                        RecordListActivity.this.backToLogin();
                        LogoutGoogleApiClient.this.cancel(true);
                    }
                });
            } else {
                Log.e(LOG_LABEL, "Not connected to Google to logout; giving up.");
                RecordListActivity.this.backToLogin();
            }
        }
    }
}

