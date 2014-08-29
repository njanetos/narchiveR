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
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    /**
     *
     * @param args
     */
    public static void main(String[] args) {        

        // Initialize properties
        
        try {
            FileInputStream is = new FileInputStream("initialize.json");
            JSONObject initialization = new JSONObject(IOUtils.toString(is));

            // Create a writer
            Writer writer = new Writer();

            /**
             * The ini file has the following:
             * 
             * SITES: An array of JSONObjects with:
             *      BASE_URL (string)
             *      DEPTH (number)
             *      EXCLUDE (array of tag urls to exclude)
             *      BEGIN (array of beginning tag urls)
             *      FREQUENCY (daily, weekly, monthly, yearly)
             *      COMPRESS (yes, no)
             *      CONNECTION_TYPE (tor, normal, indexed by ConnectionType)
             *      LOCATION (the name of the place to store the info)
             *      PARENT_CHILD_INCLUDE (if neither the parent site nor the prospective child site have this url then don't follow the child)
             */

            // Loop through all the sites
            JSONArray sites = initialization.getJSONArray("SITES");
            for (int i = 0; i < sites.length(); ++i) {

                JSONObject site = sites.getJSONObject(i);

                logger.info("Preparing to trawl " + site.getString("LOCATION"));

                // Create a new trawler
                Trawler trawler = new Trawler(site);
                    
                // Store what it found
                writer.storePages(trawler, site.getString("LOCATION"), site.getBoolean("COMPRESS"));
            }
        }
        catch (FileNotFoundException e) {
            logger.error(e.getMessage());
            Alerter alert = new Alerter();
            alert.alert();
            System.exit(1);
        }
        catch (IOException e) {
            logger.error(e.getMessage());
            Alerter alert = new Alerter();
            alert.alert();
            System.exit(1);
        }
        
    }
}
