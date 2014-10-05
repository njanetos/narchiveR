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

import com.salsaberries.narchiver.exceptions.ConnectionException;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njanetos
 */
public class HttpRequest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HttpRequest.class);

    private String html;
    private ArrayList<Header> headers;
    private int statusCode;
    private BufferedImage image;

    /**
     * Opens a new HTTP connection using the information contained in an
     * {@link HttpMessage}.
     *
     * @param message The information used to open the session. If incomplete an
     * exception will occur.
     * @throws com.salsaberries.narchiver.exceptions.ConnectionException Throws
     * a connection exception after any error reading content.
     * @throws java.net.MalformedURLException Throws if the URL is malformed.
     * (This should never happen.)
     * @throws java.net.ProtocolException Throws if there was an error reading
     * from the stream.
     */
    public HttpRequest(HttpMessage message) throws ConnectionException, MalformedURLException, ProtocolException {

        logger.debug(message.getFormattedMessage());
        
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8118));
            HttpURLConnection.setFollowRedirects(false);

            URL url = new URL(message.getUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);

            // Default timeout is 120 seconds
            connection.setConnectTimeout(120000);

            // Allow the request to have content, if turned on
            if (!message.getContent().equals("")) {
                connection.setDoOutput(true);
            }

            // Set what type of request this is
            connection.setRequestMethod(message.getHttpType().toString());

            // Add headers
            for (Header h : message.getHeaders()) {
                connection.setRequestProperty(h.getName(), h.getValue());
            }

            // Open and write output stream, if the message has output
            if (!message.getContent().equals("")) {
                try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
                    writer.writeBytes(message.getContent());
                    writer.flush();
                }
            }

            // Get the status code
            statusCode = connection.getResponseCode();

            // Get the response, if not an image
            if (!message.isImage()) {

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String input;
                    StringBuilder response = new StringBuilder();

                    while ((input = reader.readLine()) != null) {
                        response.append(input);
                    }

                    // Unzip if necessary
                    String finalResponse = response.toString();
                    if ("gzip".equals(connection.getContentEncoding())) {
                        finalResponse = decompress(response.toString().getBytes("UTF-8"));
                    }

                    // Get the headers
                    Map<String, List<String>> heads = connection.getHeaderFields();
                    headers = new ArrayList<>();

                    for (String headName : heads.keySet()) {
                        for (String headValue : heads.get(headName)) {
                            headers.add(new Header(headName, headValue));
                        }
                    }

                    // Get html
                    html = finalResponse;
                    logger.debug(finalResponse);
                }
            } else {
                logger.info("Attempting to download image at " + message.getUrl());

                InputStream is = connection.getInputStream();
                image = ImageIO.read(is);

                File outputfile = new File("image.jpg");
                ImageIO.write(image, "jpg", outputfile);
            }

        } catch (IOException e) {
            logger.error("IOException " + e.getMessage());
            throw new ConnectionException(statusCode);
        }
    }

    /**
     *
     * @return The html body of the response.
     */
    public String getHtml() {
        return html;
    }

    /**
     *
     * @return The HTTP headers.
     */
    public ArrayList<Header> getHeaders() {
        return headers;
    }

    /**
     *
     * @return The HTTP status code of the response.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Compresses a string using gzip.
     *
     * @param string The string to compress.
     * @return A compressed array of bytes.
     * @throws IOException
     */
    public static byte[] compress(String string) throws IOException {
        byte[] compressed = null;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream(string.length()); GZIPOutputStream gos = new GZIPOutputStream(os)) {
            gos.write(string.getBytes());
        }
        return compressed;
    }

    /**
     * Decompresses a string using gzip.
     *
     * @param compressed The compressed array of bytes.
     * @return A decompressed string.
     * @throws IOException
     */
    public static String decompress(byte[] compressed) throws IOException {
        final int BUFFER_SIZE = 32;
        StringBuilder string;
        try (ByteArrayInputStream is = new ByteArrayInputStream(compressed); GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE)) {
            string = new StringBuilder();
            byte[] data = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = gis.read(data)) != -1) {
                string.append(new String(data, 0, bytesRead));
            }
        }
        return string.toString();
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

}
