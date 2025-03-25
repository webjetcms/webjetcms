# Thymeleaf šablony

Thymeleaf je šablonovací framework pro Spring aplikace. Kromě naší dokumentace z levého menu doporučujeme přečíst také:
- [Oficiální dokumentace](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html)
- [Používání objektů z modelu](https://www.baeldung.com/thymeleaf-in-spring-mvc#model)

Ve WebJET CMS striktně používáme Thymeleaf přes data atributy, tedy nikdy nepoužívejte atributy jako `th:text` ale vždy jako `data-th-text`.

Výhoda používání data atributů je v tom, že je plně možné prototypovat šablonu i mimo WebJET CMS, například výpis názvu stránky:

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
```

při provedení přes WebJET CMS se statický text "Titulek stránky" nahradí hodnotou objektu `${docDetails.title}`. Toto je všeobecná vlastnost Thymeleaf a je aplikovatelná na libovolný atribut (např. `href` vs `data-th-href`).
