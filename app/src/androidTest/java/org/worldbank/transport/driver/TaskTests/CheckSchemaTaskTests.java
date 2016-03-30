package org.worldbank.transport.driver.TaskTests;

import android.test.AndroidTestCase;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.mockito.Mockito;
import org.worldbank.transport.driver.MockCurrentSchemaUrlBuilder;
import org.worldbank.transport.driver.activities.RecordListActivity;
import org.worldbank.transport.driver.staticmodels.DriverUserAuth;
import org.worldbank.transport.driver.staticmodels.DriverUserInfo;
import org.worldbank.transport.driver.tasks.CheckSchemaTask;

import java.io.IOException;

/**
 * Unit test CheckSchemaTask handling of schema updates
 *
 * Created by kathrynkillebrew on 1/28/16.
 */
public class CheckSchemaTaskTests extends AndroidTestCase {

    RecordListActivity mockActivity;
    CheckSchemaTask checkSchemaTask;
    MockWebServer server;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mockActivity = Mockito.mock(RecordListActivity.class);
        server = new MockWebServer();

        CheckSchemaTask.CurrentSchemaUrl currentSchemaUrl = new MockCurrentSchemaUrlBuilder(server);
        checkSchemaTask = new CheckSchemaTask(mockActivity, currentSchemaUrl);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mockActivity = null;
        checkSchemaTask.cancel(true);
        checkSchemaTask = null;
        server = null;
    }

    public void testSuccessfulSchemaCheck() {

        MockResponse foundSchema = new MockResponse()
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody("{\n" +
                        "    \"count\": 1,\n" +
                        "    \"next\": null,\n" +
                        "    \"previous\": null,\n" +
                        "    \"results\": [\n" +
                        "        {\n" +
                        "            \"uuid\": \"bf3547b2-877d-4c1a-9725-b7d72a5232c8\",\n" +
                        "            \"current_schema\": \"e3290fbe-56a4-4527-ad96-e2e6561a9ac5\",\n" +
                        "            \"created\": \"2015-10-28T20:32:56.446799Z\",\n" +
                        "            \"modified\": \"2015-10-28T21:42:40.040549Z\",\n" +
                        "            \"label\": \"Incident\",\n" +
                        "            \"plural_label\": \"Incidents\",\n" +
                        "            \"description\": \"Historical incident data\",\n" +
                        "            \"active\": true\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}");
        server.enqueue(foundSchema);
        try {
            server.start();

            DriverUserInfo userInfo = new DriverUserInfo();
            DriverUserAuth auth = new DriverUserAuth();
            auth.token = "11111111111111111";
            userInfo.setUserToken(auth);

            checkSchemaTask.execute(userInfo);

            // should get back schema UUID above
            Mockito.verify(mockActivity, Mockito.timeout(2000)).foundSchema("e3290fbe-56a4-4527-ad96-e2e6561a9ac5");

            assertEquals("Should have requested current schema from server", 1, server.getRequestCount());

            server.shutdown();

        } catch (IOException e) {
            e.printStackTrace();
            fail("Mock web server error");
        }
    }

    public void testSchemaCheckWithBadCredentials() {

        MockResponse foundSchema = new MockResponse().setResponseCode(403);
        server.enqueue(foundSchema);
        try {
            server.start();

            DriverUserInfo userInfo = new DriverUserInfo();
            DriverUserAuth auth = new DriverUserAuth();
            auth.token = "11111111111111111";
            userInfo.setUserToken(auth);

            checkSchemaTask.execute(userInfo);

            // should get callback that credentials didn't work
            Mockito.verify(mockActivity, Mockito.timeout(2000)).haveInvalidCredentials();

            assertEquals("Should have requested current schema from server", 1, server.getRequestCount());

            server.shutdown();

        } catch (IOException e) {
            e.printStackTrace();
            fail("mock web server error");
        }
    }

    public void testSchemaCheckUnexpectedResponse() {

        MockResponse foundSchema = new MockResponse()
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody("somejunk");
        server.enqueue(foundSchema);
        try {
            server.start();

            DriverUserInfo userInfo = new DriverUserInfo();
            DriverUserAuth auth = new DriverUserAuth();
            auth.token = "11111111111111111";
            userInfo.setUserToken(auth);

            checkSchemaTask.execute(userInfo);

            Mockito.verify(mockActivity, Mockito.timeout(2000))
                    .schemaCheckError("Server error checking schema version. Please try again at another time.");

            assertEquals("Should have requested current schema from server", 1, server.getRequestCount());

            server.shutdown();

        } catch (IOException e) {
            e.printStackTrace();
            fail("Mock web server error");
        }
    }
}
