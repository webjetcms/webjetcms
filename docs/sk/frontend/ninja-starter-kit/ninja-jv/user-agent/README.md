# Informácie o Zariadení
| Metóda | Typ | Popis |
| --- | --- | --- |
| ${ninja.userAgent.browserName} | *String* | Názov prehliadača |
| ${ninja.userAgent.browserVersion} | *String* | Verzia prehliadača |
| ${ninja.userAgent.browserOutdated} | *Boolean* | Zastaralí prehliadač |
| ${ninja.userAgent.blind} | *Boolean* | Textová verzia stránky |
| ${ninja.userAgent.deviceType} | *String* | Typ zariadenia |
| ${ninja.userAgent.deviceOS} | *String* | Názov operačného systému |

## Názov prehliadača *String*
Názov prehliadača (vždy malými písmenami) zistený z userAgenta: `msie / chrome / safari / firefox`.

```java
${ninja.userAgent.browserName}
```

Použité v :ghost:<code>html-attributes.jsp</code>
```html
<html data-browser-name="${ninja.userAgent.browserName}" >
```

## Verzia prehliadača *String*
Verzia prehliadača zistená z userAgenta.

```java
${ninja.userAgent.browserVersion}
```

Použité v :ghost:<code>html-attributes.jsp</code>
```html
<html data-browser-version="${ninja.userAgent.browserVersion}" >
```

## Zastaralí prehliadač *Boolean*
Vráti `true` ak prehliadač nespĺňa minimálne požiadavky na verziu prehliadača.

```java
${ninja.userAgent.browserOutdated}
```

Defaultne nastavené hodnoty v `config.properties`
```properties
minBrowserVersion.MSIE=9
minBrowserVersion.CHROME=38
minBrowserVersion.SAFARI=6
minBrowserVersion.FIREFOX=45
minBrowserVersion.EDGE=12
minBrowserVersion.ANDROID_BROWSER=4
```

Použité v :ghost:<code>browser-support.jsp</code>
```java
<c:if test="${ninja.userAgent.browserOutdated}">
    //zobrazí sa hláška o používaní zastaralého prehliadača
</c:if>
```

Hodnoty v `minBrowserVersion` je možné doplniť o nové prehliadače zadaním hodnoty detegovaného prehliadača z hodnoty `${ninja.userAgent.browserName}`.

Napríklad pre `User-Agent` hodnotu:

```
Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; GTB6.6; InfoPath.1; Crazy Browser 3.0.3)
```

môžete do `config.properties` zadať kontrolu na minimálnu verziu 10:

```properties
minBrowserVersion.crazy\u0020browser=10
```

Všimnite si špeciálny znak `\u0020` ktorý reprezentuje medzeru. Hodnota sa zadáva malými písmenami.

## Textová verzia stránky *Boolean*
Ak je parameter `forceBrowserDetector` nastavený na `blind` vráti `true`.

```java
${ninja.userAgent.blind}
```

Príklad zapnutia v URL adrese:
```url
www.nazovdomeny.sk?forceBrowserDetector=blind
```

Príklad vypnutia v URL adrese:
```url
www.nazovdomeny.sk?forceBrowserDetector=pc
```

Použité v :ghost:<code>head.jsp</code>
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

## Typ zariadenia *String*
Typ zariadenia (vždy malímy písmenamy) zistený z userAgenta:
`desktop / phone / tablet`.

```java
${ninja.userAgent.deviceType}
```

Použité v :ghost:<code>html-attributes.jsp</code>
```html
<html data-device-type="${ninja.userAgent.deviceType}" >
```

## Názov operačného systému *String*
Názov operačného systému (vždy malímy písmenamy) na zariadení zistený z UserAgenta napr. `windows 7, macos high sierra`.

```java
${ninja.userAgent.deviceOS}
```

Použité v :ghost:<code>html-attributes.jsp</code>
```html
<html data-device-os="${ninja.userAgent.deviceOS}" >
```

## Ukážka detekcie prehliadača na backende (v JSP súbore)
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