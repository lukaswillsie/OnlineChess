package com.lukaswillsie.onlinechess.activities.login;

class Format {
    static boolean validUsername(String username) {
        return 5 < username.length() && username.length() < 16 && username.indexOf(' ') == -1 && username.indexOf(' ') == -1;
    }

    static boolean validPassword(String password) {
        return 5 < password.length() && password.length() < 16 && password.indexOf(' ') == -1 && password.indexOf(' ') == -1;
    }
}
