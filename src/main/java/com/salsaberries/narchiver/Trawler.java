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

import com.DeathByCaptcha.Captcha;
import com.DeathByCaptcha.Exception;
import com.DeathByCaptcha.SocketClient;
import com.salsaberries.narchiver.enums.HttpType;
import com.salsaberries.narchiver.exceptions.AuthenticationException;
import com.salsaberries.narchiver.exceptions.ConnectionException;
import com.salsaberries.narchiver.exceptions.RedirectionException;
import com.salsaberries.narchiver.exceptions.TrawlException;
import com.salsaberries.narchiver.exceptions.TrawlingInterrupt;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.commons.codec.binary.Base64;
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
    private final ArrayList<String> excludeIfEqual;
    private final ArrayList<String> stopAt;
    private final JSONObject site;
    private int loginAttempts;
    private final ArrayList<Cookie> cookies;
    private final Random random;
    private final String outputLocation;
    private boolean needToLogin = true;

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
            pages.add(new Page(site.getJSONArray("BEGIN").getString(j)));
        }

        // Create the list of EXCLUSIONS
        exclude = new ArrayList<>();
        for (int j = 0; j < site.getJSONArray("EXCLUDE").length(); ++j) {
            exclude.add(site.getJSONArray("EXCLUDE").getString(j));
        }

        // Creat the list of EXCLUDE_IF_EQUAL
        excludeIfEqual = new ArrayList<>();
        if (!site.isNull("EXCLUDE_IF_EQUAL")) {
            for (int j = 0; j < site.getJSONArray("EXCLUDE_IF_EQUAL").length(); ++j) {
                excludeIfEqual.add(site.getJSONArray("EXCLUDE_IF_EQUAL").getString(j));
            }
        }

        // Create the list of STOP_AT
        stopAt = new ArrayList<>();
        for (int j = 0; j < site.getJSONArray("STOP_AT").length(); ++j) {
            stopAt.add(site.getJSONArray("STOP_AT").getString(j));
        }

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
            }
        }

        logger.info("Trawling has terminated for " + site.getString("BASE_URL") + ". Writing to file.");

        // Finish any final flushing
        flushToFile();

    }

    private void visitNext() throws TrawlException {

        // Wait a random time period
        try {
            Thread.sleep(random.nextInt(site.getInt("UPPER_WAIT_TIME") - site.getInt("LOWER_WAIT_TIME")) + site.getInt("LOWER_WAIT_TIME"));
        } catch (InterruptedException ex) {

        }

        Page page = null;
        try {
            // Do we need to log in?

            if (needToLogin) {
                needToLogin = (!login());
                if (needToLogin) {
                    // Wait ten seconds
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ex) {

                    }
                    throw new TrawlingInterrupt("Failed to log in, but this has been detected to be a recoverable error. I've waited a minute and now I'll try again.");
                }
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
            logger.warn("Trawling interrupt: " + e.getMessage());
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

            // Wait ten seconds
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {

            }
        } catch (RedirectionException e) {
            // Set this page's URL to the new URL and push it back onto the queue.
            if (page != null) {
                // Has the redirection already been followed?
                if (trawledPages.contains(page.getTagURL())) {
                    logger.info("Redirecting to page " + e.getMessage() + ". It's already been followed, so we're ignoring it.");
                } else {
                    logger.info("Redirecting to page " + e.getMessage() + ". I'm going to set this to this page's new URL, push it back onto the queue, and restart.");
                    page.setTagURL(e.getMessage().substring(baseURL.length()));
                    pageQueue.add(page);
                }
            }
        }
    }

    /**
     * Logs into the site.
     *
     * @return
     * @throws TrawlException
     */
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
            Elements logins = doc.getElementsByAttributeValue("action", site.getString("LOGIN_SUBMIT"));

            if (logins.isEmpty()) {
                logins = doc.getElementsByAttributeValue("action", site.getString("BASE_URL") + site.getString("LOGIN_SUBMIT"));
            }
            if (logins.isEmpty()) {
                logins = doc.getElementsByAttributeValue("method", "POST");
            }

            if (logins.isEmpty()) {
                throw new TrawlException("Failed to find login form!");
            }
            if (logins.size() > 1) {
                logger.warn("Found multiple login forms. Picking the first one...");
            }

            Element login = logins.get(0);

            // Extract the captcha image if appropriate
            String captchaResult = "";
            if (!site.getString("CAPTCHA").equals("")) {
                // Download the captcha image
                HttpMessage getCaptcha = new HttpMessage(HttpType.GET);
                getCaptcha.setImage(true);
                if (!site.isNull("CAPTCHA_IMAGE")) {
                    getCaptcha.setUrl(baseURL + site.getString("CAPTCHA_IMAGE"));
                } else {
                    // Just try to get the image
                    Elements captchas = login.getElementsByTag("img");

                    if (captchas.size() != 1) {
                        throw new TrawlException("Failed to find captcha, but the initialization file says there should be one.");
                    }

                    Element captchaImage = captchas.get(0);

                    // Does it contain base64?
                    if (captchaImage.attr("src").contains("base64")) {
                        String src = captchaImage.attr("src").split(",")[1];

                        byte image[] = Base64.decodeBase64(src);
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        os.write(image);

                        SocketClient client = new SocketClient("njanetos", "2point7182");

                        Captcha result = client.decode(os.toByteArray());
                        captchaResult = result.toString();

                    } else {
                        if (captchaImage.attr("src").contains(baseURL)) {
                            getCaptcha.setUrl(captchaImage.attr("src"));
                        } else {
                            getCaptcha.setUrl(baseURL + captchaImage.attr("src"));
                        }

                        getCaptcha.initializeDefaultImageHeaders(site);
                        getCaptcha.addHeader(new Header("Referrer", baseURL + site.getString("LOGIN_URL")));
                        getCaptcha.addCookieHeaders(cookies);

                        // Send it to deathbycaptcha
                        SocketClient client = new SocketClient("njanetos", "2point7182");
                        HttpRequest image = new HttpRequest(getCaptcha);
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(image.getImage(), "png", os);
                        Captcha result = client.decode(os.toByteArray());
                        captchaResult = result.toString();
                    }
                }

                logger.debug("Decoded captcha: " + captchaResult);
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
            if (!captchaResult.equals("")) {
                httpPost.appendContent(site.getString("CAPTCHA_FIELD"), captchaResult);
            }

            for (int i = 0; i < hidden.size(); ++i) {
                httpPost.appendContent(hidden.get(i).attr("name"), hidden.get(i).attr("value"));
            }

            // Add the submit info
            Element submit = login.getElementsByAttributeValue("type", "submit").get(0);
            httpPost.appendContent(submit.attr("name"), submit.attr("value"));

            // Add the referrer
            httpPost.addHeader(new Header("Referer", baseURL + site.getString("LOGIN_URL")));

            // Log in
            HttpRequest response = new HttpRequest(httpPost);
            headers = response.getHeaders();
            // Add any relevant cookies
            getTempCookies(headers);
            logger.info("Successfully logged in, response code: " + response.getStatusCode());

            // Were we redirected? If so, visit the redirection URL before continuing. 
            if (response.getStatusCode() == 302) {
                // Send a GET request to the redirection URL before continuing. 
                httpGet = new HttpMessage(HttpType.GET);
                httpGet.initializeDefaultHeaders(site);
                httpGet.addHeader(new Header("Referer", baseURL + site.getString("LOGIN_URL")));
                String redirectionURL = getRedirectionURL(headers);
                httpGet.setUrl(redirectionURL);
                httpGet.addCookieHeaders(cookies);

                httpRequest = new HttpRequest(httpGet);
                logger.debug("Visited redirected page. Status code " + httpRequest.getStatusCode());
            }

        } catch (ConnectionException | MalformedURLException | ProtocolException ex) {
            // Did not successfully log in
            logger.error(ex.getMessage());
            return false;
        } catch (IOException ex) {
            // Did not successfully log in
            logger.error(ex.getMessage());
            return false;
        } catch (Exception | InterruptedException ex) {
            // Did not successfully log in
            logger.error(ex.getMessage());
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
            logger.debug("Check " + writeQueue.get(i).getTagURL());
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

        logger.info(page.getDepth() + "|" + pageQueue.size() + "|" + writeQueue.size() + "|" + trawledPages.size() + ": " + page.getTagURL());

        // Write current info to file
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("info/" + site.getString("LOCATION") + outputLocation + ".info", false), "utf-8"))) {
            writer.write(pageQueue.size() + "|" + trawledPages.size());
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }

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

            // Test for whether we're at the login page
            if (!site.isNull("LOGIN_TEST")) {
                Matcher loginMatch = Pattern.compile(site.getString("LOGIN_TEST")).matcher(page.getHtml());
                if (loginMatch.find()) {
                    logger.info("According to LOGIN_TEST, we're at the login page. Attempting to log in...");
                    needToLogin = true;
                    throw new TrawlingInterrupt("Log in.");
                }
            }

            page.setDate(System.currentTimeMillis() / 1000);

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
            // TODO Wait a few seconds

            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {

            }
            throw new TrawlingInterrupt("Error code: " + e);

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
                return pages;
            }
        }

        // Parse the html
        Document doc = Jsoup.parse(html);
        Elements links = doc.getElementsByTag("a");

        for (Element link : links) {

            String tagURL = "";
            String linkText = "";
            boolean alreadyFollowed;
            boolean validURL = false;

            // First format the link
            if (link.attr("href").startsWith(baseURL)) {
                tagURL = link.attr("href").replace(baseURL, "");
                linkText = link.html();
                validURL = true;
            } else if (link.attr("href").startsWith("/")) {
                tagURL = link.attr("href");
                linkText = link.html();
                validURL = true;
            } //else if (!link.attr("href").startsWith("/") && !link.attr("href").startsWith("http")) {
            //    tagURL = "/" + link.attr("href");
            //    linkText = link.html();
            //    validURL = true;
            //}

            // Has it already been followed?
            alreadyFollowed = trawledPages.contains(tagURL);

            // Does it violate the exclusion rules?
            boolean excluded = false;
            for (String e : exclude) {
                if (tagURL.contains(e)) {
                    excluded = true;
                }
            }

            // Does it violate the exclusion equal rule?
            for (String e : excludeIfEqual) {
                if (tagURL.equals(e)) {
                    excluded = true;
                }
            }

            if (!alreadyFollowed && validURL && !excluded) {
                logger.debug("Creating new page at URL " + tagURL);
                Page page = new Page(tagURL, extractPage, linkText);
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

    /**
     * Tests whether the CAPTCHA text appears in the page.
     *
     * @param string
     * @return
     */
    private boolean testForCaptcha(String string) {
        Matcher matcherFindNumber = Pattern.compile(site.getString("CAPTCHA")).matcher(string);

        return matcherFindNumber.find();
    }

    /**
     * Returns a list of cookies from the header
     *
     * @param headers
     */
    private void getTempCookies(ArrayList<Header> headers) {
        for (Header h : headers) {
            if (h.getName().toLowerCase().equals("set-cookie")) {

                // Parse out values
                String[] values = h.getValue().split(";");
                for (String s : values) {
                    if (s.contains("=")) {
                        Cookie cookie = new Cookie(s);

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
                    logger.info("We've been redirected somewhere within the site.");
                    throw new RedirectionException(h.getValue());
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

    /**
     * Returns the redirection URL to follow from the headers.
     *
     * @param headers
     * @return
     */
    private String getRedirectionURL(ArrayList<Header> headers) {
        for (Header h : headers) {
            if (h.getName().equals("Location")) {
                return h.getValue();
            }
        }

        return "";
    }

}
