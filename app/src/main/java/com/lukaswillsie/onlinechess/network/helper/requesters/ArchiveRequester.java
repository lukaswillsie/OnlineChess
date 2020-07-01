package com.lukaswillsie.onlinechess.network.helper.requesters;

import com.lukaswillsie.onlinechess.network.threads.ReturnCodeThread;

public interface ArchiveRequester extends Requester, ArchivingRequester {
    void archiveSuccessful();
}
