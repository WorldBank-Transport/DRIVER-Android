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
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class RecordListActivity extends AppCompatActivity {

    private static final String LOG_LABEL = "RecordListActivity";
    private static final DateFormat dateFormatter = SimpleDateFormat.getDateTimeInstance();

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

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == 0) {
                    long createdAt = cursor.getLong(0);
                    String dateString = dateFormatter.format(new Date(createdAt));
                    TextView textView = (TextView) view;
                    textView.setText(dateString);
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
     * Helper to launch record editor. Currently editing record should be set first, if going to
     * edit an existing record.
     */
    private void loadRecordForm() {
        Log.d(LOG_LABEL, "Going to load form...");
        Intent intent = new Intent(this, RecordFormSectionManager.getActivityClassForSection(0));
        intent.putExtra(RecordFormActivity.SECTION_ID, 0);
        startActivity(intent);
    }
}
