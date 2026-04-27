# Debug mode

| Method | Type | Description |
| --- | --- | --- |
| ${ninja.debug} | *Boolean* | Debug mode |

## Debug mode *Boolean*

<!-- tabs:start -->

#### ** Description **

Returns `true` if the parameter in the url `ninjaDebug` is set to `true`, or if the attribute `ninjaDebug` in `config.properties` is set to `true`.

```java
${ninja.debug}
```

Example of setting in URL address:
```url
www.nazovdomeny.sk?ninjaDebug=true
```

Example of setting in `config.properties`
```properties
ninjaDebug=true
```

#### ** Preview **

Used in :ghost:<code>html-attributes.jsp</code>
```html
<html data-is-ninja-debug="${ninja.debug}" >
```

Used in :ghost:<code>debug-info.jsp</code>
```java
<c:if test="${ninja.debug}">
    // Zobrazí sa na front-ende debug info panel
</c:if>
```

<!-- tabs:end -->






