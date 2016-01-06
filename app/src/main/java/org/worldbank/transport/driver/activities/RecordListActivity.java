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

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.datastore.DriverRecordContract;
import org.worldbank.transport.driver.datastore.RecordDatabaseManager;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;


public class RecordListActivity extends AppCompatActivity {

    private static final String LOG_LABEL = "RecordListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // add record button
        final AppCompatActivity thisActivity = this;
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.record_list_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d("RecordListActivity", "Going to load form...");
                Intent intent = new Intent(thisActivity,
                        RecordFormSectionManager.getActivityClassForSection(0));
                intent.putExtra(RecordFormActivity.SECTION_ID, 0);
                startActivity(intent);

            }
        });

        RecordDatabaseManager mgr = new RecordDatabaseManager(this);
        final Cursor cursor = mgr.readAllRecords();

        // set up list view
        ListView listView = (ListView) findViewById(R.id.record_list_view);
        String[] useColumns = {DriverRecordContract.RecordEntry.COLUMN_DATA};
        int[] toViews = { R.id.record_list_item_entered_at };
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                getApplicationContext(),
                R.layout.record_list_item,
                cursor,
                useColumns,
                toViews,
                0);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // id here is database _ID
                Log.d(LOG_LABEL, "Clicked at position: " + position + " where the ID is: " + id);
                // TODO: open record at ID
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
}
