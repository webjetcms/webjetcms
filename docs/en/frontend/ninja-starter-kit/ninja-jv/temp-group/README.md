# Project information

| Method | Type | Description |
| --- | --- | --- |
| ${ninja.temp.group.siteName} | *String* |  Project name |
| ${ninja.temp.group.author} | *String* | Author |
| ${ninja.temp.group.copyright} | *String* | Copyright |
| ${ninja.temp.group.developer} | *String* | Developer |
| ${ninja.temp.group.generator} | *String* | Generator |
| ${ninja.temp.group.textPrefix} | *String* | Text key prefix |
| ${ninja.temp.group.fieldA} | *String* | Field A |
| ${ninja.temp.group.fieldB} | *String* | Field B |
| ${ninja.temp.group.fieldC} | *String* | Field C |
| ${ninja.temp.group.fieldD} | *String* | Field D |

## Project name *String*

The value set to the template group in the :european_castle: `Názov projektu` field.

```java
${ninja.temp.group.siteName}
```

Used in :ghost:<code>head.jsp</code>
```html
<meta property="og:site_name" content="${ninja.temp.group.siteName}" />
```

## Author *String*

The value set to the template group in the :european_castle: `Autor` field.

```java
${ninja.temp.group.autor}
```

Used in :ghost:<code>head.jsp</code>
```html
<title>${ninja.page.doc.title} | ${ninja.temp.group.author}</title>
<meta name="author" content="${ninja.temp.group.author}" />
```

## Copyright *String*

The value set to the template group in the :european_castle: `Copyright` field.

```java
${ninja.temp.group.copyright}
```

Used in :ghost:<code>head.jsp</code>
```html
<meta name="copyright" content="${ninja.temp.group.copyright}" />
```

## Developer *String*

The value set to the template group in the :european_castle: `Developer` field.

```java
${ninja.temp.group.developer}
```

Used in :ghost:<code>head.jsp</code>
```html
<meta name="developer" content="${ninja.temp.group.developer}" />
```

## *String* Generator

The value set to the template group in the :european_castle: `Generátor` field.

```java
${ninja.temp.group.generator}
```

Used in :ghost:<code>head.jsp</code>
```html
<meta name="generator" content="${ninja.temp.group.generator}" />
```

## Text Key Prefix *String*

The value set to the template group in the :european_castle: `Prefix textových kľúčov` field.

```java
${ninja.temp.group.textPrefix}
```

## Field A *String*

The value set to the template group in the :european_castle: `Pole A` field.

```java
${ninja.temp.group.fieldA}
```

## Field B *String*

The value set to the template group in the :european_castle: `Pole B` field.

```java
${ninja.temp.group.fieldB}
```

## C field *String*

The value set to the template group in the :european_castle: `Pole C` field.

```java
${ninja.temp.group.fieldC}
```

## Field D *String*

The value set to the template group in the :european_castle: `Pole D` field.

```java
${ninja.temp.group.fieldD}
```