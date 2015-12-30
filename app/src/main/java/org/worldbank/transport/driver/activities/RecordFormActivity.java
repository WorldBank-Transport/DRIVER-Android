package org.worldbank.transport.driver.activities;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.azavea.androidvalidatedforms.FormController;
import com.azavea.androidvalidatedforms.FormWithAppCompatActivity;
import com.azavea.androidvalidatedforms.controllers.EditTextController;
import com.azavea.androidvalidatedforms.controllers.FormSectionController;
import com.azavea.androidvalidatedforms.controllers.SelectionController;

import org.jsonschema2pojo.annotations.FieldType;
import org.jsonschema2pojo.annotations.FieldTypes;
import org.jsonschema2pojo.annotations.IsHidden;

import com.azavea.androidvalidatedforms.tasks.ValidationTask;
import com.google.gson.annotations.SerializedName;
import javax.validation.constraints.NotNull;

// TODO: use these. They apply only to sections
import org.jsonschema2pojo.annotations.Description;
import org.jsonschema2pojo.annotations.Title;
import org.jsonschema2pojo.annotations.PluralTitle;
import org.jsonschema2pojo.annotations.Multiple;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.models.DriverSchema;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by kathrynkillebrew on 12/9/15.
 */
public class RecordFormActivity extends FormWithAppCompatActivity {

    // path to model classes created by jsonschema2pojo
    // this must match the targetPackage declared in the gradle build file (with a trailing period)
    private static final String MODEL_PACKAGE = "org.worldbank.transport.driver.models.";

    public static final String SECTION_ID = "driver_section_id";

    private DriverAppContext mAppContext;
    DriverApp app;

    private DriverSchema currentlyEditing;
    private int sectionId;
    private String sectionName;
    private String[] sectionOrder;
    private Class sectionClass;

    // have next/previous sections
    private boolean haveNext = false;
    private boolean havePrevious = false;

    // if selected action is to go to previous (if false, go to next or save)
    private boolean goPrevious = false;

    /**
     * Non-default constructor for testing, to set the application context.
     * @param context Mock context
     */
    public RecordFormActivity(DriverAppContext context) {
        super();
        mAppContext = context;
    }

    /**
     * Default constructor, for testing.
     */
    public RecordFormActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set up some state before calling super
        mAppContext = new DriverAppContext((DriverApp) getApplicationContext());
        app = mAppContext.getDriverApp();
        currentlyEditing = app.getEditObject();

        Bundle bundle = getIntent().getExtras();
        sectionId = bundle.getInt(SECTION_ID);
        sectionOrder = app.getSchemaSectionOrder();

        // calling super will call createFormController in turn
        super.onCreate(savedInstanceState);
    }

    /**
     * This method gets called by the form building background task after it finishes.
     */
    @Override
    public void displayForm() {
        super.displayForm();

        // now form has been built, add next or save button to it
        ViewGroup containerView = (ViewGroup) findViewById(R.id.form_elements_container);

        // reference to this, for use in button actions (validation task makes weak ref)
        final RecordFormActivity thisActivity = this;

        // put buttons in a relative layout for positioning on right or left
        RelativeLayout buttonBar = new RelativeLayout(this);
        buttonBar.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));

        // add 'previous' button
        if (sectionId > 0) {
            havePrevious = true;
            Button backBtn = new Button(this);
            RelativeLayout.LayoutParams backBtnLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            backBtnLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            backBtn.setLayoutParams(backBtnLayoutParams);

            backBtn.setId(R.id.record_back_button_id);
            backBtn.setText(getText(R.string.record_previous_button));
            buttonBar.addView(backBtn);

            backBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("RecordFormActivity", "Back button clicked");

                    // set this to let callback know next action to take
                    goPrevious = true;
                    new ValidationTask(thisActivity).execute();
                }
            });
        } else {
            havePrevious = false;
            goPrevious = false;
        }

        // add next/save button
        Button goBtn = new Button(this);
        RelativeLayout.LayoutParams goBtnLayoutParams = new RelativeLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT);
        goBtnLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        goBtn.setLayoutParams(goBtnLayoutParams);

        goBtn.setId(R.id.record_save_button_id);

        if (sectionId < sectionOrder.length - 1) {
            // add 'next' button
            haveNext = true;
            goBtn.setText(getString(R.string.record_next_button));

        } else {
            haveNext = false;
            // add 'save' button
            goBtn.setText(getString(R.string.record_save_button));
        }

        buttonBar.addView(goBtn);

        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("RecordFormActivity", "Next/save button clicked");
                goPrevious = false;
                new ValidationTask(thisActivity).execute();
            }
        });

        containerView.addView(buttonBar);
    }

    @Override
    public void validationComplete(boolean isValid) {
        Log.d("RecordFormActivity", "Valid? " + String.valueOf(isValid));

        if (isValid) {
            proceed();
        } else {
            // validation errors found in section
            // TODO: show warning dialog with options to proceed or stay to fix errors

        }

    }

    private void proceed() {
        int goToSectionId = sectionId;

        if (goPrevious) {
            if (!havePrevious) {
                Log.e("RecordFormActivity", "Trying to go to previous, but there is none!");
                return;
            } else {
                goToSectionId--;
            }
        } else if (haveNext) {
            Log.d("RecordFormActivity", "Proceed to next section now");
            goToSectionId++;
        } else {
            // at end; save
            // TODO: now what
            Log.d("RecordFormActivity", "Form complete! Now what?");
            ////////////////////////
            return;
        }

        Log.d("RecordFormActivity", "Going to section #" + String.valueOf(goToSectionId));

        Intent intent = new Intent(this, RecordFormActivity.class);
        intent.putExtra(RecordFormActivity.SECTION_ID, goToSectionId);
        startActivity(intent);
    }

    @Override
    public FormController createFormController() {
        // pass section offset to activity in intent, then find that section to use here
        try {
            sectionName = sectionOrder[sectionId];
            Field sectionField = DriverSchema.class.getField(sectionName);

            // TODO: check if multiple, and if so, get list of things instead of the thing
            Object section = sectionField.get(currentlyEditing);

            // will not exist if creating a new record
            if (section == null) {
                Log.d("RecordFormActivity", "No section found with name " + sectionName);
                // TODO: instantiate a new thing then
                try {
                    sectionClass = Class.forName(MODEL_PACKAGE + sectionName);
                    section = sectionClass.newInstance();
                    return new FormController(this, section);
                } catch (ClassNotFoundException e) {
                    Log.e("RecordFormActivity", "Could not fine class named " + sectionName);
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    Log.e("RecordFormActivity", "Failed to instantiate new section " + sectionName);
                    e.printStackTrace();
                }
            } else {
                // have existing values to edit
                Log.d("RecordFormActivity", "Found existing section " + sectionName);
                return new FormController(this, section);
            }
        } catch (NoSuchFieldException e) {
            Log.e("RecordFormActivity", "Could not find section field " + sectionName);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e("RecordFormActivity", "Do not have access to section field " + sectionName);
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            Log.e("RecordFormActivity", "Invalid form section offset: " + String.valueOf(sectionId));
        }
        return null;
    }

    @Override
    public void initForm() {
        final FormController formController = getFormController();
        if (sectionClass != null) {
            formController.addSection(addSectionModel(sectionClass));
        } else {
            // TODO: getting here if have list of things for section
            Log.e("RecordFormActivity", "No section class; cannot initialize form");
        }
    }

    private FormSectionController addSectionModel(Class sectionModel) {
        // TODO: section label would come from parent model
        FormSectionController section = new FormSectionController(this, sectionModel.getSimpleName());
        Field[] fields = sectionModel.getDeclaredFields();

        // TODO: read/respect JsonPropertyOrder annotation, if present

        HashMap<String, Class> enums = new HashMap<>();

        // find enums for select lists
        Class[] classes = sectionModel.getDeclaredClasses();
        for (Class clazz : classes) {
            if (clazz.isEnum()) {
                Log.d("RecordFormActivity", "Found enum named " + clazz.getSimpleName());
                enums.put(clazz.getSimpleName(), clazz);
            }
        }

        field_loop:
        for (Field field: fields) {
            String fieldName = field.getName();
            String fieldLabel = fieldName;
            FieldTypes fieldType = null;
            boolean isRequired = false;

            Annotation[] annotations = field.getDeclaredAnnotations();

            for (Annotation annotation: annotations) {

                Class annotationType = annotation.annotationType();
                Log.d("RecordFormActivity", fieldName + " has annotation " + annotationType.getName());

                if (annotationType.equals(IsHidden.class)) {
                    IsHidden isHidden = (IsHidden) annotation;
                    if (isHidden.value()) {
                        Log.d("RecordFormActivity", "Skipping hidden field " + fieldName);
                        continue field_loop;
                    } else {
                        Log.w("RecordFormActivity", "Have false isHidden annotation, which is inefficient. Better just leave it off.");
                    }
                } else if (annotationType.equals(FieldType.class)) {
                    Log.d("RecordFormActivity", "Found a field type annotation");
                    FieldType fieldTypeAnnotation = (FieldType) annotation;
                    fieldType = fieldTypeAnnotation.value();
                } else if (annotationType.equals(SerializedName.class)) {
                    SerializedName serializedName = (SerializedName) annotation;
                    fieldLabel = serializedName.value();
                } else if (annotationType.equals(NotNull.class)) {
                    isRequired = true;
                }
            }

            if (fieldType == null) {
                // TODO: in this case, it's probably a subsection
                Log.e("RecordFormActivity", "No field type found for field " + fieldName);
                continue;
            }

            switch (fieldType) {
                case image:
                    Log.w("RecordFormActivity", "TODO: implement image field type");
                    break;
                case selectlist:
                    Log.d("RecordFormActivity", "Going to add select field");
                    // find enum with the options in it
                    Class enumClass = enums.get(fieldName);

                    if (enumClass == null) {
                        Log.e("RecordFormActivity", "No enum class found for field named " + fieldName);
                        continue;
                    }

                    Field[] enumFields = enumClass.getFields();

                    ArrayList<String> enumLabels = new ArrayList<>(enumFields.length);
                    for (Field enumField: enumFields) {
                        if (enumField.isEnumConstant()) {
                            String enumLabel = (enumField.getAnnotation(SerializedName.class)).value();
                            enumLabels.add(enumLabel);
                        }
                    }

                    ArrayList<Object> enumValueObjectList = new ArrayList<>(Arrays.asList(enumClass.getEnumConstants()));

                    // TODO: no matter what gets passed for the prompt argument, it seems to always display "Select"
                    section.addElement(new SelectionController(this, fieldName, fieldLabel, isRequired, "Select", enumLabels, enumValueObjectList));

                    break;
                case text:
                    section.addElement(new EditTextController(this, fieldName, fieldLabel, fieldLabel));
                    break;
                case reference:
                    Log.w("RecordFormActivity", "TODO: implement reference field type");
                    break;
                default:
                    Log.e("RecordFormActivity", "Don't know what to do with field type " + fieldType.toString());
                    break;
            }
        }

        return section;
    }
}
