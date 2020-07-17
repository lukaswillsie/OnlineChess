package com.lukaswillsie.onlinechess.activities;

import android.content.Intent;
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
import com.lukaswillsie.onlinechess.data.RememberMeHelper;
import com.lukaswillsie.onlinechess.data.UserGame;
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
 * <p>
 * There is a potentially serious problem that Android forces us to be able to handle that this
 * class handles. What if, while our app is running in the background, it is terminated to free
 * up memory? Our ServerHelper reference in ChessApplication will be lost, as will any connection
 * our app had to the server before termination. When the user navigates back to our app, the OS
 * tries to re-launch our app from exactly where the user left off, except now we've got no
 * connection to the server.
 * <p>
 * If this happens in the "middle" of the app, for example on a screen depicting a game of chess, we
 * have a problem, because our app relies on the server for functionality. So we need a way to,
 * from any activity, re-establish a connection with the server and re-login the user if we detect
 * that our process was shut down. It's also possible that we lose our connection without being shut
 * down, but simply because something went wrong either server-side or on our side. So we need to be
 * able to try to re-establish the connection in this case, too.
 * <p>
 * Because most activities in our app need to be able to do this, and it's rather complicated, we
 * create this class to centralize code that can re-establish a connection with the server and
 * re-login the user while also managing the UI in a sensible way.
 * <p>
 * The process is simple: this class displays an AlertDialog with a ProgressBar and a loading
 * message until a connection is established and the user has been re-logged in, at which point the
 * dialog disappears and the class behind the dialog is notified that the process has completed.
 */
public class Reconnector implements Connector, LoginRequester {
    /**
     * Tag for logging to the console
     */
    private static final String tag = "Reconnector";

    /*
     * The activity that this class is doing its work for.
     */
    private ReconnectListener listener;

    /**
     * The Activity that we will use for UI operations, like displaying dialogs, starting other
     * activities or accessing app resources (mostly strings)
     */
    private AppCompatActivity activity;

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
     * Create a new Reconnector object set up to handle a reconnection process. The given activity
     * will be used for UI operations like displaying dialogs. The given ReconnectListener will
     * be notified whenever a reconnection attempt handled by this object completes successfully.
     *
     * @param listener - will be notified when reconnection attempts by this object complete
     *                 successfully
     * @param activity - will be used for necessary UI operations like displaying dialogs, starting
     *                 other activities, etc.
     */
    public Reconnector(ReconnectListener listener, AppCompatActivity activity) {
        this.activity = activity;
        this.listener = listener;
        this.state = ReconnectState.NOT_ACTIVE;
    }

    /**
     * Initiates the process of reconnection. This involves first connecting to the server and then
     * handling a re-login of the user. If they have saved their login information at some point
     * with 'Remember Me', we'll try and use that information to log them in again. Otherwise,
     * they'll be directed back to the login screen.
     */
    public void reconnect() {
        if (this.state == ReconnectState.NOT_ACTIVE) {
            // Create a loading dialog that just contains a ProgressBar and some text. Make it
            // uncancellable.
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
            LayoutInflater inflater = activity.getLayoutInflater();
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
                Log.e(tag, "Made multiple requests of ServerHelper");

                // Remove the loading dialog and display an error dialog
                this.activeDialog.cancel();
                this.activeDialog = null;

                this.state = ReconnectState.NOT_ACTIVE;
                showSystemErrorDialog();
            }
        }
    }

    /**
     * Called by ServerHelper if our connect request succeeds
     */
    @Override
    public void connectionEstablished(ServerHelper helper) {
        if (this.state == ReconnectState.CONNECTING) {
            // Save the given ServerHelper for later use
            ChessApplication application = ((ChessApplication) activity.getApplicationContext());
            application.setServerHelper(helper);

            // Change the text in the loading dialog box to read "Logging in..."
            TextView dialogText = this.activeDialog.findViewById(R.id.connecting_dialog_text);
            dialogText.setText(R.string.logging_in_dialog_text);

            try {
                this.state = ReconnectState.LOGGING_IN;

                try {
                    // Fetch a HashMap containing any user login information that has been saved on
                    // this device through use of the 'Remember Me' login feature
                    HashMap<String, String> savedUserData = new RememberMeHelper(activity).savedUserData();

                    String username = savedUserData.get(RememberMeHelper.USERNAME_KEY);
                    String password = savedUserData.get(RememberMeHelper.PASSWORD_KEY);
                    // If either username or password is non-null in the HashMap, there is saved
                    // user data. Otherwise, both are null, and either an error occurred or there
                    // is no saved data. In either case we require here that the user log in again
                    // by moving to LoginActivity
                    if (username != null) {
                        helper.login(this, username, password);
                        this.state = ReconnectState.LOGGING_IN;
                    } else {
                        Intent intent = new Intent(activity, LoginActivity.class);
                        activity.startActivity(intent);
                        activity.finish();
                    }
                } catch (IOException e) {
                    Log.e(tag, "IOException creating RememberMeHelper");
                    Intent intent = new Intent(activity, LoginActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                }

            } catch (MultipleRequestException e) {
                // This should never happen, but if it does, we notify the user that a problem
                // came up, and we present the option to try again. It's possible that the other
                // request will have finished by then. If this doesn't resolve the problem, it's
                // a bug, and this is the most graceful way we can handle it.
                Log.e(tag, "Made multiple requests of ServerHelper");

                // Remove the loading dialog and display an error dialog
                this.activeDialog.cancel();
                this.activeDialog = null;

                this.state = ReconnectState.NOT_ACTIVE;
                showSystemErrorDialog();
            }
        }
    }

    /**
     * Called by ServerHelper if our connect request fails
     */
    @Override
    public void connectionFailed() {
        if (this.state == ReconnectState.CONNECTING) {
            // As this is the end of a reconnection attempt, change the state back to NOT_ACTIVE,
            // cancel our loading dialog, and show an error dialog. Pressing "Try Again" on the
            // error dialog will start another connection attempt
            this.state = ReconnectState.NOT_ACTIVE;
            this.activeDialog.cancel();
            this.activeDialog = null;
            this.showConnectionFailedDialog();
        }
    }

    /**
     * Called by ServerHelper if the credentials we gave to ServerHelper are valid. Note that this
     * does not mean the login request is finished; the server still needs to send us a bunch of the
     * user's data. It's just a stage in the login process.
     */
    @Override
    public void loginSuccess() {
        if (this.state == ReconnectState.LOGGING_IN) {
            // Change the text in the loading dialog box to read "Loading..."
            ((TextView) this.activeDialog.findViewById(R.id.connecting_dialog_text)).setText(R.string.loading_dialog_text);
        }
    }

    /**
     * Called by ServerHelper if the credentials we gave as part of a login request are invalid
     * because the username we gave isn't in the system. This shouldn't really happen, because we
     * only ever provide credentials to ServerHelper if they've already been saved as part of a
     * 'Remember Me' login previously, and credentials only get saved in that case if the server
     * tells us they're correct, regardless, we cover the possibility here.
     */
    @Override
    public void usernameInvalid() {
        if (this.state == ReconnectState.LOGGING_IN) {
            Display.makeToast(activity.getApplicationContext(), "You need to log in again", Toast.LENGTH_LONG);
            Intent intent = new Intent(activity, LoginActivity.class);
            activity.startActivity(intent);
        }
    }

    /**
     * Called by ServerHelper if the credentials we gave as part of a login request are invalid
     * because the username password we gave doesn't match the username. This shouldn't really
     * happen, because we only ever provide credentials to ServerHelper if they've already been
     * saved as part of a 'Remember Me' login previously, and credentials only get saved in that
     * case if the server tells us they're correct, regardless, we cover the possibility here.
     */
    @Override
    public void passwordInvalid() {
        if (this.state == ReconnectState.LOGGING_IN) {
            Display.makeToast(activity.getApplicationContext(), "You need to log in again", Toast.LENGTH_LONG);
            Intent intent = new Intent(activity, LoginActivity.class);
            activity.startActivity(intent);
        }
    }

    /**
     * This method should always be overriden in subclasses, so that they know when the reconnection
     * process is over. However, subclasses MUST call this superclass implementation.
     *
     * @param games - A list of objects representing every game that the logged-in user is a
     *              participant in
     */
    @Override
    public void loginComplete(List<UserGame> games) {
        if (this.state == ReconnectState.LOGGING_IN) {
            this.activeDialog.cancel();
            this.activeDialog = null;

            this.state = ReconnectState.NOT_ACTIVE;
            ((ChessApplication) activity.getApplicationContext()).setGames(games);

            listener.reconnectionComplete();
        }
    }

    /**
     * Called by ServerHelper if we ask it to make a login request and it discovers that our
     * connection to the server has been lost. So we display a dialog to notify the user.
     */
    @Override
    public void connectionLost() {
        if (this.state == ReconnectState.LOGGING_IN) {
            this.activeDialog.cancel();
            this.activeDialog = null;

            this.state = ReconnectState.NOT_ACTIVE;

            showConnectionLostDialog();
        }
    }

    /**
     * Called by ServerHelper if a login request is met with a server error. This can happen if the
     * server does something we don't expect, or if it sends over a return code telling us that it
     * encountered an error. We display a dialog to notify the user.
     */
    @Override
    public void serverError() {
        if (this.state == ReconnectState.LOGGING_IN) {
            this.activeDialog.cancel();
            this.activeDialog = null;

            this.state = ReconnectState.NOT_ACTIVE;

            showServerErrorDialog();
        }
    }

    /**
     * Called by ServerHelper if a login request is foiled by a system error (like an IOException
     * encountered when interacting with the server). We display a dialog to notify the user.
     */
    @Override
    public void systemError() {
        if (this.state == ReconnectState.LOGGING_IN) {
            this.activeDialog.cancel();
            this.activeDialog = null;

            this.state = ReconnectState.NOT_ACTIVE;

            showSystemErrorDialog();
        }
    }

    /**
     * Displays a dialog notifying the user of a loss of connection
     */
    private void showConnectionLostDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ConnectionLostListener(), activity.getResources().getString(R.string.connection_lost_alert));
        fragment.show(activity.getSupportFragmentManager(), "connection_lost_dialog");
    }


    /* CODE FOR CREATING ERROR DIALOGS */

    /**
     * Displays a dialog notifying the user of a failure to establish a connection
     */
    private void showConnectionFailedDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ConnectionFailedListener(), activity.getResources().getString(R.string.connection_failed_alert));
        fragment.show(activity.getSupportFragmentManager(), "connection_failed_dialog");
    }

    /**
     * Displays a dialog notifying the user of a server error
     */
    private void showServerErrorDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ServerOrSystemErrorListener(), activity.getResources().getString(R.string.server_error_alert));
        fragment.show(activity.getSupportFragmentManager(), "server_error_dialog");
    }

    /**
     * Displays a dialog notifying the user of a system errors
     */
    private void showSystemErrorDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ServerOrSystemErrorListener(), activity.getResources().getString(R.string.system_error_alert));
        fragment.show(activity.getSupportFragmentManager(), "system_error_dialog");
    }

    /**
     * If we notify the user of a system or server error, and they want to retry, we do the same
     * thing regardless of which type of error it is: we initiate the reconnection process again.
     * This method condenses code for both errors in one place.
     */
    private void retryServerOrSystemError() {
        this.reconnect();
    }

    /**
     * When the user is notified of a server or system error, and clicks "cancel", the behaviour is
     * the same for each type of error: we move to LoadActivity, the entry point of our app
     */
    private void cancelServerOrSystemError() {
        Intent intent = new Intent(activity, LoadActivity.class);
        activity.startActivity(intent);
    }

    /**
     * Represents the three states that this object can be in with respect to a reconnection effort:
     * 1) Not engaged in a reconnection effort
     * 2) Currently attempting to connect to the server
     * 3) Connected to the server, and now currently attempting to log in a user
     */
    private enum ReconnectState {
        NOT_ACTIVE,
        CONNECTING,
        LOGGING_IN
    }

    /**
     * Listener that gets attached to a connection lost dialog
     */
    private class ConnectionLostListener implements ErrorDialogFragment.ErrorDialogListener {
        @Override
        public void retry() {
            reconnect();
        }
    }

    /**
     * Listener that gets attached to a connection failed dialog
     */
    private class ConnectionFailedListener implements ErrorDialogFragment.ErrorDialogListener {
        @Override
        public void retry() {
            reconnect();
        }
    }

    /**
     * In the event of a system or server error, either when connecting or logging in, our app does
     * the same thing. We display a dialog and give the user the option to "retry" or "cancel".
     * The behaviour of each is the same regardless of the type of error. So we combine both into
     * a single listener.
     */
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
}
