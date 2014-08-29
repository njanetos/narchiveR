package com.salsaberries.narchiver.exceptions;

/**
 * Deals with issues where the trawler is not logged in.
 * 
 * @author njanetos
 */
public class RedirectionException extends Exception {

    /**
     *
     * @param message
     */
    public RedirectionException(String message) {
        super(message);
    }
}
