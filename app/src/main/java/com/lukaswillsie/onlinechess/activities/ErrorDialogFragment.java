package com.lukaswillsie.onlinechess.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

/**
 * This fragment is used to create AlertDialogs in the event that a server error or system error
 * occurs, or the connection to the server is lost while the app is running.
 * <p>
 * It implements basic error reporting functionality: displays a message and presents the user
 * with a "Try Again" button. It may, if client code wishes, also present the user with a "Cancel"
 * button, but doesn't have to.
 */
public class ErrorDialogFragment extends DialogFragment {
    /**
     * An object listening to this dialog, which we keep a reference to so that we the dialog can
     * notify the caller if the user presses any buttons
     */
    private ErrorDialogListener listener;
    /**
     * The message this fragment's dialog will display
     */
    private String message;

    /**
     * Create a new ErrorDialogFragment to display the given message and give callbacks to the given
     * listener.
     *
     * @param listener - the object listening to this fragment
     * @param message  - the message to display in this fragment's dialog
     */
    public ErrorDialogFragment(ErrorDialogListener listener, String message) {
        this.message = message;
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Create a simple dialog box displaying the message given to this object at creation and
        // a "Try Again" button, wired to the retry() method of the listener
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            listener.retry();
                        }
                    }
                });

        // If the listener we were given at creation is a CancellableErrorDialogListener, also
        // provide a "Cancel" button, wired to the cancel() method of the listener
        if (listener instanceof CancellableErrorDialogListener) {
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        ((CancellableErrorDialogListener) listener).cancel();
                    }
                }
            })
                    // Set an on cancel listener so that pressing back or tapping outside the dialog
                    // executes the cancel() method of listener
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            ((CancellableErrorDialogListener) listener).cancel();
                        }
                    });

            return builder.create();
        }
        // If the dialog isn't cancellable, prevent it from being closed through the back
        // button or from clicking outside of it
        else {
            AlertDialog dialog = builder.create();
            setCancelable(false); // As the fragment, we need to do this ourselves
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
    }

    /**
     * A class wanting to create a basic dialog with just a "Try Again" button needs to implement
     * this interface
     */
    public interface ErrorDialogListener {
        /**
         * Will be called when the user clicks the "Try Again" button
         */
        void retry();
    }

    /**
     * If a class wants to create a dialog that also contains a "Cancel" button, it must implement
     * this interface
     */
    public interface CancellableErrorDialogListener extends ErrorDialogListener {
        /**
         * Will be called when the user clicks the "Cancel" button
         */
        void cancel();
    }
}
