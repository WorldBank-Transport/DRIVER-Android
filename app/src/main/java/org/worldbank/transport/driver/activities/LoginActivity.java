package org.worldbank.transport.driver.activities;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.staticmodels.DriverUserInfo;
import org.worldbank.transport.driver.tasks.LoginTask;
import org.worldbank.transport.driver.utilities.LoginUrlBuilder;


/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity implements LoginTask.LoginCallbackListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_LABEL = "Login";

    private static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    private static final int RC_SIGN_IN = 9001;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private LoginTask mAuthTask = null;

    private DriverAppContext mAppContext;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private TextView mErrorMessage;

    DriverApp app;
    GoogleApiClient googleApiClient;

    // public so server interactions can be mocked in testing
    public LoginTask.LoginUrls mLoginUrlBuilder;

    /**
     * Non-default constructor for testing, to set the application context.
     * @param context Mock context
     */
    public LoginActivity(DriverAppContext context) {
        super();
        mAppContext = context;
    }

    /**
     * Default constructor, for testing.
     */
    public LoginActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAppContext = new DriverAppContext((DriverApp) getApplicationContext());
        app = mAppContext.getDriverApp();
        mLoginUrlBuilder = new LoginUrlBuilder();

        if (haveSavedUserInfo()) {
            Log.d("LoginActivity", "Have saved user info; skipping login screen");
            Intent intent = new Intent(this, RecordListActivity.class);
            startActivity(intent);
            finish();
        }

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mErrorMessage = (TextView) findViewById(R.id.error_message);
        mErrorMessage.setText("");

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        // Set up SSO client
        SignInButton ssoSignInButton = (SignInButton) findViewById(R.id.sso_sign_in_button);
        String clientId = getString(R.string.oauth_client_id);
        if (clientId == null || clientId.isEmpty()) {
            Log.e(LOG_LABEL, "No OAuth client ID defined! Be sure to set it in configurables.xml to enable SSO.");
            ssoSignInButton.setEnabled(false);
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                //.requestIdToken(clientId)
                .requestServerAuthCode(clientId, true)
                .build();
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addOnConnectionFailedListener(this)
                .build();

        ssoSignInButton.setSize(SignInButton.SIZE_WIDE);
        ssoSignInButton.setScopes(gso.getScopeArray());
        ssoSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSSO();
            }
        });
    }

    /**
     * Helper to check if saved user info has been retrieved from shared preferences at launch.
     *
     * @return true if user info found
     */
    public boolean haveSavedUserInfo() {
        DriverUserInfo lastUser = app.getUserInfo();
        if (lastUser != null && lastUser.id > -1 && !lastUser.getUserToken().isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mErrorMessage.setText("");

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new LoginTask(email, password, this, mLoginUrlBuilder);
            mAuthTask.execute();
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.length() > 0;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 0;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void attemptSSO() {
        Log.d(LOG_LABEL, "attempt sso login");
        //pickUserAccount();
        initiateSSO();
    }

    // another SSO auth guide here:
    // https://developers.google.com/identity/sign-in/android/sign-in#start_the_sign-in_flow
    private void initiateSSO() {
        Intent signInItent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInItent, RC_SIGN_IN);
    }

    // SSO auth guide here:
    // https://developers.google.com/android/guides/http-auth#invoke_the_account_picker
    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(LOG_LABEL, "handleSignInResult: " + result.isSuccess());
        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            if (acct != null) {
                Log.d(LOG_LABEL, "display name: " + acct.getDisplayName());
                Log.d(LOG_LABEL, "email: " + acct.getEmail());
                Log.d(LOG_LABEL, "ID: " + acct.getId());
                Log.d(LOG_LABEL, "ID token: " + acct.getIdToken());
                Log.d(LOG_LABEL, "Server auth code: " + acct.getServerAuthCode());
                if (acct.getPhotoUrl() != null) {
                    Log.d(LOG_LABEL, "Photo URL: " + acct.getPhotoUrl().getPath());
                }
                Log.d(LOG_LABEL, "Scopes: " + acct.getGrantedScopes().toString());
            } else {
                Log.e(LOG_LABEL, "SSO result acct is null?!");
            }
        } else {
            Log.d(LOG_LABEL, "Oh noes, SSO failed! TODO: handle stuff");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_LABEL, "Got activity result!");
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                Log.d(LOG_LABEL, "got back email: " + email);
                // With the account name acquired, go get the auth token
                // TODO: getUsername();
            } else if (resultCode == RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Log.w(LOG_LABEL, "Hey! You didn't pick anything.");
                //TODO: Toast.makeText(this, R.string.pick_account, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(result);
            } else if (resultCode == RESULT_CANCELED) {
                Log.w(LOG_LABEL, "SSO sign-in attempt got cancellation result.");
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                Log.e(LOG_LABEL, result.toString());
                Status status = result.getStatus();

                if (status.getStatusMessage() != null) {
                    Log.e(LOG_LABEL, status.getStatusMessage());
                }

                Log.e(LOG_LABEL, "SSO sign-in status code: " + Integer.toString(status.getStatusCode()));
                Log.e(LOG_LABEL, "has resolution? " + status.hasResolution());

                // TODO: if there is a resolution, launch its pending intent? how to handle?

            }
        }
    }

    @Override
    public void loginCompleted(DriverUserInfo userInfo) {
        mAuthTask = null;
        showProgress(false);

        if (userInfo != null) {
            // set user info on app singleton
            app.setUserInfo(userInfo);
            Intent intent = new Intent(this, RecordListActivity.class);
            startActivity(intent);
            finish();
        } else {
            Log.e("LoginActivity", "LoginTask should not return null user info on success!");
        }
    }

    @Override
    public void loginCancelled() {
        mAuthTask = null;
        showProgress(false);
    }

    @Override
    public void loginError(String errorMessage) {
        mErrorMessage.setText(errorMessage);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO: handle
        Log.e(LOG_LABEL, "Google SSO API connection failed!");
        Log.e(LOG_LABEL, connectionResult.getErrorMessage());
    }
}
