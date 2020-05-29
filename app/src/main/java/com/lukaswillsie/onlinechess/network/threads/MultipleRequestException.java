package com.lukaswillsie.onlinechess.network.threads;

public class MultipleRequestException extends Exception {
    public MultipleRequestException(String errorMessage) {
        super(errorMessage);
    }
}
