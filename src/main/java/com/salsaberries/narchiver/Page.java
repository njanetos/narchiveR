/*
 * Copyright (C) 2014 Nick Janetos njanetos@sas.upenn.edu.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.salsaberries.narchiver;

/**
 * A Page just stores html info along with the url
 *
 * @author njanetos
 */
public class Page {

    private String tagURL;
    private String html;
    private final Page parentPage;
    private int trawlingInterruptsRemaining;
    private boolean written;

    /**
     *
     * @param tagURL The end URL, for example, '/items'.
     * @param parentPage The page from which this page was followed to.
     */
    public Page(String tagURL, Page parentPage) {
        this.tagURL = tagURL;
        this.parentPage = parentPage;
        trawlingInterruptsRemaining = 6;
        written = false;
    }

    /**
     *
     * @param tagURL The end URL, for example, '/items'.
     * @param trawlingInterruptsRemaining The number of interrupts which this page can handle before we don't contact it anymore.
     */
    public Page(String tagURL, int trawlingInterruptsRemaining) {
        this.trawlingInterruptsRemaining = trawlingInterruptsRemaining;
        parentPage = null;
        this.tagURL = tagURL;
        written = false;
    }

    /**
     * Returns the tag url.
     *
     * @return The end URL, for example, '/items'.
     */
    public String getTagURL() {
        return tagURL;
    }

    /**
     * Sets the tag url.
     *
     * @param tagURL The end URL, for example, '/items'.
     */
    public void setTagURL(String tagURL) {
        this.tagURL = tagURL;
    }

    /**
     * Gets the html source of the page.
     *
     * @return  The html source.
     */
    public String getHtml() {
        return html;
    }

    /**
     * Sets the html source.
     *
     * @param html The html source.
     */
    public void setHtml(String html) {
        this.html = html;
    }

    /**
     *
     * @return The number of trawling interrupts remaining before this page is removed.
     */
    public int getTrawlingInterruptsRemaining() {
        return trawlingInterruptsRemaining;
    }

    /**
     *
     * @param trawlingInterruptsRemaining
     */
    public void setTrawlingInterruptsRemaining(int trawlingInterruptsRemaining) {
        this.trawlingInterruptsRemaining = trawlingInterruptsRemaining;
    }

    /**
     * Returns the page's depth in the recursive search
     *
     * @return The page depth.
     */
    public int getDepth() {
        if (parentPage == null) {
            return 0;
        } else {
            return parentPage.getDepth() + 1;
        }
    }

    /**
     * Returns this page's parent, the page through which this page was reached.
     * 
     * @return Page's parent.
     */
    public Page getParent() {
        return parentPage;
    }

    /**
     *
     * @return Whether this page has had too many trawling interrupts
     */
    public boolean registerTrawlInterrupt() {
        --trawlingInterruptsRemaining;
        return (trawlingInterruptsRemaining > 0);
    }

    /**
     * Removes all high memory stuff. Only call after writing to the file or it
     * will be lost.
     */
    public void clear() {
        html = "";
    }

    /**
     *
     * @return Whether this has been flagged as written.
     */
    public boolean isWritten() {
        return written;
    }

    /**
     *
     * @param written
     */
    public void setWritten(boolean written) {
        this.written = written;
        this.html = "";
    }
    
    
}
