package org.worldbank.transport.driver.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JCodeModel;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsonschema2pojo.AnnotationStyle;
import org.jsonschema2pojo.Annotator;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.GsonAnnotator;
import org.jsonschema2pojo.JsonEditorAnnotator;
import org.jsonschema2pojo.Jsonschema2Pojo;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.SourceType;
import org.jsonschema2pojo.rules.RuleFactory;
import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.staticmodels.DriverUserInfo;
import org.worldbank.transport.driver.utilities.UpdateSchemaUrlBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * Created by kathrynkillebrew on 2/15/16.
 */
public class UpdateSchemaTask extends AsyncTask<String, String, String> {

    private static final String LOG_LABEL = "UpdateSchema";

    public interface UpdateSchemaCallbackListener {
        void schemaUpdated();
        void schemaUpdateCancelled();
        void schemaUpdateError(String errorMessage);
        void haveInvalidCredentials();
    }

    public interface RecordSchemaUrl {
        // Backend endpoints. Note that it is necessary to keep the trailing slash here.
        String RECORDSCHEMA_ENDPOINT = "api/recordschema/";

        URL schemaUrl(String serverUrl, String recordSchemaUuid);
    }

    private String serverUrl;
    private final Context context = DriverAppContext.getContext();
    private final WeakReference<UpdateSchemaCallbackListener> listener;
    private final RecordSchemaUrl schemaUrl;
    private final DriverUserInfo userInfo;

    public UpdateSchemaTask(UpdateSchemaCallbackListener listener, DriverUserInfo userInfo) {
        this(listener, new UpdateSchemaUrlBuilder(), userInfo);
    }

    /**
     * Constructor that may be used directly int testing, with a mocked URL builder
     *
     * @param listener callback listener
     * @param schemaUrl URL builder that returns endpoint to query for a schema
     */
    public UpdateSchemaTask(UpdateSchemaCallbackListener listener, RecordSchemaUrl schemaUrl, DriverUserInfo userInfo) {
        this.listener = new WeakReference<>(listener);
        serverUrl = context.getString(R.string.api_server_url);
        this.schemaUrl = schemaUrl;
        this.userInfo = userInfo;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected String doInBackground(String... params) {
        if(!DriverApp.getIsNetworkAvailable()) {
            // no network available. don't bother logging in
            publishProgress(context.getString(R.string.error_no_network));
            Log.d(LOG_LABEL, "No network");
            cancel(true);
            return null;
        }

        if (userInfo == null) {
            publishProgress(context.getString(R.string.error_schema_update));
            Log.e(LOG_LABEL, "missing user info!");
            cancel(true);
            return null;
        }

        String token = userInfo.getUserToken();

        // should not happen, but check anyways
        if (token == null || token.isEmpty()) {
            publishProgress(context.getString(R.string.error_schema_update));
            Log.e(LOG_LABEL, "missing user token!");
            cancel(true);
            return null;
        }

        // sanity check; make sure that the string found is actually a UUID
        String recordSchemaUuid = params[0];
        try {
            UUID.fromString(recordSchemaUuid);
        } catch (IllegalArgumentException e) {
            // does not conform to UUID String representation
            publishProgress(context.getString(R.string.error_schema_update));
            cancel(true);
            Log.e(LOG_LABEL, "Schema UUID string found, but it doesn't look like a UUID: " + recordSchemaUuid);
            return null;
        }

        try {
            URL url = schemaUrl.schemaUrl(serverUrl, recordSchemaUuid);

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Authorization", "Token " + token);

            // if get a 403 back, tell activity to go login again (shouldn't happen)
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                Log.w(LOG_LABEL, "User token must be invalid.");
                UpdateSchemaCallbackListener caller = listener.get();
                if (caller != null) {
                    caller.haveInvalidCredentials();
                } else {
                    Log.w(LOG_LABEL, "Cannot notify of 403 because listener has gone");
                }
                cancel(true);
                return null;
            }

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader ir = new BufferedReader(new InputStreamReader(in));

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = ir.readLine()) != null) {
                stringBuilder.append(line);
            }
            ir.close();
            in.close();
            String responseStr = stringBuilder.toString();

            // parse the JSON response find the schema UUID
            JSONObject json = new JSONObject(responseStr);
            String foundSchemaUuid = json.getString("uuid");

            // sanity check that returned schema UUID matches the requested UUID
            if (!foundSchemaUuid.equals(recordSchemaUuid)) {
                publishProgress(context.getString(R.string.error_schema_update));
                cancel(true);
                Log.e(LOG_LABEL, "Requested schema " + recordSchemaUuid + " but got " + foundSchemaUuid);
                return null;
            }

            // extract actual schema from meta-info on response
            String schema = json.getString("schema");

            JCodeModel codeModel = new JCodeModel();

            //Jsonschema2Pojo.generate(getJsonSchema2PojoConfig());

            SchemaMapper mapper = new SchemaMapper(new RuleFactory(getJsonSchema2PojoConfig(),
                    new GsonAnnotator(), new SchemaStore()), new SchemaGenerator());

            mapper.generate(codeModel, "DriverSchema", "org.worldbank.transport.driver.models", schema);

            File outFile = new File(context.getApplicationInfo().dataDir, "DriverSchema.java");
            codeModel.build(outFile);

            BufferedReader reader = new BufferedReader(new FileReader(outFile));
            stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                 stringBuilder.append(line);
            }
            reader.close();
            String code = stringBuilder.toString();
            Log.d(LOG_LABEL, "Built schema code: " + code);

            // TODO: actually write updated models out
            //codeModel.build(TODO OUTPUT FILE);

            return null; // TODO: return something useful here

        } catch (IOException e) {
            Log.e(LOG_LABEL, "Error communicating with server to perform schema check");
            publishProgress(context.getString(R.string.error_schema_update));
            e.printStackTrace();
            cancel(true);
        } catch (JSONException e) {
            Log.e(LOG_LABEL, "Error parsing JSON response to schema check");
            publishProgress(context.getString(R.string.error_schema_update));
            e.printStackTrace();
            cancel(true);
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        UpdateSchemaCallbackListener caller = listener.get();
        if (caller != null) {
            caller.schemaUpdateError(values[0]);
        } else {
            Log.w(LOG_LABEL, "Cannot notify of schema update error because listener has gone");
        }
    }

    @Override
    protected void onCancelled(String s) {
        UpdateSchemaCallbackListener caller = listener.get();
        if (caller != null) {
            caller.schemaUpdateCancelled();
        } else {
            Log.w(LOG_LABEL, "Cannot notify of schema update cancellation because listener has gone");
        }
    }

    @Override
    protected void onPostExecute(String s) {

        /*
        // sanity check; shouldn't happen
        if (s == null || s.isEmpty()) {
            Log.e(LOG_LABEL, "Schema check task finished with no schema!");
        }

        UpdateSchemaCallbackListener caller = listener.get();
        if (caller != null) {
            caller.foundSchema(s);
        } else {
            Log.w(LOG_LABEL, "Cannot send back current schema because listener has gone");
        }
        */
    }

    /**
     * Helper to build a JsonSchema2Pojo configuration. The settings here should match those
     * in the gradle section for jsonSchema2Pojo.
     *
     * @return JsonSchema2Pojo configuration
     */
    public GenerationConfig getJsonSchema2PojoConfig() {
        return new DefaultGenerationConfig() {

            @Override
            public File getTargetDirectory() {

                // context.getApplicationInfo().sourceDir

                // TODO: file("${project.buildDir}/generated/source/js2p")
                return super.getTargetDirectory();
            }

            @Override
            public String getTargetPackage() {
                return "org.worldbank.transport.driver.models";
            }

            @Override
            public boolean isGenerateBuilders() {
                return false;
            }

            @Override
            public boolean isUsePrimitives() {
                return false;
            }

            @Override
            public char[] getPropertyWordDelimiters() {
                return new char[]{' ', '_'};
            }

            @Override
            public boolean isUseLongIntegers() {
                return false;
            }

            @Override
            public boolean isUseDoubleNumbers() {
                return true;
            }

            @Override
            public boolean isIncludeHashcodeAndEquals() {
                return true;
            }

            @Override
            public boolean isIncludeToString() {
                return true;
            }

            @Override
            public boolean isIncludeAccessors() {
                return false;
            }

            @Override
            public boolean isIncludeAdditionalProperties() {
                return false;
            }

            @Override
            public AnnotationStyle getAnnotationStyle() {
                return AnnotationStyle.GSON;
            }

            @Override
            public Class<? extends Annotator> getCustomAnnotator() {
                return JsonEditorAnnotator.class;
            }

            @Override
            public boolean isIncludeJsr303Annotations() {
                return true;
            }

            @Override
            public SourceType getSourceType() {
                return SourceType.JSONSCHEMA;
            }

            @Override
            public boolean isRemoveOldOutput() {
                return true;
            }

            @Override
            public String getOutputEncoding() {
                return "UTF-8";
            }

            @Override
            public boolean isUseJodaDates() {
                return false;
            }

            @Override
            public boolean isUseCommonsLang3() {
                return false;
            }

            @Override
            public boolean isInitializeCollections() {
                return true;
            }

            @Override
            public boolean isParcelable() {
                return false;
            }
        };
    }
}
