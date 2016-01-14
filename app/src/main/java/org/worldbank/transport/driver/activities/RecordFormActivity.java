package org.worldbank.transport.driver.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.azavea.androidvalidatedforms.FormController;
import com.azavea.androidvalidatedforms.FormWithAppCompatActivity;
import com.azavea.androidvalidatedforms.controllers.DatePickerController;
import com.azavea.androidvalidatedforms.controllers.EditTextController;
import com.azavea.androidvalidatedforms.controllers.FormSectionController;
import com.azavea.androidvalidatedforms.controllers.LabeledFieldController;
import com.azavea.androidvalidatedforms.controllers.SelectionController;

import org.jsonschema2pojo.annotations.FieldType;
import org.jsonschema2pojo.annotations.FieldTypes;
import org.jsonschema2pojo.annotations.IsHidden;

import com.google.gson.annotations.SerializedName;
import javax.validation.constraints.NotNull;

import org.jsonschema2pojo.annotations.WatchTarget;
import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.annotations.ConstantFieldType;
import org.worldbank.transport.driver.annotations.ConstantFieldTypes;
import org.worldbank.transport.driver.models.DriverSchema;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.utilities.DriverUtilities;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Base class for creating dynamic forms for DriverSchema sections.
 *
 * Created by kathrynkillebrew on 12/9/15.
 */
public abstract class RecordFormActivity extends FormWithAppCompatActivity {

    public interface FormReadyListener {
        void formReadyCallback();
    }

    public static final String SECTION_ID = "driver_section_id";
    private static final String LOG_LABEL = "RecordFormActivity";

    // TODO: modify DatePickerController in form generator library:
    // take the DateFormat super class (to avoid cast) and add constructor to make date format optional
    // (use system date format as default, as here)
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();

    protected DriverAppContext mAppContext;
    protected DriverApp app;

    // flag that is true once form has been displayed
    private boolean formReady = false;
    private FormReadyListener formReadyListener;

    protected DriverSchema currentlyEditing;

    protected int sectionId;

    protected Class sectionClass;
    protected String sectionLabel;
    protected Field sectionField;

    // have next/previous sections
    protected boolean haveNext = false;
    protected boolean havePrevious = false;

    // if selected action is to go to previous (if false, go to next or save)
    protected boolean goPrevious = false;

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

        // calling super will call createFormController in turn
        super.onCreate(savedInstanceState);
    }

    protected void saveAndExit() {
        if (app.saveRecordAndClearCurrentlyEditing()) {
            Toast toast = Toast.makeText(this, getString(R.string.record_save_success), Toast.LENGTH_SHORT);
            toast.show();
            NavUtils.navigateUpFromSameTask(this);
        } else {
            Toast toast = Toast.makeText(this, getString(R.string.record_save_failure), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /**
     * This method gets called by the form building background task after it finishes.
     */
    @Override
    public void displayForm() {
        super.displayForm();

        // now form has been built, add previous, next, and/or save buttons to it
        ViewGroup containerView = (ViewGroup) findViewById(R.id.form_elements_container);
        RelativeLayout buttonBar = buildButtonBar();
        containerView.addView(buttonBar);

        // alert that form is ready to go
        formReady = true;
        if (formReadyListener != null) {
            formReadyListener.formReadyCallback();
        }
    }

    /**
     * Helper to build a layout with buttons (previous/next/save/cancel).
     *
     * @return View with buttons with handlers added to it
     */
    public abstract RelativeLayout buildButtonBar();

    public boolean isFormReady() {
        return formReady;
    }

    public void setFormReadyListener(FormReadyListener listener) {
        formReadyListener = listener;
    }

    @Override
    public void validationComplete(boolean isValid) {
        Log.d(LOG_LABEL, "Valid? " + String.valueOf(isValid));

        if (isValid) {
            proceed();
        } else {
            Log.w(LOG_LABEL, "TODO: handle validation errors");
            // validation errors found in section
            // TODO: show warning dialog with options to proceed or stay to fix errors?
        }
    }

    public abstract void proceed();

    protected abstract Object getModelObject();

    @Override
    public FormController createFormController() {

        Log.d(LOG_LABEL, "createFormController called");

        String sectionName = RecordFormSectionManager.getSectionName(sectionId);

        // section offset was passed to activity in intent; find section to use here
        sectionField = RecordFormSectionManager.getFieldForSectionName(sectionName);

        if (sectionField == null) {
            Log.e(LOG_LABEL, "Section field named " + sectionName + " not found.");
            return null;
        }

        Log.d(LOG_LABEL, "Found sectionField " + sectionField.getName());
        sectionClass = RecordFormSectionManager.getSectionClass(sectionName);

        if (sectionClass == null) {
            Log.e(LOG_LABEL, "No section class; cannot initialize form");
            return null;
        }

        Object section = getModelObject();

        // use singular title for form section label
        // TODO: also use 'Description' annotation somewhere?
        sectionLabel = sectionClass.getSimpleName(); // default
        sectionLabel = RecordFormSectionManager.getSingleTitle(sectionField, sectionLabel);

        if (section != null) {
            return new FormController(this, section);
        } else {
            Log.e(LOG_LABEL, "Section object not found for " + sectionName);
        }

        return null;
    }

    @Override
    public void initForm() {
        final FormController formController = getFormController();
        formController.addSection(addSectionModel());
    }

    private FormSectionController addSectionModel() {
        FormSectionController section = new FormSectionController(this, sectionLabel);
        Field[] fields = sectionClass.getDeclaredFields();

        HashMap<String, Class> enums = new HashMap<>();

        // find enums for select lists
        Class[] classes = sectionClass.getDeclaredClasses();
        for (Class clazz : classes) {
            if (clazz.isEnum()) {
                Log.d(LOG_LABEL, "Found enum named " + clazz.getSimpleName());
                enums.put(clazz.getSimpleName(), clazz);
            }
        }

        // map of field names to their form controls
        HashMap<String, LabeledFieldController> fieldControls = new HashMap<>(fields.length);

        field_loop:
        for (Field field: fields) {
            LabeledFieldController control = null;
            String fieldName = field.getName();
            String fieldLabel = fieldName;
            FieldTypes fieldType = null;
            boolean isRequired = false;
            ConstantFieldTypes constantFieldType = null;
            String watchTarget = null;

            Annotation[] annotations = field.getDeclaredAnnotations();

            for (Annotation annotation: annotations) {

                Class annotationType = annotation.annotationType();
                Log.d(LOG_LABEL, fieldName + " has annotation " + annotationType.getName());

                if (annotationType.equals(IsHidden.class)) {
                    IsHidden isHidden = (IsHidden) annotation;
                    if (isHidden.value()) {
                        Log.d(LOG_LABEL, "Skipping hidden field " + fieldName);
                        continue field_loop;
                    } else {
                        Log.w(LOG_LABEL, "Have false isHidden annotation, which is inefficient. Better just leave it off.");
                    }
                } else if (annotationType.equals(FieldType.class)) {
                    Log.d(LOG_LABEL, "Found a field type annotation");
                    FieldType fieldTypeAnnotation = (FieldType) annotation;
                    fieldType = fieldTypeAnnotation.value();
                } else if (annotationType.equals(SerializedName.class)) {
                    SerializedName serializedName = (SerializedName) annotation;
                    fieldLabel = serializedName.value();
                } else if (annotationType.equals(NotNull.class)) {
                    isRequired = true;
                } else if (annotationType.equals(ConstantFieldType.class)) {
                    Log.d(LOG_LABEL, "Found a constant field type annotation");
                    ConstantFieldType constantFieldTypeAnnotation = (ConstantFieldType) annotation;
                    constantFieldType = constantFieldTypeAnnotation.value();
                } else if (annotationType.equals(WatchTarget.class)) {
                    WatchTarget watchTargetAnnotation = (WatchTarget) annotation;
                    watchTarget = watchTargetAnnotation.value();
                }
            }

            if (fieldType != null) {
                // have a jsonschema2pojo field type
                switch (fieldType) {
                    case image:
                        Log.w(LOG_LABEL, "TODO: implement image field type");
                        break;
                    case selectlist:
                        Log.d(LOG_LABEL, "Going to add select field");
                        // find enum with the options in it
                        String enumFieldName = field.getType().getSimpleName();
                        Log.d(LOG_LABEL, "Using enum field name " + enumFieldName);
                        Class enumClass = enums.get(enumFieldName);

                        if (enumClass == null) {
                            Log.e(LOG_LABEL, "No enum class found for field named " + fieldName);
                            continue;
                        }

                        SelectListInfo enumListInfo = buildSelectEnumInfo(enumClass);

                        // TODO: fix or remove prompt arg in form builder library
                        // no matter what gets passed for the prompt argument, it seems to always display "Select"
                        control = new SelectionController(this, fieldName, fieldLabel, isRequired, "Select",
                                enumListInfo.labels, enumListInfo.items);
                        break;
                    case text:
                        control = new EditTextController(this, fieldName, fieldLabel);
                        break;
                    case reference:
                        if (watchTarget == null) {
                            Log.e(LOG_LABEL, "Found a reference field without a watch target! Cannot use field " + fieldName);
                            continue;
                        }
                        Log.d(LOG_LABEL, "Found reference field type pointing to " + watchTarget);
                        SelectListInfo refSelectInfo = buildReferencedFieldInfo(watchTarget);
                        control = new SelectionController(this, fieldName, fieldLabel, isRequired, "Select",
                                refSelectInfo.labels, refSelectInfo.items);

                        break;
                    default:
                        Log.e(LOG_LABEL, "Don't know what to do with field type " + fieldType.toString());
                        break;
                }
            } else {
                if (constantFieldType == null) {
                    // TODO: in this case, it's probably a subsection. Does that ever happen?
                    Log.e(LOG_LABEL, "No field type found for field " + fieldName);
                    continue;
                } else {
                    // have a constant field type
                    switch (constantFieldType) {
                        case date:
                            Log.d(LOG_LABEL, "Going to add date control to constant field");
                            control = new DatePickerController(this, fieldName, fieldLabel, isRequired, DEFAULT_DATE_FORMAT, true);
                            break;
                        case location:
                            // TODO: implement
                            Log.w(LOG_LABEL, "Found location field type. TODO: implement");
                            break;
                        default:
                            Log.e(LOG_LABEL, "Unrecognized constant field type " + constantFieldType);
                            break;
                    }
                }
            }

            if (control != null) {
                fieldControls.put(fieldName, control);
            }
        }

        // read/respect JsonPropertyOrder annotation of fields, if present
        String[] orderedFields = DriverUtilities.getFieldOrder(sectionClass);

        // add form controls in order
        for (String nextField : orderedFields) {
            LabeledFieldController control = fieldControls.get(nextField);
            if (control != null) {
                section.addElement(control);
            } else {
                Log.w(LOG_LABEL, "No control found for ordered field " + nextField);
            }
        }

        return section;
    }

    /**
     * Structure to hold labels and items for a select controller.
     * Order of items in both collections should match.
     */
    private class SelectListInfo {
        public final ArrayList<String> labels;
        public final ArrayList<Object> items;

        public SelectListInfo(ArrayList<String> labels, ArrayList<Object> items) {
            this.labels = labels;
            this.items = items;
        }
    }

    /**
     * Helper to build the labels and items to go in a select control for a field of enums.
     *
     * @param enumClass Enum class containing options to put in the control
     * @return SelectListInfo structure with labels and items to use in select field
     */
    private SelectListInfo buildSelectEnumInfo(Class enumClass) {
        ArrayList<Object> enumValueObjectList = new ArrayList<>(Arrays.asList(enumClass.getEnumConstants()));
        ArrayList<String> enumLabels = new ArrayList<>(enumValueObjectList.size());

        for (Object enumConstant : enumValueObjectList) {
            String prettyLabel = enumConstant.toString();

            Enum myEnum = (Enum) enumConstant;
            Log.d(LOG_LABEL, "found enum #" + myEnum.ordinal() + ": " + myEnum.name());

            // Find the field of the same name as the enum constant, to get its label from
            // the SerializedName annotation.
            try {
                Field enumField = enumClass.getField(myEnum.name());
                SerializedName serializedName = enumField.getAnnotation(SerializedName.class);
                if (serializedName != null) {
                    prettyLabel = serializedName.value();
                }
            } catch (NoSuchFieldException e) {
                Log.e(LOG_LABEL, "Failed to find enum field to build label for " + prettyLabel);
                e.printStackTrace();
            }
            enumLabels.add(prettyLabel);
        }

        return new SelectListInfo(enumLabels, enumValueObjectList);
    }

    /**
     * Helper to build the labels and items to go in a select control for a referenced field.
     *
     * @param watchTarget Name of the referenced field on DriverSchema that holds a collection
     * @return SelectListInfo structure with labels and items to use in select field
     */
    @Nullable
    private SelectListInfo buildReferencedFieldInfo(String watchTarget) {

        Field refField = RecordFormSectionManager.getFieldForSectionName(watchTarget);

        if (refField == null) {
            Log.e(LOG_LABEL, "No field found for ref target " + watchTarget);
            return null;
        }

        Class refClass = RecordFormSectionManager.getSectionClass(watchTarget);

        if (refClass == null) {
            Log.e(LOG_LABEL, "No class found for ref target " + watchTarget);
            return null;
        }

        Object refObj = RecordFormSectionManager.getOrCreateSectionObject(refField, refClass, currentlyEditing);

        if (refObj == null) {
            Log.e(LOG_LABEL, "Referenced watch target object not found/created: " + watchTarget);
            return null;
        }

        String prettyRefLabel = RecordFormSectionManager.getSingleTitle(refField, refField.getName());
        ArrayList refList = RecordFormSectionManager.getSectionList(refObj);
        ArrayList<String> refLabels = DriverUtilities.getListItemLabels(refList, refClass, prettyRefLabel);

        Field refIdField;
        try {
            refIdField = refClass.getField("LocalId");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Log.e(LOG_LABEL, "Failed to find LocalId field on referenced field " + watchTarget);
            return null;
        }

        // String representation of _localId UUIDs is the actual value stored for the reference
        ArrayList<Object> refIDs = new ArrayList<>(refList.size());
        for (Object refItem : refList) {
            try {
                refIDs.add(refIdField.get(refItem));
            } catch (IllegalAccessException e) {
                Log.e(LOG_LABEL, "Failed to get LocalId for item on referenced field " + watchTarget);
                e.printStackTrace();
                return null;
            }
        }

        // sanity check that everything is there and labelled
        if (refIDs.size() != refLabels.size() || refIDs.size() != refList.size()) {
            Log.e(LOG_LABEL, "Unexpected counts creating ref type field " + watchTarget);
            return null;
        }

        return new SelectListInfo(refLabels, refIDs);
    }
}

