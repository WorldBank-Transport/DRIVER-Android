package org.worldbank.transport.driver.utilities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.lang.StringUtils;
import org.jsonschema2pojo.annotations.Multiple;
import org.jsonschema2pojo.annotations.PluralTitle;
import org.jsonschema2pojo.annotations.Title;
import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.activities.RecordFormActivity;
import org.worldbank.transport.driver.activities.RecordFormConstantsActivity;
import org.worldbank.transport.driver.activities.RecordFormSectionActivity;
import org.worldbank.transport.driver.activities.RecordItemListActivity;
import org.worldbank.transport.driver.models.DriverSchema;
import org.worldbank.transport.driver.staticmodels.DriverApp;

import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

/**
 * Static methods to support moving around in paginated form sections.
 *
 * Each section is one field on the DriverSchema class.
 * Their order is determined by the JsonPropertyOrder annotation on the DriverSchema class.
 *
 * Created by kathrynkillebrew on 1/4/16.
 */
public class RecordFormSectionManager {

    private static final String LOG_LABEL = "FormSectionManager";

    private static String[] schemaSectionOrder;

    // path to model classes created by jsonschema2pojo
    // this must match the targetPackage declared in the gradle build file (with a trailing period)
    private static final String MODEL_PACKAGE = "org.worldbank.transport.driver.models.";

    /**
     * Get the appropriate Activity to launch for a given form section.
     *
     * @param sectionId Offset of section to check within ordered list of DriverSchema fields
     * @return Either a form activity class, or form item list class if section has multiple items.
     */
    public static Class getActivityClassForSection(int sectionId) {
        Log.d(LOG_LABEL, "Going to section #" + sectionId);

        if (sectionId < 0) {
            return RecordFormConstantsActivity.class;
        }

        if (sectionHasMultiple(sectionId)) {
            return RecordItemListActivity.class;
        }

        return RecordFormSectionActivity.class;
    }

    /**
     * Get the order in which the form sections should appear.
     *
     * Lazily builds schemaSectionOrder on first reference;
     * use this method instead of referencing the schemaSectionOrder field directly.
     *
     * @return Array of ordered field names
     */
    private static String[] getSchemaSectionOrder() {
        if (schemaSectionOrder == null) {
            schemaSectionOrder = DriverUtilities.getFieldOrder(DriverSchema.class);
        }
        return schemaSectionOrder;
    }

    /**
     * Get the name of a DriverSchema section field.
     *
     * @param sectionId Offset of a section within the ordered list of DriverSchema fields
     * @return Name of the section's field name on the DriverSchema class.
     */
    @Nullable
    public static String getSectionName(int sectionId) {
        try {
            return getSchemaSectionOrder()[sectionId];
        } catch (IndexOutOfBoundsException e) {
            Log.e(LOG_LABEL, "Invalid form section offset: " + String.valueOf(sectionId));
        }
        return null;
    }

    /**
     * Find the class of a section. For a section holding multiple items, it will be the class
     * of the items within its ArrayList.
     *
     * @param sectionName Field name of the section on the DriverSchema class
     * @return The class of the section, if found
     */
    @Nullable
    public static Class getSectionClass(String sectionName) {
        try {
            // class names are capitalized; field names of that type may not be
            sectionName = StringUtils.capitalize(sectionName);
            return Class.forName(MODEL_PACKAGE + sectionName);
        } catch (ClassNotFoundException e) {
            Log.e(LOG_LABEL, "Could not fine class named " + sectionName);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Check if there is another form section after the one for the given section index.
     * A helper to determine whether to show 'next' button on form, or 'save' at the end.
     *
     * @param sectionId Offset of current section within the ordered list of DriverSchema fields
     * @return True if there are sections after the current section
     */
    public static boolean sectionHasNext(int sectionId) {
        return sectionId < getSchemaSectionOrder().length - 1;
    }

    /**
     * Check if section contains a list of items, using 'Multiple' annotation set by
     * jsonschema2pojo, based on the schema, which should have the 'multiple' property set
     * for all sections.
     *
     * @param sectionId Offset of section to check within ordered list of DriverSchema fields
     * @return True if section field has Multiple annotation set to "true"
     */
    public static boolean sectionHasMultiple(int sectionId) {

        Field sectionField = getFieldForSectionName(getSectionName(sectionId));

        if (sectionField != null) {
            Multiple multipleAnnotation = sectionField.getAnnotation(Multiple.class);
            if (multipleAnnotation != null && multipleAnnotation.value()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the field from the DriverSchema class for a given section field name.
     *
     * @param sectionName Field name of section on DriverSchema class
     * @return Field from DriverSchema class
     */
    @Nullable
    public static Field getFieldForSectionName(String sectionName) {
        try {
            return DriverSchema.class.getField(sectionName);

        } catch (NoSuchFieldException e) {
            Log.e(LOG_LABEL, "Could not find section field named " + sectionName);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get plural title for section (should exist for all fields that hold multiples).
     *
     * @param sectionField Field on DriverSchema class containing a collection
     * @param defaultTitle Descriptive string to use if PluralTitle annotation not found
     * @return String plural title from 'PluralTitle' field annotation, or default
     */
    public static String getPluralTitle(Field sectionField, String defaultTitle) {

        if (sectionField == null) {
            Log.e(LOG_LABEL, "Cannot find plural label for unknown field");
            return defaultTitle;
        }

        PluralTitle pluralAnnotation = sectionField.getAnnotation(PluralTitle.class);
        if (pluralAnnotation != null) {
            String pluralTitle = pluralAnnotation.value();
            if (pluralTitle.length() > 0) {
                return pluralTitle;
            } else {
                Log.w(LOG_LABEL, "No plural title found for section");
            }
        } else {
            Log.w(LOG_LABEL, "No plural title found for section");
        }

        return defaultTitle;
    }

    /**
     * Get section title (singular)
     *
     * @param sectionField Field of a section on DriverSchema to check
     * @param defaultTitle String to return if Title annotation not found on section
     * @return Title annotation value for field, or default if not ofund
     */
    public static String getSingleTitle(Field sectionField, String defaultTitle) {
        Title titleAnnotation = sectionField.getAnnotation(Title.class);
        if (titleAnnotation != null) {
            String title = titleAnnotation.value();
            if (title.length() > 0) {
                return title;
            } else {
                Log.w(LOG_LABEL, "No title found for section");
            }
        } else {
            Log.w(LOG_LABEL, "No title found for section");
        }

        return defaultTitle;
    }

    /**
     * Get the currently editing object for a given section, or create it if adding a record.
     *
     * @param sectionField Field for the section on the DriverSchema class
     * @param sectionClass Class of the field
     * @param currentlyEditing DriverSchema object currently editing, as managed by the app singleton
     * @return Section object, ready for use by form
     */
    @Nullable
    public static Object getOrCreateSectionObject(Field sectionField, Class sectionClass, Object currentlyEditing) {
        // attempt to get the section from the currently editing model object;
        // it will not exist if creating a new record

        try {
            Object section = sectionField.get(currentlyEditing);

            if (section == null) {
                Log.d(LOG_LABEL, "No section found for field " + sectionField.getName());
                // instantiate a new thing then
                section = sectionClass.newInstance();

                // add the new section to the currently editing model object
                sectionField.set(currentlyEditing, section);

                if (sectionField.get(currentlyEditing) == null) {
                    Log.e(LOG_LABEL, "Section field is still null after set to new instance!");
                } else {
                    Log.d(LOG_LABEL, "Section field successfully set to new instance");
                    return section;
                }
            } else {
                // have existing values to edit
                Log.d(LOG_LABEL, "Found existing section " + sectionField.getName());
                return section;
            }
        } catch (InstantiationException e) {
            Log.e(LOG_LABEL, "Failed to instantiate new section " + sectionField.getName());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(LOG_LABEL, "Do not have access to section field " + sectionField.getName());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get an item from a section with multiple items, or create a new item and add it to the section.
     *
     * @param sectionField Field on DriverSchema object for multiple items
     * @param sectionClass Class of the items within the field's collection
     * @param currentlyEditing DriverSchema object containing sectionField with list to use
     * @param index Offset of the item to get or create on the collection
     * @return Item of type sectionClass from collection on sectionField if found or added
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static Object getOrCreateListItem(Field sectionField, Class sectionClass, Object currentlyEditing, int index) {
        Object section = RecordFormSectionManager.getOrCreateSectionObject(sectionField, sectionClass, currentlyEditing);
        ArrayList items = getSectionList(section);

        if (items.size() > index) {
            Log.d(LOG_LABEL, "Returning existing list item at index " + index);
            return items.get(index);
        }

        try {
            if (items.size() == 0) {
                Log.d(LOG_LABEL, "Created new list for section " + sectionField.getName());
                sectionField.set(currentlyEditing, items);
            }
            Object item = sectionClass.newInstance();
            items.add(index, item);
            Log.d(LOG_LABEL, "Item added to section " + sectionField.getName());
            return item;
        } catch (InstantiationException e) {
            Log.e(LOG_LABEL, "Failed to instantiate new list item " + sectionField.getName());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(LOG_LABEL, "Do not have access to section list field " + sectionField.getName());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Delete an item from a record section containing multiple items.
     *
     * @param sectionField Field on DriverSchema class containing multiple items
     * @param sectionClass Class of the items within the section collection
     * @param currentlyEditing DriverSchema object from which to delete the item
     * @param index Offset of the item to delete from the collection
     * @return True if deleted successfully
     */
    public static boolean deleteListItem(Field sectionField, Class sectionClass, Object currentlyEditing, int index) {
        Object section = RecordFormSectionManager.getOrCreateSectionObject(sectionField, sectionClass, currentlyEditing);
        ArrayList items = getSectionList(section);

        if (items.size() <= index) {
            Log.e(LOG_LABEL, "Cannot delete item from " + sectionField.getName() + " at index " + index + "; index out of range");
            return false;
        }

        Object item = items.remove(index);
        if (item == null) {
            Log.e(LOG_LABEL, "Failed to delete from " + sectionField.getName() + " at index " + index);
            return false;
        } else {
            Log.d(LOG_LABEL, "Successfully deleted item from " + sectionField.getName() + " at index " + index);
            return true;
        }
    }

    /**
     * Get the collection of items from a section containing multiple items.
     *
     * @param section The object from a DriverSchema object that is a section for multiple items.
     * @return The ArrayList of items, if found successfully, or a new ArrayList if not.
     */
    public static ArrayList getSectionList(Object section) {
        Log.d(LOG_LABEL, "Getting section list");

        if (ArrayList.class.isInstance(section)) {
            return (ArrayList) section;
        } else {
            Log.e(LOG_LABEL, "Section has unexpected type; expected an ArrayList for objects with multiple annotation");
        }

        return new ArrayList();
    }

    public static void checkUnsavedChangesBeforeExit(DriverApp app, AppCompatActivity activity) {
        Log.w(LOG_LABEL, "Prompt user to save any changes before exiting");
        WarnUnsavedChangesDialog dialog = new WarnUnsavedChangesDialog();
        dialog.show(activity.getSupportFragmentManager(), "unsavedchangeswarning");
    }

    public static void saveAndExit(DriverApp app, AppCompatActivity activity) {
        Log.d(LOG_LABEL, "Going to save and exit");
        // check if location has been set first
        if (app.isLocationMissing()) {
            Log.w(LOG_LABEL, "Location is missing! Warn user.");
            // show warning dialog to ask user to continue to wait for a location reading
            if (LocationServiceManager.isRunning() &&
                    LocationServiceManager.getCurrentStatus() == LocationServiceManager.Status.GETTING_LOCATIONS) {
                // warn user that location is refining
                WarnLocationStillUpdatingDialog dialog = new WarnLocationStillUpdatingDialog();
                dialog.show(activity.getSupportFragmentManager(), "refininglocationwarning");
            } else {
                // warn user there has been no location reading at all
                WarnLocationNotSetDialog dialog = new WarnLocationNotSetDialog();
                dialog.show(activity.getSupportFragmentManager(), "nolocationwarning");
            }
        } else {
            // location is set; go ahead and save
            Log.d(LOG_LABEL, "Have location; save and exit");
            Log.d(LOG_LABEL, "location is: " + app.getEditConstants().location.toString());
            saveAndExitWithoutWarnings(app, activity);
        }
    }

    /**
     * Internal helper to save record. Should only be called via {@link #saveAndExit(DriverApp, AppCompatActivity)},
     * which performs checks first.
     */
    private static void saveAndExitWithoutWarnings(DriverApp app, AppCompatActivity activity) {
        if (app.saveRecordAndClearCurrentlyEditing()) {
            Toast toast = Toast.makeText(activity, activity.getString(R.string.record_save_success), Toast.LENGTH_SHORT);
            toast.show();
            NavUtils.navigateUpFromSameTask(activity);
        } else {
            Toast toast = Toast.makeText(activity, activity.getString(R.string.record_save_failure), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public static class WarnLocationNotSetDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.location_not_set_dialog_title)
                    .setMessage(R.string.location_not_set_dialog_warning)
                    .setPositiveButton(R.string.confirm_action, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // user willing to wait for GPS reading; show toast message
                            // with current location reading status
                            int status = LocationServiceManager.getCurrentStatus();
                            Toast toast = Toast.makeText(getContext(), getString(status), Toast.LENGTH_LONG);
                            toast.show();
                        }
                    })
                    .setNegativeButton(R.string.stop_action, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User does not want to wait for a GPS reading; exit.
                            LocationServiceManager.stopService();
                            AppCompatActivity activity = (AppCompatActivity)getActivity();
                            DriverApp app = (DriverApp)activity.getApplication();
                            saveAndExitWithoutWarnings(app, activity);
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    public static class WarnLocationStillUpdatingDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.location_settling_dialog_title)
                    .setMessage(R.string.location_settling_dialog_warning)
                    .setPositiveButton(R.string.confirm_action, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // user willing to wait for location reading to refine; show toast message
                            // with current location reading status
                            int status = LocationServiceManager.getCurrentStatus();
                            Toast toast = Toast.makeText(getContext(), getString(status), Toast.LENGTH_LONG);
                            toast.show();
                        }
                    })
                    .setNegativeButton(R.string.stop_action, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User does not want to wait for a GPS reading; exit.
                            // Use the most recent reading with the highest accuracy.
                            LocationServiceManager.stopService();
                            AppCompatActivity activity = (AppCompatActivity)getActivity();
                            DriverApp app = (DriverApp)activity.getApplication();
                            saveAndExitWithoutWarnings(app, activity);
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    /**
     * Prompt user to save or lose unsaved changes to currently editing record before exiting.
     * Validation should be run before opening this dialog.
     */
    public static class WarnUnsavedChangesDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.record_unsaved_changes_dialog_title)
                    .setMessage(R.string.record_unsaved_changes_dialog_message)
                    .setPositiveButton(R.string.confirm_action, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            AppCompatActivity activity = (AppCompatActivity)getActivity();
                            DriverApp app = (DriverApp)activity.getApplication();
                            saveAndExit(app, activity);
                        }
                    })
                    .setNegativeButton(R.string.stop_action, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // exit without saving changes
                            NavUtils.navigateUpFromSameTask(getActivity());
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

}
