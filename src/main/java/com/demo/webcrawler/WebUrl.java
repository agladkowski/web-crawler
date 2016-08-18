package com.demo.webcrawler;

/**
 * Created by andrzej on 2016-08-18.
 */
public class WebUrl {
    private final String url;
    private final boolean crawlable;

    private WebUrl(String url, boolean crawlable) {
        this.url = url;
        this.crawlable = crawlable;
    }

    public String getUrl() {
        return url;
    }

    public boolean isCrawlable() {
        return crawlable;
    }

    // factory methods

    public static WebUrl notCrawlable(String url) {
        return new WebUrl(url, false);
    }

    public static WebUrl crawlable(String url) {
        return new WebUrl(url, true);
    }

}
