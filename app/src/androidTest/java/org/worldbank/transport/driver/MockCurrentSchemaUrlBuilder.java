package org.worldbank.transport.driver;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.worldbank.transport.driver.tasks.CheckSchemaTask;

import java.net.URL;

/**
 * Use mock server for testing current schema check.
 *
 * Created by kathrynkillebrew on 1/28/16.
 */
public class MockCurrentSchemaUrlBuilder implements CheckSchemaTask.CurrentSchemaUrl {
    private MockWebServer server;

    public MockCurrentSchemaUrlBuilder(MockWebServer server) {
        this.server = server;
    }
    @Override
    public URL currentSchemaUrl(String serverUrl, String recordTypeLabel) {
        HttpUrl httpUrl = server.url("/mockcheckschema");
        return httpUrl.url();
    }
}
