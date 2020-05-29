package com.lukaswillsie.onlinechess;

import android.app.Application;

import com.lukaswillsie.onlinechess.network.ServerHelper;

/**
 * We override Application to provide a place to store our ServerHelper, the object responsible for
 * managing network requests
 */
public class ChessApplication extends Application {
    private ServerHelper serverHelper;

    public ServerHelper getServerHelper() {
        return serverHelper;
    }

    public void setServerHelper(ServerHelper serverHelper) {
        this.serverHelper = serverHelper;
    }
}
