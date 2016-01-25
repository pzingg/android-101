package gnatware.com.amber;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.LogOutCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.ui.ParseLoginBuilder;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private CoordinatorLayout mLayout;
    private TextView mTxtWelcome;
    private Switch mSwRiderOrDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        initializeState();
        validateUser(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        ParseUser user = ParseUser.getCurrentUser();
        if (user != null) {
            String role = user.getString("role");
            mSwRiderOrDriver.setChecked(role != null && role.equals("driver"));
        }
        updateLoginStateUI();
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
     */

    // Get Started button onclick method
    public void onGetStartedClicked(View view) {

        final String role = mSwRiderOrDriver.isChecked() ? "driver" : "rider";
        validateUser(role);
    }

    public void onLogInClicked(View view) {
        // Log in as another user
        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {
                updateLoginStateUI();
                if (e != null) {
                    Log.d(TAG, "onLoginClicked, could not log out: " + e.getMessage());
                }
                startLogInActivity();
            }
        });
    }

    // Private methods
    private void initializeState() {
        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        /*
        // Action bar is supplied by theme in AndroidManifest.xml
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
         */

        mLayout = (CoordinatorLayout) getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(mLayout);

        mTxtWelcome = (TextView) findViewById(R.id.txtWelcome);
        mSwRiderOrDriver = (Switch) findViewById(R.id.swRiderOrDriver);
    }

    private void showSnack(String message) {
        Snackbar snackbar = Snackbar.make(mLayout, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void updateLoginStateUI() {
        ParseUser user = ParseUser.getCurrentUser();
        String userId = (user != null) ? user.getObjectId() : null;
        String sessionId = (user != null) ? user.getSessionToken() : null;
        Boolean authenticated = (user != null && user.isAuthenticated());
        Boolean anonymous = (user != null && ParseAnonymousUtils.isLinked(user));
        String name = (!anonymous && user != null) ? user.getString("name") : null;
        if (name == null) {
            name = "Guest";
        }
        mTxtWelcome.setText("Welcome, " + name);
    }

    private void validateUser(final String role) {
        ParseUser user = ParseUser.getCurrentUser();
        String userId = (user != null) ? user.getObjectId() : null;
        String sessionId = (user != null) ? user.getSessionToken() : null;
        Boolean authenticated = (user != null && user.isAuthenticated());
        Boolean anonymous = (user != null && ParseAnonymousUtils.isLinked(user));

        if (anonymous) {
            if (sessionId == null) {
                Log.d(TAG, "validateUser: Creating and logging in new anonymous user");

                // No log out, because user is anonymous
                ParseAnonymousUtils.logIn(new LogInCallback() {

                    @Override
                    public void done(ParseUser loggedInUser, ParseException e) {
                        if (e != null) {
                            Log.d(TAG, "Anonymous login failed.");
                        } else {
                            if (role != null) {
                                saveRoleAndStartActivity(loggedInUser, role);
                            }
                        }
                    }
                });
            } else {
                Log.d(TAG, "validateUser: Anonymous user " + userId + " already has session");
                if (role != null) {
                    saveRoleAndStartActivity(user, role);
                }
            }
            if (role == null) {
                showSnack("Welcome, new user!");
            }
        } else {
            if (sessionId == null) {
                // Avoid "invalid session token" errors
                Log.d(TAG, "validateUser: No session token for user " + userId +
                        ", auth=" + String.valueOf(authenticated) +
                        " - logging out");

                // Force log out
                showSnack("Your previous session expired. Please log in again.");
                ParseUser.logOutInBackground(new LogOutCallback() {

                    @Override
                    public void done(ParseException e) {
                        updateLoginStateUI();
                        if (e != null) {
                            Log.d(TAG, "validateUser, could not log out: " + e.getMessage());
                        }
                    }
                });
            } else {
                if (!authenticated) {

                    // Returning non-anonymous user without a session
                    if (role != null) {
                        // Get started button was clicked
                        Log.d(TAG, "validateUser: Unauthenticated user " + userId +
                                ", auth=false" +
                                " - require log in");

                        // LoginState UI will set in askUserToLogIn method
                        askUserToLogIn();
                    } else {
                        Log.d(TAG, "validateUser: Unauthenticated user " + userId +
                                ", auth=" + String.valueOf(authenticated) +
                                " - no action taken");
                    }
                } else {

                    // Returning user is OK
                    Log.d(TAG, "validateUser " + userId +
                            ", auth=true" +
                            " - startActivity with role " + role);
                    if (role != null) {
                        // Returning user (anonymous or not) with a session
                        saveRoleAndStartActivity(user, role);
                    } else {
                        // See if returning user had a role
                        startActivityForRole(null);
                    }
                }
            }
        }
    }

    private void saveRoleAndStartActivity(final ParseUser user, final String role) {
        user.put("role", role);
        user.saveInBackground(new SaveCallback() {

            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.d(TAG, "User " + user.getObjectId() + " failed to save " + role + " role");
                } else {
                    Log.d(TAG, "User " + user.getObjectId() + " signed up as a " + role);
                    startActivityForRole(role);
                }
            }
        });
    }

    private void askUserToLogIn() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.user_login_required_message)
                .setTitle(R.string.user_login_required_title)
                .setPositiveButton(R.string.com_parse_ui_parse_login_button_label, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        // User clicked OK button
                        startLogInActivity();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        // User cancelled the dialog
                        showSnack("Log in canceled");
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startLogInActivity() {
        ParseLoginBuilder builder = new ParseLoginBuilder(MainActivity.this);
        startActivityForResult(builder.build(), 0);
    }

    private void startActivityForRole(String role) {
        if (role == null) {
            ParseUser user = ParseUser.getCurrentUser();
            if (user == null) {
                Log.d(TAG, "startActivityForRole(null): No current user");
            } else {
                role = user.getString("role");
                if (role == null) {
                    Log.d(TAG, "startActivityForRole(null): No role for user " + user.getObjectId());
                }
            }
            Log.d(TAG, "startActivityForRole(null): user.role=" + role);
        } else {
            Log.d(TAG, "startActivityForRole " + role);
        }
        if (role != null) {
            if (role.equals("rider")) {
                Log.d(TAG, "Starting rider map activity");
                Intent riderMapIntent = new Intent(getApplicationContext(), RiderMapActivity.class);
                startActivity(riderMapIntent);
            } else if (role.equals("driver")) {
                Log.d(TAG, "Starting driver requests activity");
                Intent driverRequestsIntent = new Intent(getApplicationContext(), DriverRequestsActivity.class);
                startActivity(driverRequestsIntent);
            }
        }
    }
}
