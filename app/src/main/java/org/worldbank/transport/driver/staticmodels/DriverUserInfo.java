package org.worldbank.transport.driver.staticmodels;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.worldbank.transport.driver.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by kathrynkillebrew on 12/8/15.
 */
public class DriverUserInfo {

    public static final String ADMIN_GROUP = "admin";
    public static final String ANALYST_GROUP = "analyst";

    public int id;
    public String username;
    public String email;
    public List<String> groups;

    private String token; // must be written from DriverUserAuth response

    /* Determine whether user has access to add new records or not.
     *
     * @return True if user has access to write records.
     */
    public boolean hasWritePermission() {
        return groups != null && (groups.contains(ADMIN_GROUP) || groups.contains(ANALYST_GROUP));
    }

    /* Set authentication token for this user.
     *
     * @param auth Authentication information for this user
     * @return True if token set successfully
     */
    public boolean setUserToken(DriverUserAuth auth) {
        if (auth == null || auth.token.isEmpty()) {
            Log.e("DriverUserInfo", "Missing authentication info for user; cannot set");
            return false;
        }

        if (auth.user != id) {
            Log.e("DriverUserInfo", "Got authentication info for different user ID; cannot set");
            return false;
        }

        token = auth.token;
        Log.d("DriverUserInfo", "Authentication info set for user");
        return true;
    }

    public String getUserToken() {
        return token;
    }

    public void writeToSharedPreferences(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.shared_preferences_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        // keep stored schema version (all other keys are for user info)
        String currentStoredSchema = preferences.getString(context.getString(R.string.shared_preferences_schema_version), "");
        editor.clear(); // clears last saved user, if there is one

        // set back stored schema version, if any, after clearing out the app shared preferences
        if (!currentStoredSchema.isEmpty()) {
            editor.putString(context.getString(R.string.shared_preferences_schema_version), currentStoredSchema);
        }

        editor.putInt(context.getString(R.string.shared_preferences_user_id_key), id);
        editor.putString(context.getString(R.string.shared_preferences_username_key), username);
        editor.putString(context.getString(R.string.shared_preferences_token_key), token);
        editor.putString(context.getString(R.string.shared_preferences_email_key), email);

        Set<String> groupSet = new HashSet<>(groups.size());
        groupSet.addAll(groups);

        editor.putStringSet(context.getString(R.string.shared_preferences_groups_key), groupSet);

        editor.apply();
        Log.d("DriverUserInfo", "User info written to shared preferences");
    }

    public void readFromSharedPreferences(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.shared_preferences_file), Context.MODE_PRIVATE);

        id = preferences.getInt(context.getString(R.string.shared_preferences_user_id_key), -1);
        username = preferences.getString(context.getString(R.string.shared_preferences_username_key), "");
        token = preferences.getString(context.getString(R.string.shared_preferences_token_key), "");
        email = preferences.getString(context.getString(R.string.shared_preferences_email_key), "");

        Set<String> groupSet = preferences.getStringSet(
                context.getString(R.string.shared_preferences_groups_key), new HashSet<String>(0));
        groups = new ArrayList<>(groupSet.size());
        groups.addAll(groupSet);

        Log.d("DriverUserInfo", "User info read in from shared preferences");
    }
}

