package com.lukaswillsie.onlinechess.network.helper.requesters;

import com.lukaswillsie.onlinechess.data.Game;

import java.util.List;

/**
 * Activities wishing to make login requests of a ServerHelper need to implement this interface so
 * that they can be notified of relevant events like success/failure of the request.
 *
 * We note that the login process determined by the server is as follows: first, the server returns
 * a code that tells the client whether or not the given username and password are valid. Then, if
 * they were, the server sends over the logged-in user's game data. In the event of a successful
 * login, we have two successive callbacks: first the caller is notified that the username and
 * password were accepted, then they are notified that all the game data has been received, and are
 * given the collected data. In the event of a server error or a rejection of the user's
 * credentials, there is only one callback.
 *
 */
public interface LoginRequester extends Requester {
    /**
     * This callback is for when the server has responded that the user's credentials are valid.
     * Note that this occurs BEFORE the server sends over the user's game data, so it doesn't
     * mean the login process is complete and the receiver of the callback should move to the next
     * Activity. It simply allows the activity to notify the user of the progress of the login
     * request.
     *
     * In this app, we give the login button a different colour and display different text depending
     * on where we are in the login request process.
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
     * This is the second callback of a successful login. Will only be called AFTER loginSuccess()
     * has already been called, but will not always be called. For example, if a system error or
     * server error occurs in the intervening time, it is possible for loginSuccess() to be called
     * without any successive loginComplete() call.
     *
     * Is called after the ServerHelper has fully processed data sent over by the server, to notify
     * the activity that the application can now proceed. Takes a list of Game objects, each of
     * which is a wrapper for data representing a game the newly logged-in user is playing, so that
     * this data can be saved for later access.
     * @param games - A list of objects representing every game that the logged-in user is a
     *              participant in
     */
    void loginComplete(List<Game> games);
}
