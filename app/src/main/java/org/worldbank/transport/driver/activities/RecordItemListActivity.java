package org.worldbank.transport.driver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
import org.worldbank.transport.driver.models.DriverSchema;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class RecordItemListActivity extends AppCompatActivity {

    private static final String LOG_LABEL = "RecordItemListActivity";

    private RecyclerView recyclerView;
    private FormItemListAdapter recyclerViewAdapter;

    private DriverApp app;
    protected DriverSchema currentlyEditing;
    protected int sectionId;
    String sectionLabel;
    Class sectionClass;
    ArrayList sectionItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set up some state before calling super
        DriverAppContext mAppContext = new DriverAppContext((DriverApp) getApplicationContext());
        app = mAppContext.getDriverApp();
        currentlyEditing = app.getEditObject();
        Bundle bundle = getIntent().getExtras();
        sectionId = bundle.getInt(RecordFormActivity.SECTION_ID);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_record_item_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.record_item_list_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int newItemIndex = sectionItems.size();
                launchItemForm(newItemIndex);
            }
        });

        // set up list view
        recyclerView = (RecyclerView) findViewById(R.id.record_item_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        buildItemList();
    }

    private void launchItemForm(int index) {
        Intent intent = new Intent(this, RecordFormItemActivity.class);
        intent.putExtra(RecordFormActivity.SECTION_ID, sectionId);
        intent.putExtra(RecordFormItemActivity.ITEM_INDEX, index);
        startActivity(intent);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d(LOG_LABEL, "in onPostResume for RecordItemList");

        // refresh item list whenever activity comes back into view
        recyclerViewAdapter.rebuildLabelList(sectionItems, sectionClass);
    }

    private void buildItemList() {
        Log.d(LOG_LABEL, "buildItemList called");
        String sectionName = RecordFormSectionManager.getSectionName(sectionId);

        // section offset was passed to activity in intent; find section to use here
        Field sectionField = RecordFormSectionManager.getFieldForSectionName(sectionName);

        if (sectionField == null) {
            Log.e(LOG_LABEL, "Section field named " + sectionName + " not found.");
            return;
        }

        Log.d(LOG_LABEL, "Found sectionField " + sectionField.getName());
        sectionClass = RecordFormSectionManager.getSectionClass(sectionName);
        Object section = RecordFormSectionManager.getOrCreateSectionObject(sectionField, sectionClass, currentlyEditing);

        // use singular title for form section label
        // TODO: also use 'Description' annotation somewhere?
        sectionLabel = sectionClass.getSimpleName(); // default
        sectionLabel = RecordFormSectionManager.getPluralTitle(sectionField, sectionLabel);

        // set up action bar with label
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(sectionLabel);
        }

        if (section != null) {
            sectionItems = RecordFormSectionManager.getSectionList(section);
            Log.d(LOG_LABEL, "Found " + sectionItems.size() + " list items");
        } else {
            Log.e(LOG_LABEL, "Section object not found for " + sectionName);
            return;
        }

        FormItemListAdapter.FormItemClickListener clickListener = new FormItemListAdapter.FormItemClickListener() {
            @Override
            public void clickedItem(View view, int position) {
                Log.d(LOG_LABEL, "Clicked item at position " + position);
                launchItemForm(position);
            }
        };

        String defaultItemLabel = RecordFormSectionManager.getSingleTitle(sectionField, sectionLabel);
        recyclerViewAdapter = new FormItemListAdapter(sectionItems, sectionClass, defaultItemLabel, clickListener);
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        if (RecordFormSectionManager.sectionHasNext(sectionId)) {
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
                        RecordFormSectionManager.getActivityClassForSection(goToSectionId));

                intent.putExtra(RecordFormActivity.SECTION_ID, goToSectionId);
                startActivity(intent);
                return true;

            case R.id.action_save:
                Log.d(LOG_LABEL, "Save button clicked");
                RecordFormSectionManager.saveAndExit(app, this);
                return true;

            case R.id.action_save_and_exit:
                Log.d(LOG_LABEL, "Save and exit button clicked");
                RecordFormSectionManager.saveAndExit(app, this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
