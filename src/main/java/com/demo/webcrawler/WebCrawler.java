package com.demo.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 2016-08-18.
 */
public class WebCrawler implements Crawler {
    private static final int DEFAULT_MAX_PAGE_TREE_DEPTH = 1;
    private static final String USER_AGENT = "web-crawler_1.0";
    private static final String NEW_LINE = "\n";

    private final Logger logger = LoggerFactory.getLogger(WebCrawler.class);
    private final int pageTimeoutInMillis;
    private final int maxSearchDepth;

    public WebCrawler(int pageTimeoutInMillis) {
        this.pageTimeoutInMillis = pageTimeoutInMillis;
        this.maxSearchDepth = DEFAULT_MAX_PAGE_TREE_DEPTH;
    }

    public WebCrawler(int pageTimeoutInMillis, int maxSearchDepth) {
        this.pageTimeoutInMillis = pageTimeoutInMillis;
        this.maxSearchDepth = maxSearchDepth;
    }

    public static void assertNotNull(Object o, String message) {
        if (o == null) throw new IllegalArgumentException(message);
    }

    private static String addProtocolToUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return "http://" + url;
    }

    /**
     * Tradeofs:
     * 1. This program is single threaded so this limits the speed of crawling, particularly for slow websites.
     *  - This is not necessary a bad thing as websites may treat crawling attempts as DoS attacks and block crawlers IP.
     *  - Ideal solution (achieve best speed) would be to combine multithreaded version with configurable throttling to prevent black-listing.
     *
     * 2. API returns site-map String, for large websites this cause OOM errors.
     * - Solution to this would be to pass in (as an argument) implementation of 'SiteMapOutputStream' that writes to output file or prints to console.
     *
     * 3. I could manually parse the page html but I chose to use https://jsoup.org/ library to help me with that (it add extra memory overhead for page model)
     *
     * Possible improvements:
     * - De-duplication of static resources (most of the pages re-use css, js, images) and external links
     *
     * Notes:
     * - Web crawling is full of edge-cases. I think I implemented most of the major ones but to be absolutely sure this would have to be tested on more websites than I have done it.
     */

    @Override
    public String createSiteMap(String baseUrl) {
        assertNotNull(baseUrl, "Base URL should not be null.");

        WebUrl webUrl = WebUrl.crawlable(addProtocolToUrl(baseUrl));
        Set<String> visitedUrls = new HashSet<>();
        StringBuilder siteMapBuffer = new StringBuilder();
        int startingSearchDepth = 0;

        createSiteMapRecursive(webUrl, siteMapBuffer, visitedUrls, startingSearchDepth);

        return siteMapBuffer.toString();
    }

    private void createSiteMapRecursive(WebUrl parent, final StringBuilder outputSiteMap, Set<String> alreadyVisitedUrls, int currentSearchDepth) {
        String pageUrl = parent.getUrl();

        try {
            if (alreadyVisitedUrls.contains(pageUrl)) {
                return;// stopping infinite loop
            }

            if (currentSearchDepth > maxSearchDepth) {
                return;// stopping, reached max search depth
            }

            logger.info("[" + currentSearchDepth + "] " + pageUrl);

            alreadyVisitedUrls.add(pageUrl);

            // load the pageContent
            Document pageContent = Jsoup.connect(pageUrl).timeout(pageTimeoutInMillis).userAgent(USER_AGENT).get();

            // extracting all the possible links page content
            List<WebUrl> childPages = extractLinks(pageUrl, pageContent, "a", "href");
            List<WebUrl> cssFiles = extractLinks(pageUrl, pageContent, "link[rel=\"stylesheet\"]", "href");
            List<WebUrl> jsFiles = extractLinks(pageUrl, pageContent, "script[type=\"text/javascript\"]", "src");
            List<WebUrl> imageUrls = extractLinks(pageUrl, pageContent, "img", "src");

            // adding urls to outputSiteMap
            outputSiteMap.append(pageUrl + NEW_LINE);
            addToSiteMap(outputSiteMap, cssFiles);
            addToSiteMap(outputSiteMap, jsFiles);
            addToSiteMap(outputSiteMap, imageUrls);

            // crawling recursively children
            childPages.stream() // potential place for parallelization (stream.parallel), but then the writes to outputSiteMap would have to be synchronized
                    .filter(child -> child.isCrawlable())
                    .forEach(childPage -> createSiteMapRecursive(childPage, outputSiteMap, alreadyVisitedUrls, currentSearchDepth + 1));

            // adding external links
            List<WebUrl> externalUrls = childPages.stream()
                    .filter(child -> !child.isCrawlable())
                    .collect(Collectors.toList());
            addToSiteMap(outputSiteMap, externalUrls);

        } catch (UnknownHostException e) {
            outputSiteMap.append(pageUrl + " - unknown host." + NEW_LINE);
        } catch (SocketTimeoutException e) {
            outputSiteMap.append(pageUrl + " - read timeout." + NEW_LINE);
        } catch (Exception e) {
            logger.error(pageUrl + " - " +  e.getLocalizedMessage());
            if (e.getCause() instanceof MalformedURLException) {
                outputSiteMap.append(pageUrl + " - not a valid url." + NEW_LINE);
            } else {
                outputSiteMap.append(pageUrl + " - " + e.getMessage() + NEW_LINE);
            }
        }
    }

    private void addToSiteMap(StringBuilder siteMap, List<WebUrl> urls) {
        if (!urls.isEmpty()) {
            String partialSiteMap = urls.stream()
                    .filter(webUrl -> webUrl != null && webUrl.getUrl() != null)
                    .map(webUrl -> webUrl.getUrl())
                    .collect(Collectors.joining(NEW_LINE));

            siteMap.append(partialSiteMap);
            siteMap.append(NEW_LINE);
        }
    }

    private List<WebUrl> extractLinks(String pageUrl, Document pageContent, String elementSelector, String attributeSelector) {
        return pageContent.select(elementSelector).stream()
            .map(linkElement -> linkElement.attr(attributeSelector))
            .filter(childUrl -> childUrl != null && !childUrl.isEmpty())// not null or empty
            .filter(childUrl -> !childUrl.startsWith("#") && !childUrl.startsWith("/#"))// starts with #
            .map(childUrl -> createChildPageUrl(pageUrl, childUrl))
            .sorted((url1, url2) -> url1.getUrl().compareTo(url2.getUrl()))
            .collect(Collectors.toList());
    }

    private WebUrl createChildPageUrl(String pageUrl, String childUrl) {
        try {
            String baseUrl = getBaseUrl(pageUrl);

            if (childUrl.startsWith(baseUrl)) {
                return WebUrl.crawlable(childUrl);

            } else if (childUrl.matches("http[s]?.*|www\\..*")) {
                // external link like google/facebook
                return WebUrl.notCrawlable(childUrl);

            } else if (childUrl.startsWith("//")) {
                // link without protocol e.g. //page.com/some/url
                return WebUrl.crawlable(getProtocol(pageUrl) + childUrl);

            } else if (childUrl.startsWith("/")) {
                // child page relative to the root of the domain, e.g. /parent/child -> http://some.domain/parent/child
                return WebUrl.crawlable(baseUrl + childUrl);
            }
        } catch (MalformedURLException e) {
            return WebUrl.notCrawlable(pageUrl + " - malformed url");
        }

        // child page relative to parent page, e.g. child2 -> http://some.domain/child1/child2
        return WebUrl.crawlable(pageUrl + childUrl);
    }

    private String getProtocol(String parentUrl) throws MalformedURLException {
        return new URL(parentUrl).getProtocol() + ":";
    }

    private String getBaseUrl(String parentUrl) throws MalformedURLException {
        URL url = new URL(parentUrl);
        String urlToReturn = url.getProtocol() + "://" + url.getHost();
        if (url.getPort() != 80 && url.getPort()!= 443 && url.getPort() != -1) {
            urlToReturn = urlToReturn + ":"+url.getPort();
        }
        return urlToReturn;
    }

}
