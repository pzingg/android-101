package com.gnatware.amber;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import junit.framework.Assert;

/**
 * Activities that contain this fragment must implement the
 * LoginFragmentListener to handle interaction events.
 * Use the {@link CreateAccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CreateAccountFragment extends LoginFragmentBase implements View.OnClickListener {

    private static final String LOG_TAG = "CreateAccountFragment";

    private static final int DEFAULT_MIN_PASSWORD_LENGTH = 6;

    // View widgets
    private TextView mTxtEmailAddress;
    private EditText mEdtPassword;
    private EditText mEdtConfirmPassword;
    private EditText mEdtName;
    private Button mBtnCancelAccount;
    private Button mBtnCreateAccount;

    private String mEmailAddress;
    private int mMinPasswordLength = DEFAULT_MIN_PASSWORD_LENGTH;

    public CreateAccountFragment() {
        // Required empty public constructor
    }

    public static CreateAccountFragment newInstance(Bundle configOptions, String emailAddress) {
        CreateAccountFragment fragment = new CreateAccountFragment();

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

        // Inflate the layout for this fragment and create ParseLoginConfig object
        Bundle arguments = initConfigAndView(inflater, container, R.layout.fragment_create_account);

        mEmailAddress = arguments.getString(SignInActivity.EMAIL_ADDRESS);
        Assert.assertNotNull(mEmailAddress);

        mMinPasswordLength = DEFAULT_MIN_PASSWORD_LENGTH;
        if (mParseLoginConfig.getParseSignupMinPasswordLength() != null) {
            mMinPasswordLength = mParseLoginConfig.getParseSignupMinPasswordLength();
        }
        mTxtEmailAddress = (TextView) mLayout.findViewById(R.id.txtEmailAddress);
        mEdtPassword = (EditText) mLayout.findViewById(R.id.edtPassword);
        mEdtConfirmPassword = (EditText) mLayout.findViewById(R.id.edtConfirmPassword);
        mEdtName = (EditText) mLayout.findViewById(R.id.edtName);
        mBtnCancelAccount = (Button) mLayout.findViewById(R.id.btnCancelAccount);
        mBtnCreateAccount = (Button) mLayout.findViewById(R.id.btnCreateAccount);

        setUpView();

        return mLayout;
    }

    // LoginFragmentBase method
    @Override
    protected String getLogTag() { return LOG_TAG; }

    // View.OnClickListener method (create account button clicked)
    @Override
    public void onClick(View view) {
        String password = mEdtPassword.getText().toString();
        String passwordAgain = mEdtConfirmPassword.getText().toString();
        String name = mEdtName.getText().toString();

        if (password.length() == 0) {
            showSnack(R.string.com_parse_ui_no_password_toast);
        } else if (password.length() < mMinPasswordLength) {
            showSnack(getResources().getQuantityString(
                    R.plurals.com_parse_ui_password_too_short_toast,
                    mMinPasswordLength, mMinPasswordLength));
        } else if (passwordAgain.length() == 0) {
            showSnack(R.string.com_parse_ui_reenter_password_toast);
        } else if (!password.equals(passwordAgain)) {
            showSnack(R.string.com_parse_ui_mismatch_confirm_password_toast);
            mEdtConfirmPassword.selectAll();
            mEdtConfirmPassword.requestFocus();
        } else if (name.length() == 0) {
            showSnack(R.string.com_parse_ui_no_name_toast);
        } else {
            final ParseUser user = ParseObject.create(ParseUser.class);

            // Set standard fields
            user.setUsername(mEmailAddress);
            user.setEmail(mEmailAddress);
            user.setPassword(password);

            // Set additional custom fields only if the user filled it out
            if (name.length() != 0) {
                user.put(SignInActivity.USER_OBJECT_NAME_FIELD, name);
            }

            loadingStart();
            user.signUpInBackground(new SignUpCallback() {

                @Override
                public void done(ParseException e) {
                    if (isActivityDestroyed()) {
                        Log.e(LOG_TAG, "Activity was destroyed during sign up");
                        return;
                    }

                    loadingFinish();
                    if (e == null) {
                        signUpSuccess(user);
                    } else {
                        switch (e.getCode()) {
                            case ParseException.INVALID_EMAIL_ADDRESS:
                                showSnack(R.string.com_parse_ui_invalid_email_toast);
                                break;
                            case ParseException.USERNAME_TAKEN:
                                showSnack(R.string.com_parse_ui_username_taken_toast);
                                break;
                            case ParseException.EMAIL_TAKEN:
                                showSnack(R.string.com_parse_ui_email_taken_toast);
                                break;
                            default:
                                Log.e(LOG_TAG, "Error signing up " + mEmailAddress + ": " + e.toString());
                                showSnack(R.string.com_parse_ui_signup_failed_unknown_toast);
                        }
                    }
                }
            });
        }
    }

    // Private methods
    private void setUpView() {
        mTxtEmailAddress.setText(mEmailAddress);

        mBtnCancelAccount.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mLoginFragmentListener.onBackClicked();
            }
        });

        mBtnCreateAccount.setOnClickListener(this);
    }

    private void signUpSuccess(ParseUser user) {
        mLoginFragmentListener.onCreateAccountSuccess(user);
    }
}
