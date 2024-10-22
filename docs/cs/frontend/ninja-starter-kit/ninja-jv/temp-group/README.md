# Informace o projektu

| Metoda | Typ | Popis
| ------------------------------ | -------- | ----------------------- |
| ${ninja.temp.group.siteName}   | *Řetězec* | Název projektu |
| ${ninja.temp.group.autor}      | *Řetězec* | Autor |
| ${ninja.temp.group.copyright}  | *Řetězec* | Autorská práva |
| ${ninja.temp.group.developer}  | *Řetězec* | Vývojář |
| ${ninja.temp.group.generator}  | *Řetězec* | Generátor |
| ${ninja.temp.group.textPrefix} | *Řetězec* | Předpona textového klíče |
| ${ninja.temp.group.fieldA}     | *Řetězec* | Pole A |
| ${ninja.temp.group.fieldB}     | *Řetězec* | Pole B |
| ${ninja.temp.group.fieldC}     | *Řetězec* | Pole C |
| ${ninja.temp.group.fieldD}     | *Řetězec* | Pole D |

## Název projektu *Řetězec*

Hodnota nastavená pro skupinu šablon v poli :european\_castle: `Názov projektu`.

```java
${ninja.temp.group.siteName}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta property="og:site_name" content="${ninja.temp.group.siteName}" />
```

## Autor *Řetězec*

Hodnota nastavená pro skupinu šablon v poli :european\_castle: `Autor`.

```java
${ninja.temp.group.autor}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<title>${ninja.page.doc.title} | ${ninja.temp.group.author}</title>
<meta name="author" content="${ninja.temp.group.author}" />
```

## Autorská práva *Řetězec*

Hodnota nastavená pro skupinu šablon v poli :european\_castle: `Copyright`.

```java
${ninja.temp.group.copyright}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta name="copyright" content="${ninja.temp.group.copyright}" />
```

## Vývojář *Řetězec*

Hodnota nastavená pro skupinu šablon v poli :european\_castle: `Developer`.

```java
${ninja.temp.group.developer}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta name="developer" content="${ninja.temp.group.developer}" />
```

## Generátor *Řetězec*

Hodnota nastavená pro skupinu šablon v poli :european\_castle: `Generátor`.

```java
${ninja.temp.group.generator}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta name="generator" content="${ninja.temp.group.generator}" />
```

## Předpona textových klíčů *Řetězec*

Hodnota nastavená pro skupinu šablon v poli :european\_castle: `Prefix textových kľúčov`.

```java
${ninja.temp.group.textPrefix}
```

## Pole A *Řetězec*

Hodnota nastavená pro skupinu šablon v poli :european\_castle: `Pole A`.

```java
${ninja.temp.group.fieldA}
```

## Pole B *Řetězec*

Hodnota nastavená pro skupinu šablon v poli :european\_castle: `Pole B`.

```java
${ninja.temp.group.fieldB}
```

## Pole C *Řetězec*

Hodnota nastavená pro skupinu šablon v poli :european\_castle: `Pole C`.

```java
${ninja.temp.group.fieldC}
```

## Pole D *Řetězec*

Hodnota nastavená pro skupinu šablon v poli :european\_castle: `Pole D`.

```java
${ninja.temp.group.fieldD}
```
