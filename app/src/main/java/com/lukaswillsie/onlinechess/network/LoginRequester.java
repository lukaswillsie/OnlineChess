package com.lukaswillsie.onlinechess.network;

import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.network.Requester;

import java.util.List;

/**
 * Activities wishing to make login requests of a ServerHelper need to implement this interface so
 * that they can be notified of events pertaining to the login request
 */
public interface LoginRequester extends Requester, Networker {
    /**
     * This callback is for when the server has responded that the user's credentials are valid.
     * Note that this occurs BEFORE the server sends over the user's game data, so it doesn't
     * necessarily mean the login process is complete and the receiver of the callback should move
     * to the next Activity. It simply allows the activity to notify the user of the progress of the
     * login request.
     */
    void loginSuccess();

    /**
     * This callback is for when the server has responded that the username entered by the user
     * doesn't exist in its records.
     */
    void usernameInvalid();

    /**
     * Called when the server has responded that the password entered by the user doesn't match the
     * username entered by the user.
     */
    void passwordInvalid();

    /**
     * Will only be called AFTER loginSuccess() has already been called, but not always after
     * loginSuccess() has been called. For example, a system error or server error could occur in
     * the intervening time.
     *
     * Is called after the ServerHelper has fully processed data sent over by the server, to notify
     * the activity that the application can now proceed. Passes a list of Game objects, each of
     * which is a wrapper for data representing a game the newly logged-in user is playing, so that
     * this data can be saved for later access.
     * @param games - A list of objects representing every game that the logged-in user is a
     *              participant in
     */
    void loginComplete(List<Game> games);
}
