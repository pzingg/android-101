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

import junit.framework.Assert;

/**
 * Activities that contain this fragment must implement the
 * LoginFragmentListener to handle interaction events.
 * Use the {@link ExistingAccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExistingAccountFragment extends LoginFragmentBase implements View.OnClickListener {

    private static final String LOG_TAG = "ExistingAccountFragment";

    // View widgets
    private TextView mTxtEmailAddress;
    private EditText mEdtPassword;
    private Button mBtnCancel;
    private Button mBtnSignIn;
    private TextView mBtnForgotPassword;

    private String mEmailAddress;

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
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment and create ParseLoginConfig object
        Bundle arguments = initConfigAndView(inflater, container, R.layout.fragment_existing_account);

        mEmailAddress = arguments.getString(SignInActivity.EMAIL_ADDRESS);
        Assert.assertNotNull(mEmailAddress);

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

    // View.OnClickListener method (sign in button clicked)
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
                        Log.e(LOG_TAG, "Activity was destroyed during log in");
                        return;
                    }

                    loadingFinish();
                    if (user != null) {
                        Log.d(LOG_TAG, "Account " + user.getObjectId() + " logged in for " + mEmailAddress);
                        loginSuccess(user);
                    } else if (e != null) {
                        if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                            if (mParseLoginConfig.getParseLoginInvalidCredentialsToastText() != null) {
                                showSnack(mParseLoginConfig.getParseLoginInvalidCredentialsToastText());
                            } else {
                                showSnack("Invalid credentials");
                            }
                            mEdtPassword.selectAll();
                            mEdtPassword.requestFocus();
                        } else {
                            Log.e(LOG_TAG, "Error logging in " + mEmailAddress + ": " + e.toString());
                            showSnack(R.string.com_parse_ui_parse_login_failed_unknown_toast);
                        }
                    }
                }
            });
        }
    }

    // Private methods
    private void setUpView() {
        mTxtEmailAddress.setText(mEmailAddress);

        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoginFragmentListener.onBackClicked();
            }
        });

        mBtnSignIn.setOnClickListener(this);

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

    private void loginSuccess(ParseUser user) {
        mLoginFragmentListener.onLoginSuccess(user);
    }
}
