# Debug režim

 | Metoda | Typ | Popis |
 | -------------- | --------- | ----------- |
 | ${ninja.debug} | *Boolean* | Debug režim |

## Debug režim *Boolean*

<!-- tabs:start -->

#### ** Popis **

Vrátí `true`, je-li parametr v url `ninjaDebug` nastaven na `true`, nebo je-li v `config.properties` nastavený atribut `ninjaDebug` na `true`.

```java
${ninja.debug}
```

Příklad nastavení v URL adrese:

```url
www.nazovdomeny.sk?ninjaDebug=true
```

Příklad nastavení v `config.properties`

```properties
ninjaDebug=true
```

#### ** Ukázka **

Použité v :ghost:<code>html-attributes.jsp</code>

```html
<html data-is-ninja-debug="${ninja.debug}" >
```

Použité v :ghost:<code>debug-info.jsp</code>

```java
<c:if test="${ninja.debug}">
    // Zobrazí sa na front-ende debug info panel
</c:if>
```

<!-- tabs:end -->
