# Information about the project

| Method | Type | Description | | | ------------------------------ | -------- | ----------------------- | | ${ninja.temp.group.siteName}   | *String* | | | ${ninja.temp.group.autor}      | *String* | Author | | | ${ninja.temp.group.copyright}  | *String* | Copyright | | ${ninja.temp.group.developer}  | *String* | Developer | | ${ninja.temp.group.generator}  | *String* | Generator | | | ${ninja.temp.group.textPrefix} | *String* | Text key prefix | | ${ninja.temp.group.fieldA}     | *String* | Field A | | ${ninja.temp.group.fieldB}     | *String* | Field B | | ${ninja.temp.group.fieldC}     | *String* | Field C | | ${ninja.temp.group.fieldD}     | *String* | Field D |

## Project name *String*

The value set for the template group in the :european\_castle field: `Názov projektu`.

```java
${ninja.temp.group.siteName}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta property="og:site_name" content="${ninja.temp.group.siteName}" />
```

## Author *String*

The value set for the template group in the :european\_castle field: `Autor`.

```java
${ninja.temp.group.autor}
```

Used in :ghost:<code>head.jsp</code>

```html
<title>${ninja.page.doc.title} | ${ninja.temp.group.author}</title> <meta name="author" content="${ninja.temp.group.author}" />
```

## Copyright *String*

The value set for the template group in the :european\_castle field: `Copyright`.

```java
${ninja.temp.group.copyright}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta name="copyright" content="${ninja.temp.group.copyright}" />
```

## Developer *String*

The value set for the template group in the :european\_castle field: `Developer`.

```java
${ninja.temp.group.developer}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta name="developer" content="${ninja.temp.group.developer}" />
```

## Generator *String*

The value set for the template group in the :european\_castle field: `Generátor`.

```java
${ninja.temp.group.generator}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta name="generator" content="${ninja.temp.group.generator}" />
```

## *String*

The value set for the template group in the :european\_castle field: `Prefix textových kľúčov`.

```java
${ninja.temp.group.textPrefix}
```

## *String*

The value set for the template group in the :european\_castle field: `Pole A`.

```java
${ninja.temp.group.fieldA}
```

## *String*

The value set for the template group in the :european\_castle field: `Pole B`.

```java
${ninja.temp.group.fieldB}
```

## *String*

The value set for the template group in the :european\_castle field: `Pole C`.

```java
${ninja.temp.group.fieldC}
```

## *String*

The value set for the template group in the :european\_castle field: `Pole D`.

```java
${ninja.temp.group.fieldD}
```
