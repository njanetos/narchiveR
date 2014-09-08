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
import java.lang.management.ManagementFactory;
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

        String initialize;
        //if (ManagementFactory.getRuntimeMXBean().getInputArguments().isEmpty()) {
        //    logger.info("No initialization file specified! Defaulting to initialize.json.");
            initialize = "/home/njanetos/Dropbox/Programming/Narchiver/initialize.json";
        //}
        //else {
        //    initialize = ManagementFactory.getRuntimeMXBean().getInputArguments().get(0).split("=")[1];
        //}

        try {
            Initializer init = new Initializer(initialize);
        } catch (TerminalException e) {
            logger.info("Encountered a terminal exception: " + e.getMessage());
        }
        
        logger.info("Finished trawling.");
    }
}
