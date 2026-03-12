# Test translations

The purpose of this file is to test the compiler and formatting retention. This file should contain various types of formatting such as **bold text**, *italics*, `kód`, and [Google](https://www.google.com) links.

For testing, set the value of `javascript` in the `deepmark.config.mjs` file as follows:

```javascript
    directories: [
        ['sk/translation-test', '$langcode$/translation-test'],
    ]
```

It should also contain lists, it is important here that there is **a blank line** after this heading.

- First point
- Second point
- Third point

It is important that it preserves structure, empty lines, and the like.

## `Sidebar` problem

Here it will delete the empty line after `point_left`, which will cause this link to merge with the next text. WARNING: test the behavior in the `_sidebar.md` file as well, because it sometimes works correctly here.

ATTENTION: also that it will turn `point_left` into `point\_left`.

<div class="sidebar-section">Operation manual</div>

- [:point_left: Back to Home](/?back)

- Security
  - [Security Tests](../sysadmin/pentests/README.md)
  - [Library Vulnerability Check](../sysadmin/dependency-check/README.md)
  - [WebJET Update](../sysadmin/update/README.md)

## Tables

Example of a table. Here, BE CAREFUL not to change `perex_group_id` to `perex\_group\_id` and so on.

| perex_group_id | perex_group_name      | `domain_id` | `available_groups` |
|----------------|-----------------------|-------------|--------------------|
| 3              | another perex group  | 1           | test               |
| 645            | `deletedPerexGroup`   | 1           | `NULL`             |
| 794            | event-calendar     | 1           |                    |
| 1438           | another perex group  | 83          | have a nice day          |
| 1439           | `deletedPerexGroup`   | 83          | `NULL`             |
| 1440           | event-calendar     | 83          | `NULL`             |

Table with spaces in the header:

| Module code | Description | Reason for deactivation |
| --- | --- | --- |
| `cmp_forum` | Forum and discussion board | Reduces the risk of XSS and spam attacks |
| `cmp_blog` | Blog | Reduces the risk of XSS and spam attacks |
| `cmp_dmail` | Distribution list (newsletter) | Reduces the risk of spam attacks through mass mailing |

## HTML code

The MD file can also contain HTML code, such as a YouTube video. Here, you must not forget the quotation marks after the `allow` attribute.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/XRnwipQ-mH4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Changelog

We have various strange constructions in the changelog.

- Modified **file upload** processing `multipart/form-data`, more in [programmer section](#test-translations) (#57793-3).
- We recommend **checking the functionality of all forms** due to adjustments to their processing, more information in the [for programmers](#test-translations) section (#58161).

### Websites

- Added the ability to insert a `PICTURE` element that displays [image based on screen resolution](../frontend/setup/ckeditor.md#picture-element) of the visitor. This allows you to display different images on a mobile phone, tablet, or computer (#58141).

![](../frontend/setup/picture-element.png)

- Added the ability to insert [custom icons](../frontend/setup/ckeditor.md#svg-icons) defined in a shared SVG file (#58181).

![](../frontend/setup/svgicon.png)

### Various formatting

Here is bold text **bold**, italics *italics*, but also their combination ***bold italics***. Then there is ~~strikethrough text~~ and text with `inline kódom`.

Link with title: [Google](https://www.google.com "Google Search").

Reference link: [WebJET CMS][webjet] and other [link][webjet].

[webjet]: https://www.webjetcms.sk "WebJET CMS"

## Headings of all levels

### H3 section heading

#### H4 sub-section heading

##### H5 heading

###### H6 heading

## Quotes

Simple quote:

> This is a quote. It may contain **bold text**, *italics*, or `kód`.

Inline quote:

> External citation with some text.
>
> > Nested quote inside.
> >
> > > An even deeper quote.

Multi-paragraph quote:

> The first paragraph of the quote contains text that may be longer.
>
> Second paragraph of the same quote.

## Alerts/Admonitions

!> This is a warning. It contains important information that you should pay attention to.

?> This is a tip (info). It contains useful information for the user.

## Ordered lists

1. First point
2. Second point
3. Third point
   1. Nested first point
   2. Nested second point
4. Fourth point

List with longer text:

1. **First bullet point** – contains bold text and a description of what will happen with this choice.
2. *Second point* – contains italics and additional description.
3. Third point with a link to [documentation](https://www.google.com).

## Unordered lists - nested

- Main point A
  - Nested point A1
  - Nested point A2
    - Deeply embedded point A2a
    - Deeply embedded point A2b
  - Nested point A3
- Main point B
- Main point C

## Task list

- [x] Task completed
- [x] Another completed task
- [ ] Unfinished task
- [ ] Another unfinished task with **bold** text

## Horizontal line

Text before the horizontal line.

---

Text behind the horizontal line.

## Code blocks

SQL block:

```sql
SELECT wp.doc_id, wp.title, wp.perex_group_id
FROM web_pages wp
WHERE wp.domain_id = 1
  AND wp.deleted = 0
ORDER BY wp.doc_id;
```

Java block:

```java
@RestController
@RequestMapping("/api/v1/pages")
public class PageRestController {
    @GetMapping("/{id}")
    public ResponseEntity<PageBean> getPage(@PathVariable Long id) {
        return ResponseEntity.ok(pageService.getPage(id));
    }
}
```

Block with language `Bash`:

```bash
./gradlew appStart -Pprofile=local
```

YAML block:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/webjet
    username: webjet
    password: webjet
```

## Hard line break

This is the first line.\
This is the second line after the solid break.

This is the first line with two spaces at the end (hard break before it).
This is the second line after the soft-break.

## Escaped characters

These characters must remain intact after translation: \*asterisk\*, \_underscore\_, \`spätný apostrof\`, \[square bracket\], \#grid\`, \&ampersand\.

## Images

Image with alt text:

![Alternative image description](../frontend/setup/picture-element.png)

Image with alt text and `title`:

![Alternative image description](../frontend/setup/picture-element.png "Image title")

Reference image:

![Logo][logo]

[logo]: ../frontend/setup/picture-element.png "WebJET Logo"

## Mixed formatting in text

This is a paragraph with **bold**, *italic* and ***bold-italic*** text. It also contains `inline kód`, ~~strikethrough text~~ and [Google link](https://www.google.com). It may also contain the URL: https://www.webjetcms.sk.

Another paragraph with a combination of formatting in the sentence: When setting the value `domain_id` in the table `web_pages`, the correct value must be used **before saving the record**, otherwise an error may occur.

## Table with different formatting in cells

| Column | Bold text | Code | Link |
| -------- | ----------- | ----- | ------- |
| Line 1 | **fat** | `code_value` | [link](https://www.google.com) |
| Line 2 | *italics* | `NULL` | [Documentation](../frontend/setup/ckeditor.md) |
| Line 3 | ~~crossed out~~ | `perex_group_id` | – |

Table with column alignment:

| Left column | Middle column | Right column |
|:------------|:--------------:|-------------:|
| left       | middle          | right       |
| A-value   | B-value      | C-value    |

## Inline HTML

Text with inline HTML: this is <strong>bold HTML text</strong> and this is <em>italic HTML</em>.

Inline <code>HTML code</code> and <a href="https://www.google.com">HTML link</a>.

<p>A paragraph in HTML with <strong>bold</strong> text and a <a href="https://www.google.com" title="Google">link</a>.</p>

## Multiline lists with paragraphs

- First item on the list.

    This paragraph belongs to the first bullet point and is indented.

- Second item on the list.

    This paragraph belongs to the second point on the list.

    > Quote nested in a list point.

- Third item in the list with the code:

  ```javascript
  const value = config.get('domain_id');
  ```
