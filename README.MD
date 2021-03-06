### Web-crawler

Tool for building site-maps.
Crawling is limited to a single domain. 


#### Requirements
Git, 
Java 8

#### How to get the code

```
git clone https://github.com/agladkowski/web-crawler.git web-crawler
```

#### How to build
Cd to the root of the project, and then execute this command

Unix
```
./gradlew clean test shadowJar
```

Windows
```
gradlew.bat clean test shadowJar
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

#### Credits
- Jsoup - Java HTML Parser (https://jsoup.org/)
- Jadler - Java HTTP mocking library (https://github.com/jadler-mocking/jadler/wiki)

#### License
This project is licensed under http://www.apache.org/licenses/LICENSE-2.0

Author bears no responsibility on the way this project is used.


