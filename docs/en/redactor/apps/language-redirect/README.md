# Redirect by language

The application allows automatic redirection of visitors to the language version of the site based on the language of their browser. It detects the language from the HTTP header `Accept-Language` and redirects the user to the appropriate URL according to the configured rules.

## Purpose

This application is designed for multilingual websites where you want to automatically divide visitors into the appropriate language variants. For example:

- A visitor with a Slovak browser will be redirected to `/sk/`
- Visitor with English browser will be redirected to `/en/`
- If language is not detected, default language will be used

## Installation

You insert the **Language Redirection** application through the application list into a web page in the root folder - typically a web page with the URL `/`.

## Application settings

The application has two settings tabs: **Basic** and **Advanced**.

### Basic settings

![](editor-basic.png)

#### Default language

Specifies the language to use if the browser language is not detected or no mapping is found in the configuration. The default value is `sk`.

#### Language mappings to URL addresses

You can configure **up to 8 pairs** of language mappings to the redirect URL. Each pair consists of:

1. **Language** – select a language from the available languages ​​of your design (same list as when editing the template)
2. **Redirect** – URL to which the user will be redirected (selected language)

If the language field is empty, the given assignment is ignored. You can select the URL field using the link selector.

**Configuration example:**

| # | Language | Redirection |
| - | ----- | --------- |
| 1 | sk | /sk/ |
| 2 | en | /en/ |
| 3 | cs | /cs/ |
| 4–8 | (empty) | (empty) |

### Advanced settings

![](editor-advanced.png)

#### Root URL only

If this option is enabled, the redirection will be performed **only on the root URL** of the page (e.g. `/` or `/index.html`). The redirection will not be performed on other pages. Use if the application is embedded in some common part such as the header or footer. However, we recommend embedding the application directly into the page with the URL `/`, so that it will not be unnecessarily executed on other websites.

#### Respect language cookie

If this option is enabled (default), the application checks for the presence of a **language cookie named `lng`**. If the user has this cookie set, its value will be used instead of the detected language from the browser.

**Benefit:** Users will be redirected to the page in the language variant they were last shown.

## How it works

The redirection process occurs in the following order:

1. **Root URL Check** – If the *Root URL Only* option is enabled and the current URL is not the root URL, the redirect will not be performed.
2. **Language Cookie Check** – If *Respect Language Cookie* is enabled and the cookie `lng` exists, its value will be used as the language.
3. **Language Detection** – If there is no cookie, the application detects the language from the HTTP header `Accept-Language`. Parsed is the first language with the highest priority, regional variants (e.g. `en-US` → `en`) and quality factors (e.g. `;q=0.7`) are removed.
4. **URL Lookup** – The application goes through all 8 configured mappings and looks for a match with the detected language.
5. **Fallback to default language** – If no mapping is found for the detected language, a mapping for the **default language** is attempted.
6. **Redirect** – If a URL is found, an HTTP redirect is performed to that address. Otherwise, the page loads normally.

### Language detection

The application checks the value of the HTTP header `Accept-Language` as follows:

- Splits the value by line ( `,` ) and takes the first language
- Removes quality factor (e.g. `;q=0.8`)
- Removes regional part (e.g. `en-US` → `en`, `sk_SK` → `sk`)
- Converts the result to lowercase

**Examples:**

| Header | Detected language |
| -------- | ---------------- |
| `sk-SK,sk;q=0.9,en-US;q=0.5` | en |
| `en-GB,en;q=0.9` | en |
| `cs-CZ,cs;q=0.8,en;q=0.7` | en |
| (empty) | default language |

## Usage examples

### Example 1: Simple division into SK and EN

For a multilingual site with Slovak and English versions:

```txt
Predvolený jazyk: sk
Mapping 1: sk → /sk/
Mapping 2: en → /en/
```

### Example 2: Multiple languages ​​with cookie respect

For a page with Slovak, Czech and English versions where users can change the language manually:

```txt
Predvolený jazyk: sk
Mapping 1: sk → /sk/
Mapping 2: en → /en/
Mapping 3: cs → /cs/
Len koreňová URL: ✓
Rešpektovať jazykový cookie: ✓
```

### Example 3: Just redirect to root

In case you only want to redirect visitors coming to the homepage:

```txt
Len koreňová URL: ✓
Rešpektovať jazykový cookie: ✓
Mapping 1: en → /english/
```

## Important notes

- The application must be inserted on the page **before** the content to be redirected.
- If the application is embedded on an internal page and the *Root URL only* option is enabled, the redirect will not be performed.
- The cookie value `lng` takes precedence over language detection from the browser.
- If no mapping is found for the detected language or the default language, the redirection is not performed and the page loads normally.
- Language options in the editor are dynamically loaded from the page design configuration using `LayoutService.getLanguages()`.
