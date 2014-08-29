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
import com.salsaberries.narchiver.exceptions.TrawlException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njanetos
 */
public class Initializer {
    
    private static final Logger logger = LoggerFactory.getLogger(Initializer.class);
    
    /**
     *
     * @throws TerminalException
     */
    public Initializer() throws TerminalException {
        
        // Initialize properties
        try {
            FileInputStream is = new FileInputStream("initialize.json");
            JSONObject initialization = new JSONObject(IOUtils.toString(is));

            // Register shutdown hook for graceful termination.
            final Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    // Email the logs to the specified address.
                    Alerter alerter = new Alerter();
                    try {
                        alerter.alert();
                    }
                    catch (IOException e) {
                        // Nothing could be done to prevent hard crash.
                        logger.error("Unable to send email! " + e.getMessage());
                        System.exit(1);
                    }
                }
            });

            // Loop through all the sites
            JSONArray sites = initialization.getJSONArray("SITES");
            for (int i = 0; i < sites.length(); ++i) {

                JSONObject site = sites.getJSONObject(i);

                logger.info("Preparing to trawl " + site.getString("LOCATION"));

                // Create a new trawler
                try {
                    Trawler trawler = new Trawler(site);
                }
                catch (TrawlException e) {
                    logger.error("Trawler for " + site.getString("LOCATION") + " failed: " + e.getMessage());
                }
            }
        }
        catch (FileNotFoundException e) {
            logger.error(e.getMessage());
            throw new TerminalException("Unable to find initialization file initialization.json.");
        }
        catch (IOException e) {
            throw new TerminalException("IOException: " + e.getMessage());
        }
    }
}
