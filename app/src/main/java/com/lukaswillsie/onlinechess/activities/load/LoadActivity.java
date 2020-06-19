package com.lukaswillsie.onlinechess.activities.load;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.MainActivity;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.ErrorDialogFragment;
import com.lukaswillsie.onlinechess.activities.login.LoginActivity;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.RememberMeHelper;
import com.lukaswillsie.onlinechess.network.helper.requesters.Connector;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoginRequester;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        // Immediately try and establish a connection with the server.
        try {
            new ServerHelper().connect(this);
        }
        catch (MultipleRequestException e) {
            Log.e(tag, "Multiple network requests occurred. Retrying connection");
            retry();
        }
    }

    /**
     * After a ServerHelper is tasked with establishing a connection, they will call this method on
     * the success, and pass a reference to themselves so they can be used for future network
     * operations.
     *
     * This implementation puts the ServerHelper as a Serializable extra into an Intent and then
     * starts LoginActivity
     *
     * @param helper - the ServerHelper object that has successfully established a connection with
     *               the server
     */
    @Override
    public void connectionEstablished(ServerHelper helper) {
        // We add the ServerHelper to ChessApplication for use by all subsequent activities
        ((ChessApplication)getApplicationContext()).setServerHelper(helper);

        // We try to check if we have any saved user data, that is, if a user has recently
        // clicked "Remember Me" when logging in.
        try {
            HashMap<String, String> savedData = new RememberMeHelper(this).savedUserData();

            // If there was an error querying saved data or there is no saved user data, we simply
            // start the LoginActivity
            if(savedData.get(RememberMeHelper.ERROR_KEY).equals("1") || savedData.get(RememberMeHelper.USERNAME_KEY) == null) {
                startActivity(new Intent(this, LoginActivity.class));
            }
            // Otherwise, we have saved user data that we can use to log in, and we attempt to do
            // just that
            else {
                try {
                    helper.login(this, savedData.get(RememberMeHelper.USERNAME_KEY), savedData.get(RememberMeHelper.PASSWORD_KEY));
                } catch (MultipleRequestException e) {
                    // This shouldn't happen. If it does, we log the problem and then move the user
                    // to the login page, after creating an apologetic toast
                    Log.e(tag, "Submitted multiple requests to ServerHelper");
                    Toast.makeText(this, R.string.automatic_login_failure, Toast.LENGTH_LONG).show();

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
     *
     * This implementation creates a dialog box to inform the user of the failure and present the
     * option to try again.
     */
    @Override
    public void connectionFailed() {
        DialogFragment failedDialog = new ErrorDialogFragment(this, getResources().getString(R.string.connection_failed_alert));
        failedDialog.show(getSupportFragmentManager(), "connection_failed_dialog");
    }

    /**
     * Called when the user clicks "Try Again" on a connection failed dialog
     */
    public void retry() {
        try {
            new ServerHelper().connect(this);
        }
        catch (MultipleRequestException e) {
            Log.e(tag, "Multiple network requests occurred. Retrying connection");
            retry();
        }
    }

    /**
     * Called by ServerHelper when a connection attempt is met with a system error. In this event,
     * we display a dialog box notifying the user that we couldn't establish a connection, and give
     * them the option to try again.
     */
    @Override
    public void systemError() {
        this.connectionFailed();
    }

    /**
     * This callback is for when the server has responded that the user's credentials are valid.
     * Note that this occurs BEFORE the server sends over the user's game data, so it doesn't
     * mean the login process is complete and the receiver of the callback should move to the next
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
        Toast.makeText(this, R.string.automatic_login_failure, Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, LoginActivity.class));
    }

    @Override
    public void passwordInvalid() {
        Toast.makeText(this, R.string.automatic_login_failure, Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, LoginActivity.class));
    }

    @Override
    public void loginComplete(List<Game> games) {
        ((ChessApplication)this.getApplicationContext()).setGames(games);
        Toast.makeText(this, R.string.automatic_login_success, Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    public void connectionLost() {
        this.connectionFailed();
    }

    @Override
    public void serverError() {

    }
}
