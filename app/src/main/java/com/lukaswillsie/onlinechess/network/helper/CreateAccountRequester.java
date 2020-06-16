package com.lukaswillsie.onlinechess.network.helper;

/**
 * Defines what events an Activity needs to be able to handle if it wants to use ServerHelper to
 * make account creation requests of the server.
 */
public interface CreateAccountRequester extends Requester{
    /**
     * Called when the server responds that the account creation request was successful
     */
    void createAccountSuccess();

    /**
     * Called when the server responds that the given username is already in use
     */
    void usernameInUse();

    /**
     * Called when the server responds that the given username and/or password are invalidly
     * formatted and can't be accepted
     */
    void formatInvalid();
}
