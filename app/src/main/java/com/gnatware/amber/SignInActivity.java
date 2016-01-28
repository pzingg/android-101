package com.gnatware.amber;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.parse.ParseFacebookUtils;
import com.parse.ui.ParseOnLoadingListener;

/*
 * Pretty much copied from ParseLoginActivity, but modified
 * to handle our unified sign-in / sign-up system.
 */
public class SignInActivity extends AppCompatActivity implements
        LoginFragmentListener,
        ParseOnLoadingListener {

    public static final String LOG_TAG = "SignInActivity";

    public static final int RESULT_ACCOUNT_SIGNED_IN = RESULT_OK;
    public static final int RESULT_ACCOUNT_CREATED = RESULT_FIRST_USER;
    public static final int RESULT_FACEBOOK_LOGIN = RESULT_FIRST_USER+1;
    public static final int RESULT_TWITTER_LOGIN = RESULT_FIRST_USER+2;

    public static final String EMAIL_ADDRESS = "com.gnatware.amber.SignInActivity.EMAIL_ADDRESS";
    public static final String USER_OBJECT_NAME_FIELD = "name";

    // All login UI fragment transactions will happen within this parent layout element.
    // Change this if you are modifying this code to be hosted in your own activity.
    private int mContainerViewId;

    private ProgressDialog mProgressDialog;
    private Bundle mConfigOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // These must be called before super.onCreate
        // Force portrait, no titles, please
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        // Use a layout or android.R.id.content
        View layout = (View) getLayoutInflater().inflate(R.layout.activity_sign_in, null);
        // A FrameLayout container (child view of the layout)
        View containerView = layout.findViewById(R.id.signInContainer);
        if (containerView != null) {
            mContainerViewId = R.id.signInContainer;
            setContentView(layout);
        } else {
            mContainerViewId = android.R.id.content;
        }

        mConfigOptions = getIntent().getExtras();

        // Show the unified sign-in / sign-up form
        if (savedInstanceState == null) {
            Log.d(LOG_TAG, "onCreate, new instance - load SignInFragment");
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            SignInFragment fragment = SignInFragment.newInstance(mConfigOptions);
            fragmentTransaction.add(mContainerViewId, fragment);
            fragmentTransaction.commit();
        } else {
            Log.d(LOG_TAG, "onCreate, existing instance");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Required for making Facebook login work
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Called when the user clicked the Next button on the main sign in fragment, and
     * the email address was associated with an existing account.
     */
    @Override
    public void onPushExistingAccount(String emailAddress) {
        Log.d(LOG_TAG, "onPushExistingAccount");

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        ExistingAccountFragment fragment = ExistingAccountFragment.newInstance(
                mConfigOptions, emailAddress);
        transaction.replace(mContainerViewId, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Called when the user clicked the Next button on the main sign in fragment, and
     * the email address was not associated with an existing account.
     */
    @Override
    public void onPushCreateAccount(String emailAddress) {
        Log.d(LOG_TAG, "onPushCreateAccount");

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        CreateAccountFragment fragment = CreateAccountFragment.newInstance(
                mConfigOptions, emailAddress);
        transaction.replace(mContainerViewId, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Called when the user clicked any of the back buttons on either the create account or
     * the existing account fragment.
     */
    @Override
    public void onBackClicked() {
        Log.d(LOG_TAG, "onBackClicked");

        // Display the login form, which is the previous item onto the stack
        getSupportFragmentManager().popBackStackImmediate();
    }

    /**
     * Called when the user clicked any of the back buttons on the main sign in fragment.
     */
    @Override
    public void onCancelClicked() {
        Log.d(LOG_TAG, "onCancelClicked");

        // Display the calling activity, which is the previous item onto the stack
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Called when the user clicked the "forgot password" button on the main sign in fragment.
     */
    @Override
    public void onLoginHelpClicked() {
        Log.d(LOG_TAG, "onLoginHelpClicked");

        // Show the login help form for resetting the user's password.
        // Keep the transaction on the back stack so that if the user clicks
        // the back button, they are brought back to the login form.
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(mContainerViewId, LoginHelpFragment.newInstance(mConfigOptions));
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Called when the user successfully completes the login help flow.
     */
    @Override
    public void onLoginHelpSuccess() {
        Log.d(LOG_TAG, "onLoginHelpSuccess");

        // Display the login form, which is the previous item onto the stack
        getSupportFragmentManager().popBackStackImmediate();
    }

    /**
     * Called when the user successfully logs in to an existing account.
     */
    @Override
    public void onLoginSuccess() {
        Log.d(LOG_TAG, "onLoginSuccess");

        setResult(RESULT_ACCOUNT_SIGNED_IN);
        finish();
    }

    /**
     * Called when the user successfully creates a new account.
     */
    @Override
    public void onCreateAccountSuccess() {
        Log.d(LOG_TAG, "onCreateAccountSuccess");

        setResult(RESULT_ACCOUNT_CREATED);
        finish();
    }

    /**
     * Called when we are in progress retrieving some data.
     *
     * @param showSpinner
     *     Whether to show the loading dialog.
     */
    @Override
    public void onLoadingStart(boolean showSpinner) {
        if (showSpinner) {
            mProgressDialog = ProgressDialog.show(this, null,
                    getString(R.string.com_parse_ui_progress_dialog_text), true, false);
        }
    }

    /**
     * Called when we are finished retrieving some data.
     */
    @Override
    public void onLoadingFinish() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}
