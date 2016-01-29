package org.worldbank.transport.driver.utilities;

import android.net.Uri;
import android.util.Log;

import org.worldbank.transport.driver.tasks.PostRecordsTask;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by kathrynkillebrew on 1/28/16.
 */
public class UploadRecordUrlBuilder implements PostRecordsTask.UploadRecordUrl {
    @Override
    public URL recordUrl(String serverUrl) {
        try {
            return new URL(Uri.parse(serverUrl)
                    .buildUpon()
                    .appendEncodedPath(RECORD_ENDPOINT)
                    .build()
                    .toString());
        } catch (MalformedURLException e) {
            Log.e("UploadUrlBuilder", "Bad record upload URL! Check if api_server_url set properly in configurables.xml.");
            e.printStackTrace();
        }

        return null;
    }
}
