package com.lukaswillsie.onlinechess.network;

import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.network.Requester;

import java.util.List;

/**
 * Activities wishing to make login requests of a ServerHelper need to implement this interface so
 * that they can be notified of events pertaining to the login request
 */
public interface LoginRequester extends Requester, Networker {
    void loginSuccess();

    void usernameInvalid();

    void passwordInvalid();

    void loginComplete(List<Game> games);

    void serverError();
}
