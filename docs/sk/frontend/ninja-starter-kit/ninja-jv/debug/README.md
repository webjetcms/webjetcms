# Debug režim
| Metóda | Typ | Popis |
| --- | --- | --- |
| ${ninja.debug} | *Boolean* | Debug režim |

## Debug režim *Boolean*

<!-- tabs:start -->

#### ** Popis **
Vráti `true`, ak je parameter v url `ninjaDebug` nastavený na `true`, alebo ak je v `config.properties` nastavený atribút `ninjaDebug` na `true`.

```java
${ninja.debug}
```

Príklad nastavenia v URL adrese:
```url
www.nazovdomeny.sk?ninjaDebug=true
```

Príklad nastavenia v `config.properties`
```properties
ninjaDebug=true
```

#### ** Ukážka **
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






