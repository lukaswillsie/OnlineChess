package com.lukaswillsie.onlinechess.data;

public class Format {
    public static boolean validUsername(String username) {
        return 4 < username.length() && username.length() < 16 && username.indexOf(' ') == -1 && username.indexOf(' ') == -1;
    }

    public static boolean validPassword(String password) {
        return 4 < password.length() && password.length() < 16 && password.indexOf(' ') == -1 && password.indexOf(' ') == -1;
    }

    public static boolean validGameID(String gameID) {
        // gameID's are valid as long as they are non-empty and don't contain commas
        return gameID.indexOf(',') == -1 && gameID.length() != 0;
    }
}
