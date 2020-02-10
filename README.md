# logger-java-logback
 [ ![Download](https://api.bintray.com/packages/epam/reportportal/logger-java-logback/images/download.svg) ](https://bintray.com/epam/reportportal/logger-java-logback/_latestVersion)
 
[![Join Slack chat!](https://reportportal-slack-auto.herokuapp.com/badge.svg)](https://reportportal-slack-auto.herokuapp.com)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![UserVoice](https://img.shields.io/badge/uservoice-vote%20ideas-orange.svg?style=flat)](https://rpp.uservoice.com/forums/247117-report-portal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
#### Configuration

Add a report portal appender into `logback.xml` configuration file.

```XML
<appender name="ReportPortalAppender" class="com.epam.reportportal.logback.appender.ReportPortalAppender">
   <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%t] %-5level - %msg%n</pattern>
   </encoder>
</appender>
<root>
    <appender-ref ref="ReportPortalAppender" />
</root>
```

#### Attaching files 

For **logback** it is possible to attach binary data to the file only by adding to the log message additional text information.

In this case a log message should have next format:

  ```properties
  RP_MESSAGE#FILE#FILENAME#MESSAGE_TEST
  RP_MESSAGE#BASE64#BASE_64_REPRESENTATION#MESSAGE_TEST
  ```
  > RP_MESSAGE - message header
  > FILE, BASE64 - attaching data representation type
  > FILENAME, BASE_64_REPRESENTATION - path to sending file/ base64 representation of sending data
  > MESSAGE_TEST - string log message

  Example:
  ```java

    private static final String JSON_FILE_PATH = "files/file.json"

    @Test
    public void logJsonBase64() throws IOException {
        /* here we are logging some binary data as BASE64 string */
        LOGGER.info(
            "RP_MESSAGE#BASE64#{}#{}",
            BaseEncoding.base64().encode(Resources.asByteSource(Resources.getResource(JSON_FILE_PATH)).read()),
            "I'm logging content via BASE64"
        );
    }
        
    @Test
    public void logJsonFile() throws IOException, InterruptedException {
      /* here we are logging some binary data as file (useful for selenium) */
      File file = File.createTempFile("rp-test", ".json");
      Resources.asByteSource(Resources.getResource(JSON_FILE_PATH)).copyTo(Files.asByteSink(file));
      LOGGER.info("RP_MESSAGE#FILE#{}#{}", file.getAbsolutePath(), "I'm logging content via temp file");
    }
  ```

#### Grayscale images
There is a client parameter into `reportportal.properties` with `boolean` type value for screenshots sending in `grayscale` or `color` 
view. By default it is set as `true` and all pictures for Report Portal will be in `grayscale` format.

**reportportal.properties**
```properties
rp.convertimage=true
```

 Possible values:
 
`true` - all images will be converted into `grayscale`

`false` - all images will be as `color`
