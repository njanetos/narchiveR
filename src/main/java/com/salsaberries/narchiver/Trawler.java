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
import com.salsaberries.narchiver.exceptions.AuthenticationException;
import com.salsaberries.narchiver.exceptions.ConnectionException;
import com.salsaberries.narchiver.exceptions.RedirectionException;
import com.salsaberries.narchiver.exceptions.TrawlException;
import com.salsaberries.narchiver.exceptions.TrawlingInterrupt;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trawler is given an initial set of pages, then recursively finds pages within
 * those pages and uses a queue to organize everything.
 *
 * @author njanetos
 */
public class Trawler {

    private static final Logger logger = LoggerFactory.getLogger(Trawler.class);

    private final LinkedList<Page> pageQueue;
    private final HashSet<String> trawledPages;
    private final LinkedList<Page> writeQueue;
    private final int maxDepth;
    private final String baseURL;
    private final ArrayList<String> exclude;
    private final ArrayList<String> stopAt;
    private final ArrayList<String> mustInclude;
    private final ArrayList<String> captchas;
    private final boolean cookiesEnabled;
    private final JSONObject site;
    private int loginAttempts;
    private final ArrayList<Cookie> cookies;
    private final Random random;
    private final String outputLocation;
    private boolean needToLogin = false;

    /**
     * Trawler implements the recursive algorithm to search the web page.
     *
     * @param site
     * @throws com.salsaberries.narchiver.exceptions.TrawlException
     */
    public Trawler(JSONObject site) throws TrawlException {

        // Initialize the random variable
        random = new Random();

        this.site = site;

        // Create the initial beginning pages
        ArrayList<Page> pages = new ArrayList<>();
        for (int j = 0; j < site.getJSONArray("BEGIN").length(); ++j) {
            pages.add(new Page(site.getJSONArray("BEGIN").getString(j), 0));
        }

        // Create the list of EXCLUSIONS
        exclude = new ArrayList<>();
        for (int j = 0; j < site.getJSONArray("EXCLUDE").length(); ++j) {
            exclude.add(site.getJSONArray("EXCLUDE").getString(j));
        }

        // Create the list of STOP_AT
        stopAt = new ArrayList<>();
        for (int j = 0; j < site.getJSONArray("STOP_AT").length(); ++j) {
            stopAt.add(site.getJSONArray("STOP_AT").getString(j));
        }

        // Create the list of MUST_INCLUDE
        mustInclude = new ArrayList<>();
        for (int j = 0; j < site.getJSONArray("MUST_INCLUDE").length(); ++j) {
            mustInclude.add(site.getJSONArray("MUST_INCLUDE").getString(j));
        }

        // Create the list of CAPTCHA
        captchas = new ArrayList<>();
        for (int j = 0; j < site.getJSONArray("CAPTCHA").length(); ++j) {
            captchas.add(site.getJSONArray("CAPTCHA").getString(j));
        }

        // Are cookies enabled?
        cookiesEnabled = site.getBoolean("COOKIES");

        // Set the maximum number of login attempts
        loginAttempts = site.getInt("MAX_LOGIN_ATTEMPTS");

        pageQueue = new LinkedList<>();
        trawledPages = new HashSet<>();
        writeQueue = new LinkedList<>();

        this.maxDepth = site.getInt("DEPTH");
        this.baseURL = site.getString("BASE_URL");

        // Push the initial pages onto the queue
        for (int i = 0; i < pages.size(); ++i) {
            pageQueue.add(pages.get(i));
        }

        // Initialize cookies
        cookies = new ArrayList<>();

        Date date = new Date();
        outputLocation = Long.toString(date.getTime());

        logger.info("Begun trawling at " + date.getTime() + ".");

        // Start trawling
        while (pageQueue.size() != 0) {
            // If there are pages remaining to visit, visit the next one.
            visitNext();

            // If the number of final pages is higher than some number, write them to file.
            // This will remove everything from finalPages. Also, it will remove their
            // html content. Also, run a pass filter.
            if (writeQueue.size() > site.getInt("WRITE_BUFFER")) {
                flushToFile();
                logger.info("Page queue: " + pageQueue.size() + ", write queue: " + writeQueue.size() + ". Total sites found: " + trawledPages.size());
                logger.info("Total memory: " + Long.toString(Runtime.getRuntime().totalMemory()));
                logger.info("Used memory: " + Long.toString(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
            }
        }

        logger.info("Trawling has terminated for " + site.getString("BASE_URL") + ". Writing to file.");
        
        // Finish any final flushing
        flushToFile();

        // Compress the file
        try {
            Writer.zipDirectory(site.getString("LOCATION") + "/" + outputLocation, site.getString("LOCATION") + "/" + outputLocation + ".zip");
            // Remove the original directory
            FileUtils.deleteDirectory(new File(site.getString("LOCATION") + "/" + outputLocation));
        } catch (IOException e) {
            logger.error("Failed to zip " + site.getString("LOCATION") + "/" + outputLocation);
        }
    }

    private void visitNext() throws TrawlException {

        // Wait a random time period
        try {
            Thread.sleep(random.nextInt(site.getInt("LOWER_WAIT_TIME")) + site.getInt("UPPER_WAIT_TIME"));
        } catch (InterruptedException ex) {

        }

        Page page = null;
        try {
            // Do we need to log in?
            if (needToLogin) {
                needToLogin = (!login());
            } else {
                // Reset the login attempts
                loginAttempts = site.getInt("MAX_LOGIN_ATTEMPTS");
                page = pageQueue.removeFirst();
                visit(page);
            }
        } catch (AuthenticationException e) {
            // Re-add the page (at the beginning) if necessary
            if (page != null) {
                if (!page.registerTrawlInterrupt()) {
                    // Don't do anything: It's been interrupted too many times.
                    logger.error("Trawling has been interrupted for this page too many times. Removing from site map.");
                } else {
                    // Push the page back on.
                    pageQueue.push(page);
                }
            }
            needToLogin = true;
        } catch (TrawlingInterrupt e) {
            if (page != null) {
                if (!page.registerTrawlInterrupt()) {
                    // Don't do anything: It's been interrupted too many times.
                    logger.error("Trawling has been interrupted for this page too many times. Removing from site map.");
                } else {
                    // Push the page back on.
                    pageQueue.push(page);
                }
            }

            logger.debug("Warning! Trawling was interrupted. Retrying in 10 seconds: " + e.getMessage());

            // Wait one minute
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {

            }
        } catch (RedirectionException e) {
            // Set this page's URL to the new URL and push it back onto the queue.
            if (page != null) {
                logger.info("Redirecting to page " + e.getMessage() + ". I'm going to set this to this page's new URL, push it back onto the queue, and restart.");
                page.setTagURL(e.getMessage().substring(baseURL.length()));
                pageQueue.add(page);
            }
        }
    }

    private boolean login() throws TrawlException {
        --loginAttempts;

        if (loginAttempts < 0) {
            logger.error("Warning! Exceeded maximum number of login attempts! Program is now exiting.");
            throw new TrawlException("Maximum login attempts exceeded.");
        }

        logger.info("Attempting to log in at " + baseURL + site.getString("LOGIN_URL"));

        HttpMessage httpGet = new HttpMessage(HttpType.GET);
        httpGet.setUrl(baseURL + site.getString("LOGIN_URL"));
        httpGet.initializeDefaultHeaders(site);
        httpGet.addCookieHeaders(cookies);

        try {
            HttpRequest httpRequest = new HttpRequest(httpGet);

            // Get headers
            ArrayList<Header> headers = httpRequest.getHeaders();
            // Parse the cookies
            getTempCookies(headers);

            String body = httpRequest.getHtml();

            Document doc = Jsoup.parse(body);
            Element login = doc.getElementById(site.getString("LOGIN_ELEMENT"));

            if (login == null) {
                throw new TrawlException("Failed to find login form.");
            }

            // Grab any hidden fields
            Elements hidden = login.getElementsByAttributeValue("type", "hidden");

            // Build the post response
            HttpMessage httpPost = new HttpMessage(HttpType.POST);
            httpPost.initializeDefaultHeaders(site);
            httpPost.addCookieHeaders(cookies);
            // TODO: Read this from the html!
            httpPost.setUrl(baseURL + site.getString("LOGIN_SUBMIT"));

            httpPost.appendContent(site.getString("USERNAME_FIELD"), site.getString("USERNAME"));
            httpPost.appendContent(site.getString("PASSWORD_FIELD"), site.getString("PASSWORD"));

            for (int i = 0; i < hidden.size(); ++i) {
                httpPost.appendContent(hidden.get(i).attr("name"), hidden.get(i).attr("value"));
            }
            //httpPost.appendContent("commit", "enter");

            // Log in
            HttpRequest response = new HttpRequest(httpPost);
            headers = response.getHeaders();
            // Add any relevant cookies
            getTempCookies(headers);
            logger.info("Successfully logged in, response code: " + response.getStatusCode());

            // Send a GET request to the redirection URL before continuing. 
            httpGet = new HttpMessage(HttpType.GET);
            httpGet.initializeDefaultHeaders(site);
            httpGet.addHeader(new Header("Referer", baseURL + site.getString("LOGIN_URL")));
            String redirectionURL = getRedirectionURL(headers);
            httpGet.setUrl(redirectionURL);
            httpGet.addCookieHeaders(cookies);

            httpRequest = new HttpRequest(httpGet);
            logger.debug("Visited redirected page. Status code " + httpRequest.getStatusCode());

        } catch (ConnectionException | MalformedURLException | ProtocolException e) {
            // Did not successfully log in
            return false;
        }

        // Did we successfully log in? Then return true.
        return true;

    }

    private void flushToFile() {
        // Create regex pattern for final pass
        Pattern passFilter = Pattern.compile(site.getString("PASS_FILTER"));

        logger.info("\n\n=== Flushing data to file ====\n\n");

        // Run the pass filters
        for (int i = writeQueue.size() - 1; i >= 0; --i) {
            logger.info("Check " + writeQueue.get(i).getTagURL());
            if (!passFilter.matcher(writeQueue.get(i).getTagURL()).find()) {
                logger.info("Final pass: Removing page " + writeQueue.get(i).getTagURL());
                writeQueue.remove(i);
            }
        }

        Writer.storePages(writeQueue, site.getString("LOCATION") + "/" + outputLocation);

        logger.info("\n\n=== Finished flushing data ===\n\n");

    }

    /**
     * Visit the first page in the queue. Download all the info. Extract
     * relevant URLs. Add them to the queue. Add this page to the list of final
     * pages.
     */
    private void visit(Page page) throws AuthenticationException, TrawlingInterrupt, RedirectionException {

        logger.info(page.getDepth() + ": " + page.getTagURL());

        // Initialize the get request
        HttpMessage httpGet = new HttpMessage(HttpType.GET);
        httpGet.setUrl(baseURL + page.getTagURL());
        httpGet.initializeDefaultHeaders(site);
        httpGet.addCookieHeaders(cookies);

        // Get the page
        try {
            HttpRequest httpRequest = new HttpRequest(httpGet);

            // Get the headers
            ArrayList<Header> headers = httpRequest.getHeaders();

            // Read cookies into memory ALWAYS do this after httpRequest
            getTempCookies(headers);

            // Check whether we were redirected to the login page. Throws an 
            // authentication exception if we're redirected to the login page.
            // Throws a redirection exception otherwise. 
            checkHeaders(httpRequest);

            // Set the html
            page.setHtml(httpRequest.getHtml());

            // If we're below the max depth, extract pages from this site
            if (page.getDepth() < maxDepth) {
                ArrayList<Page> newPages = extractPages(page);

                // Add new pages to the queue.
                for (int i = 0; i < newPages.size(); ++i) {
                    pageQueue.add(newPages.get(i));
                }
            }

            // If all went well, add this page to the final queue for writing
            writeQueue.add(page);

        } catch (ConnectionException e) {
            switch (e.getMessage()) {
                // Server timed out. Re-add the page, wait, and restart.
                case "408":
                    throw new TrawlingInterrupt("408");
                case "500":
                    throw new TrawlingInterrupt("500");
            }
        } catch (MalformedURLException e) {
            // Malformed URL, remove this page from the list
            logger.error("There was a malformed url: " + baseURL + page.getTagURL() + ". This page will not be included in the final pages.");
        } catch (ProtocolException e) {
            throw new TrawlingInterrupt("Protocol exception. Will retry later...");
        }
    }

    /**
     * Extracts links from html, and returns a set of Pages with their parent
     * page already defined.
     *
     * @param html
     * @return A list of pages to follow.
     */
    private ArrayList<Page> extractPages(Page extractPage) {

        String html = extractPage.getHtml();

        ArrayList<Page> pages = new ArrayList<>();

        // Are we at a stop at page?
        for (String e : stopAt) {
            if (extractPage.getTagURL().contains(e)) {
                logger.info("Stopping at tag URL " + extractPage.getTagURL() + " because it contains " + e + ".");
                return pages;
            }
        }

        // Parse the html
        Document doc = Jsoup.parse(html);
        Elements links = doc.getElementsByTag("a");

        for (Element link : links) {

            String tagURL = "";
            boolean alreadyFollowed = false;
            boolean validURL = false;

            // First format the link
            if (link.attr("href").startsWith(baseURL)) {
                tagURL = link.attr("href").replace(baseURL, "");
                validURL = true;
            } else if (link.attr("href").startsWith("/")) {
                tagURL = link.attr("href");
                validURL = true;
            }

            // Has it already been followed?
            alreadyFollowed = trawledPages.contains(tagURL);

            // Does it violate the exclusion rules?
            boolean excluded = false;
            for (String e : exclude) {
                if (tagURL.contains(e)) {
                    excluded = true;
                }
            }

            if (!alreadyFollowed && validURL && !excluded) {
                logger.debug("Creating new page at URL " + tagURL);
                Page page = new Page(tagURL, extractPage.getDepth() + 1);
                trawledPages.add(tagURL);
                pages.add(page);
            }

            if (alreadyFollowed) {
                logger.debug("Skipping duplicate at URL " + tagURL);
            }
            if (!validURL) {
                logger.debug("Invalid URL at " + link.attr("href"));
            }
            if (excluded) {
                logger.debug("Exclusion at " + link.attr("href"));
            }
        }
        return pages;
    }

    /**
     *
     * @return The pages which are queued to be written.
     */
    public LinkedList<Page> getWriteQueue() {
        return writeQueue;
    }

    /**
     *
     * @return The base URL to visit, e.g., 'www.google.com'.
     */
    public String getBaseURL() {
        return baseURL;
    }

    private boolean testForCaptcha(String string) {
        for (String e : captchas) {
            if (string.contains(e)) {
                return true;
            }
        }

        return false;
    }

    private void getTempCookies(ArrayList<Header> headers) {
        for (Header h : headers) {
            if (h.getName().equals("Set-Cookie")) {
                Cookie cookie = new Cookie(h.getValue());
                // Check whether the cookie is already present, if so, replace it
                // with the newest one.
                if (!Cookie.isAlreadyIn(cookies, cookie)) {
                    cookies.add(cookie);
                } else {
                    Cookie.replace(cookies, cookie);
                }
            }
        }
    }

    /**
     * Searches through the headers to see if any redirect to the login page.
     *
     * @param headers
     * @return A response code if no issue was found.
     */
    private String checkHeaders(HttpRequest response) throws AuthenticationException, RedirectionException, TrawlingInterrupt {

        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            return Integer.toString(response.getStatusCode());
        }

        // We've been redirected
        if (response.getStatusCode() >= 300 && response.getStatusCode() < 400) {
            ArrayList<Header> headers = response.getHeaders();
            // Find the redirection
            for (Header h : headers) {
                if (h.getName().equals("Location")) {
                    // Have we been redirected to the login page?
                    if (h.getValue().contains(site.getString("LOGIN_URL"))) {
                        logger.info("We've been redirected to the login page! " + h.getValue());
                        throw new AuthenticationException(h.getValue());
                    } else {
                        logger.info("We've been redirected somewhere within the site.");
                        throw new RedirectionException(h.getValue());
                    }
                }
            }
        }

        // Permissions status
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            if (response.getStatusCode() == 403) {
                throw new TrawlingInterrupt(Integer.toString(response.getStatusCode()));
            }
        }

        // Server error
        if (response.getStatusCode() >= 500 && response.getStatusCode() < 600) {
            throw new TrawlingInterrupt(Integer.toString(response.getStatusCode()));
        }

        return Integer.toString(response.getStatusCode());
    }

    private String getRedirectionURL(ArrayList<Header> headers) {
        for (Header h : headers) {
            if (h.getName().equals("Location")) {
                return h.getValue();
            }
        }

        return "";
    }

}
