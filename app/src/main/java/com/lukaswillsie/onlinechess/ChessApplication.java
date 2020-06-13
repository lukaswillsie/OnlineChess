package com.lukaswillsie.onlinechess;

import android.app.Application;

import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;

import java.util.List;

/**
 * We override Application to provide a place to store our ServerHelper, the object responsible for
 * managing network requests
 */
public class ChessApplication extends Application {
    private ServerHelper serverHelper;

    private List<Game> games;

    public List<Game> getGames() {
        return games;
    }

    public void setGames(List<Game> games) {
        this.games = games;
    }

    public ServerHelper getServerHelper() {
        return serverHelper;
    }

    public void setServerHelper(ServerHelper serverHelper) {
        this.serverHelper = serverHelper;
    }
}
