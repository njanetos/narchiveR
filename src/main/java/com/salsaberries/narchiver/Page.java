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

import java.util.ArrayList;

/**
 * A Page just stores html info along with the url
 *
 * @author njanetos
 */
public class Page {

    private String tagURL;
    private String html;
    private int trawlingInterruptsRemaining;
    private int depth;
    private long date;
    private String path;

    /**
     *
     * @param tagURL The end URL, for example, '/items'.
     * @param referringPage
     * @param clickedLink
     */
    public Page(String tagURL, Page referringPage, String clickedLink) {
        this.tagURL = tagURL;
        trawlingInterruptsRemaining = 6;
        this.depth = referringPage.getDepth()+1;
        this.path = referringPage.getPath() + ", " + referringPage.getTagURL() + ": " + clickedLink;
    }

    /**
     *
     * @param tagURL The end URL, for example, '/items'.
     */
    public Page(String tagURL) {
        trawlingInterruptsRemaining = 6;
        this.tagURL = tagURL;
        depth = 0;
        this.path = "|";
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
        return depth;
    }
    
    /**
     * 
     * @param depth
     */
    public void setDepth(int depth) {
        this.depth = depth;
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

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public String toString() {
        String output = "";
        output += path;
        output += "\n";
        output += html;
        return output;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
}
