package com.lukaswillsie.onlinechess.activities;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.lukaswillsie.onlinechess.R;

/**
 * This class defines some useful functionality for Activities that might have to display error
 * dialogs to the user as a result of a failed network operation. Any Activity that wants to allow
 * the user to make a request of the server needs to be prepared for the possibility of server
 * error, system error, or loss of connection. So this class centralizes logic for creating dialogs
 * and prevents copy-pasting code.
 *
 * The way I've decided to handle these events is pretty simple. Each event has an error message
 * associated with it (defined in strings.xml). When the event occurrs, an AlertDialog appears with
 * that message, accompanied by a "Try Again" button. System and Server error dialogs also have a
 * "Cancel" button. The Activity wishing to create an error dialog must implement the below abstract
 * methods to lend functionality to the button(s). For example, if a server error occurs during a
 * login, the "Cancel" button might let the user access the EditTexts again so they can try logging
 * in a different account, if they want. Even though there is no guarantee that subsequent requests
 * will work, it's better to allow the user this freedom rather than only allowing them the option
 * of trying again over and over.
 *
 * Lost connection error dialogs do not have cancel buttons because the app cannot function without
 * a connection.
 */
public abstract class ErrorDialogActivity extends AppCompatActivity {
    private class SystemErrorDialogListener implements ErrorDialogFragment.CancellableErrorDialogListener {
        @Override
        public void retry() {
            retrySystemError();
        }

        @Override
        public void cancel() {
            cancelSystemError();
        }
    }

    private class ServerErrorDialogListener implements ErrorDialogFragment.CancellableErrorDialogListener {
        @Override
        public void retry() {
            retryServerError();
        }

        @Override
        public void cancel() {
            cancelServerError();
        }
    }

    private class ConnectionLostDialogListener implements ErrorDialogFragment.ErrorDialogListener {
        @Override
        public void retry() {
            retryConnection();
        }
    }

    public void createSystemErrorDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new SystemErrorDialogListener(), getResources().getString(R.string.system_error_alert));
        fragment.show(getSupportFragmentManager(), "system_error_dialog");
    }

    public void createServerErrorDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ServerErrorDialogListener(), getResources().getString(R.string.server_error_alert));
        fragment.show(getSupportFragmentManager(), "server_error_dialog");
    }

    public void createConnectionLostDialog() {
        ErrorDialogFragment fragment = new ErrorDialogFragment(new ConnectionLostDialogListener(), getResources().getString(R.string.connection_lost_alert));
        fragment.show(getSupportFragmentManager(), "system_error_dialog");
    }


    public abstract void retrySystemError();

    public abstract void cancelSystemError();


    public abstract void retryServerError();

    public abstract void cancelServerError();


    public abstract void retryConnection();
}
