package gnatware.com.amber;

import android.content.Intent;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {

    Switch mSwRiderOrDriver;

    protected void saveRole(final ParseUser user, final String role) {

        user.put("role", role);
        user.saveInBackground(new SaveCallback() {

            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.d("MainActivity", "User " + user.getObjectId() + " failed to save " + role + " role");
                } else {
                    Log.d("MainActivity", "User " + user.getObjectId() + " signed up as a " + role);
                    redirectRiderToLocationActivity();
                }
            }
        });
    }

    // Get Started button onclick method
    public void onGetStartedClicked(View view) {

        final String role = mSwRiderOrDriver.isChecked() ? "driver" : "rider";

        // Verify anonymous user - see Android QuickStart
        ParseUser anonymousUser = ParseUser.getCurrentUser();
        if (anonymousUser == null) {
            ParseAnonymousUtils.logIn(new LogInCallback() {

                @Override
                public void done(ParseUser newUser, ParseException e) {
                    if (e != null) {
                        Log.d("MainActivity", "Anonymous login failed.");
                    } else {
                        saveRole(newUser, role);
                    }
                }
            });
        } else {
            saveRole(anonymousUser, role);
        }
    }

    protected void redirectRiderToLocationActivity() {

        ParseUser anonymousUser = ParseUser.getCurrentUser();
        if (anonymousUser == null) {
            Log.d("MainActivity", "No current user");
        } else {
            String role = anonymousUser.getString("role");
            if (role == null) {
                Log.d("MainActivity", "No role defined for user " + anonymousUser.getObjectId());
            } else {
                Log.d("MainActivity", "User " + anonymousUser.getObjectId() + " logged in as a " + role);
                if (role == null) {
                    Log.d("MainActivity", "No role defined for user " + anonymousUser.getObjectId());
                } else if (role.equals("rider")) {
                    Log.d("MainActivity", "Starting rider map activity");
                    Intent riderMapIntent = new Intent(getApplicationContext(), RiderMapActivity.class);
                    startActivity(riderMapIntent);
                } else if (role.equals("driver")) {
                    Log.d("MainActivity", "Starting driver requests activity");
                    Intent driverRequestsIntent = new Intent(getApplicationContext(), DriverRequestsActivity.class);
                    startActivity(driverRequestsIntent);
                }
            }
        }
    }

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

        redirectRiderToLocationActivity();
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
}
