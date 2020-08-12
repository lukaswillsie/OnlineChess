package com.lukaswillsie.onlinechess.network.helper;

public class MultipleRequestException extends Exception {
    public MultipleRequestException(String errorMessage) {
        super(errorMessage);
    }
}
