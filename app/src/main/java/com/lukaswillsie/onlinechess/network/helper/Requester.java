package com.lukaswillsie.onlinechess.network.helper;

/**
 * This interface is used to identify Activities that may need to SEND REQUESTS TO THE SERVER via a
 * ServerHelper object. Each Activity that does this needs to implement a more specific interface,
 * depending on the nature of the request, for example LoginRequester. This interface also defines
 * behaviour central to all types of requests: namely handling the eventuality that the server has
 * disconnected since the last time a request was issued, and the eventuality that the server
 * encounters an error or sends  bad data.
 *
 * This also allows the front-end of the app to display different error messages to the user
 * depending on the nature of the problem, if it wishes.
 */
public interface Requester extends Networker {
    // TODO: Implement pinging on the server-side and then come back here and build it into serverHelper
    /**
     * To be called if a request has been made (other than simply a connect request) but the server
     * is found to be unresponsive during the course of handling the request
     */
    void connectionLost();

    /**
     * To be called if the server ever responds to a request with ReturnCodes.SERVER_ERROR, or if
     * the data received from the server is wrong, and doesn't correspond to its established
     * protocols
     */
    void serverError();
}
