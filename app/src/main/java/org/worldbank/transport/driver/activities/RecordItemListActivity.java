package org.worldbank.transport.driver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.adapters.FormItemListAdapter;
import org.worldbank.transport.driver.utilities.RecordFormPaginator;

import java.util.ArrayList;
import java.util.Arrays;

public class RecordItemListActivity extends AppCompatActivity {

    private static final String LOG_LABEL = "RecordItemListActivity";

    private RecyclerView recyclerView;
    private RecyclerView.Adapter recyclerViewAdapter;
    private int sectionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // get the ID for this section first
        Bundle bundle = getIntent().getExtras();
        sectionId = bundle.getInt(RecordFormActivity.SECTION_ID);
        String sectionLabel = RecordFormPaginator.getPluralTitle(sectionId);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_item_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.record_item_list_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(sectionLabel);
        }

        // set up list view
        recyclerView = (RecyclerView) findViewById(R.id.record_item_recycler_View);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<String> testList = new ArrayList<>(Arrays.asList(
                "zero",
                "one",
                "two",
                "three",
                "four",
                "five",
                "six",
                "seven",
                "eight",
                "nine",
                "ten",
                "eleven",
                "twelve",
                "thirteen",
                "fourteen",
                "fifteen",
                "sixteen",
                "seventeen",
                "eighteen",
                "nineteen",
                "twenty",
                "twenty one",
                "twenty two",
                "twenty three",
                "twenty four"
        ));

        recyclerViewAdapter = new FormItemListAdapter(testList);
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        if (RecordFormPaginator.sectionHasNext(sectionId)) {
            Log.d(LOG_LABEL, "Form has at least one more section; use menu with next button");
            getMenuInflater().inflate(R.menu.menu_form_item_list_next, menu);
        } else {
            Log.d(LOG_LABEL, "This is the last section; use menu with save button");
            getMenuInflater().inflate(R.menu.menu_form_item_list_save, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            // up/home button
            case android.R.id.home:
                // TODO: remove and warn instead?
                // This isn't Android-y; back button is for going back, but don't want to pull
                // user suddenly from a partially completed form.

                // go back instead of returning to main screen
                finish();
                return true;

            case R.id.action_next:
                Log.d(LOG_LABEL, "Next button clicked");

                int goToSectionId = sectionId + 1;
                Log.d(LOG_LABEL, "Going to section #" + String.valueOf(goToSectionId));
                Intent intent = new Intent(this,
                        RecordFormPaginator.getActivityClassForSection(goToSectionId));

                intent.putExtra(RecordFormActivity.SECTION_ID, goToSectionId);
                startActivity(intent);

            case R.id.action_save:
                Log.d(LOG_LABEL, "Save button clicked");
                // TODO: implement 'save' menu option
        }

        return super.onOptionsItemSelected(item);
    }
}
