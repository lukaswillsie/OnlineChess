package com.lukaswillsie.onlinechess.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.lukaswillsie.onlinechess.R;

/**
 * This fragment is used to create AlertDialogs in the event that the server can't be connected to
 * when the app starts up. It provides a button that gives the user the option to try the connection
 * again.
 */
public class ErrorDialogFragment extends DialogFragment {
    /**
     * Activities creating instances of this fragment must implement this interface to receive
     * event callbacks.
     */
    public interface ErrorDialogListener {
        void retry();
    }

    /**
     * The Activity that created this dialog, which we keep a reference to so that we the dialog can
     * notify the Activity if the user presses "Try again"
     */
    private ErrorDialogListener listener;

    private String message;

    public ErrorDialogFragment(ErrorDialogListener listener, String message) {
        this.message = message;
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Create a simple dialog box that alerts the user to the failure to connect, and provides
        // a button for them to retry the connection. Passes this request on to listener, the
        // calling Activity
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == DialogInterface.BUTTON_POSITIVE) {
                            listener.retry();
                        }
                    }
                });

        return builder.create();
    }
}
