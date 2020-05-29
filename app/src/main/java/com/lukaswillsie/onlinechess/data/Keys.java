package com.lukaswillsie.onlinechess.data;

import com.lukaswillsie.onlinechess.network.ServerHelper;

import java.util.List;

/**
 * A class that stores crucial objects and information as static fields at runtime, to prevent
 * objects from having to be passed around as extras in intents.
 */
public class Keys {
    public static final String USERNAME = "username";

    public static final String SERVER_HELPER = "server_helper";

    public static final String GAMES = "games";
}
