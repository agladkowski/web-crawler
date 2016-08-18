package com.demo.webcrawler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static net.jadler.Jadler.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by andrzej on 2016-08-18.
 */
public class WebCrawlerTest {
    private Crawler crawler;

    @Before
    public void setUp() {
        initJadler();
        crawler = new WebCrawler(1000, 5);
    }

    @After
    public void tearDown() {
        closeJadler();
    }

    private String mockUrl(String path) {
        return "http://localhost:" + port() + path;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullBaseUrl() {
        // prepare
        String baseUrl = null;

        // act
        crawler.createSiteMap(baseUrl);
    }

    @Test
    public void testMalformedUrl() {
        //prepare
        String baseUrl = "http://google:com";

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals("http://google:com - not a valid url.\n", siteMap);
    }

    @Test
    public void testUnknownHost() {
        //prepare
        String baseUrl = "http://xhhhghghghgh.com";

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals("http://xhhhghghghgh.com - unknown host.\n", siteMap);
    }

    @Test
    public void testErrorResponseCodes() {
        testErrorResponseCodes(400);
        testErrorResponseCodes(401);
        testErrorResponseCodes(402);
        testErrorResponseCodes(403);
        testErrorResponseCodes(404);
        testErrorResponseCodes(500);
        testErrorResponseCodes(503);
    }

    private void testErrorResponseCodes(int statusCode) {
        //prepare
        resetJadler();
        String baseUrl = mockUrl("/parent/");
        onRequest().havingPathEqualTo("/parent/").respond().withBody(
                "<html><a href=\"/child1\">Child 1 relative to root domain</a></html>");

        onRequest().havingPathEqualTo("/child1").respond().withStatus(statusCode);


        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals(
                baseUrl + "\n" +
                mockUrl("/child1 - HTTP error fetching URL") + "\n"
                , siteMap);
    }

    @Test
    public void testLinkWithoutHrefAttribute() {
        //prepare
        String baseUrl = mockUrl("/parent/");
        onRequest().havingPathEqualTo("/parent/").respond().withBody(
                "<html><a >Child 1 relative to root domain</a></html>");

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals(baseUrl + "\n", siteMap);
    }

    @Test
    public void testLinkStartingWithTwoSlashes() {
        //prepare
        String baseUrl = mockUrl("/");
        onRequest().havingPathEqualTo("/").respond().withBody(
                "<html>" +
                    "<a href=\"//localhost:" + port() + "/child1\">Child 1</a>" +
                "</html>");

        onRequest().havingPathEqualTo("/child1").respond().withBody("");

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals(
                baseUrl + "\n" +
                mockUrl("/child1") + "\n"
                , siteMap);
    }

    @Test
    public void testBaseUrlWithoutProtocol() {
        //prepare
        String baseUrl = "localhost:" + port() + "/";
        onRequest().havingPathEqualTo("/").respond().withBody("");

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals("http://" + baseUrl + "\n", siteMap);
    }

    @Test
    public void testPageReadTimeout() {
        //prepare
        crawler = new WebCrawler(100);// TIMEOUT=100ms
        String baseUrl = mockUrl("/parent/");
        onRequest().havingPathEqualTo("/parent/").respond().withBody(
                "<html><a href=\"/child1\">Child 1</a></html>");

        onRequest().havingPathEqualTo("/child1").respond().withDelay(200, TimeUnit.MILLISECONDS).withBody("");

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals(
                baseUrl + "\n" +
                mockUrl("/child1") + " - read timeout.\n"
                , siteMap);
    }


    @Test
    public void testSimpleSiteMap() {
        //prepare
        String baseUrl = mockUrl("/parent/");
        onRequest().havingPathEqualTo("/parent/").respond().withBody(
                        "<html>" +
                            "<a href=\"/child1\">Child 1 relative to root domain</a>" +
                            "<a href=\"child2\">Child 2 relative to current page url</a>" +
                            "<a href=\"" + mockUrl("/parent/child3") + "\">Child 3 fully formed url</a>" +
                        "</html>");

        onRequest().havingPathEqualTo("/child1").respond().withBody("");
        onRequest().havingPathEqualTo("/parent/child2").respond().withBody("");
        onRequest().havingPathEqualTo("/parent/child3").respond().withBody("");

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals(
                baseUrl + "\n" +
                mockUrl("/child1") + "\n" +
                mockUrl("/parent/child2") + "\n" +
                mockUrl("/parent/child3") + "\n"
                , siteMap);
    }

    @Test
    public void testSimpleSiteMapWithExternalLinks() {
        //prepare
        String baseUrl = mockUrl("/parent/");
        onRequest().havingPathEqualTo("/parent/").respond().withBody(
                "<html>" +
                    "<a href=\"/child1\">Child 1 relative to root domain</a>" +
                    "<a href=\"www.twitter.com\">Twitter</a>" +
                    "<a href=\"http://google.com\">Google</a>" +
                "</html>");
        onRequest().havingPathEqualTo("/child1").respond().withBody("");

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals(
                baseUrl + "\n" +
                mockUrl("/child1") + "\n" +
                "www.twitter.com" + "\n" +
                "http://google.com\n"
                , siteMap);
    }

    @Test
    public void testSimpleSiteMapWithStaticContent() {
        //prepare
        String baseUrl = mockUrl("/parent/");
        onRequest().havingPathEqualTo("/parent/").respond().withBody(
                    "<html>" +
                        "<head>" +
                            "<link rel=\"stylesheet\" type=\"text/css\" href=\"/static/main.css\"  />" +
                            "<script type=\"text/javascript\" src=\"/static/main.js\"></script>" +
                        "</head>" +
                        "<a href=\"/child1\"><img src=\"/static/logo.gif\" ></a>" +
                    "</html>");

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals(
                baseUrl + "\n" +
                mockUrl("/static/main.css") + "\n" +
                mockUrl("/static/main.js") + "\n" +
                mockUrl("/static/logo.gif") + "\n" +
                mockUrl("/child1") + " - HTTP error fetching URL\n"
                , siteMap);
    }


    @Test
    public void testSiteMapThreeLevelsDeep() {
        //prepare
        String baseUrl = mockUrl("/");
        onRequest().havingPathEqualTo("/").respond().withBody(
                "<html><a href=\"/child1\">Child 1</a></html>");

        onRequest().havingPathEqualTo("/child1").respond().withBody(
                "<html><a href=\"/child1/child2\">Child 2 (2 levels deep)</a></html>");

        onRequest().havingPathEqualTo("/child1/child2").respond().withBody(
                "<html>" +
                    "<head>" +
                        "<link rel=\"stylesheet\" type=\"text/css\" href=\"/static/main.css\"  />" +
                        "<script type=\"text/javascript\" src=\"/static/main.js\"></script>" +
                    "</head>" +
                    "<a href=\"/child1/child2/child3\">Child 3 (2 levels deep)</a>" +
                "</html>");

        onRequest().havingPathEqualTo("/child1/child2/child3").respond().withBody("");

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals(
                baseUrl + "\n" +
                mockUrl("/child1") + "\n" +
                mockUrl("/child1/child2") + "\n" +
                mockUrl("/static/main.css") + "\n" +
                mockUrl("/static/main.js") + "\n" +
                mockUrl("/child1/child2/child3") + "\n"
                , siteMap);
    }

    @Test
    public void testSiteMapWithMaxSearchDepth() {
        //prepare
        int maxSearchDepth = 2;
        int pageTimeoutInMillis = 2000;
        crawler = new WebCrawler(pageTimeoutInMillis, maxSearchDepth);
        String baseUrl = mockUrl("/");
        onRequest().havingPathEqualTo("/").respond().withBody(
                "<html><a href=\"/child1\">Child 1</a></html>");

        onRequest().havingPathEqualTo("/child1").respond().withBody(
                "<html><a href=\"/child1/child2\">Child 2 (2 levels deep)</a></html>");

        onRequest().havingPathEqualTo("/child1/child2").respond().withBody(
                "<html><a href=\"/child1/child2/child3\">Child 3 (3 levels deep)</a></html>");

        onRequest().havingPathEqualTo("/child1/child2/child3").respond().withBody("");

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals(
                baseUrl + "\n" +
                mockUrl("/child1") + "\n" +
                mockUrl("/child1/child2") + "\n"
                , siteMap);
    }

    @Test
    public void testInfiniteLoop() {
        //prepare
        String baseUrl = mockUrl("/");
        onRequest().havingPathEqualTo("/").respond().withBody(
                "<html><a href=\"/child1\">Child 1</a></html>");

        onRequest().havingPathEqualTo("/child1").respond().withBody(
                "<html><a href=\"/child1/child2\">Child 2</a></html>");

        onRequest().havingPathEqualTo("/child1/child2").respond().withBody(
                "<html><a href=\"/child1\">Infinite loop (child refers to parent 2 levels up)</a></html>");

        // act
        String siteMap = crawler.createSiteMap(baseUrl);

        // assert
        assertEquals(
                baseUrl + "\n" +
                mockUrl("/child1") + "\n" +
                mockUrl("/child1/child2") + "\n"
                , siteMap);
    }

}
