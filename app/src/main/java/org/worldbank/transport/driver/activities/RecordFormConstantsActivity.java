package org.worldbank.transport.driver.activities;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.azavea.androidvalidatedforms.FormActivityBase;
import com.azavea.androidvalidatedforms.FormController;
import com.azavea.androidvalidatedforms.tasks.ValidationTask;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.services.DriverLocationService;
import org.worldbank.transport.driver.staticmodels.DriverConstantFields;
import org.worldbank.transport.driver.utilities.LocationServiceManager;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;


/**
 * Present form for constant fields as first form, before those for DriverSchema fields.
 *
 * Created by kathrynkillebrew on 1/8/16.
 */
public class RecordFormConstantsActivity extends RecordFormActivity implements FormActivityBase.FormReadyListener {

    private static final String LOG_LABEL = "RecordConstants";

    // section label (web app has none for this section)
    public static final String CONSTANTS_SECTION_NAME = "Record Basic Information";

    private TextView locationStatusView;
    private TextView locationView;
    private String initialLocationText;
    RelativeLayout locationBar;
    RelativeLayout buttonBar;

    @Override
    public RelativeLayout buildButtonBar() {

        // add location label and status
        ViewGroup containerView = (ViewGroup) findViewById(R.id.form_elements_container);
        locationBar = (RelativeLayout)getLayoutInflater().inflate(R.layout.location_constant_field, null);

        containerView.addView(locationBar);

        // set location text views once form layout done
        setFormReadyListener(this);

        // add button bar

        // put buttons in a relative layout for positioning on right or left
        buttonBar = new RelativeLayout(this);
        buttonBar.setId(R.id.record_button_bar_id);
        buttonBar.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));

        // add next/save button
        Button goBtn = new Button(this);
        RelativeLayout.LayoutParams goBtnLayoutParams = new RelativeLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT);
        goBtnLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        goBtn.setLayoutParams(goBtnLayoutParams);

        goBtn.setId(R.id.record_save_button_id);

        // add 'next' button
        haveNext = true;
        goBtn.setText(getString(R.string.record_next_button));

        final RecordFormActivity thisActivity = this;
        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // launch DriverSchema first form here
                goPrevious = false;
                Log.d(LOG_LABEL, "Going to validate constant fields");
                new ValidationTask(thisActivity).execute();
            }
        });

        buttonBar.addView(goBtn);
        return buttonBar;
    }

    @Override
    public void proceed() {
        int goToSectionId = 0;
        Log.d(LOG_LABEL, "Going to section #" + String.valueOf(goToSectionId));
        Intent intent = new Intent(this,
                RecordFormSectionManager.getActivityClassForSection(goToSectionId));

        intent.putExtra(RecordFormActivity.SECTION_ID, goToSectionId);
        startActivity(intent);
    }

    @Override
    protected Object getModelObject() {
        return app.getEditConstants();
    }

    @Override
    public FormController createFormController() {

        Log.d(LOG_LABEL, "createFormController called for constants activity");

        sectionField = null; // does not apply here

        sectionClass = DriverConstantFields.class;

        Object section = getModelObject();

        sectionLabel = CONSTANTS_SECTION_NAME;

        if (section != null) {
            return new FormController(this, section);
        } else {
            Log.e(LOG_LABEL, "Section object not found for record constants");
        }

        return null;
    }

    /**
     * Set location text views once form layout done, in order to capture any status changes
     * that happened during form layout.
     */
    @Override
    public void formReadyCallback() {
        Log.d(LOG_LABEL, "In form ready callback");

        locationView = (TextView) locationBar.findViewById(R.id.location_constant);
        locationStatusView = (TextView) locationBar.findViewById(R.id.location_constant_status);

        Log.d(LOG_LABEL, "Initial setup of location views");

        if (LocationServiceManager.isRunning()) {
            int status = LocationServiceManager.getCurrentStatus();
            locationStatusView.setText(getString(status));
            locationStatusView.setVisibility(View.VISIBLE);
        } else {
            locationStatusView.setVisibility(View.GONE);
        }

        if (initialLocationText != null) {
            locationView.setText(initialLocationText);
            locationView.setVisibility(View.VISIBLE);
        } else {
            locationView.setVisibility(View.GONE);
        }
    }

    private void setLocationStatusText(String status) {
        if (locationStatusView != null) {
            if (status != null) {
                Log.d(LOG_LABEL, "Set location status text to: " + status);
                locationStatusView.setText(status);
                locationStatusView.setVisibility(View.VISIBLE);
            } else {
                Log.d(LOG_LABEL, "Hiding location status text");
                locationStatusView.setVisibility(View.GONE);
            }

            locationStatusView.requestLayout();
        }
    }

    private void setLocationText() {
        Location location = getLocation();
        if (location != null) {
            Log.d(LOG_LABEL, "Going to display location");
            String locationText = getString(R.string.location_display, location.getLatitude(), location.getLongitude());
            if (locationView != null) {
                locationView.setText(locationText);
                locationView.setVisibility(View.VISIBLE);
                locationView.requestLayout();
            } else {
                initialLocationText = locationText;
            }
        } else {
            Log.d(LOG_LABEL, "No location set to display");
            if (locationView != null) {
                locationView.setVisibility(View.GONE);
                locationView.requestLayout();
            } else {
                initialLocationText = null;
            }
        }
    }

    public void onBestLocationFound() {
        // show location view and update status message
        setLocationStatusText(getString(R.string.location_best_found));
        setLocationText();
    }

    public void onGotGpxFix() {
        setLocationStatusText(getString(R.string.location_gps_fix_found));
    }

    private Location getLocation() {
        DriverConstantFields constants = app.getEditConstants();
        if (constants != null) {
            return constants.location;
        }

        return null;
    }

    private boolean haveLocationSet() {
        return getLocation() != null;
    }

    private void startLocationService() {
        if (LocationServiceManager.isRunning()) {
            Log.w(LOG_LABEL, "Location service is already running; listen to it");
            LocationServiceManager.setListeningActivity(this);
            return;
        }

        setLocationStatusText(getString(R.string.location_awaiting_gps_fix));
        setLocationText(); // will hide view

        LocationServiceManager locationServiceManager = LocationServiceManager.getInstance();
        locationServiceManager.startService(this);
    }

    /**
     * Start location service when form appears, if needed. This gets called on creation, or
     * when activity comes back to the foreground.
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();

        // start location service, if needed
        if (!haveLocationSet()) {
            Log.d(LOG_LABEL, "Do not have location set on model yet; starting location service");
            startLocationService();
        } else {
            Log.d(LOG_LABEL, "Already have location set; not starting location service");
            setLocationStatusText(null); // will hide view
            setLocationText();
        }
    }

    /**
     * Callback from permissions request result.
     *
     * If user granted app permission to use location services as result of a prompt initiated by
     * DriverLocationService, then DriverLocationService needs to be restarted now.
     *
     * @param requestCode identifier for request; should match value set by DriverLocationService
     * @param permissions what permissions were requested
     * @param grantResults response is either PERMISSION_GRANTED or PERMISSION_DENIED
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == DriverLocationService.PERMISSION_REQUEST_ID) {
            // DriverLocationService asked for location access, and now response has come back
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // restart location service (it quit)
                    startLocationService();
                } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    // let user know we needed that, and bail
                    DriverLocationService.displayPermissionRequestRationale(getApplicationContext());
                    finish();
                }
            }
        }
    }
}
