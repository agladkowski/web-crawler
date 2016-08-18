package com.demo.webcrawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by andrzej on 2016-08-18.
 */
public class Main {
    public static void main(String args[]) throws IOException {
        if (args.length == 0 || args.length > 3) {
            System.out.println("Usage: java -jar crawler.jar <baseUrl> <maxSearchDepth, default=1> <outputFile, default=siteMap.txt>");
            return;
        }

        String baseUrl = args[0];
        int pageTimeoutInMillis = 1000;

        int maxSearchDepth = 1;
        if (args.length > 1) {
            maxSearchDepth = Integer.parseInt(args[1]);
        }

        Path outputFilePath = Paths.get("siteMap.txt");
        if (args.length > 2) {
            outputFilePath = Paths.get(args[2]);
        }

        System.out.println("Configuration");
        System.out.println(" baseUrl: " + baseUrl);
        System.out.println(" maxSearchDepth: " + maxSearchDepth);
        System.out.println(" outputFile: " + outputFilePath.toAbsolutePath());
        System.out.println("================================");

        Crawler crawler = new WebCrawler(pageTimeoutInMillis, maxSearchDepth);
        String siteMap = crawler.createSiteMap(baseUrl);
        Files.write(outputFilePath, siteMap.getBytes());

        System.out.println("================================");
        System.out.println("SiteMap saved to: " + outputFilePath.toAbsolutePath());
    }

}
