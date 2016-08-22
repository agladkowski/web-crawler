package com.demo.webcrawler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static net.jadler.Jadler.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by andrzej on 2016-08-21.
 */
public class MainTest {

    @Before
    public void setUp() {
        initJadler();
    }

    @After
    public void tearDown() {
        closeJadler();
    }

    private String mockUrl(String path) {
        return "http://localhost:" + port() + path;
    }

    @Test
    public void testMainWithoutAnyArguments() throws Exception {
        // prepare
        ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(consoleOutput);
        System.setOut(printStream);

        // act
        Main.main(new String[0]);
        String mainOutput = consoleOutput.toString("utf-8").trim();

        // assert
        assertEquals("Usage: java -jar crawler.jar <baseUrl> <maxSearchDepth, default=1> <outputFile, default=siteMap.txt>", mainOutput);
    }

    @Test
    public void testMainWithBaseUrlArgument() throws IOException {
        // prepare
        String baseUrl = mockUrl("/page1");
        onRequest().havingPathEqualTo("/page1").respond().withBody("");

        // act
        Main.main(new String[] {baseUrl});
        String outputSiteMap = new String(Files.readAllBytes(Paths.get("siteMap.txt")), "utf-8");


        // assert
        assertEquals(baseUrl + "\n", outputSiteMap);
    }

}