# Thymeleaf šablóny

Thymeleaf je šablónovací framework pre Spring aplikácie. Okrem našej dokumentácia z ľavého menu odporúčame prečítať aj:

- [Oficiálna dokumentácia](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html)
- [Používanie objektov z modelu](https://www.baeldung.com/thymeleaf-in-spring-mvc#model)

Vo WebJET CMS striktne používame Thymeleaf cez data atribúty, čiže nikdy nepoužívajte atribúty ako ```th:text``` ale vždy ako ```data-th-text```.

Výhoda používania data atribútov je v tom, že je plne možné prototypovať šablónu aj mimo WebJET CMS, napríklad výpis názvu stránky:

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
```

pri vykonaní cez WebJET CMS sa statický text "Titulok stránky" nahradí hodnotou objektu ```${docDetails.title}```. Toto je všeobecná vlastnosť Thymeleaf a je aplikovateľná na ľubovoľný atribút (napr. ```href``` vs ```data-th-href```).