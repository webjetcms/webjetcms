# Debug mode

| Method | Type | Description | | | -------------- | --------- | ----------- | | ${ninja.debug} | *Boolean* | Debug mode |

## Debug mode *Boolean*

<!-- tabs:start -->

#### \*\* Description \*\*

Returns `true`if the parameter is in the url `ninjaDebug` set to `true`, or if it is in `config.properties` attribute set `ninjaDebug` at `true`.

```java
${ninja.debug}
```

Example setting in URL:

```url
www.nazovdomeny.sk?ninjaDebug=true
```

Example of setting in `config.properties`

```properties
ninjaDebug=true
```

#### \*\* Preview \*\*

Used in :ghost:<code>html-attributes.jsp</code>

```html
<html data-is-ninja-debug="${ninja.debug}"></html>
```

Used in :ghost:<code>debug-info.jsp</code>

```java
<c:if test="${ninja.debug}">
    // Zobrazí sa na front-ende debug info panel
</c:if>
```

<!-- tabs:end -->
