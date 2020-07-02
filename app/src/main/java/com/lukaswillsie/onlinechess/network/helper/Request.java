package com.lukaswillsie.onlinechess.network.helper;

import com.lukaswillsie.onlinechess.network.helper.requesters.Networker;

public class Request {
    private boolean active = false;

    public void setActive() {
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }
}
