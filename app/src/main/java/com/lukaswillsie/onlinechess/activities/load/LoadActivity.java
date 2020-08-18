package com.lukaswillsie.onlinechess.activities.load;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.Display;
import com.lukaswillsie.onlinechess.activities.ErrorDialogFragment;
import com.lukaswillsie.onlinechess.activities.MainActivity;
import com.lukaswillsie.onlinechess.activities.login.LoginActivity;
import com.lukaswillsie.onlinechess.data.RememberMeHelper;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.Server;
import com.lukaswillsie.onlinechess.network.helper.MultipleRequestException;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;
import com.lukaswillsie.onlinechess.network.helper.requesters.Connector;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoginRequester;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Code behind a simple loading screen that is displayed when the app first starts, covering up the
 * process of establishing a connection with the server.
 */
public class LoadActivity extends AppCompatActivity implements Connector, LoginRequester, ErrorDialogFragment.ErrorDialogListener {
    /**
     * Tag for logging information to the console
     */
    private static final String tag = "LoadActivity";
    /**
     * If a "remember me" login attempt is active, stores the name of the user being logged in.
     * Is null otherwise
     */
    String username;
    /**
     * Tracks where we are in our loading process
     */
    private Request activeRequest = Request.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);


        Server.build(this);
        this.activeRequest = Request.CONNECT;
    }

    /**
     * After a ServerHelper is tasked with establishing a connection, they will call this method on
     * the success, and pass a reference to themselves so they can be used for future network
     * operations.
     * <p>
     * This implementation puts the ServerHelper as a Serializable extra into an Intent and then
     * starts LoginActivity
     *
     * @param helper - the ServerHelper object that has successfully established a connection with
     *               the server
     */
    @Override
    public void connectionEstablished(ServerHelper helper) {
        // We try to check if we have any saved user data, that is, if a user has recently
        // clicked "Remember Me" when logging in.
        try {
            HashMap<String, String> savedData = new RememberMeHelper(this).savedUserData();

            // If there was an error querying saved data or there is no saved user data, we simply
            // start the LoginActivity
            if (savedData.get(RememberMeHelper.ERROR_KEY).equals("1") || savedData.get(RememberMeHelper.USERNAME_KEY) == null) {
                startActivity(new Intent(this, LoginActivity.class));
            }
            // Otherwise, we have saved user data that we can use to log in, and we attempt to do
            // just that
            else {
                try {
                    helper.login(this, savedData.get(RememberMeHelper.USERNAME_KEY), savedData.get(RememberMeHelper.PASSWORD_KEY));
                    this.username = savedData.get(RememberMeHelper.USERNAME_KEY);
                    this.activeRequest = Request.LOGIN;
                } catch (MultipleRequestException e) {
                    // This shouldn't happen. If it does, we log the problem and then move the user
                    // to the login page, after creating an apologetic Toast
                    Log.e(tag, "Submitted multiple requests to ServerHelper");
                    Display.makeToast(this, R.string.automatic_login_failure, Toast.LENGTH_LONG);

                    startActivity(new Intent(this, LoginActivity.class));
                }
            }
        }
        // If an error is thrown, we simply move to LoginActivity, because we can't automatically
        // log anyone in
        catch (IOException e) {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    /**
     * If a ServerHelper has been tasked by an implementation of this interface to establish a
     * connection, and the connection could not be successfully created, the ServerHelper will
     * call this method so that this event can be handled.
     * <p>
     * This implementation creates a dialog box to inform the user of the failure and present the
     * option to try again.
     */
    @Override
    public void connectionFailed() {
        DialogFragment failedDialog = new ErrorDialogFragment(this, getResources().getString(R.string.connection_failed_alert));
        failedDialog.show(getSupportFragmentManager(), "connection_failed_dialog");

        this.activeRequest = Request.NONE;
    }

    /**
     * Called when the user clicks "Try Again" on a connection failed dialog
     */
    public void retry() {
        Server.build(this);
        this.activeRequest = Request.CONNECT;
    }

    /**
     * Called by ServerHelper when a network request is met with a system error. Note that this
     * activity makes two kinds of network requests: it can ask a ServerHelper to establish a
     * connection to the server, and it can ask a ServerHelper to submit a login request to the
     * server. If either encounters a system error, ServerHelper calls this callback.
     */
    @Override
    public void systemError() {
        // We check if the request that was met with an error was a connect or login request
        if (this.activeRequest == Request.CONNECT) {
            // Display a dialog notifying the user that the connection failed
            this.connectionFailed();
        } else if (this.activeRequest == Request.LOGIN) {
            // Display an apologetic Toast and move the user to the login screen
            Display.makeToast(this, R.string.automatic_login_failure, Toast.LENGTH_LONG);

            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    /**
     * This callback is for when the server has responded that the user's credentials are valid.
     * This occurs BEFORE the server sends over the user's game data, so it doesn't mean the login
     * process is complete and the receiver of the callback should move to the next
     * Activity. It simply allows the activity to notify the user of the progress of the login
     * request.
     */
    @Override
    public void loginSuccess() {
        // In this activity, we won't do anything special when the login is validated.
    }

    /**
     * Called by ServerHelper when our login attempt is unsuccessful because the given username is
     * not in the server's records. In this activity, we handle that by displaying a message
     * telling the user that they couldn't be automatically logged in, before moving to the
     * LoginActivity
     */
    @Override
    public void usernameInvalid() {
        Display.makeToast(this, R.string.automatic_login_failure, Toast.LENGTH_LONG);
        startActivity(new Intent(this, LoginActivity.class));
    }

    /**
     * Called by ServerHelper when our login attempt is unsuccessful because the password we gave
     * doesn't match the username. We display an error message and move the user to the LoginActivity
     */
    @Override
    public void passwordInvalid() {
        Display.makeToast(this, R.string.automatic_login_failure, Toast.LENGTH_LONG);
        startActivity(new Intent(this, LoginActivity.class));
    }

    /**
     * Called by ServerHelper after our login attempt is complete.
     *
     * @param games - A list of objects representing every game that the logged-in user is a player
     *              in, sent by the server
     */
    @Override
    public void loginComplete(List<UserGame> games) {
        Server.loggedIn(username, games);

        Display.makeToast(this, R.string.automatic_login_success, Toast.LENGTH_LONG);
        startActivity(new Intent(this, MainActivity.class));
    }

    /**
     * Called by ServerHelper when a login attempt fails because ServerHelper discovers our
     * connection with the server has been lost
     */
    @Override
    public void connectionLost() {
        this.connectionFailed();
    }

    /**
     * Called by ServerHelper when a login attempt fails because of an error server-side
     */
    @Override
    public void serverError() {
        // Tell the user we couldn't log them in automatically and move to the manual login screen
        Display.makeToast(this, R.string.automatic_login_failure, Toast.LENGTH_LONG);

        startActivity(new Intent(this, LoginActivity.class));
    }

    private enum Request {
        NONE,
        CONNECT,
        LOGIN
    }
}
