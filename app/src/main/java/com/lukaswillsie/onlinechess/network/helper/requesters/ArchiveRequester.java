package com.lukaswillsie.onlinechess.network.helper.requesters;

public interface ArchiveRequester extends Requester, ArchivingRequester {
    void archiveSuccessful();
}
