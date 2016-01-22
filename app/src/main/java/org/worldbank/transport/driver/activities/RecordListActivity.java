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
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.datastore.DriverRecordContract;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.utilities.LocationServiceManager;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class RecordListActivity extends AppCompatActivity {

    private static final String LOG_LABEL = "RecordListActivity";

    private static final DateFormat sourceDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private static final DateFormat displayDateFormatter =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DriverAppContext appContext = new DriverAppContext((DriverApp) getApplicationContext());
        final DriverApp app = appContext.getDriverApp();

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
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                getApplicationContext(),
                R.layout.record_list_item,
                app.getAllRecords(),
                useColumns,
                toViews,
                0);

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper to launch record editor. Currently editing record should be set first
     * (or cleared, if adding a new record).
     */
    private void loadRecordForm() {

        // TODO: handle with form navigation management
        // Should handle stopping location service before allowing user to bail from form,
        // by listening to both 'back' and 'up' actions and warning user if they're about to exit
        // an unsaved record.
        if (LocationServiceManager.isRunning()) {
            Log.w(LOG_LABEL, "Location service manager still running outside of form. Stopping it.");
            LocationServiceManager.stopService();
        }

        Log.d(LOG_LABEL, "Going to load form...");
        Intent intent = new Intent(this, RecordFormSectionManager.getActivityClassForSection(-1));
        intent.putExtra(RecordFormActivity.SECTION_ID, -1);
        startActivity(intent);
    }
}
