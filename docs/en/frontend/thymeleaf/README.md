# Thymeleaf templates

Thymeleaf is a templating framework for Spring applications. In addition to our documentation from the left menu, we also recommend reading:

- [Official documentation](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html)
- [Using objects from the model](https://www.baeldung.com/thymeleaf-in-spring-mvc#model)

In WebJET CMS we strictly use Thymeleaf over data attributes, so never use attributes like ```th:text``` but always like ```data-th-text```.

The advantage of using data attributes is that it is fully possible to prototype the template outside of WebJET CMS, for example, listing the page title:

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
```

when executed via WebJET CMS, the static text "Page Title" will be replaced with the value of the ```${docDetails.title}``` object. This is a general Thymeleaf property and is applicable to any attribute (e.g. ```href``` vs ```data-th-href```).