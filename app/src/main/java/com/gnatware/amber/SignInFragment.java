package com.gnatware.amber;

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
import com.parse.ui.ParseLoginConfig;

import org.json.JSONObject;


/**
 * Lots of code copied from ParseUI-Login's ParseLoginFragment class.
 *
 * Activities that contain this fragment must implement the
 * LoginFragmentListener to handle interaction events.
 * Use the {@link SignInFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SignInFragment extends LoginFragmentBase {

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
        Log.d(LOG_TAG, "onCreateView");

        mParseLoginConfig = ParseLoginConfig.fromBundle(getArguments(), getActivity());
        mLayout = inflater.inflate(R.layout.fragment_sign_in, container, false);

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

    // Private methods
    private void setUpEmailSignIn() {
        mEdtEmailAddress.setHint(R.string.com_parse_ui_email_input_hint);
        mEdtEmailAddress.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        mBtnNext.setOnClickListener(new View.OnClickListener() {

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
        });

        mBtnCancelSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "User canceled sign-in");
                mLoginFragmentListener.onCancelClicked();
            }
        });

        if (mParseLoginConfig.getParseLoginHelpText() != null) {
            mBtnForgotPassword.setText(mParseLoginConfig.getParseLoginHelpText());
        }

        mBtnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "onLoginHelpClicked");
                mLoginFragmentListener.onLoginHelpClicked();
            }
        });
    }

    private void setUpFacebookLogin() {
        mBtnFacebookLogin.setVisibility(View.VISIBLE);

        if (mParseLoginConfig.getFacebookLoginButtonText() != null) {
            mBtnFacebookLogin.setText(mParseLoginConfig.getFacebookLoginButtonText());
        }

        mBtnFacebookLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                loadingStart(false); // Facebook login pop-up already has a spinner

                // Package-private method - we assume no publish permissions needed
                // if (mParseLoginConfig.isFacebookLoginNeedPublishPermissions()) { ... }
                ParseFacebookUtils.logInWithReadPermissionsInBackground(getActivity(),
                        mParseLoginConfig.getFacebookLoginPermissions(), new LogInCallback() {

                            @Override
                            public void done(ParseUser user, ParseException e) {
                                if (isActivityDestroyed()) {
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
                                } else {
                                    if (user.isNew()) {
                                        saveFacebookNameForNewUser(user);
                                    } else {
                                        Log.d(LOG_TAG, "Existing account " + user.getObjectId() + " logged in via Facebook");
                                        loginSuccess();
                                    }
                                }
                            }
                        });

            }
        });
    }

    private void saveFacebookNameForNewUser(ParseUser user) {
        final String userId = user.getObjectId();

        GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {

                    @Override
                    public void onCompleted(JSONObject fbUser,
                                            GraphResponse response) {
                  /*
                    If we were able to successfully retrieve the Facebook
                    user's name, let's set it on the fullName field.
                  */
                        ParseUser parseUser = ParseUser.getCurrentUser();
                        if (parseUser != null) {
                            final String parseUserId = parseUser.getObjectId();
                            if (fbUser != null
                                    && fbUser.optString("name").length() > 0) {
                                parseUser.put(SignInActivity.USER_OBJECT_NAME_FIELD,
                                        fbUser.optString("name"));
                                parseUser.saveInBackground(new SaveCallback() {

                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null) {
                                            Log.d(LOG_TAG,
                                                    getString(
                                                            R.string.com_parse_ui_login_warning_facebook_login_user_update_failed) +
                                                            e.toString());
                                        } else {
                                            Log.d(LOG_TAG, "New account " + parseUserId + " logged in via Facebook");
                                        }
                                        loginSuccess();
                                    }
                                });
                            } else {
                                Log.d(LOG_TAG, "New account " + parseUserId + " logged in via Facebook, but could not retrieve name");
                            }
                        } else {
                            Log.d(LOG_TAG, "New account " + userId + " logged in via Facebook, but could not get current user");
                        }
                        loginSuccess();
                    }
                }
        ).executeAsync();
    }

    private void setUpTwitterLogin() {
        mBtnTwitterLogin.setVisibility(View.VISIBLE);

        if (mParseLoginConfig.getTwitterLoginButtonText() != null) {
            mBtnTwitterLogin.setText(mParseLoginConfig.getTwitterLoginButtonText());
        }

        mBtnTwitterLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingStart(false); // Twitter login pop-up already has a spinner
                ParseTwitterUtils.logIn(getActivity(), new LogInCallback() {

                    @Override
                    public void done(ParseUser user, ParseException e) {
                        if (isActivityDestroyed()) {
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
                        } else {
                            if (user.isNew()) {
                                saveTwitterNameForNewUser(user);
                            } else {
                                Log.d(LOG_TAG, "Existing account " + user.getObjectId() + " logged in via Twitter");
                                loginSuccess();
                            }
                        }
                    }
                });
            }
        });
    }

    private void saveTwitterNameForNewUser(ParseUser user) {
        final String userId = user.getObjectId();

        Twitter twitterUser = ParseTwitterUtils.getTwitter();
        if (twitterUser != null
                && twitterUser.getScreenName().length() > 0) {
                /*
                  To keep this example simple, we put the users' Twitter screen name
                  into the name field of the Parse user object. If you want the user's
                  real name instead, you can implement additional calls to the
                  Twitter API to fetch it.
                */
            user.put(SignInActivity.USER_OBJECT_NAME_FIELD, twitterUser.getScreenName());
            user.saveInBackground(new SaveCallback() {

                @Override
                public void done(ParseException e) {
                    if (e != null) {
                        Log.d(LOG_TAG,
                            getString(R.string.com_parse_ui_login_warning_twitter_login_user_update_failed) +
                                    e.toString());
                    }
                    Log.d(LOG_TAG, "New account " + userId + " created via Twitter");
                    loginSuccess();
                }
            });
        } else {
            Log.d(LOG_TAG, "New account " + userId + " created via Twitter, but could not retrieve name");
            loginSuccess();
        }
    }

    private void loginSuccess() {
        mLoginFragmentListener.onLoginSuccess();
    }

    private boolean allowEmailSignIn() {
        return mParseLoginConfig.isParseLoginEnabled();
    }

    private boolean allowFacebookLogin() {
        return mParseLoginConfig.isFacebookLoginEnabled();
    }

    private boolean allowTwitterLogin() {
        return mParseLoginConfig.isTwitterLoginEnabled();
    }
}
