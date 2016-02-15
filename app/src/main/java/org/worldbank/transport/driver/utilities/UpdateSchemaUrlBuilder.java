package org.worldbank.transport.driver.utilities;

import android.net.Uri;
import android.util.Log;

import org.worldbank.transport.driver.tasks.UpdateSchemaTask;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Helper to build URL for fetching full schema in order to update app models.
 *
 * Created by kathrynkillebrew on 2/15/16.
 */
public class UpdateSchemaUrlBuilder implements UpdateSchemaTask.RecordSchemaUrl {
    @Override
    public URL schemaUrl(String serverUrl, String recordSchemaEndpoint) {
        try {
            return new URL(Uri.parse(serverUrl)
                    .buildUpon()
                    .appendEncodedPath(RECORDSCHEMA_ENDPOINT)
                    .appendEncodedPath(recordSchemaEndpoint)
                    .build()
                    .toString());
        } catch (MalformedURLException e) {
            Log.e("UpdateSchemaUrl", "Bad schema URL for record schema " + recordSchemaEndpoint +
                    "! Check if api_server_url set properly in configurables.xml.");
            e.printStackTrace();
        }

        return null;
    }
}
