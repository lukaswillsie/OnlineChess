package com.lukaswillsie.onlinechess;

import android.app.Application;

import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;

import java.util.List;

/**
 * We override Application to provide a place to store our ServerHelper, the object responsible for
 * managing network requests
 */
public class ChessApplication extends Application {
    /**
     * The name of the currently logged-in user; null if there is no such user
     */
    private String username;

    /**
     * The ServerHelper object that has established a connection with the server and can be used for
     * network requests by any Activity.
     */
    private ServerHelper serverHelper;

    /**
     * A list of games that the currently logged-in user is a part of
     */
    private List<UserGame> games;

    public List<UserGame> getGames() {
        return games;
    }

    public void setGames(List<UserGame> games) {
        this.games = games;
    }

    public ServerHelper getServerHelper() {
        return serverHelper;
    }

    public void setServerHelper(ServerHelper serverHelper) {
        this.serverHelper = serverHelper;
    }

    /**
     * Record that a user with the given username has been logged in and record the username for
     * later referencing.
     *
     * @param username - the username of the user that has been logged in
     */
    public void login(String username){
        this.username = username;
    }

    /**
     * Record that the current user has been logged out
     */
    public void logout() {
        this.username = null;
    }

    /**
     * Get the username of the user that is currently logged in
     *
     * @return the username of the user that is currently logged in
     */
    public String getUsername() {
        return username;
    }
}
