package com.salsaberries.narchiver.exceptions;

/**
 * Deals with issues where the trawler is not logged in.
 * 
 * @author njanetos
 */
public class AuthenticationException extends Exception {

    /**
     *
     * @param message
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
