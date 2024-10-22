# Šablony Thymeleaf

Thymeleaf je šablonovací framework pro aplikace Spring. Kromě naší dokumentace v levém menu doporučujeme také četbu:
- [Oficiální dokumentace](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html)
- [Použití objektů z modelu](https://www.baeldung.com/thymeleaf-in-spring-mvc#model)

V systému WebJET CMS striktně používáme Thymeleaf nad datovými atributy, takže nikdy nepoužívejte atributy jako např. `th:text` ale vždy jako `data-th-text`.

Výhodou použití datových atributů je, že je plně možné vytvořit prototyp šablony mimo systém WebJET CMS, například vypsat název stránky:

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
```

při spuštění přes WebJET CMS je statický text "Page Title" nahrazen hodnotou objektu `${docDetails.title}`. Jedná se o obecnou vlastnost Thymeleafu, která se vztahuje na jakýkoli atribut (např. `href` vs `data-th-href`).
