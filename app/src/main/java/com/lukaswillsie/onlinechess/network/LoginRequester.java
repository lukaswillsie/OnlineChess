package com.lukaswillsie.onlinechess.network;

import com.lukaswillsie.onlinechess.network.Requester;

public interface LoginRequester extends Requester {
    void loginSuccess();

    void usernameInvalid();

    void passwordInvalid();

    void serverError();
}
