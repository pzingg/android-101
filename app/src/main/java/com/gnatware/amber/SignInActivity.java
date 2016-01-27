package com.gnatware.amber;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

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

    public static final String EMAIL_ADDRESS = "com.gnatware.amber.SignInActivity.EMAIL_ADDRESS";
    public static final String USER_OBJECT_NAME_FIELD = "name";

    // All login UI fragment transactions will happen within this parent layout element.
    // Change this if you are modifying this code to be hosted in your own activity.
    private int mContainerViewId;

    private CoordinatorLayout mLayout;
    private ProgressDialog mProgressDialog;
    private Bundle mConfigOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate");

        // Set a global layout listener which will be called when the layout pass is completed and the view is drawn
        mLayout = (CoordinatorLayout) getLayoutInflater().inflate(R.layout.activity_sign_in, null);
        if (mLayout != null) {
            setContentView(mLayout);
            mContainerViewId = mLayout.getId();
        } else {
            mContainerViewId = android.R.id.content;
        }

        // Disable landscape
        // this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Log.d(LOG_TAG, "onCreate - requested portrait");

        // this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        mConfigOptions = getIntent().getExtras();
        Log.d(LOG_TAG, "onCreate - got options");

        // Show the unified sign-in / sign-up form
        if (savedInstanceState == null) {
            Log.d(LOG_TAG, "onCreate - new instance state");

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            SignInFragment fragment = SignInFragment.newInstance(mConfigOptions);
            fragmentTransaction.add(mContainerViewId, fragment);
            fragmentTransaction.commit();
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

    @Override
    public void onPushExistingAccount(String emailAddress) {

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        ExistingAccountFragment fragment = ExistingAccountFragment.newInstance(mConfigOptions, emailAddress);
        transaction.replace(mContainerViewId, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onPushCreateAccount(String emailAddress) {

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        CreateAccountFragment fragment = CreateAccountFragment.newInstance(mConfigOptions, emailAddress);
        transaction.replace(mContainerViewId, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Called when the user clicked any of the back buttons on the
     * create account or existing account fragments.
     */
    @Override
    public void onBackClicked() {
        // Display the login form, which is the previous item onto the stack
        getSupportFragmentManager().popBackStackImmediate();
    }

    /**
     * Called when the user clicked any of the back buttons on the
     * main sign in fragment.
     */
    @Override
    public void onCancelClicked() {
        // Display the calling activity, which is the previous item onto the stack
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Called when the user clicked the log in button on the login form.
     */
    @Override
    public void onLoginHelpClicked() {

        // Show the login help form for resetting the user's password.
        // Keep the transaction on the back stack so that if the user clicks
        // the back button, they are brought back to the login form.
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(mContainerViewId, ParseLoginHelpFragment.newInstance(mConfigOptions));
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Called when the user successfully completes the login help flow.
     */
    @Override
    public void onLoginHelpSuccess() {
        // Display the login form, which is the previous item onto the stack
        getSupportFragmentManager().popBackStackImmediate();
    }

    /**
     * Called when the user successfully logs in or signs up.
     */
    @Override
    public void onLoginSuccess() {

        // This default implementation returns to the parent activity with
        // RESULT_OK.
        // You can change this implementation if you want a different behavior.
        // TODO: Add intent with exsiting account info, or use RESULT_FIRST_USER+nnn
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onCreateAccountSuccess() {

        // This default implementation returns to the parent activity with
        // RESULT_OK.
        // You can change this implementation if you want a different behavior.
        // TODO: Add intent with new account info, or use RESULT_FIRST_USER+nnn
        setResult(RESULT_OK);
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
