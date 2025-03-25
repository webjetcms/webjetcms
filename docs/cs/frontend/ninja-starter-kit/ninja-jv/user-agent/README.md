# Informace o Zařízení

| Metoda | Typ | Popis |
| ---------------------------------- | --------- | ------------------------ |
| ${ninja.userAgent.browserName}     | *String*  | Název prohlížeče |
| ${ninja.userAgent.browserVersion}  | *String*  | Verze prohlížeče |
| ${ninja.userAgent.browserOutdated} | *Boolean* | Zastaralí prohlížeč |
| ${ninja.userAgent.blind}           | *Boolean* | Textová verze stránky |
| ${ninja.userAgent.deviceType}      | *String*  | Typ zařízení |
| ${ninja.userAgent.deviceOS}        | *String*  | Název operačního systému |

## Název prohlížeče *String*

Název prohlížeče (vždy malými písmeny) zjištěn z userAgenta: `msie / chrome / safari / firefox`.

```java
${ninja.userAgent.browserName}
```

Použité v :ghost:<code>html-attributes.jsp</code>

```html
<html data-browser-name="${ninja.userAgent.browserName}" >
```

## Verze prohlížeče *String*

Verze prohlížeče zjištěna z userAgenta.

```java
${ninja.userAgent.browserVersion}
```

Použité v :ghost:<code>html-attributes.jsp</code>

```html
<html data-browser-version="${ninja.userAgent.browserVersion}" >
```

## Zastaralí prohlížeč *Boolean*

Vrátí `true` pokud prohlížeč nesplňuje minimální požadavky na verzi prohlížeče.

```java
${ninja.userAgent.browserOutdated}
```

Defaultně nastavené hodnoty v `config.properties`

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

Hodnoty v `minBrowserVersion` je možné doplnit o nové prohlížeče zadáním hodnoty detekovaného prohlížeče z hodnoty `${ninja.userAgent.browserName}`.

Například pro `User-Agent` hodnotu:

```
Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; GTB6.6; InfoPath.1; Crazy Browser 3.0.3)
```

můžete do `config.properties` zadat kontrolu na minimální verzi 10:

```properties
minBrowserVersion.crazy\u0020browser=10
```

Všimněte si speciální znak `\u0020` který reprezentuje mezeru. Hodnota se zadává malými písmeny.

## Textová verze stránky *Boolean*

Je-li parametr `forceBrowserDetector` nastaven na `blind` vrátí `true`.

```java
${ninja.userAgent.blind}
```

Příklad zapnutí v URL adrese:

```url
www.nazovdomeny.sk?forceBrowserDetector=blind
```

Příklad vypnutí v URL adrese:

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

## Typ zařízení *String*

Typ zařízení (vždy malímy písmeny) zjištěn z userAgenta: `desktop / phone / tablet`.

```java
${ninja.userAgent.deviceType}
```

Použité v :ghost:<code>html-attributes.jsp</code>

```html
<html data-device-type="${ninja.userAgent.deviceType}" >
```

## Název operačního systému *String*

Název operačního systému (vždy malímy písmeny) na zařízení zjištěný z UserAgenta Např. `windows 7, macos high sierra`.

```java
${ninja.userAgent.deviceOS}
```

Použité v :ghost:<code>html-attributes.jsp</code>

```html
<html data-device-os="${ninja.userAgent.deviceOS}" >
```

## Ukázka detekce prohlížeče na backendu (v JSP souboru)

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
