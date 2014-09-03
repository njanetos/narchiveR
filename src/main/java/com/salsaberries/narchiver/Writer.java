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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.LinkedList;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import static org.apache.commons.lang.SystemUtils.FILE_SEPARATOR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer writes data to the server.
 *
 * @author njanetos
 */
public class Writer {

    private static final Logger logger = LoggerFactory.getLogger(Writer.class);

    /**
     * Writes all the pages to file.
     *
     * @param pages
     * @param location
     */
    public static void storePages(LinkedList<Page> pages, String location) {

        logger.info("Dumping " + pages.size() + " pages to file at " + location + "/");

        File file = new File(location);
        // Make sure the directory exists
        if (!file.exists()) {
            try {
                file.mkdirs();
                logger.info("Directory " + file.getAbsolutePath() + " does not exist, creating.");
            } catch (SecurityException e) {
                logger.error(e.getMessage());
            }
        }
        // Write them to the file if they haven't been already written
        while (!pages.isEmpty()) {
            Page page = pages.removeFirst();
            if (!page.isWritten()) {
                logger.info("Writing to " + location + page.getTagURL());
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getAbsolutePath() + "/" + URLEncoder.encode(page.getTagURL())), "utf-8"))) {
                    writer.write(page.getHtml());
                } catch (IOException e) {
                    logger.warn(e.getMessage());
                }
            }
        }
    }

    /**
     *
     * @param files
     * @param output
     * @throws IOException
     */
    public static void compressFiles(Collection<File> files, File output) throws IOException {
        // Wrap the output file stream in streams that will tar and gzip everything
        try (FileOutputStream fos = new FileOutputStream(output);
                // Wrap the output file stream in streams that will tar and gzip everything
                TarArchiveOutputStream taos = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(fos)))) {

            // Enable support for long file names
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            // Put all the files in a compressed output file
            for (File f : files) {
                addFilesToCompression(taos, f, ".");
            }
        }
    }

    private static void addFilesToCompression(TarArchiveOutputStream taos, File file, String dir) throws IOException {
        // Create an entry for the file
        taos.putArchiveEntry(new TarArchiveEntry(file, dir + FILE_SEPARATOR + file.getName()));
        if (file.isFile()) {
            // Add the file to the archive
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            IOUtils.copy(bis, taos);
            taos.closeArchiveEntry();
            bis.close();
        } else if (file.isDirectory()) {
            // Close the archive entry
            taos.closeArchiveEntry();
            // Go through all the files in the directory and using recursion, add them to the archive
            for (File childFile : file.listFiles()) {
                addFilesToCompression(taos, childFile, file.getName());
            }
        }
    }

    /**
     * Zip the contents of the directory, and save it in the zipfile
     *
     * @param dir
     * @param zipfile
     * @throws java.io.IOException
     */
    public static void zipDirectory(String dir, String zipfile) throws IOException, IllegalArgumentException {
        // Check that the directory is a directory, and get its contents
        File d = new File(dir);
        if (!d.isDirectory()) {
            throw new IllegalArgumentException("Compress: not a directory:  " + dir);
        }
        String[] entries = d.list();
        byte[] buffer = new byte[4096]; // Create a buffer for copying
        int bytes_read;

        // Loop through all entries in the directory
        try ( // Create a stream to compress data and write it to the zipfile
                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile))) {
            for (String entrie : entries) {
                File f = new File(d, entrie);
                if (f.isDirectory()) {
                    continue; // Don't zip sub-directories
                }
                try (FileInputStream in = new FileInputStream(f) // Stream to read file
                        ) {
                    ZipEntry entry = new ZipEntry(f.getPath()); // Make a ZipEntry
                    out.putNextEntry(entry); // Store entry
                    while ((bytes_read = in.read(buffer)) != -1) // Copy bytes
                    {
                        out.write(buffer, 0, bytes_read);
                    }
                } // Make a ZipEntry
            }
        }
    }

}
