package com.gnatware.amber;

/**
 * Created by pzingg on 1/26/16.
 */

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.parse.ui.ParseLoginConfig;
import com.parse.ui.ParseOnLoadingListener;

import junit.framework.Assert;

/**
 * Activities that contain this fragment must implement the
 * LoginFragmentListener to handle interaction events.
 * Use the {@link CreateAccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public abstract class LoginFragmentBase extends Fragment {

    protected ParseLoginConfig mParseLoginConfig;
    protected LoginFragmentListener mLoginFragmentListener;
    protected ParseOnLoadingListener mLoadingListener;
    protected View mLayout;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(getLogTag(), "onAttach");

        if (context instanceof LoginFragmentListener) {
            mLoginFragmentListener = (LoginFragmentListener) context;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implement ParseLoginFragmentListener");
        }
        if (context instanceof ParseOnLoadingListener) {
            mLoadingListener = (ParseOnLoadingListener) context;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implement ParseOnLoadingListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(getLogTag(), "onDetach");

        mLoginFragmentListener = null;
        mLoadingListener = null;
    }

    protected Bundle initConfigAndView(LayoutInflater inflater, ViewGroup container, int layoutId) {
        mLayout = inflater.inflate(layoutId, container, false);

        Bundle arguments = getArguments();
        mParseLoginConfig = ParseLoginConfig.fromBundle(arguments, getActivity());
        Assert.assertTrue(mParseLoginConfig.isParseLoginEmailAsUsername());

        return arguments;
    }

    protected void showSnack(int resId) {
        showSnack(getString(resId));
    }

    protected void showSnack(CharSequence message) {
        Snackbar snackbar = Snackbar.make(mLayout, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    protected void loadingStart() {
        loadingStart(true);
    }

    protected void loadingStart(boolean showSpinner) {
        if (mLoadingListener != null) {
            mLoadingListener.onLoadingStart(showSpinner);
        }
    }

    protected void loadingFinish() {
        if (mLoadingListener != null) {
            mLoadingListener.onLoadingFinish();
        }
    }

    protected boolean isActivityDestroyed() {
        FragmentActivity activity = getActivity();
        return activity == null || activity.isDestroyed();
    }

    // Subclasses return their LOG_TAG with this method
    protected abstract String getLogTag();
}