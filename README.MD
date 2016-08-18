### Web-crawler

Tool for building site-maps.
Crawling is limited to a single domain. 


#### Requirements
Java 8

#### How to build
Cd to the root of the project, and then execute this command

Unix
```
./gradlew clean test shadowJar
```

Windows
```
gradlew.bat shadowJar
```

#### How to run

```
java -jar build/libs/web-crawler-1.0-all.jar
Usage: java -jar crawler.jar <baseUrl> <maxSearchDepth, default=1> <outputFile, default=siteMap.txt>
```
##### Examples
```
java -jar build/libs/web-crawler-1.0-all.jar www.bbc.co.uk
java -jar build/libs/web-crawler-1.0-all.jar www.bbc.co.uk 1
java -jar build/libs/web-crawler-1.0-all.jar www.bbc.co.uk 1 bbc-sitemap.txt
```

