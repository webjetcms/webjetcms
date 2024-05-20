# Information about the Facility

| Method | Type | Description | | | ---------------------------------- | --------- | ------------------------ | | ${ninja.userAgent.browserName}     | *String*  | Browser name | | ${ninja.userAgent.browserVersion}  | *String*  | Browser version | | ${ninja.userAgent.browserOutdated} | *Boolean* | Outdated browser | | ${ninja.userAgent.blind}           | *Boolean* | Text version of the page | | ${ninja.userAgent.deviceType}      | *String*  | Device type | | ${ninja.userAgent.deviceOS}        | *String*  | Operating system name |

## Browser name *String*

Browser name (always in lowercase) detected from userAgent: `msie / chrome / safari / firefox`.

```java
${ninja.userAgent.browserName}
```

Used in :ghost:<code>html-attributes.jsp</code>

```html
<html data-browser-name="${ninja.userAgent.browserName}"></html>
```

## Browser version *String*

Browser version detected from userAgent.

```java
${ninja.userAgent.browserVersion}
```

Used in :ghost:<code>html-attributes.jsp</code>

```html
<html data-browser-version="${ninja.userAgent.browserVersion}"></html>
```

## Outdated browser *Boolean*

Returns `true` if the browser does not meet the minimum browser version requirements.

```java
${ninja.userAgent.browserOutdated}
```

Default set values in `config.properties`

```properties
minBrowserVersion.MSIE=9
minBrowserVersion.CHROME=38
minBrowserVersion.SAFARI=6
minBrowserVersion.FIREFOX=45
minBrowserVersion.EDGE=12
minBrowserVersion.ANDROID_BROWSER=4
```

Used in :ghost:<code>browser-support.jsp</code>

```java
<c:if test="${ninja.userAgent.browserOutdated}">
    //zobrazí sa hláška o používaní zastaralého prehliadača
</c:if>
```

The values in `minBrowserVersion` new browsers can be added by entering the value of the detected browser from the value `${ninja.userAgent.browserName}`.

For example, for `User-Agent` Value:

```
Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; GTB6.6; InfoPath.1; Crazy Browser 3.0.3)
```

you can do `config.properties` specify a check for a minimum version of 10:

```properties
minBrowserVersion.crazy\u0020browser=10
```

Note the special character `\u0020` which represents a gap. The value is entered in lower case.

## Text version of the page *Boolean*

If the parameter `forceBrowserDetector` set to `blind` Returns `true`.

```java
${ninja.userAgent.blind}
```

Example of turning on in URL:

```url
www.nazovdomeny.sk?forceBrowserDetector=blind
```

Example of disabling in URL:

```url
www.nazovdomeny.sk?forceBrowserDetector=pc
```

Used in :ghost:<code>head.jsp</code>

```java
<c:choose>
    <c:when test="${ninja.userAgent.blind}">
        // CSS štýly pre blind friendly verziu
    </c:when>
    <c:otherwise>
        // Štandardné CSS štýly
    </c:otherwise>
</c:choose>
```

## Type of device *String*

Device type (always in lower case) detected from userAgent: `desktop / phone / tablet`.

```java
${ninja.userAgent.deviceType}
```

Used in :ghost:<code>html-attributes.jsp</code>

```html
<html data-device-type="${ninja.userAgent.deviceType}"></html>
```

## Name of the operating system *String*

The name of the operating system (always in lower case) on the device as detected from UserAgent, e.g. `windows 7, macos high sierra`.

```java
${ninja.userAgent.deviceOS}
```

Used in :ghost:<code>html-attributes.jsp</code>

```html
<html data-device-os="${ninja.userAgent.deviceOS}"></html>
```

## Example of browser detection on the backend (in JSP file)

```jsp
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:choose>
  <c:when test="${ninja.userAgent.browserName=='msie'}">
    Som MSIE
  </c:when>
  <c:otherwise>
    Niesom MSIE
  </c:otherwise>
</c:choose>
```
