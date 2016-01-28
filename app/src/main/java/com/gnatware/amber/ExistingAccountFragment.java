package com.gnatware.amber;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginConfig;

import junit.framework.Assert;

/**
 * Activities that contain this fragment must implement the
 * LoginFragmentListener to handle interaction events.
 * Use the {@link ExistingAccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExistingAccountFragment extends LoginFragmentBase {

    private static final String LOG_TAG = "ExistingAccountFragment";

    private String mEmailAddress;

    private TextView mTxtEmailAddress;
    private EditText mEdtPassword;
    private Button mBtnCancel;
    private Button mBtnSignIn;
    private TextView mBtnForgotPassword;

    public ExistingAccountFragment() {
        // Required empty public constructor
    }

    // Bundle must contain "emailAddress" key
    public static ExistingAccountFragment newInstance(Bundle configOptions, String emailAddress) {
        ExistingAccountFragment fragment = new ExistingAccountFragment();

        // Merge email address with existing configuration
        Bundle args = new Bundle(configOptions);
        args.putString(SignInActivity.EMAIL_ADDRESS, emailAddress);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        mEmailAddress = arguments.getString(SignInActivity.EMAIL_ADDRESS);
        Assert.assertNotNull(mEmailAddress);

        mParseLoginConfig = ParseLoginConfig.fromBundle(arguments, getActivity());
        Assert.assertTrue(mParseLoginConfig.isParseLoginEmailAsUsername());

        mLayout = inflater.inflate(R.layout.fragment_existing_account, container, false);

        mTxtEmailAddress = (TextView) mLayout.findViewById(R.id.txtEmailAddress);
        mEdtPassword = (EditText) mLayout.findViewById(R.id.edtPassword);
        mBtnCancel = (Button) mLayout.findViewById(R.id.btnCancel);
        mBtnSignIn = (Button) mLayout.findViewById(R.id.btnSignIn);
        mBtnForgotPassword = (Button) mLayout.findViewById(R.id.parse_login_help);

        setUpView();

        return mLayout;
    }

    // LoginFragmentBase method
    @Override
    protected String getLogTag() { return LOG_TAG; }

    // Private methods
    private void setUpView() {
        mTxtEmailAddress.setText(mEmailAddress);

        mBtnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = mEdtPassword.getText().toString();

                if (password.length() == 0) {
                    showSnack(R.string.com_parse_ui_no_password_toast);
                } else {
                    loadingStart(true);
                    ParseUser.logInInBackground(mEmailAddress, password, new LogInCallback() {

                        @Override
                        public void done(ParseUser user, ParseException e) {
                            if (isActivityDestroyed()) {
                                return;
                            }

                            loadingFinish();
                            if (user != null) {
                                Log.d(LOG_TAG, "Account " + user.getObjectId() + " logged in for " + mEmailAddress);
                                loginSuccess();
                            } else if (e != null) {
                                Log.d(LOG_TAG,
                                        getString(R.string.com_parse_ui_login_warning_parse_login_failed) +
                                                e.toString());
                                if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                                    if (mParseLoginConfig.getParseLoginInvalidCredentialsToastText() != null) {
                                        showSnack(mParseLoginConfig.getParseLoginInvalidCredentialsToastText());
                                    } else {
                                        showSnack("Invalid credentials");
                                    }
                                    mEdtPassword.selectAll();
                                    mEdtPassword.requestFocus();
                                } else {
                                    Log.d(LOG_TAG, "Error logging in " + mEmailAddress + ": " + e.getMessage());
                                    showSnack(R.string.com_parse_ui_parse_login_failed_unknown_toast);
                                }
                            }
                        }
                    });
                }
            }
        });

        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "User canceled existing account sign-in");
                mLoginFragmentListener.onBackClicked();
            }
        });

        if (mParseLoginConfig.getParseLoginHelpText() != null) {
            mBtnForgotPassword.setText(mParseLoginConfig.getParseLoginHelpText());
        }

        mBtnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoginFragmentListener.onLoginHelpClicked();
            }
        });
    }

    private void loginSuccess() {
        mLoginFragmentListener.onLoginSuccess();
    }
}
