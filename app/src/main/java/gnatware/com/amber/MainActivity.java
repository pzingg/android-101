package gnatware.com.amber;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.ui.ParseLoginBuilder;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    
    private Switch mSwRiderOrDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        /*
        // Action bar is supplied by theme in AndroidManifest.xml
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
         */

        mSwRiderOrDriver = (Switch) findViewById(R.id.swRiderOrDriver);
        startActivityForRole(null);
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

    // Get Started button onclick method
    public void onLogOutClicked(View view) {
        ParseUser.logOutInBackground();
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

    private void validateUser(final String role) {
        ParseUser user = ParseUser.getCurrentUser();

        if (user == null) {
            Log.d(TAG, "validateUser: Creating and logging in new anonymous user");
            ParseAnonymousUtils.logIn(new LogInCallback() {

                @Override
                public void done(ParseUser loggedInUser, ParseException e) {
                    if (e != null) {
                        Log.d(TAG, "Anonymous login failed.");
                    } else if (role != null) {
                        saveRoleAndStartActivity(loggedInUser, role);
                    } else {
                        startActivityForRole(null);
                    }
                }
            });
        } else {
            String userId = user.getObjectId();
            Boolean authenticated = user.isAuthenticated();
            Boolean anonymous = ParseAnonymousUtils.isLinked(user);
            if (user.getSessionToken() == null) {

                // Avoid "invalid session token" errors
                Log.d(TAG, "validateUser: No session token for user " + userId +
                        ", anon=" + String.valueOf(anonymous) +
                        ", auth=" + String.valueOf(authenticated) +
                        " - logging out");
                ParseUser.logOut();
                // And return
            } else if (!authenticated && !anonymous) {

                // Returning non-anonymous user without a session
                if (role != null) {
                    // Get started button was clicked
                    Log.d(TAG, "validateUser: Unauthenticated user " + userId +
                            ", anon=" + String.valueOf(anonymous) +
                            ", auth=" + String.valueOf(authenticated) +
                            " - require log in");
                    logInUser();
                } else {
                    Log.d(TAG, "validateUser: Unauthenticated user " + userId +
                            ", anon=" + String.valueOf(anonymous) +
                            ", auth=" + String.valueOf(authenticated) +
                            " - no action taken");
                    // TODO: Show snackbar
                }
            } else {
                // Returning user is OK
                Log.d(TAG, "validateUser " + userId +
                        ", anon=" + String.valueOf(anonymous) +
                        ", auth=" + String.valueOf(authenticated) +
                        " - startActivity with role " + role);
                if (role != null) {
                    // Returning user (anonymous or not) with a session
                    saveRoleAndStartActivity(user, role);
                } else {
                    startActivityForRole(null);
                }
            }
        }
    }

    private void logInUser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.user_login_required_message)
                .setTitle(R.string.user_login_required_title)
                .setPositiveButton(R.string.com_parse_ui_parse_login_button_label, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button

                        ParseLoginBuilder builder = new ParseLoginBuilder(MainActivity.this);
                        startActivityForResult(builder.build(), 0);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        // TODO: Show snackbar
                    }
                });
        AlertDialog dialog = builder.create();
    }

    private void startActivityForRole(String role) {
        if (role == null) {
            ParseUser user = ParseUser.getCurrentUser();
            if (user == null) {
                Log.d(TAG, "No current user");
            } else {
                role = user.getString("role");
                if (role == null) {
                    Log.d(TAG, "No role defined for user " + user.getObjectId());
                }
            }
            Log.d(TAG, "startActivityForRole(null): role=" + role);
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
