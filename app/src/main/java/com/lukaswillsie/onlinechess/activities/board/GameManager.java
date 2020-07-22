package com.lukaswillsie.onlinechess.activities.board;

/**
 * This class is responsible for managing the state of a game of chess, by processing and responding
 * to actions made by the user. It interacts with the UI at a high-level by using a BoardDisplay
 * object. It accesses data about the game it is managing by using a GamePresenter object.
 */
public class GameManager {
    /**
     * The object containing all the data about the game that this object is managing
     */
    private  GamePresenter presenter;

    /**
     * The object managing the display for this GameManager
     */
    private BoardDisplay display;

    /**
     * Create a new GameManager that will manage the game represented by the given GamePresenter
     * and use the given BoardDisplay object to interact with the UI. The BoardDisplay object
     * must already have been built when it is given to this object (.build() must already have been
     * called).
     *
     * @param presenter - a GamePresenter representing the game that this object will manage
     * @param display - the object that this GameManager will use to interact with the UI
     */
    GameManager(GamePresenter presenter, BoardDisplay display) {
        this.presenter = presenter;
        this.display = display;

        this.display.setPresenter(presenter);
    }
}
