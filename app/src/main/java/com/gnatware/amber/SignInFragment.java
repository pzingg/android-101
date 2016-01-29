package com.gnatware.amber;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.GetCallback;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseQuery;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.twitter.Twitter;

import org.json.JSONObject;

import java.util.Collection;


/**
 * Lots of code copied from ParseUI-Login's ParseLoginFragment class.
 *
 * Activities that contain this fragment must implement the
 * LoginFragmentListener to handle interaction events.
 * Use the {@link SignInFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SignInFragment extends LoginFragmentBase implements View.OnClickListener {

    private static final String LOG_TAG = "SignInFragment";

    private EditText mEdtEmailAddress;
    private Button mBtnCancelSignIn;
    private Button mBtnNext;
    private TextView mBtnForgotPassword;
    private Button mBtnFacebookLogin;
    private Button mBtnTwitterLogin;

    public SignInFragment() {
        // Required empty public constructor
    }

    // Public static factory convenience method
    public static SignInFragment newInstance(Bundle configOptions) {
        SignInFragment fragment = new SignInFragment();
        fragment.setArguments(configOptions);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment and create ParseLoginConfig object
        initConfigAndView(inflater, container, R.layout.fragment_sign_in);

        mEdtEmailAddress = (EditText) mLayout.findViewById(R.id.edtEmailAddress);
        mBtnForgotPassword = (Button) mLayout.findViewById(R.id.parse_login_help);
        mBtnCancelSignIn = (Button) mLayout.findViewById(R.id.btnCancelSignIn);
        mBtnNext = (Button) mLayout.findViewById(R.id.btnNext);
        mBtnFacebookLogin = (Button) mLayout.findViewById(R.id.facebook_login);
        mBtnTwitterLogin = (Button) mLayout.findViewById(R.id.twitter_login);

        if (allowEmailSignIn()) {
            setUpEmailSignIn();
        }
        if (allowFacebookLogin()) {
            setUpFacebookLogin();
        }
        if (allowTwitterLogin()) {
            setUpTwitterLogin();
        }
        return mLayout;
    }

    // LoginFragmentBase method
    @Override
    protected String getLogTag() { return LOG_TAG; }

    // View.OnClickListener method (next button clicked)
    @Override
    public void onClick(View v) {
        final String emailAddress = mEdtEmailAddress.getText().toString();

        if (emailAddress.length() == 0) {
            showSnack(R.string.com_parse_ui_no_email_toast);
        } else {
            // TODO: Validate email with regex?
            // TODO: Strip spaces?
            // TODO: Lowercase?
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("email", emailAddress);
            loadingStart();

            query.getFirstInBackground(new GetCallback<ParseUser>() {

                @Override
                public void done(ParseUser user, ParseException e) {
                    if (isActivityDestroyed()) {
                        return;
                    }

                    loadingFinish();
                    if (e == null) {

                        // Found user with that email address, so send to existing account
                        Log.d(LOG_TAG, "onPushExistingAccount with " + emailAddress);
                        mLoginFragmentListener.onPushExistingAccount(emailAddress);
                    } else {
                        if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {

                            // Email not found, so send to create account
                            Log.d(LOG_TAG, "onPushCreateAccount with " + emailAddress);
                            mLoginFragmentListener.onPushCreateAccount(emailAddress);
                        } else {
                            Log.d(LOG_TAG, "Error looking up " + emailAddress + ": " +
                                    e.getMessage());
                        }
                    }
                }
            });
        }
    }

    // Private methods
    private boolean allowEmailSignIn() {
        return mParseLoginConfig.isParseLoginEnabled();
    }

    private boolean allowFacebookLogin() {
        return mParseLoginConfig.isFacebookLoginEnabled();
    }

    private boolean allowTwitterLogin() {
        return mParseLoginConfig.isTwitterLoginEnabled();
    }

    private void setUpEmailSignIn() {
        mEdtEmailAddress.setHint(R.string.com_parse_ui_email_input_hint);
        mEdtEmailAddress.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        mBtnCancelSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "User canceled sign-in");
                mLoginFragmentListener.onCancelClicked();
            }
        });

        mBtnNext.setOnClickListener(this);

        if (mParseLoginConfig.getParseLoginHelpText() != null) {
            mBtnForgotPassword.setText(mParseLoginConfig.getParseLoginHelpText());
        }

        mBtnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { mLoginFragmentListener.onLoginHelpClicked(); }
        });
    }

    private void setUpFacebookLogin() {
        if (mParseLoginConfig.getFacebookLoginButtonText() != null) {
            mBtnFacebookLogin.setText(mParseLoginConfig.getFacebookLoginButtonText());
        }
        mBtnFacebookLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onFacebookButtonClicked(v); }
        });
    }

    private void setUpTwitterLogin() {
        if (mParseLoginConfig.getTwitterLoginButtonText() != null) {
            mBtnTwitterLogin.setText(mParseLoginConfig.getTwitterLoginButtonText());
        }
        mBtnTwitterLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTwitterButtonClicked(v);
            }
        });
    }

    private void copyDetailsToUser(final ParseUser user, final String auth,
                                          String name, String emailAddress) {

        boolean detailsToSave = false;
        if (name != null && name.length() > 0) {
            user.put(SignInActivity.USER_OBJECT_NAME_FIELD, name);
            detailsToSave = true;
        }
        if (emailAddress != null && emailAddress.length() > 0) {
            user.put("email", emailAddress);
            detailsToSave = true;
        }
        if (!detailsToSave) {
            Log.e(LOG_TAG, "No " + auth + " details to save for logged in user");
            loginSuccess(user);
            return;
        }

        user.saveInBackground(new SaveCallback() {

            @Override
            public void done(ParseException e) {
                if (isActivityDestroyed()) {
                    Log.e(LOG_TAG, "Activity was destroyed while saving " + auth + " details");
                    return;
                }

                if (e != null) {
                    Log.e(LOG_TAG,
                            getString(
                                    R.string.com_parse_ui_login_warning_facebook_login_user_update_failed) +
                                    e.toString());
                } else {
                    Log.d(LOG_TAG, auth + " details saved to " + user.getObjectId() + " account");
                }
                loginSuccess(user);
            }
        });
    }

    private void copyFacebookDetailsToUser(final ParseUser user) {

        AccessToken token = AccessToken.getCurrentAccessToken();
        GraphRequest.newMeRequest(token, new GraphRequest.GraphJSONObjectCallback() {

            @Override
            public void onCompleted(JSONObject fbUser, GraphResponse response) {
                if (isActivityDestroyed()) {
                    Log.e(LOG_TAG, "Activity was destroyed during Facebook graph request");
                    return;
                }

                if (fbUser == null) {
                    Log.e(LOG_TAG, "Could not fetch Facebook user information");
                    loginSuccess(user);
                } else {

                    // If we were able to successfully retrieve the Facebook
                    // user's name, let's set it on the fullName field.
                    String name = fbUser.optString("name");
                    copyDetailsToUser(user, "Facebook", name, null);
                }
            }
        }).executeAsync();
    }

    private void onFacebookButtonClicked(View v) {

        // Facebook login pop-up already has a spinner
        loadingStart(false);

        Activity activity = getActivity();
        Collection<String> permissions = mParseLoginConfig.getFacebookLoginPermissions();

        // isFacebookLoginNeedPublishPermissions() is a package-private method -
        // we assume no publish permissions needed.
        ParseFacebookUtils.logInWithReadPermissionsInBackground(activity, permissions, new LogInCallback() {

            @Override
            public void done(ParseUser user, ParseException e) {
                if (isActivityDestroyed()) {
                    Log.e(LOG_TAG, "Activity was destroyed during Facebook log in");
                    return;
                }

                if (user == null) {
                    loadingFinish();
                    if (e != null) {
                        showSnack(R.string.com_parse_ui_facebook_login_failed_toast);
                        Log.d(LOG_TAG, getString(R.string.com_parse_ui_login_warning_facebook_login_failed) +
                                e.toString());
                    } else {
                        Log.d(LOG_TAG, "Facebook login canceled?");
                    }
                } else if (user.isNew()) {
                    final ParseUser currentUser = ParseUser.getCurrentUser();
                    if (currentUser == null) {
                        Log.e(LOG_TAG, "Cannot copy Facebook details - no current user");
                        loginSuccess(user);
                    } else {
                        copyFacebookDetailsToUser(currentUser);
                    }
                } else {
                    Log.d(LOG_TAG, "Existing account " + user.getObjectId() + " logged in via Facebook");
                    loginSuccess(user);
                }
            }
        });
    }

    private void copyTwitterDetailsToUser(final ParseUser user) {
        Twitter twitterUser = ParseTwitterUtils.getTwitter();
        if (twitterUser == null) {
            Log.e(LOG_TAG, "Could not fetch Twitter user information");
            loginSuccess(user);
        } else {

            // To keep this example simple, we put the users' Twitter screen name
            // into the name field of the Parse user object. If you want the user's
            // real name instead, you can implement additional calls to the
            // Twitter API to fetch it.
            String name = twitterUser.getScreenName();
            copyDetailsToUser(user, "Twitter", name, null);
        }
    }

    private void onTwitterButtonClicked(View v) {

        // Twitter login pop-up already has a spinner
        loadingStart(false);
        ParseTwitterUtils.logIn(getActivity(), new LogInCallback() {

            @Override
            public void done(ParseUser user, ParseException e) {
                if (isActivityDestroyed()) {
                    Log.e(LOG_TAG, "Activity was destroyed during Twitter log in");
                    return;
                }

                if (user == null) {
                    loadingFinish();
                    if (e != null) {
                        showSnack(R.string.com_parse_ui_twitter_login_failed_toast);
                        Log.d(LOG_TAG, getString(R.string.com_parse_ui_login_warning_twitter_login_failed) +
                                e.toString());
                    } else {
                        Log.d(LOG_TAG, "Twitter login canceled?");
                    }
                } else if (user.isNew()) {
                    final ParseUser currentUser = ParseUser.getCurrentUser();
                    if (currentUser == null) {
                        Log.e(LOG_TAG, "Cannot copy Facebook details - no current user");
                        loginSuccess(user);
                    } else {
                        copyTwitterDetailsToUser(currentUser);
                    }
                } else {
                    Log.d(LOG_TAG, "Existing account " + user.getObjectId() + " logged in via Twitter");
                    loginSuccess(user);
                }
            }
        });
    }

    private void loginSuccess(ParseUser user) {
        mLoginFragmentListener.onLoginSuccess(user);
    }
}
