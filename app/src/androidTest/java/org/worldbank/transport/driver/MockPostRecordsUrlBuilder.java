package org.worldbank.transport.driver;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.worldbank.transport.driver.tasks.PostRecordsTask;

import java.net.URL;

/**
 * Created by kathrynkillebrew on 1/28/16.
 */
public class MockPostRecordsUrlBuilder implements PostRecordsTask.UploadRecordUrl {

    private MockWebServer server;

    public MockPostRecordsUrlBuilder(MockWebServer server) {
        this.server = server;
    }

    @Override
    public URL recordUrl(String serverUrl) {
        HttpUrl httpUrl = server.url("/mockrecords");
        return httpUrl.url();
    }
}
