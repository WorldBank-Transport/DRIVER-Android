package org.worldbank.transport.driver.utilities;

import android.net.Uri;
import android.util.Log;

import org.worldbank.transport.driver.tasks.CheckSchemaTask;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Helper to build URL for querying for current schema.
 *
 * Created by kathrynkillebrew on 1/26/16.
 */
public class CheckSchemaUrlBuilder implements CheckSchemaTask.CurrentSchemaUrl {
    @Override
    public URL currentSchemaUrl(String serverUrl, String recordTypeLabel) {

        try {
            return new URL(Uri.parse(serverUrl)
                    .buildUpon()
                    .appendEncodedPath(RECORDTYPE_ENDPOINT)
                    .appendQueryParameter("active", "True") // must be capitalized for DRF
                    .appendQueryParameter("label", recordTypeLabel)
                    .appendEncodedPath("") // ensure trailing slash at end of URL
                    .build()
                    .toString());
        } catch (MalformedURLException e) {
            Log.e("SchemaUrlBuilder", "Bad schema check URL for record type label " + recordTypeLabel +
                    "! Check if api_server_url set properly in configurables.xml.");
            e.printStackTrace();
        }

        return null;
    }
}
