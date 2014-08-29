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

import com.salsaberries.narchiver.exceptions.TerminalException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njanetos
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     *
     * @param args
     */
    public static void main(String[] args) {

        try {
            Initializer init = new Initializer();
        } catch (TerminalException e) {
            logger.error("Encountered a terminal exception: " + e.getMessage());
        }

        Alerter alerter = new Alerter();
        try {
            alerter.alert();
        } catch (IOException e) {
            logger.error("Unable to send email! " + e.getMessage());
        }
    }
}
