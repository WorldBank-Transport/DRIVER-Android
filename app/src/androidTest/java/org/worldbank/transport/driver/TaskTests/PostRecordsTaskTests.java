package org.worldbank.transport.driver.TaskTests;

import android.database.Cursor;
import android.location.Location;
import android.test.AndroidTestCase;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.mockito.Mockito;
import org.worldbank.transport.driver.MockPostRecordsUrlBuilder;
import org.worldbank.transport.driver.activities.RecordListActivity;
import org.worldbank.transport.driver.datastore.RecordDatabaseManager;
import org.worldbank.transport.driver.staticmodels.DriverConstantFields;
import org.worldbank.transport.driver.staticmodels.DriverUserAuth;
import org.worldbank.transport.driver.staticmodels.DriverUserInfo;
import org.worldbank.transport.driver.tasks.PostRecordsTask;

import java.io.IOException;
import java.util.Date;

/**
 * Unit test uploading records.
 *
 * Created by kathrynkillebrew on 1/28/16.
 */
public class PostRecordsTaskTests extends AndroidTestCase {

    RecordListActivity mockActivity;
    MockWebServer server;
    PostRecordsTask postRecordsTask;
    RecordDatabaseManager testDbManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mockActivity = Mockito.mock(RecordListActivity.class);
        server = new MockWebServer();

        DriverUserInfo userInfo = new DriverUserInfo();
        DriverUserAuth auth = new DriverUserAuth();
        auth.token = "11111111111111111";
        userInfo.setUserToken(auth);

        testDbManager = new RecordDatabaseManager(getContext(), true);

        PostRecordsTask.UploadRecordUrl uploadRecordUrl = new MockPostRecordsUrlBuilder(server);
        postRecordsTask = new PostRecordsTask(mockActivity, userInfo, uploadRecordUrl, testDbManager);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        postRecordsTask.cancel(true);
        postRecordsTask = null;
        server = null;
        mockActivity = null;
    }

    public void testSuccessfulRecordPost() {
        MockResponse foundSchema = new MockResponse().setResponseCode(201);
        server.enqueue(foundSchema);

        DriverConstantFields constantFields = new DriverConstantFields();
        constantFields.occurredFrom = new Date();
        constantFields.location = new Location("");
        constantFields.location.setLatitude(30);
        constantFields.location.setLongitude(30);
        testDbManager.addRecord("1111", "{\"foos\": 1}", constantFields);

        try {
            server.start();

            postRecordsTask.execute();

            Mockito.verify(mockActivity, Mockito.timeout(5000)).uploadedOneRecord();
            Mockito.verify(mockActivity, Mockito.timeout(5000)).recordUploadFinished(0);

            assertEquals("Should have gotten response to record post from server", 1, server.getRequestCount());

            server.shutdown();

        } catch (IOException e) {
            e.printStackTrace();
            fail("Mock web server error");
        }

        Cursor cursor = testDbManager.readAllRecords();
        assertEquals("Record should have been deleted from database", 0, cursor.getCount());
        cursor.close();
    }

}
