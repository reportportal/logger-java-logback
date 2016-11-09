# logger-java-logback

#### Configuration

Add report portal appender into `logback.xml` configuration file.

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

#### Screenshots

For **logback** it is possible to attach binary data to file only by adding to log message additional text information.

In this case log message should have next format:

  ```properties
  RP_MESSAGE#FILE#FILENAME#MESSAGE_TEST
  RP_MESSAGE#BASE64#BASE_64_REPRESENTATION#MESSAGE_TEST
  RP_MESSAGE - message header
  FILE, BASE64 - attaching data representation type
  FILENAME, BASE_64_REPRESENTATION - path to sending file/ base64 representation of sending data
  MESSAGE_TEST - string log message
 ```

#### Grayscale images
There is client parameter into `reportportal.properties` with `boolean` type value for screenshots sending in `grayscale` or `color` view. By default it is set as `true` and all pictures for Report Portal will be in `grayscale` format.

**reportportal.properties**
```properties
rp.convertimage=true
```

 Possible values:
 
`true` - all images will be converted into `grayscale`

`false` - all images will be as `color`
