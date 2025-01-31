# Server performance

For optimal server performance, several requirements and settings must be met. Each application (e.g. photo gallery, poll, etc.) embedded in a web page causes a slowdown. Applications typically make additional database requests or need to read data from the file system.

Search engines that constantly crawl and index web pages on your server can also have a significant impact on performance. Their traffic may not be visible e.g. in Google Analytics, but it is visible in [Statistics](../../redactor/apps/stat/README.md) provided by WebJET CMS.

## Identification of problems

The first thing to do is to identify where the slowdown is occurring. If you can identify at a glance a web page that seems slow to you, you can use the URL parameter `?_writePerfStat=true`. Otherwise, turn on server monitoring in which you can identify the web pages that are taking the longest to execute.

### URL parameter

Using the URL parameter `?_writePerfStat=true` it is possible to get a list of applications embedded in the web page with the time of their execution. For example, a page `/sk/` view as `/sk/?_writePerfStat=true`.

When displaying a web page in this way, an expression of the type `PerfStat: 3 ms (+3) !INCLUDE(...)`. It may not be easily searchable in a standard web page, so we recommend to view the source code of the page - in Chrome menu View-Developer-View Source Code. Then use the browser search term `PerfStat:`.

This expression is in the format `PerfStat: 3 ms (+3)` where the first number is the total execution time of one `iwcm:write` expression and the number in parentheses is the execution time of this application. This is followed by the path to the application and its parameters. So you are interested in the primary number in parentheses.

Using the URL parameter `_disableCache=true` you can turn off application caching.

### Server monitoring

For a comprehensive view, you can turn on the [server monitoring](../monitoring/README.md) by setting the following configuration variables:
- `serverMonitoringEnable` - enables the server monitoring and logging function
- `serverMonitoringEnablePerformance` - turns on application and website performance monitoring
- `serverMonitoringEnableJPA` - enables the SQL query monitoring function

!>**Warning:** application performance monitoring and SQL query monitoring puts a strain on the server, we do not recommend to have this feature permanently enabled.

After setting the configuration variables, you need to perform **restart the application server** to activate performance monitoring on initialization.

Then, in the Server Monitoring - Applications/WEB Pages/SQL Queries section, you can identify the parts that are taking a long time to execute. Focus on the most frequently executed applications/SQL queries and optimize them.

### Total web page generation time

There is an app `/components/_common/generation_time.jsp` which, if inserted into the footer of the web page template, will generate the total time of web page generation into the HTML code.

The following application parameters can be set:
- `hide` - default `true` - the generation time is displayed as a comment in the HTML code
- `onlyForAdmin` - default `false` - generation time is displayed only if an administrator is logged in

Insert the following code into the footer (or a suitable free field) of the web page template:

```html
!INCLUDE(/components/_common/generation_time.jsp, hide=true, onlyForAdmin=false)!
```

At the location of the embedded application, information about the execution time of the entire web page in ms is displayed:

```html
<!-- generation time: 4511 ms -->
```

## Measuring database server and file system performance

To compare the performance of environments - e.g. test VS production environments, the scripts below can be used. Running them requires the right to update WebJET. You can measure and compare environments without load, but also during operation or performance tests.
- `/admin/update/dbspeedtest.jsp` - measures the performance of reading data from the database server.

Good values are for example:

```html
Image read, count=445
...
Total time: 649 ms, per item: 1.4584269662921348 ms
Total bytes: 4.8050469E7, per second: 7.403770261941448E7 B/s

Random web page read, count=3716
...
Total time: 3608 ms, per item: 0.9709364908503767 ms
Total bytes: 1371566.0, per second: 380145.78713968955 B/s

Only documents.data web page read, count=3716
...
Total time: 2205 ms, per item: 0.5933799784714747 ms
Total bytes: 685783.0, per second: 311012.6984126984 B/s

Documents read using web page API, count=3716
...
Total time: 1869 ms, per item: 0.5029601722282023 ms
Total bytes: 685783.0, per second: 366925.09363295883 B/s
```

Due to the different number of records in the database, it is necessary to compare `per item` Values.

- `/admin/update/fsspeedtest.jsp` - checks the speed of reading a list of files from the file system, it should be checked especially if you are using a network file system.

Good values are for example:

```html
Testing mime speed, start=0 ms
has base file object, fullPath=/Users/jeeff/Documents.nosync/workspace-visualstudio/webjet/webjet8v9-hotfix/src/main/webapp/components/_common/mime diff=1 ms
listFiles, size=678, diff=284 ms
listing done, diff=16 ms


Testing modinfo speed, start=0 ms
modinfo list, size=102, diff=1 ms
modinfo listing done, diff=220 ms
Total time=522ms
```

## Database query optimization

To optimize the number of database requests, you can enable caching - `cache`.

### Web pages

Each web page has an option in the Basic tab **Enable page caching**. Turning this option on takes the web page content from the table `documents` is cached. When a web page is displayed, it will not be necessary to make a database call to retrieve the contents of the web page.

We recommend enabling this option on the most visited websites, which you can get a list of in the app [Statistics](../../redactor/apps/stat/README.md#top-pages).

![](../../redactor/webpages/editor/tab-basic.png)

### Applications

Similar to web pages, you can also enable caching for applications. Some applications have this option available directly in [application settings](../../custom-apps/appstore/README.md#tab-view) embedded in the web page in the View as field tab **Buffer time**.

![](../../custom-apps/appstore/common-settings-tab.png)

If the application does not have this setting available you can still set the parameter in the HTML code of the web page text by adding the parameter `, cacheMinutes=xxx` to the parameters of the embedded application, for example:

```html
!INCLUDE(sk.iway.iwcm.components.reservation.TimeBookApp, reservationObjectIds=2560+2561, device=, cacheMinutes=10)!
```

!>**Warning:** it is important to note that the cache is global for the entire application server. The application file path, the individual parameters specified in the HTML code of the web page, and the language of the currently displayed web page are used as the key. The URL parameters of the web page are not taken into account.

Thus, the cache cannot be used if, for example, a paging list is displayed where the page number is passed using a URL parameter. However, there is an exception for applications containing a list of news items in the filename `/news/news` the buffer is used only if no parameter is specified in the URL `page`, or the value of this parameter is different than `1`. In this way, the buffer is also used for the news list, but only the first page of results is saved. Other pages are not saved.

## File system optimization

Web pages typically contain many additional files - images, CSS stylesheets, JavaScript files, and so on - that need to be loaded along with the web page. Therefore, the display speed also depends on the number and size of these files.

### Setting the buffer

It is possible to set the browser to use a cache for web page files - this way the file will not be read repeatedly each time the web page is viewed, but if the browser already has it cached, it will be used. This will speed up the web page display and reduce the load on the server. An example is the logo image, which is typically on every page, but is highly unlikely to change - or changes on the order of once every few months.

It is possible to set the following configuration variables that affect the HTTP header `Cache-Control`:
- `cacheStaticContentSeconds` - set number of seconds, default `300`.
- `cacheStaticContentSuffixes` - a list of extensions for which the HTTP header `Cache-Control` generated, by default `.gif,.jpg,.png,.swf,.css,.js,.woff,.svg,.woff2`.

For a more precise setting, you can use the app [HTTP headers](../../admin/settings/response-header/README.md) where you can set different values for different URLs.

![](../../admin/settings/response-header/editor-wildcard.png)

## Behaviour for the administrator

If an administrator is logged in the application buffer is not used (it is assumed that the administrator always wants to see the current state).

This behaviour can be changed by setting the configuration variable `cacheStaticContentForAdmin` to the value of `true`. It is particularly appropriate to set this value for intranet installations where users authenticate against `SSO/ActiveDirectory` server and even when working in the intranet environment they have administrator rights.

## Search engines

Search engines and various other bots can put a significant load on the server. Especially with the advent of AI learning, there is significant internet crawling and database populating for AI learning. Bots often try different URL parameters to retrieve additional data.

### Setting robots.txt

The behaviour of the robots can be influenced by settings in the file `/robots.txt`. This if it does not exist is generated by default. Place your modified version in `/files/robots.txt`, from this location WebJET will display it when calling `/robots.txt`.

Using the file [robots.txt](https://en.wikipedia.org/wiki/Robots.txt) you can influence the behaviour of robots and search engines - limit the URLs they can use, set the spacing between requests, etc.

## Other settings

### Reverse DNS server

Statistics, auditing, and other applications can retrieve the reverse DNS record from the IP address. API calls are used `InetAddress.getByName(ip).getHostName()`. However, the DNS server may not be available on the servers/DMZ and this call may take a few seconds before an error occurs. Generally such a call slows down the execution of the HTTP request.

By setting the configuration variable `disableReverseDns` to the value of `true` it is possible to disable DNS name retrieval from the visitor's IP address and speed up the execution of queries. In the field for the value `hostname` the value of the IP address is then written.

### Turning off statistics

Writing statistics data is asynchronous, it is done in batches so that the web page display does not wait for the statistics data to be written to the database.

When traffic is high, or you are looking for performance issues, you can temporarily disable the logging of traffic statistics by setting the configuration variable `statMode` to the value of `none`. The standard value is `new`.
