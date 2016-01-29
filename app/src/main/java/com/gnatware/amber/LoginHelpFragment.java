/*
 *  Copyright (c) 2014, Parse, LLC. All rights reserved.
 *
 *  You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 *  copy, modify, and distribute this software in source code or binary form for use
 *  in connection with the web services and APIs provided by Parse.
 *
 *  As with any software that integrates with the Parse platform, your use of
 *  this software is subject to the Parse Terms of Service
 *  [https://www.parse.com/about/terms]. This copyright notice shall be
 *  included in all copies or substantial portions of the software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

/**
 * Fragment for the login help screen for resetting the user's password.
 */
public class LoginHelpFragment extends LoginFragmentBase implements View.OnClickListener {

    private static final String LOG_TAG = "LoginHelpFragment";

    // View widgets
    private TextView mTxtInstructions;
    private EditText mEdtEmailAddress;
    private Button mBtnSubmit;

    private boolean mEmailSent = false;

    public static LoginHelpFragment newInstance(Bundle configOptions) {
        LoginHelpFragment loginHelpFragment = new LoginHelpFragment();
        loginHelpFragment.setArguments(configOptions);
        return loginHelpFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment and create ParseLoginConfig object
        initConfigAndView(inflater, container, R.layout.com_parse_ui_parse_login_help_fragment);

        mTxtInstructions = (TextView) mLayout.findViewById(R.id.login_help_instructions);
        mEdtEmailAddress = (EditText) mLayout.findViewById(R.id.login_help_email_input);
        mBtnSubmit = (Button) mLayout.findViewById(R.id.login_help_submit);

        mBtnSubmit.setOnClickListener(this);

        return mLayout;
    }

    // View.OnClickListener method (submit button clicked)
    @Override
    public void onClick(View v) {
        if (!mEmailSent) {
            String email = mEdtEmailAddress.getText().toString();
            if (email.length() == 0) {
                showSnack(R.string.com_parse_ui_no_email_toast);
        } else {
            loadingStart();
            ParseUser.requestPasswordResetInBackground(email,
                new RequestPasswordResetCallback() {

                @Override
                public void done(ParseException e) {
                    if (isActivityDestroyed()) {
                        Log.e(LOG_TAG, "Activity was destroyed during password reset");
                        return;
                    }

                    loadingFinish();
                    if (e == null) {
                        mTxtInstructions.setText(
                                R.string.com_parse_ui_login_help_email_sent);
                        mEdtEmailAddress.setVisibility(View.INVISIBLE);
                        mBtnSubmit.setText(R.string.com_parse_ui_login_help_login_again_button_label);
                        mEmailSent = true;
                    } else {
                        Log.e(LOG_TAG, getString(R.string.com_parse_ui_login_warning_password_reset_failed) +
                          e.toString());
                        if (e.getCode() == ParseException.INVALID_EMAIL_ADDRESS ||
                            e.getCode() == ParseException.EMAIL_NOT_FOUND) {
                                showSnack(R.string.com_parse_ui_invalid_email_toast);
                            } else {
                                showSnack(R.string.com_parse_ui_login_help_submit_failed_unknown);
                            }
                        }
                    }
                });
            }
        } else {
            mLoginFragmentListener.onLoginHelpSuccess();
        }
    }

    // LoginFragmentBase method
    @Override
    protected String getLogTag() { return LOG_TAG; }
}
