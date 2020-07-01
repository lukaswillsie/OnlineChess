package com.lukaswillsie.onlinechess.network.helper.requesters;

public interface RestoreRequester extends Requester, ArchivingRequester {
    void restoreSuccessful();
}
