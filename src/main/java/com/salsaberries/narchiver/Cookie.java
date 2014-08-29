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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Cookie stores some temporary information from the website. Corresponds
 * to the concept of 'cookie' in a browser.
 * 
 * @author njanetos
 */
public class Cookie {
    
    /**
     *
     */
    public static final Logger logger = LoggerFactory.getLogger(Cookie.class);
    
    private Date expiration;
    private String value;
    private String name;
    
    /**
     * Builds a cookie from the input.
     * 
     * @param raw 
     */
    public Cookie(String raw) {
        
        name = raw.substring(0, raw.indexOf(";")).split("=")[0];
        value = raw.substring(0, raw.indexOf(";")).split("=")[1];
        raw = raw.substring(raw.indexOf(";")+2);
        raw = raw.substring(0, raw.indexOf(";")).split("=")[1];
        
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
        
        try {
            expiration = formatter.parse(raw);
        }
        catch (ParseException e) {
            logger.info("Unable to parse cookie date. Assuming it's way in the future.");
            expiration = new Date(17534932134000L);
        }        
    }

    /**
     *
     * @return
     */
    public Date getExpiration() {
        return expiration;
    }

    /**
     *
     * @param expiration
     */
    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    /**
     *
     * @return
     */
    public String getValue() {
        return value;
    }

    /**
     *
     * @param value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     *
     * @return
     */
    public Header getHeader() {
        return new Header("Cookie", name+"="+value);
    }
    
    /**
     *
     * @param otherCookie
     * @return
     */
    public boolean isTheSameAs(Cookie otherCookie) {
        return (otherCookie.getName().equals(getName()));
    }
    
    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return getName() + "=" + getValue();
    }
    
    /**
     *
     * @param cookies
     * @param cookie
     * @return
     */
    public static boolean isAlreadyIn(ArrayList<Cookie> cookies, Cookie cookie) {
        for (Cookie c : cookies) {
            if (c.isTheSameAs(cookie)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     *
     * @param cookies
     * @param cookie
     */
    public static void replace(ArrayList<Cookie> cookies, Cookie cookie) {
        
        ArrayList<Cookie> flaggedCookies = new ArrayList<>();
        
        for (Cookie c : cookies) {
            if (c.isTheSameAs(cookie)) {
                flaggedCookies.add(c);
            }
        }
        
        for (Cookie c : flaggedCookies) {
            cookies.remove(c);
        }
        
        cookies.add(cookie);
    }
}
