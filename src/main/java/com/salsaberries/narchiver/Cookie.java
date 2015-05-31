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
 * A Cookie stores some temporary information from the website. Corresponds to
 * the concept of 'cookie' in a browser.
 *
 * @author njanetos
 */
public class Cookie {

    /**
     *
     */
    public static final Logger logger = LoggerFactory.getLogger(Cookie.class);

    private final String value;
    private final String name;

    /**
     * Builds a cookie from the input.
     *
     * @param raw
     */
    public Cookie(String raw) {
        name = raw.split("=")[0];
        value = raw.split("=")[1];
    }

    /**
     *
     * @return The value of this cookie.
     */
    public String getValue() {
        return value;
    }

    /**
     *
     * @return The name of this cookie.
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return Returns a header formatted for this cookie.
     */
    public Header getHeader() {
        return new Header("Cookie", toString());
    }

    /**
     * Checks whether this cookie has the same name as another.
     *
     * @param otherCookie The cookie to compare to.
     * @return True if a match.
     */
    public boolean isTheSameAs(Cookie otherCookie) {
        return (otherCookie.getName().equals(getName()));
    }

    /**
     * Returns a string formatted for this cookie.
     *
     * @return The formatted string.
     */
    @Override
    public String toString() {
        return getName() + "=" + getValue();
    }

    /**
     * Checks whether this cookie is already in a list (albeit possibly with a
     * different value.)
     *
     * @param cookies The list to search in.
     * @param cookie The cookie to compare.
     * @return True if it's already in the list.
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
     * Adds a {@link Cookie} to an array, but removes any existing cookies with
     * the same name first.
     *
     * @param cookies The list to add to.
     * @param cookie The cookie to add.
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
