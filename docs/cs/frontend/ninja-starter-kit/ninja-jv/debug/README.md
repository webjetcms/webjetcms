# Režim ladění

| Metoda | Typ | Popis
| -------------- | --------- | ----------- |
| ${ninja.debug} | *Boolean* | Režim ladění |

## Režim ladění *Boolean*

<!-- tabs:start -->

#### ** Popis **

Vrací se `true` pokud je parametr v url `ninjaDebug` nastavit na `true`, nebo pokud je v `config.properties` sada atributů `ninjaDebug` na adrese `true`.

```java
${ninja.debug}
```

Příklad nastavení v adrese URL:

```url
www.nazovdomeny.sk?ninjaDebug=true
```

Příklad nastavení v `config.properties`

```properties
ninjaDebug=true
```

#### ** Náhled **

Používá se v :ghost:<code>html-attributes.jsp</code>

```html
<html data-is-ninja-debug="${ninja.debug}" >
```

Používá se v :ghost:<code>debug-info.jsp</code>

```java
<c:if test="${ninja.debug}">
    // Zobrazí sa na front-ende debug info panel
</c:if>
```

<!-- tabs:end -->
