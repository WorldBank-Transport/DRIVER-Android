package org.worldbank.transport.driver.activities;

import android.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.azavea.androidvalidatedforms.FormController;
import com.azavea.androidvalidatedforms.tasks.ValidationTask;

import org.jsonschema2pojo.annotations.PluralTitle;
import org.worldbank.transport.driver.R;

import java.util.ArrayList;

/**
 * Form for an item in a section that contains multiple elements.
 *
 * Created by kathrynkillebrew on 12/31/15.
 */
public class RecordFormItemActivity extends RecordFormActivity {

    private static final String LOG_LABEL = "FormItemActivity";

    /**
     * Helper to build a layout with previous/next/save buttons.
     *
     * @return View with buttons with handlers added to it
     */
    @Override
    public RelativeLayout buildButtonBar() {
        // reference to this, for use in button actions (validation task makes weak ref)
        final RecordFormActivity thisActivity = this;

        // put buttons in a relative layout for positioning on right or left
        RelativeLayout buttonBar = new RelativeLayout(this);
        buttonBar.setId(R.id.record_button_bar_id);
        buttonBar.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));

        // TODO: add 'cancel' button
        /*
        Button backBtn = new Button(this);
        RelativeLayout.LayoutParams backBtnLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        backBtnLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        backBtn.setLayoutParams(backBtnLayoutParams);

        backBtn.setId(R.id.record_back_button_id);
        backBtn.setText(getText(R.string.record_previous_button));
        buttonBar.addView(backBtn);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_LABEL, "Back button clicked");

                // set this to let callback know next action to take
                goPrevious = true;
                new ValidationTask(thisActivity).execute();
            }
        });
        */

        // add save button
        Button goBtn = new Button(this);
        RelativeLayout.LayoutParams goBtnLayoutParams = new RelativeLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT);
        goBtnLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        goBtn.setLayoutParams(goBtnLayoutParams);

        goBtn.setId(R.id.record_save_button_id);

        buttonBar.addView(goBtn);

        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_LABEL, "Save button clicked");
                goPrevious = false;
                new ValidationTask(thisActivity).execute();
            }
        });

        return buttonBar;
    }

    @Override
    public void proceed() {
        Log.d(LOG_LABEL, "TODO: proceed.");
        // TODO: go back to section list view
        // save item on model if it isn't there already (probably should just be there already)
    }
}
