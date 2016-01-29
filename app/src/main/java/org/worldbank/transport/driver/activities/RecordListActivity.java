package org.worldbank.transport.driver.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.datastore.DriverRecordContract;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.tasks.CheckSchemaTask;
import org.worldbank.transport.driver.tasks.PostRecordsTask;
import org.worldbank.transport.driver.utilities.LocationServiceManager;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class RecordListActivity extends AppCompatActivity implements CheckSchemaTask.CheckSchemaCallbackListener,
        PostRecordsTask.PostRecordsListener {

    private static final String LOG_LABEL = "RecordListActivity";

    private static final DateFormat sourceDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    SimpleCursorAdapter adapter;
    DriverApp app;
    CheckSchemaTask checkSchemaTask;
    PostRecordsTask postRecordsTask;
    ProgressBar progressBar;

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
        ListView listView = (ListView) findViewById(R.id.record_list_view);
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
                        String dateString = displayDateFormatter.format(date);
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
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // id here is database _ID
                Log.d(LOG_LABEL, "Going to edit record with ID: " + id);
                app.setCurrentlyEditingRecord(id);
                loadRecordForm();
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
        } else {
            Log.w(LOG_LABEL, "Unrecognized menu action: " + id);
        }

        return super.onOptionsItemSelected(item);
    }

    private void startRecordUpload() {
        if (postRecordsTask != null) {
            Log.d(LOG_LABEL, "Already uploading records");
            return;
        }

        // set up progress bar
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setIndeterminate(false);
                progressBar.requestLayout();
                progressBar.setMax(adapter.getCount());
                progressBar.setProgress(0);

                Log.d(LOG_LABEL, "progress bar max set to " + adapter.getCount());
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        postRecordsTask = new PostRecordsTask(this, app.getUserInfo());
        postRecordsTask.execute();
    }

    private void startSchemaUpdateCheck() {
        if (checkSchemaTask != null) {
            Log.d(LOG_LABEL, "Already checking schema");
            return;
        }

        Log.d(LOG_LABEL, "Going to check schema");
        checkSchemaTask = new CheckSchemaTask(this);
        checkSchemaTask.execute(app.getUserInfo());
        // set up progress bar
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
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

    @Override
    public void foundSchema(String currentSchema) {
        Log.d(LOG_LABEL, "Found schema " + currentSchema);
        checkSchemaTask = null;

        if (!DriverApp.getCurrentSchema().equals(currentSchema)) {
            // TODO: update schema and restart app if a new one is available
            Log.w(LOG_LABEL, "There is a new schema available! Go get it.");
        } else {
            Log.d(LOG_LABEL, "This schema version is the latest!");
            Toast toast = Toast.makeText(this, getString(R.string.schema_current), Toast.LENGTH_SHORT);
            toast.show();
        }

        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void schemaCheckCancelled() {
        Log.d(LOG_LABEL, "Schema check cancelled");
        checkSchemaTask = null;
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void schemaCheckError(String errorMessage) {
        Log.d(LOG_LABEL, "Got schema check error: " + errorMessage);
        Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_LONG);
        toast.show();
        progressBar.setVisibility(View.GONE);
        checkSchemaTask = null;
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
        progressBar.setVisibility(View.GONE);
        finish();
    }

    @Override
    public void recordUploadFinished(int failed) {
        postRecordsTask = null;

        if (failed > 0) {
            Log.e(LOG_LABEL, failed + " records failed to upload");
            Toast toast = Toast.makeText(this, getString(R.string.records_uploaded_some_failed, failed), Toast.LENGTH_LONG);
            toast.show();
        } else {
            Toast toast = Toast.makeText(this, getString(R.string.records_uploaded_checking_schema), Toast.LENGTH_SHORT);
            toast.show();
        }

        // clear now-outdated list view of the uploaded records
        adapter.changeCursor(app.getAllRecords());
        adapter.notifyDataSetChanged();

        // start schema update check
        startSchemaUpdateCheck();
    }

    @Override
    public void recordUploadCancelled(String errorMessage) {
        Log.e(LOG_LABEL, "Record upload failed with error: " + errorMessage);
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);
        postRecordsTask = null;

        Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void uploadedOneRecord() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_LABEL, "uploaded one record, updating progress...");
                progressBar.incrementProgressBy(1);
            }
        });
    }
}
