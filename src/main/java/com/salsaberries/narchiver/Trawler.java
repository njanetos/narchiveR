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

import com.salsaberries.narchiver.exceptions.AuthenticationException;
import com.salsaberries.narchiver.exceptions.ConnectionException;
import com.salsaberries.narchiver.exceptions.RedirectionException;
import com.salsaberries.narchiver.exceptions.TrawlingInterrupt;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trawler is given an initial set of pages, then recursively finds
 * pages within those pages and uses a queue to organize everything.
 * 
 * @author njanetos
 */
public class Trawler {

    private static final Logger logger = LoggerFactory.getLogger(Trawler.class);

    private final LinkedList<Page> pageQueue;
    private final ArrayList<Page> pagesExclude;
    private final ArrayList<Page> finalPages;
    private final int maxDepth;
    private final String baseURL;
    private final ArrayList<String> parentChildExclude;
    private final ArrayList<String> exclude;
    private final ArrayList<String> stopAt;
    private final ArrayList<String> mustInclude;
    private final ArrayList<String> passInclude;
    private final ArrayList<String> captchas;
    private final boolean cookiesEnabled;
    private final JSONObject site;
    private int loginAttempts;
    private ArrayList<Cookie> cookies;
    private Random random;
    
    /**
     * Trawler implements the recursive algorithm to search the web page.
     * 
     * @param site
     */
    public Trawler(JSONObject site) {

        // Initialize the random variable
        random = new Random();
        
        this.site = site;

        // Create the initial beginning pages
        ArrayList<Page> pages = new ArrayList<>();
        for (int j = 0; j < site.getJSONArray("BEGIN").length(); ++j) {
            pages.add(new Page(site.getJSONArray("BEGIN").getString(j), null));
        }

        // Create the pages to exclude
        pagesExclude = new ArrayList<>();
        for (int j = 0; j < site.getJSONArray("EXCLUDE").length(); ++j) {
            pagesExclude.add(new Page(site.getJSONArray("EXCLUDE").getString(j), null));
        }

        // Create the list of PARENT_CHILD exclusions
        parentChildExclude = new ArrayList<>();
        for (int j = 0; j < site.getJSONArray("PARENT_CHILD_EXCLUDE").length(); ++j) {
            parentChildExclude.add(site.getJSONArray("PARENT_CHILD_EXCLUDE").getString(j));
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

        // Create the list of PASS_INCLUDE
        passInclude = new ArrayList<>();
        for (int j = 0; j < site.getJSONArray("PASS_INCLUDE").length(); ++j) {
            passInclude.add(site.getJSONArray("PASS_INCLUDE").getString(j));
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
        finalPages = new ArrayList<>();
        this.maxDepth = site.getInt("DEPTH");
        this.baseURL = site.getString("BASE_URL");
        
        // Push the initial pages onto the queue
        for (int i = 0; i < pages.size(); ++i) {
            pageQueue.push(pages.get(i));
        }
        
        // Initialize cookies
        cookies = new ArrayList<>();

        // Start trawling
        visitNext(false);
        
        logger.info("Trawling has terminated for " + site.getString("BASE_URL") + ". Writing to file.");
        
        // Run the pass filters
        for (String e : passInclude) {
            for (int i = finalPages.size()-1; i >= 0; --i) {
                if (!finalPages.get(i).getTagURL().contains(e)) {
                    logger.info("Final pass: Removing page " + finalPages.get(i).getTagURL());
                    logger.info(finalPages.get(i).getHtml());
                    finalPages.remove(i);
                }
            }
        }
    }
    
    private boolean login() throws TrawlingInterrupt {
        --loginAttempts;

       if (loginAttempts < 0) {
           logger.error("Warning! Exceeded maximum number of login attempts! Program is now exiting.");
           Alerter alert = new Alerter();
           alert.alert();
           // TODO: Handle this better
           System.exit(1);
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
                 logger.error("Failed to find login form.");
                 System.exit(1);
                 // TODO: Fix this up
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
             //TODO FIX THIS!
             httpPost.appendContent("commit", "enter");

             // Add additional cookies
             httpPost.getHeaders().add(new Header("Content-Type", "application/x-www-form-urlencoded"));
             httpPost.getHeaders().add(new Header("Referer", baseURL + site.getString("LOGIN_URL")));
             httpPost.getHeaders().add(new Header("Content-Length", Integer.toString(httpPost.getContent().length())));

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

       }
       catch (ConnectionException e) {
           throw new TrawlingInterrupt(e.getMessage());
       }
       
       // Did we successfully log in? Then return true.
       return true;
       
    }
    
    private void visitNext(boolean needToLogin) {

        // Wait a random time period
        logger.info("Waiting some random time before visiting");
        try {
            Thread.sleep(random.nextInt(2500) + 5000);
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
            logger.error("This should not happen.");
            System.exit(1);
            // TODO: Make this better.
        }

        Page page = null;
        try {
            // Do we need to log in?
            if (needToLogin) {
                try {
                    needToLogin = (!login());
                }
                catch (TrawlingInterrupt e) {
                    needToLogin = true;
                }
            }
            else {
                page = pageQueue.pop();
                visit(page);
            }
        }
        catch (AuthenticationException e) {
            // Re-add the page if necessary
            if (page != null) { pageQueue.push(page); }
            needToLogin = true;
        }
        catch (TrawlingInterrupt e) {
            if (page != null) { pageQueue.push(page); }

            logger.info("Warning! Trawling was interrupted. Retrying in 1 minute.");
            
            // Wait one minute
            try {
                Thread.sleep(60000);
            }
            catch (InterruptedException ex) {
                e.printStackTrace();
                logger.error("This should not happen.");
                System.exit(1);
                // TODO: Make this better.
            }
        }
        catch (RedirectionException e) {
            // Set this page's URL to the new URL and push it back onto the queue.
            if (page != null) {
                logger.info("Redirecting to page " + e.getMessage() + ". I'm going to set this to this page's new URL, push it back onto the queue, and restart.");
                page.setTagURL(e.getMessage().substring(baseURL.length()));
                pageQueue.push(page);
            }
        }

        if (pageQueue.size() != 0) {
            visitNext(needToLogin);
        }
    }
    
    /**
     * Visit the first page in the queue. Download all the info. Extract
     * relevant URLs. Add them to the queue. Add this page to the list of
     * final pages.
     */
    private void visit(Page page) throws AuthenticationException, TrawlingInterrupt, RedirectionException {

        logger.info("Downloading and analyzing " + baseURL + page.getTagURL() + ", current depth: " + page.getDepth());

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

                // Add new pages to the queue
                for (int i = 0; i < newPages.size(); ++i) {
                    pageQueue.push(newPages.get(i));
                }
            }

            // If all went well, add this page to the final list
            finalPages.add(page);

        }
        catch (ConnectionException e) {
            switch (e.getMessage()) {
                // Server timed out. Re-add the page, wait, and restart.
                case "408":
                    throw new TrawlingInterrupt("408");
                case "500":
                    throw new TrawlingInterrupt("500");
            }
        }
    }
    
    /**
     * Extracts links from html.
     * 
     * @param html
     * @return 
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
            boolean parentChildExcluded = false;
            
            // First format the link
            if (link.attr("href").startsWith(baseURL)) {
                tagURL = link.attr("href").replace(baseURL, "");
                validURL = true;
            }
            else if (link.attr("href").startsWith("/")) {
                tagURL = link.attr("href");
                validURL = true;
            }
            
            // Has it already been followed?
            for (Page page : finalPages) {
                if (page.getTagURL().equals(tagURL)) {
                    alreadyFollowed = true;
                    break;
                }
            }
            for (Page page : pageQueue) {
                if (page.getTagURL().equals(tagURL)) {
                    alreadyFollowed = true;
                    break;
                }
            }
            for (Page page : pages) {
                if (page.getTagURL().equals(tagURL)) {
                    alreadyFollowed = true;
                    break;
                }
            }
            
            // Does it violate parent/child exclude?
            boolean parentMissing = true;
            boolean childMissing = true;
            if (parentChildExclude.size() > 0) {
                for (String e : parentChildExclude) {
                    if (extractPage.getTagURL().contains(e)) {
                        parentMissing = false;
                    }
                    if (tagURL.contains(e)) {
                        childMissing = false;
                    }
                }
                parentChildExcluded = parentMissing & childMissing;
            }
            else {
                parentChildExcluded = false;
            }
            
            // Does it violate the exclusion rules?
            boolean excluded = false;
            for (String e : exclude) {
                if (tagURL.contains(e)) {
                    excluded = true;
                }
            }
            
            // Does it violate the must include rules?
            boolean mustIncluded = false;

            if (!alreadyFollowed && validURL && !parentChildExcluded && !excluded && !mustIncluded) {
                logger.info("Creating new page at URL " + tagURL);
                Page page = new Page(tagURL, extractPage);           
                pages.add(page);
            }
            
            if (alreadyFollowed) {
                logger.info("Skipping duplicate at URL " + tagURL);
            }
            
            if (!validURL) {
                logger.info("Invalid URL at " + link.attr("href"));
            }
            
            if (parentChildExcluded) {
                logger.info("Parent child exclusion at " + link.attr("href"));
            }
            
            if (excluded) {
                logger.info("Exclusion at " + link.attr("href"));
            }

            if (mustIncluded) {
                logger.info(link.attr("href") + " did not include required inclusion.");
            }
        }
        return pages;
    }

    /**
     *
     * @return
     */
    public ArrayList<Page> getFinalPages() {
        return finalPages;
    }

    /**
     *
     * @return
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
                }
                else {
                    Cookie.replace(cookies, cookie);
                }
            }
        }
    }
    
    /**
     * Searches through the headers to see if any redirect to the login page.
     * @param headers
     * @return 
     */
    private String checkHeaders(HttpRequest response) throws AuthenticationException, RedirectionException {
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
                    }
                    else {
                        logger.info("We've been redirected somewhere within the site.");
                        throw new RedirectionException(h.getValue());
                    }
                }
            }
            
            // TODO: Fix this.
            return "";
        }
        
        // TODO: Fix this.
        return "";
    }
    
    public String getRedirectionURL(ArrayList<Header> headers) {
        for (Header h : headers) {
            if (h.getName().equals("Location")) {
                return h.getValue();
            }
        }
        
        return "";
    }

}
