package org.worldbank.transport.driver.tasks;

/**
 * Created by kathrynkillebrew on 12/8/15.
 */

import android.os.AsyncTask;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class LoginTask extends AsyncTask<Void, Void, Boolean> {

    public interface LoginCallbackListener {
        void loginCompleted(boolean successful);
        void loginCancelled();
    }

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };

    private final String mEmail;
    private final String mPassword;
    private final LoginCallbackListener mListener;

    public LoginTask(String email, String password, LoginCallbackListener listener) {
        mEmail = email;
        mPassword = password;
        mListener = listener;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        // TODO: attempt authentication against a network service.

        try {
            // Simulate network access.
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return false;
        }

        for (String credential : DUMMY_CREDENTIALS) {
            String[] pieces = credential.split(":");
            if (pieces[0].equals(mEmail)) {
                // Account exists, return true if the password matches.
                return pieces[1].equals(mPassword);
            }
        }

        // TODO: register the new account here.
        return true;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        mListener.loginCompleted(success);
    }

    @Override
    protected void onCancelled() {
        mListener.loginCancelled();
    }
}
