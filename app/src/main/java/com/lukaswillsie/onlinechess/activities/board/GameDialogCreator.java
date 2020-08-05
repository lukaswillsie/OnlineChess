package com.lukaswillsie.onlinechess.activities.board;

import androidx.annotation.StringRes;

import com.lukaswillsie.onlinechess.activities.ErrorDialogFragment;

/**
 * Defines what functionality a ChessManager needs to have access to so that it can notify the user
 * of errors and receive the user's response.
 */
public interface GameDialogCreator {
    /**
     * Displays an error dialog for the user. The error dialog will contain the specified String
     * resource in its message. The dialog will have "Try Again" and "Cancel" buttons. These buttons
     * will be wired to the retry() and cancel() method of the given listener, respectively. The
     * dialog will be cancellable (the user can click outside of the dialog to make it go away), and
     * the given listener will receive a callback to its cancel() method if the dialog is cancelled.
     *
     * @param messageID - the ID of the String resource that will be the dialog's message
     * @param listener  - the object that will receive the callbacks from the created dialog
     */
    void showErrorDialog(@StringRes int messageID, ErrorDialogFragment.CancellableErrorDialogListener listener);

    /**
     * Displays an error dialog for the user, notifying them of a loss of connection. The dialog
     * will contain the given String resource as its message and will have one button, which will
     * say "Try Again", and will be connected to the retry() method of the listener.
     *
     * @param messageID - the ID of the String resource that will be the dialog's message
     * @param listener  - the object that will receive the callbacks from the created dialog
     */
    void showConnectionLostDialog(@StringRes int messageID, ErrorDialogFragment.ErrorDialogListener listener);
}
