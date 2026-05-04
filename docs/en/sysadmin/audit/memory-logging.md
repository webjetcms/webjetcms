# Latest logs

The application is designed to display the latest logs, in case you do not have access to the logs on the file system. It displays logs that pass through the log framework (i.e. use the ```Logger``` class), it does not display logs written directly via ```System.out``` or ```System.err```.

![](memory-logging.png)

It supports cluster, so it is possible to request the latest logs from another cluster node. The ```Stack Trace``` tab contains a stack dump (but the content is only displayed for error records, it is empty for standard log levels).

## Configuration options/settings:

- ```loggingInMemoryEnabled``` - ​​setting to ```true/false``` enables or disables saving logs to memory.
- ```loggingInMemoryQueueSize``` - ​​maximum number of logs written to memory (default 200). Please note that all data is loaded into the table at once and due to the transfer ```stack trace``` may be large. We do not recommend setting this variable to an extremely high value.

For proper functioning, ```logger``` must also be set in the ```logback.xml``` file. It is set this way by default, but if you have changed the file, you need to add ```IN_MEMORY appender``` and add its call for the ```root``` element.

```xml
    ...
    <appender name="IN_MEMORY" class="sk.iway.iwcm.system.logging.InMemoryLoggerAppender" />

    <root level="ERROR">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="IN_MEMORY" />
    </root>
    ...
```

## Implementation details

- ```sk.iway.iwcm.system.logging.InMemoryLoggerAppender``` - ​​```appender``` for ```logback```, which ensures that logs are sent to ```InMemoryLoggingDB```
- ```sk.iway.iwcm.system.logging.InMemoryLoggingDB``` - ​​class provides writing and retrieving logs to and from ```queue```, loading logs on the cluster
- ```sk.iway.iwcm.system.logging.InMemoryLoggingEvent``` - ​​model for log event
- ```sk.iway.iwcm.system.logging.InMemoryLoggerRestController``` - ​​controller for outputting logs to DataTable
