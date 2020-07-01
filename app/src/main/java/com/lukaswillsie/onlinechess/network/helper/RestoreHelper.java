package com.lukaswillsie.onlinechess.network.helper;

import com.lukaswillsie.onlinechess.network.threads.callers.ReturnCodeCaller;

public class RestoreHelper extends SubHelper implements ReturnCodeCaller {
    /**
     * Create a new SubHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    RestoreHelper(ServerHelper container) {
        super(container);
    }

    @Override
    public void onServerReturn(int code) {

    }
}
