package com.lukaswillsie.onlinechess.network;

import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;
import com.lukaswillsie.onlinechess.network.helper.requesters.Connector;

import java.util.List;

public class Server {
    private static ServerHelper serverHelper;

    private static String username;

    private static List<UserGame> games;

    public static void loggedIn(String username, List<UserGame> games) {
        Server.username = username;
        Server.games = games;
    }

    public static void logout() {
        username = null;
    }

    public static String getUsername() {
        return username;
    }

    public static List<UserGame> getGames() {
        return games;
    }

    public static void setGames(List<UserGame> games) {
        Server.games = games;
    }

    public static ServerHelper getServerHelper() {
        return serverHelper;
    }

    public static void build(Connector requester) {
        serverHelper = new ServerHelper(requester);
    }
}
