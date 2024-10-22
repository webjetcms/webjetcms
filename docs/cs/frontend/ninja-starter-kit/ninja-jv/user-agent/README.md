# Informace o zařízení

| Metoda | Typ | Popis
| ---------------------------------- | --------- | ------------------------ |
| ${ninja.userAgent.browserName}     | *Řetězec*  | Název prohlížeče |
| ${ninja.userAgent.browserVersion}  | *Řetězec*  | Verze prohlížeče |
| ${ninja.userAgent.browserOutdated} | *Boolean* | Zastaralý prohlížeč |
| ${ninja.userAgent.blind}           | *Boolean* | Textová verze stránky |
| ${ninja.userAgent.deviceType}      | *Řetězec*  | Typ zařízení |
| ${ninja.userAgent.deviceOS}        | *Řetězec*  | Název operačního systému |

## Název prohlížeče *Řetězec*

Název prohlížeče (vždy malými písmeny) zjištěný z userAgent: `msie / chrome / safari / firefox`.

```java
${ninja.userAgent.browserName}
```

Používá se v :ghost:<code>html-attributes.jsp</code>

```html
<html data-browser-name="${ninja.userAgent.browserName}" >
```

## Verze prohlížeče *Řetězec*

Verze prohlížeče zjištěná z userAgent.

```java
${ninja.userAgent.browserVersion}
```

Používá se v :ghost:<code>html-attributes.jsp</code>

```html
<html data-browser-version="${ninja.userAgent.browserVersion}" >
```

## Zastaralý prohlížeč *Boolean*

Vrací se `true` pokud prohlížeč nesplňuje minimální požadavky na verzi prohlížeče.

```java
${ninja.userAgent.browserOutdated}
```

Výchozí nastavené hodnoty v `config.properties`

```properties
minBrowserVersion.MSIE=9
minBrowserVersion.CHROME=38
minBrowserVersion.SAFARI=6
minBrowserVersion.FIREFOX=45
minBrowserVersion.EDGE=12
minBrowserVersion.ANDROID_BROWSER=4
```

Používá se v :ghost:<code>browser-support.jsp</code>

```java
<c:if test="${ninja.userAgent.browserOutdated}">
    //zobrazí sa hláška o používaní zastaralého prehliadača
</c:if>
```

Hodnoty v `minBrowserVersion` nové prohlížeče lze přidat zadáním hodnoty zjištěného prohlížeče z hodnoty `${ninja.userAgent.browserName}`.

Například pro `User-Agent` Hodnota:

```
Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; GTB6.6; InfoPath.1; Crazy Browser 3.0.3)
```

můžete udělat `config.properties` zadat kontrolu minimální verze 10:

```properties
minBrowserVersion.crazy\u0020browser=10
```

Všimněte si speciálního znaku `\u0020` což představuje mezeru. Hodnota se zadává malými písmeny.

## Textová verze stránky *Boolean*

Pokud je parametr `forceBrowserDetector` nastavit na `blind` Vrací se `true`.

```java
${ninja.userAgent.blind}
```

Příklad zapnutí v adrese URL:

```url
www.nazovdomeny.sk?forceBrowserDetector=blind
```

Příklad zakázání v adrese URL:

```url
www.nazovdomeny.sk?forceBrowserDetector=pc
```

Používá se v :ghost:<code>head.jsp</code>

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

## Typ zařízení *Řetězec*

Typ zařízení (vždy malá písmena) zjištěný z userAgent: `desktop / phone / tablet`.

```java
${ninja.userAgent.deviceType}
```

Používá se v :ghost:<code>html-attributes.jsp</code>

```html
<html data-device-type="${ninja.userAgent.deviceType}" >
```

## Název operačního systému *Řetězec*

Název operačního systému (vždy malými písmeny) v zařízení zjištěný z UserAgent, např. `windows 7, macos high sierra`.

```java
${ninja.userAgent.deviceOS}
```

Používá se v :ghost:<code>html-attributes.jsp</code>

```html
<html data-device-os="${ninja.userAgent.deviceOS}" >
```

## Příklad detekce prohlížeče na backendu (v souboru JSP)

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
