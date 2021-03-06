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

import com.salsaberries.narchiver.enums.HttpType;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 * HttpMessage stores all the information for an HTTP request/response. Send an
 * HtppMessage using an HttpRequest.
 *
 * @author njanetos
 */
public class HttpMessage {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HttpMessage.class);

    private HttpType httpType;
    private ArrayList<Header> headers;
    private String content;
    private String url;
    private boolean image;

    /**
     *
     * @param httpType Usually GET or POST.
     */
    public HttpMessage(HttpType httpType) {
        this.httpType = httpType;
        this.image = false;
    }

    /**
     *
     * @return The HTTP type, either GET or POST.
     */
    public HttpType getHttpType() {
        return httpType;
    }

    /**
     *
     * @param httpType The HTTP type, either GET or POST.
     */
    public void setHttpType(HttpType httpType) {
        this.httpType = httpType;
    }

    /**
     *
     * @return A list of the headers.
     */
    public ArrayList<Header> getHeaders() {
        return headers;
    }

    /**
     *
     * @param headers A list of the headers.
     */
    public void setHeaders(ArrayList<Header> headers) {
        this.headers = headers;
    }

    /**
     *
     * @return The body of this request.
     */
    public String getContent() {
        if (content == null) {
            return "";
        } else {
            return content;
        }
    }

    /**
     *
     * @param content The body of this request.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     *
     * @return The request url.
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     * @param url The request url.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isImage() {
        return image;
    }

    public void setImage(boolean image) {
        this.image = image;
    }

    /**
     * Intitializes a set of default headers that we should have to spoof a
     * browser.
     *
     * @param site The initialization file.
     */
    public void initializeDefaultHeaders(JSONObject site) {
        
        if (image) {
            initializeDefaultImageHeaders(site);
            return;
        }
        
        if (headers == null) {
            headers = new ArrayList<>();
        } else {
            headers.clear();
        }

        headers.add(new Header("Host", site.getString("BASE_URL").substring(7)));
        headers.add(new Header("User-Agent", site.getString("USER-AGENT")));
        headers.add(new Header("Accept", site.getString("ACCEPT")));
        headers.add(new Header("Accept-Language", site.getString("ACCEPT-LANGUAGE")));
        headers.add(new Header("Accept-Encoding", site.getString("ACCEPT-ENCODING")));
        headers.add(new Header("Connection", site.getString("CONNECTION")));
        headers.add(new Header("Content-Type", "application/x-www-form-urlencoded"));
    }
    
    public void initializeDefaultImageHeaders(JSONObject site) {
                if (headers == null) {
            headers = new ArrayList<>();
        } else {
            headers.clear();
        }

        headers.add(new Header("Host", site.getString("BASE_URL").substring(7)));
        headers.add(new Header("User-Agent", site.getString("USER-AGENT")));

        if (site.isNull("ACCEPT-IMAGE")) {
            headers.add(new Header("Accept", site.getString("ACCEPT")));
        }
        else {
            headers.add(new Header("Accept", site.getString("ACCEPT-IMAGE")));
        }

        headers.add(new Header("Accept-Language", site.getString("ACCEPT-LANGUAGE")));
        headers.add(new Header("Accept-Encoding", site.getString("ACCEPT-ENCODING")));
        headers.add(new Header("Connection", site.getString("CONNECTION")));
        //headers.add(new Header("Content-Type", "application/x-www-form-urlencoded"));
    }

    /**
     * Converts a list of cookies into a list of appropriate headers of the form
     * "Cookie: [Cookie text]".
     *
     * @param cookies The cookies to convert.
     */
    public void addCookieHeaders(ArrayList<Cookie> cookies) {
        if (cookies.isEmpty()) {
            return;
        }

        String add = cookies.get(0).toString();
        for (int i = 1; i < cookies.size(); ++i) {
            add = add + "; " + cookies.get(i).toString();
        }
        headers.add(new Header("Cookie", add));
    }

    /**
     * Returns the message formatted nicely for logging.
     *
     * @return Nicely formatted header.
     */
    public String getFormattedMessage() {

        String firstString = "\n\n ========= " + getHttpType().toString() + ": " + getUrl() + " =========";
        String string = firstString;

        for (Header h : headers) {
            string = string + "\n " + h.getName() + ": " + h.getValue();
        }

        string = string + "\n \n" + " Content: " + getContent() + "\n";
        return string + StringUtils.repeat("=", firstString.length() - 3) + "\n \n";
    }

    /**
     * Appends content onto the end of already existing content and encodes it
     * properly for sending through a GET or POST method using
     * {@link URLEncoder}.
     *
     * @param name
     * @param value
     */
    public void appendContent(String name, String value) {
        if (getContent().equals("")) {
            content = URLEncoder.encode(name) + "=" + URLEncoder.encode(value);
        } else {
            content = content + "&" + URLEncoder.encode(name) + "=" + URLEncoder.encode(value);
        }

        // Set the new content length header
        replaceHeader(new Header("Content-Length", Integer.toString(content.length())));
    }

    /**
     * Adds on a header.
     *
     * @param header
     */
    public void addHeader(Header header) {
        if (headers == null) {
            headers = new ArrayList<>();
            headers.add(header);
        } else {
            headers.add(header);
        }
    }

    /**
     * Checks to see if the header is already there and replaces it if so.
     *
     * @param header
     */
    public void replaceHeader(Header header) {
        for (int i = headers.size() - 1; i > 0; --i) {
            if (headers.get(i).getName().equals(header.getName())) {
                headers.remove(i);
            }
        }

        headers.add(header);
    }

}
