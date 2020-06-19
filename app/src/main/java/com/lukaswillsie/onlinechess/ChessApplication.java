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
    /**
     * The ServerHelper object that has established a connection with the server and can be used for
     * network requests by any Activity.
     */
    private ServerHelper serverHelper;

    /**
     * A list of games that the currently logged-in user is a part of
     */
    private List<Game> games;

    /**
     * The credentials of the currently logged-in user. null if there is no such user
     */
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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
