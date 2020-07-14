package com.lukaswillsie.onlinechess.network.threads.callers;

import com.lukaswillsie.onlinechess.data.Game;

import java.util.List;

/**
 * Defines what behaviour an object must implement to be able to create LoginThreads to handle
 * login requests. Simply defines a set of callbacks that the creator of the Thread needs to
 * have available.
 */
public interface LoginCaller extends ThreadCaller {
    /**
     * To be called immediately once the server has confirmed the success of the login request
     */
    void loginSuccess();

    /**
     * To be called immediately once the server has responded that the given username does not exist
     */
    void usernameInvalid();

    /**
     * To be called immediately once the server has responded that the given password is invalid for
     * the given username
     */
    void passwordInvalid();

    /**
     * To be called once the whole login process is complete. That is, the login has been validated
     * by the server and all of the user's game data has been received and processed by the
     * LoginThread.
     * <p>
     * Note that a loginSuccess() call will ALWAYS proceed a loginComplete() call. However, it is
     * possible for there to be a loginSuccess() call without a corresponding loginComplete() call
     * if the server encounters an error or the LoginThread encounters a SystemError after having
     * called loginSuccess().
     * <p>
     * The Thread passes the result, a list of Game objects, each representing a game the logged-in
     * user is playing, to this method.
     */
    void loginComplete(List<Game> games);


    /**
     * To be called if, in the midst of the login call, the server returns -1, indicating that it
     * encountered an error. Can also be called if the server returns something that does not
     * conform to protocol.
     */
    void serverError();

}
