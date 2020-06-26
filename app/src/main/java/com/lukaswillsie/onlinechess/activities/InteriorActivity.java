package com.lukaswillsie.onlinechess.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.load.LoadActivity;
import com.lukaswillsie.onlinechess.activities.login.LoginActivity;
import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.data.RememberMeHelper;
import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;
import com.lukaswillsie.onlinechess.network.helper.requesters.Connector;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoginRequester;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * In short, this class provides a way to re-establish a connection with the server in the midst of
 * execution of the app, if we discover that we've lost our connection.
 *
 * There is a potentially serious problem that Android forces us to be able to handle that, so far,
 * we haven't handled. What if, while our app is running in the background, it is terminated to free
 * up memory? Our ServerHelper reference in ChessApplication will be lost, as will any connection
 * our app had to the server before termination. When the user navigates back to our app, the OS
 * tries to re-launch our app from exactly where the user left off, except now we've got no
 * connection to the server.
 *
 * If this happens in the "middle" of the app, for example on a screen depicting a game of chess, we
 * have a problem, because our app relies on the server for functionality. So we need a way to,
 * from any activity, re-establish a connection with the server and re-login the user if we detect
 * that our process was shut down. It's also possible that we lose our connection without being shut
 * down, but simply because something went wrong either server-side or on our side. So we need to be
 * able to try to re-establish the connection in this case, too.
 *
 * Because most activities in our app need to be able to do this, and it's rather complicated, we
 * create this class to centralize code that can re-establish a connection with the server and
 * re-login the user while also managing the UI in a sensible way.
 *
 * The process is simple: this class displays an AlertDialog with a ProgressBar and a loading
 * message until a connection is established and the user has been re-logged in.
 */
public abstract class InteriorActivity extends AppCompatActivity implements Connector, LoginRequester {
    /**
     * Tag for logging to the console
     */
    private static final String tag = "InteriorActivity";

    /**
     * The current state of this object
     */
    private ReconnectState state;

    /**
     * If there is currently an active loading dialog (one displaying loading text and a
     * ProgressBar), this keeps a reference to it. Is null otherwise.
     */
    private AlertDialog activeDialog;

    /**
     * Represents the three states that this object can be in with respect to a reconnection effort:
     * 1) Not engaged in a reconnection effort
     * 2) Currently attempting to connect to the server
     * 3) Connected to the server, and now currently attempting to log in a user
     */
    private enum ReconnectState {
        NOT_ACTIVE,
        CONNECTING,
        LOGGING_IN;
    }

    private void showConnectionLostDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ConnectionLostListener(), getResources().getString(R.string.connection_lost_alert));
        fragment.show(getSupportFragmentManager(), "connection_lost_dialog");
    }

    private class ConnectionLostListener implements ErrorDialogFragment.ErrorDialogListener {
        @Override
        public void retry() {
            reconnect();
        }
    }

    private void showConnectionFailedDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ConnectionFailedListener(), getResources().getString(R.string.connection_failed_alert));
        fragment.show(getSupportFragmentManager(), "connection_failed_dialog");
    }


    private class ConnectionFailedListener implements ErrorDialogFragment.ErrorDialogListener {
        @Override
        public void retry() {
            reconnect();
        }
    }

    private void showServerErrorDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ServerOrSystemErrorListener(), getResources().getString(R.string.server_error_alert));
        fragment.show(getSupportFragmentManager(), "server_error_dialog");
    }

    private void showSystemErrorDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ServerOrSystemErrorListener(), getResources().getString(R.string.system_error_alert));
        fragment.show(getSupportFragmentManager(), "system_error_dialog");
    }
    
    private class ServerOrSystemErrorListener implements ErrorDialogFragment.CancellableErrorDialogListener {

        @Override
        public void retry() {
            retryServerOrSystemError();
        }

        @Override
        public void cancel() {
            cancelServerOrSystemError();
        }
    }

    public void retryServerOrSystemError() {
        this.reconnect();
    }

    public void cancelServerOrSystemError() {
        Intent intent = new Intent(this, LoadActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.state = ReconnectState.NOT_ACTIVE;
    }

    /**
     * Initiates the process of reconnection. This involves first connecting to the server and then
     * handling a re-login of the user. If they have saved their login information at some point
     * with 'Remember Me', we'll try and use that information to log them in again. Otherwise,
     * they'll be directed back to the login screen.
     */
    public void reconnect() {
        if(this.state == ReconnectState.NOT_ACTIVE) {
            // Create a loading dialog that just contains a ProgressBar and some text. Make it
            // uncancellable.
            AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog);
            LayoutInflater inflater = this.getLayoutInflater();
            View layoutView = inflater.inflate(R.layout.loading_dialog_layout, null);
            builder.setView(layoutView);
            builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();

            this.activeDialog = dialog;

            try {
                ServerHelper helper = new ServerHelper();
                this.state = ReconnectState.CONNECTING;
                helper.connect(this);
            } catch (MultipleRequestException e) {
                // This should never happen, but if it does, we notify the user that a problem
                // came up, and we present the option to try again. It's possible that the other
                // request will have finished by then. If this doesn't resolve the problem, it's
                // a bug, and this is the most graceful way we can handle it.
                Log.e(getTag(), "Made multiple requests of ServerHelper");

                // Remove the loading dialog and display an error dialog
                this.activeDialog.cancel();
                this.activeDialog = null;

                this.state = ReconnectState.NOT_ACTIVE;
                showSystemErrorDialog();
            }
        }
    }

    @Override
    public void connectionEstablished(ServerHelper helper) {
        if(this.state == ReconnectState.CONNECTING) {
            // Save the given ServerHelper for later use
            ChessApplication application = ((ChessApplication) getApplicationContext());
            application.setServerHelper(helper);

            // Change the text in the loading dialog box to read "Logging in..."
            TextView dialogText = this.activeDialog.findViewById(R.id.connecting_dialog_text);
            dialogText.setText(R.string.logging_in_dialog_text);

            try {
                this.state = ReconnectState.LOGGING_IN;

                try {
                    // Fetch a HashMap containing any user login information that has been saved on
                    // this device through use of the 'Remember Me' login feature
                    HashMap<String, String> savedUserData = new RememberMeHelper(this).savedUserData();

                    String username = savedUserData.get(RememberMeHelper.USERNAME_KEY);
                    String password = savedUserData.get(RememberMeHelper.PASSWORD_KEY);
                    // If either username or password is non-null in the HashMap, there is saved
                    // user data. Otherwise, if both are null, either an error occurred or are there
                    // is no saved data. In either case we require here that the user log in again
                    // by moving to LoginActivity
                    if(username != null) {
                        helper.login(this, username, password);
                        this.state = ReconnectState.LOGGING_IN;
                    }
                    else {
                        Intent intent = new Intent(this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } catch (IOException e) {
                    Log.e(tag, "IOException creating RememberMeHelper");
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }

            } catch (MultipleRequestException e) {
                // This should never happen, but if it does, we notify the user that a problem
                // came up, and we present the option to try again. It's possible that the other
                // request will have finished by then. If this doesn't resolve the problem, it's
                // a bug, and this is the most graceful way we can handle it.
                Log.e(getTag(), "Made multiple requests of ServerHelper");

                // Remove the loading dialog and display an error dialog
                this.activeDialog.cancel();
                this.activeDialog = null;

                this.state = ReconnectState.NOT_ACTIVE;
                showSystemErrorDialog();
            }
        }
    }

    @Override
    public void connectionFailed() {
        if(this.state == ReconnectState.CONNECTING) {
            // As this is the end of a reconnection attempt, change the state back to NOT_ACTIVE,
            // cancel our loading dialog, and show an error dialog. Pressing "Try Again" on the
            // error dialog will start another connection attempt
            this.state = ReconnectState.NOT_ACTIVE;
            this.activeDialog.cancel();
            this.activeDialog = null;
            this.showConnectionFailedDialog();
        }
    }

    @Override
    public void loginSuccess() {
        if(this.state == ReconnectState.LOGGING_IN) {
            // Change the text in the loading dialog box to read "Loading..."
            ((TextView)this.activeDialog.findViewById(R.id.connecting_dialog_text)).setText(R.string.loading_dialog_text);
        }
    }

    @Override
    public void usernameInvalid() {
        if(this.state == ReconnectState.LOGGING_IN) {
            Toast.makeText(getApplicationContext(), "You need to log in again", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void passwordInvalid() {
        if(this.state == ReconnectState.LOGGING_IN) {
            Toast.makeText(getApplicationContext(), "You need to log in again", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }

    /**
     * This method should always be overriden in subclasses, so that they know when the reconnection
     * process is over. However, subclasses MUST call this superclass implementation.
     * @param games - A list of objects representing every game that the logged-in user is a
     *                participant in
     */
    @Override
    public void loginComplete(List<Game> games) {
        if(this.state == ReconnectState.LOGGING_IN) {
            this.activeDialog.cancel();
            this.activeDialog = null;

            this.state = ReconnectState.NOT_ACTIVE;
            ((ChessApplication)getApplicationContext()).setGames(games);
        }
    }

    @Override
    public void connectionLost() {
        if(this.state == ReconnectState.LOGGING_IN) {
            this.activeDialog.cancel();
            this.activeDialog = null;

            this.state = ReconnectState.NOT_ACTIVE;

            showConnectionLostDialog();
        }
    }

    @Override
    public void serverError() {
        if(this.state == ReconnectState.LOGGING_IN) {
            this.activeDialog.cancel();
            this.activeDialog = null;

            this.state = ReconnectState.NOT_ACTIVE;

            showServerErrorDialog();
        }
    }

    @Override
    public void systemError() {
        if(this.state == ReconnectState.LOGGING_IN) {
            this.activeDialog.cancel();
            this.activeDialog = null;

            this.state = ReconnectState.NOT_ACTIVE;

            showSystemErrorDialog();
        }
    }

    /**
     * Return the tag that this object uses for logging to the console. Ensures that when code in
     * this superclass logs to the console, the proper subclass tag is actually used, to ensure
     * accurate tagging
     */
    public abstract String getTag();
}
