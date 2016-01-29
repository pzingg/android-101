package com.gnatware.amber;

/**
 * Created by pzingg on 1/25/16.
 */

import com.parse.ParseUser;

/**
 * This interface must be implemented by activities that contain this
 * fragment to allow an interaction in this fragment to be communicated
 * to the activity and potentially other fragments contained in that
 * activity.
 * <p/>
 * See the Android Training lesson <a href=
 * "http://developer.android.com/training/basics/fragments/communicating.html"
 * >Communicating with Other Fragments</a> for more information.
 */
public interface LoginFragmentListener {
    public void onPushExistingAccount(String emailAddress);
    public void onPushCreateAccount(String emailAddress);
    public void onBackClicked();
    public void onCancelClicked();
    public void onLoginHelpClicked();
    public void onLoginHelpSuccess();
    public void onLoginSuccess(ParseUser user);
    public void onCreateAccountSuccess(ParseUser user);
}
