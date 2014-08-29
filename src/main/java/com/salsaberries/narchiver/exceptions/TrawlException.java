package com.salsaberries.narchiver.exceptions;

/**
 * Deals with issues where the trawler is not logged in.
 * 
 * @author njanetos
 */
public class TrawlException extends Exception {

    /**
     *
     * @param message
     */
    public TrawlException(String message) {
        super(message);
    }
}
