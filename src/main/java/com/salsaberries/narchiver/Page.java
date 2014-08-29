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

    /**
     *
     * @param tagURL
     * @param parentPage
     */
    public Page(String tagURL, Page parentPage) {
        this.tagURL = tagURL;
        this.parentPage = parentPage;
        if (parentPage == null) {
            trawlingInterruptsRemaining = 6;
        } else {
            this.trawlingInterruptsRemaining = parentPage.getTrawlingInterruptsRemaining();
        }
    }
    
    public Page(String tagURL, int trawlingInterruptsRemaining) {
        this.trawlingInterruptsRemaining = trawlingInterruptsRemaining;
        parentPage = null;
        this.tagURL = tagURL;
    }

    /**
     * Returns the tag url.
     *
     * @return
     */
    public String getTagURL() {
        return tagURL;
    }

    /**
     * Sets the tag url.
     *
     * @param tagURL
     */
    public void setTagURL(String tagURL) {
        this.tagURL = tagURL;
    }

    /**
     * Gets the html source of the page.
     *
     * @return
     */
    public String getHtml() {
        return html;
    }

    /**
     * Returns the html source.
     *
     * @param html
     */
    public void setHtml(String html) {
        this.html = html;
    }

    /**
     * Returns the page's depth in the recursive search
     *
     * @return
     */
    public int getDepth() {
        if (parentPage == null) {
            return 0;
        } else {
            return parentPage.getDepth() + 1;
        }
    }
    
    public Page getParent() {
        return parentPage;
    }

    public int getTrawlingInterruptsRemaining() {
        return trawlingInterruptsRemaining;
    }

    public void setTrawlingInterruptsRemaining(int trawlingInterruptsRemaining) {
        this.trawlingInterruptsRemaining = trawlingInterruptsRemaining;
    }
}
