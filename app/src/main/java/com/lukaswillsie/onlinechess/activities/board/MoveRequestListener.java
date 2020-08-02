package com.lukaswillsie.onlinechess.activities.board;

public interface MoveRequestListener {
    void moveSuccess(boolean promotionNeeded);

    void moveFailed();

    void connectionLost();
}
