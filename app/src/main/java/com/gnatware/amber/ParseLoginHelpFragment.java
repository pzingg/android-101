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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.ui.ParseLoginConfig;
import com.parse.ui.ParseOnLoadingListener;

/**
 * Fragment for the login help screen for resetting the user's password.
 */
public class ParseLoginHelpFragment extends Fragment implements OnClickListener {

  private static final String LOG_TAG = "ParseLoginHelpFragment";

  private ParseLoginConfig config;

  private LoginFragmentListener mLoginFragmentListener;
  private ParseOnLoadingListener mLoadingListener;

  private View mLayout;

  private TextView instructionsTextView;
  private EditText emailField;
  private Button submitButton;
  private boolean emailSent = false;

  public static ParseLoginHelpFragment newInstance(Bundle configOptions) {
    ParseLoginHelpFragment loginHelpFragment = new ParseLoginHelpFragment();
    loginHelpFragment.setArguments(configOptions);
    return loginHelpFragment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                           Bundle savedInstanceState) {
    config = ParseLoginConfig.fromBundle(getArguments(), getActivity());

    View v = inflater.inflate(R.layout.com_parse_ui_parse_login_help_fragment,
        parent, false);
    instructionsTextView = (TextView) v
        .findViewById(R.id.login_help_instructions);
    emailField = (EditText) v.findViewById(R.id.login_help_email_input);
    submitButton = (Button) v.findViewById(R.id.login_help_submit);

    submitButton.setOnClickListener(this);
    return v;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    if (context instanceof LoginFragmentListener) {
      mLoginFragmentListener = (LoginFragmentListener) context;
    } else {
      throw new IllegalArgumentException(
              "Activity must implemement ParseLoginFragmentListener");
    }
    if (context instanceof ParseOnLoadingListener) {
      mLoadingListener = (ParseOnLoadingListener) context;
    } else {
      throw new IllegalArgumentException(
              "Activity must implemement ParseOnLoadingListener");
    }
  }

  @Override
  public void onClick(View v) {
    if (!emailSent) {
      String email = emailField.getText().toString();
      if (email.length() == 0) {
        showSnack(R.string.com_parse_ui_no_email_toast);
      } else {
        loadingStart();
        ParseUser.requestPasswordResetInBackground(email,
            new RequestPasswordResetCallback() {
              @Override
              public void done(ParseException e) {
                if (isActivityDestroyed()) {
                  return;
                }

                loadingFinish();
                if (e == null) {
                  instructionsTextView
                      .setText(R.string.com_parse_ui_login_help_email_sent);
                  emailField.setVisibility(View.INVISIBLE);
                  submitButton
                      .setText(R.string.com_parse_ui_login_help_login_again_button_label);
                  emailSent = true;
                } else {
                  Log.d(LOG_TAG, getString(R.string.com_parse_ui_login_warning_password_reset_failed) +
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

  // Private methods from ParseLoginFragmentBase
  private void showSnack(CharSequence message) {
    Snackbar snackbar = Snackbar.make(mLayout, message, Snackbar.LENGTH_LONG);
    snackbar.show();
  }

  private void showSnack(int resId) {
    showSnack(getString(resId));
  }

  private void loadingStart() {
    loadingStart(true);
  }

  private void loadingStart(boolean showSpinner) {
    if (mLoadingListener != null) {
      mLoadingListener.onLoadingStart(showSpinner);
    }
  }

  private void loadingFinish() {
    if (mLoadingListener != null) {
      mLoadingListener.onLoadingFinish();
    }
  }

  private boolean isActivityDestroyed() {
    FragmentActivity activity = getActivity();
    return activity == null || activity.isDestroyed();
  }
}
