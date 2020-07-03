package com.lukaswillsie.onlinechess.network.helper;

/**
 * Represents the most basic possible idea of a network request. Captures the state of either being
 * active or non-active. Subclasses should represent concrete examples of a network request, like
 * a request to mark a game as archived, and add the necessary satellite data, like the ID of the
 * game.
 */
public abstract class Request {
    private boolean active = false;

    public void setActive() {
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }
}
