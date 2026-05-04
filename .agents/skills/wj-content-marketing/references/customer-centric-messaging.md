# Customer-Centric Messaging Guide

Guidelines for writing marketing content that focuses on customer benefits rather than technical implementation details.

## Core Principle

Every piece of content should answer: **"What's in it for the customer?"**

Instead of describing what was built, describe what the customer can now **do**, **achieve**, or **avoid**.

## Before vs. After Examples

### Feature Announcements

| Developer-Centric (Avoid) | Customer-Centric (Prefer) |
| --- | --- |
| "We refactored the caching layer" | "Pages now load 2x faster" |
| "Added REST API endpoint for X" | "You can now integrate X with your tools" |
| "Migrated to Spring Boot 3" | "Improved security and performance under the hood" |
| "Fixed null pointer exception in module Y" | "Module Y no longer crashes unexpectedly" |
| "Implemented new database indexing" | "Search results appear instantly" |

### Headlines

| Developer-Centric (Avoid) | Customer-Centric (Prefer) |
| --- | --- |
| "New MediaGroupBean component added" | "Organize your media files into groups with one click" |
| "Upgraded Tomcat to version 11" | "Your site now runs on the latest, most secure platform" |
| "Added CSRF token validation" | "Your forms are now better protected against attacks" |

## Messaging Framework

### 1. Lead with the Benefit

Start every message with what the customer gains:

- **Good**: "Save time managing media — new bulk operations let you edit hundreds of files at once."
- **Bad**: "We added bulk operations to the MediaController REST API."

### 2. Follow with the Feature

After stating the benefit, briefly mention the feature:

- "Pages load faster thanks to our new smart caching system."

### 3. End with a Call to Action

Tell the customer what to do next:

- "Try it now in the admin panel under Settings → Performance."
- "Read the full guide to get started."

## Tone Guidelines

- **Conversational** — write as if explaining to a colleague, not writing a specification
- **Confident** — use active voice ("You can now..." not "It is now possible to...")
- **Specific** — use real numbers and concrete examples when possible
- **Empathetic** — acknowledge pain points before presenting the solution

## Content Type Templates

### Tweet Template

```txt
[Emoji] [Benefit statement in 1 sentence]

[Feature name] in [Product] lets you [action] so you can [outcome].

[Link] [Hashtags]
```

### Blog Post Opening Template

```txt
[Pain point question or statement]

Until now, [how it used to work — the frustration].

With [feature name], you can now [benefit]. Here's how.
```

### Changelog Entry Template

```txt
- **[Feature name]**: [What you can now do] ([link to docs])
```

### Email Subject Line Formulas

- "Now you can [benefit] in [Product]"
- "[Number]x faster [action] — here's what changed"
- "Stop [pain point]. Start [benefit]."
- "New: [Feature] makes [task] effortless"

## Checklist Before Publishing

- [ ] Does the headline mention a customer benefit (not a technical detail)?
- [ ] Would a non-developer understand the value?
- [ ] Is there a clear call to action?
- [ ] Are screenshots or visuals included where relevant?
- [ ] Is the language active and confident?
- [ ] Does it answer "What's in it for me?" within the first sentence?

## Applying to WebJET CMS

When writing about WebJET CMS changes:

- **Target audience**: Web administrators, content editors, site managers
- **Language**: Slovak (primary), English (secondary)
- **Focus areas**: Time saved, easier workflows, better security, improved performance
- **Avoid**: Internal class names, package structures, framework upgrade details (unless relevant to developers using the API)
