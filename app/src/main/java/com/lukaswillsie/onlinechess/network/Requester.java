package com.lukaswillsie.onlinechess.network;

/**
 * A tagging interface. Is used to identify Activities that may need to make network requests of a
 * ServerHelper object. Each Activity that does this needs to implement an interface, depending on
 * the nature of the request. Each of these interfaces extends this interface, lending a common data
 * type to all Activities that make requests of the ServerHelper.
 */
public interface Requester {
}
