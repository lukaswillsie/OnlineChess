package com.lukaswillsie.onlinechess.activities.login;

public class Format {
    static boolean validUsername(String username) {
        return username.length() > 0 && username.indexOf(' ') == -1 && username.indexOf(' ') == -1;
    }

    static boolean validPassword(String password) {
        return password.length() > 0 && password.indexOf(' ') == -1 && password.indexOf(' ') == -1;
    }
}
