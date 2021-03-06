package com.lukaswillsie.onlinechess.activities.board;

import androidx.annotation.StringRes;

import com.lukaswillsie.onlinechess.activities.ErrorDialogFragment;

/**
 * Defines what functionality a ChessManager needs to have access to so that it can notify the user
 * of events like errors, or the termination of a game, and receive the user's response.
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

    /**
     * Show a dialog to the user notifying them that they won the game! The dialog doesn't have to
     * have any buttons, and can be closed or cancelled so the user can view the state of the board.
     *
     * @param resigned - whether or not the victory was achieved because the opponent resigned
     */
    void showUserWinDialog(boolean resigned);

    /**
     * Show a dialog to the user notifying them that they lost the game. The dialog doesn't have to
     * have any buttons, and can be closed or cancelled so the user can view the state of the board.
     */
    void showUserLoseDialog();

    /**
     * Show a dialog to the user notifying them that the game ended in a draw. The dialog doesn't
     * have to have any buttons, and can be closed or cancelled so the user can view the state of
     * the board.
     */
    void showUserDrawDialog();
}
