package org.worldbank.transport.driver;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.worldbank.transport.driver.tasks.LoginTask;

import java.net.URL;

/**
 * Provide mocked version of LoginUrlBuilder using MockWebServer, for server responses in testing.
 *
 * Created by kat on 12/19/15.
 */
public class MockLoginUrlBuilder implements LoginTask.LoginUrls {

    private MockWebServer server;

    public MockLoginUrlBuilder(MockWebServer server) {
        this.server = server;
    }
    @Override
    public URL userTokenUrl(String serverUrl, boolean isSso) {
        HttpUrl httpUrl = server.url("/token");
        return httpUrl.url();
    }

    @Override
    public URL userInfoUrl(String serverUrl, int userId) {
        HttpUrl httpUrl = server.url("/user");
        return httpUrl.url();
    }
}
