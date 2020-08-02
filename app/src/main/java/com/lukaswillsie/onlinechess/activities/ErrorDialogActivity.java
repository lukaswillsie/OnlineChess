package com.lukaswillsie.onlinechess.activities;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.lukaswillsie.onlinechess.R;

/**
 * This class defines some useful functionality for Activities that might have to display error
 * dialogs to the user as a result of a failed network operation. Any Activity that wants to allow
 * the user to make a request of the server needs to be prepared for the possibility of server
 * error, system error, or loss of connection. So this class centralizes logic for creating dialogs
 * and prevents copy-pasting code.
 * <p>
 * The way I've decided to handle these events is pretty simple. Each event has an error message
 * associated with it (defined in strings.xml). When the event occurrs, an AlertDialog appears with
 * that message, accompanied by a "Try Again" button. System and Server error dialogs also have a
 * "Cancel" button. The Activity wishing to create an error dialog must implement the below abstract
 * methods to lend functionality to the button(s). For example, if a server error occurs during a
 * login, the "Cancel" button might let the user access the EditTexts again so they can try logging
 * in a different account, if they want. Even though there is no guarantee that subsequent requests
 * will work, it's better to allow the user this freedom rather than only allowing them the option
 * of clicking "Try again" over and over.
 * <p>
 * Lost connection error dialogs do not have cancel buttons because the app cannot function without
 * a connection.
 */
public abstract class ErrorDialogActivity extends AppCompatActivity {
    /**
     * Build and show a dialog communicating a system error to the user. The shown dialog provides
     * a "Cancel" and "Try Again" button. This dialog can be closed by clicking outside of it or
     * by pressing the back button. Either of these actions will be communicated back to the
     * Activity exactly as if the user had pressed cancel.
     */
    public void showSystemErrorDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new SystemErrorDialogListener(), getResources().getString(R.string.system_error_alert));
        fragment.show(getSupportFragmentManager(), "system_error_dialog");
    }

    /**
     * Build and show a dialog communicating a server error to the user. The shown dialog provides
     * a "Cancel" and "Try Again" button. This dialog can be closed by clicking outside of it or
     * by pressing the back button. Either of these actions will be communicated back to the
     * Activity exactly as if the user had pressed cancel.
     */
    public void showServerErrorDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ServerErrorDialogListener(), getResources().getString(R.string.server_error_alert));
        fragment.show(getSupportFragmentManager(), "server_error_dialog");
    }

    /**
     * Build and show a dialog communicating a system error to the user. The shown dialog provides
     * only a "Try Again" button. This dialog cannot be closed by clicking outside of it or by
     * pressing the back button. The only option is to press "Try Again" or close the app.
     */
    public void showConnectionLostDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ConnectionLostDialogListener(), getResources().getString(R.string.connection_lost_alert));
        fragment.show(getSupportFragmentManager(), "system_error_dialog");
    }

    /**
     *
     * @param resID
     * @param listener
     */
    public void showCustomDialog(@StringRes int resID, ErrorDialogFragment.ErrorDialogListener listener) {
        ErrorDialogFragment fragment = new ErrorDialogFragment(listener, getResources().getString(resID));
        fragment.show(getSupportFragmentManager(), "custom_error_dialog");
    }

    /**
     * Provides behaviour in the event that the user presses "Try Again" on a system error dialog
     */
    public abstract void retrySystemError();

    /**
     * Provides behaviour in the event that the user presses "Cancel" on a system error dialog
     */
    public abstract void cancelSystemError();

    /**
     * Provides behaviour in the event that the user presses "Try Again" on a server error dialog
     */
    public abstract void retryServerError();

    /**
     * Provides behaviour in the event that the user presses "Cancel" on a server error dialog
     */
    public abstract void cancelServerError();

    /**
     * Provides behaviour in the event that the user presses "Try Again" on a lost connection error
     * dialog.
     */
    public abstract void retryConnection();

    /**
     * A class built to allow the Activity and an error dialog communicating a system error to the
     * user to communicate.
     */
    private class SystemErrorDialogListener implements ErrorDialogFragment.CancellableErrorDialogListener {
        /**
         * Passes the fact that the user clicked "Try Again" on a system error dialog to the
         * Activity
         */
        @Override
        public void retry() {
            retrySystemError();
        }

        /**
         * Passes the fact that the user clicked "Cancel" on a system error dialog to the
         * Activity
         */
        @Override
        public void cancel() {
            cancelSystemError();
        }
    }

    /**
     * A class built to allow the Activity and an error dialog communicating a server error to the
     * user to communicate.
     */
    private class ServerErrorDialogListener implements ErrorDialogFragment.CancellableErrorDialogListener {
        /**
         * Passes the fact that the user clicked "Try Again" on a server error dialog to the
         * Activity
         */
        @Override
        public void retry() {
            retryServerError();
        }

        /**
         * Passes the fact that the user clicked "Cancel" on a server error dialog to the
         * Activity
         */
        @Override
        public void cancel() {
            cancelServerError();
        }
    }

    /**
     * A class built to allow the Activity and an error dialog communicating a loss of connection to
     * the user to communicate.
     */
    private class ConnectionLostDialogListener implements ErrorDialogFragment.ErrorDialogListener {
        /**
         * Passes the fact that the user clicked "Try Again" on a lost connection dialog to the
         * Activity
         */
        @Override
        public void retry() {
            retryConnection();
        }
    }
}
