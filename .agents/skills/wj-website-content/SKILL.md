---
name: wj-website-content
description: 'Maintain the official WebJET CMS marketing websites (webjetcms.sk and webjetcms.com): update copy, feature coverage, media, news and navigation using docs/website. Use for these product websites, not customer websites or general CMS development. Core repository only.'
---

# WebJET website content

Maintain the official product website content in this core checkout. The working deliverables and durable decisions live in [docs/website](../../../docs/website/README.md); this skill routes to them instead of copying the chat or freezing a feature list. Keep this skill in the core repository's [distribution exclusion list](../../../.github/skills-distribution/excluded-skills.txt).

## Establish the change

Read the [delivery guide](../../../docs/website/README.md), the relevant part of the [content strategy](../../../docs/website/content-strategy.md), and the affected HTML before editing. Use [editorial notes](../../../docs/website/editorial-notes.md) for approved messaging, CTA wording and page metadata. Existing decisions are the baseline; clarify only information missing for this request, without repeating the original strategy interview.

Determine the requested page, language and purpose. SK targets `webjetcms.sk`; EN targets `webjetcms.com`, not an `/en/` directory on the SK domain. The delivery guide maps filenames to public URLs. For a product-wide factual correction, identify affected SK and EN counterparts; for a language-specific copy request, keep that scope and report any counterpart needing a separate update.

Inspect local changes before editing. The user also edits pages directly in CMS and may copy the exported HTML back into the repository. Preserve those edits and formatting, especially application macros. When a request concerns the live page or navigation, inspect that page and reconcile relevant differences rather than replacing it with an older local draft. Repository preparation alone does not authorize publishing to CMS or changing live navigation.

## Choose placement and substantiate claims

Use the page roles in the content strategy: the homepage introduces the system and next step; Benefits highlights the top capabilities and their practical value; Features explains capabilities in more detail; Applications groups visitor-facing uses and edition availability. Developer and agency pages serve different audiences. Extend the relevant sections instead of repeating a full feature description on every page.

Preserve the agreed enterprise and public-sector focus, concrete AI assistance, approval and scheduling workflows, and the prominence of headless delivery and multi-step forms. Smaller projects remain welcome. Keep the demo as the primary conversion and the Open Source GitHub link as a secondary route. Use the editorial notes for exact CTA wording and confirmed reference clients; a reference does not imply that client uses every advertised feature.

For a new or changed capability:

1. Find current evidence in `docs/sk/`, particularly `docs/sk/sales/`, and the corresponding EN documentation when relevant. Follow implementation links when availability, prerequisites or behavior are unclear. For a supplied PR, inspect its actual changes and release state using the repository's issue-tracker conventions.
2. Use [product evidence](../../../docs/website/product-evidence.md) and [developer/agency evidence](../../../docs/website/developer-agency-evidence.md) as starting points with dates, not as proof of today's release status. Spring Boot and multi-page publication were originally marked **Pripravujeme**; verify delivery before changing that wording. A merged PR or development/SNAPSHOT build alone does not establish general availability.
3. Explain the user's task and benefit, then relevant conditions. Distinguish Community/Open Source from commercial editions and support. For edition tables, follow the module mapping procedure in the delivery guide and verify against current project licensing; do not infer module availability from a feature description.
4. Record material new evidence or changed decisions in the relevant existing Markdown document. Refresh the primary sources in [competitor analysis](../../../docs/website/competitor-analysis.md) before making new BUXUS or WordPress comparisons. Avoid unsupported savings, security guarantees, compliance claims or partner benefits.

## Produce paste-ready content

Edit the existing fragment in `docs/website/sk/` or `docs/website/en/`. Keep notes and evidence in `docs/website/*.md`. The output is the content **inside** the existing `article.ly-article`, with one H1 and the `.wj-website-content` wrapper; page metadata belongs in editorial notes for CMS properties.

Retain the existing public template design and CSS classes. Its Bootstrap version differs from the current CMS administration. Reuse the fragment's layout; put necessary article-specific additions in its scoped inline `style`. A content update is not a redesign. Preserve IDs and cross-page anchors; check incoming links when deliberately changing them. EN uses British English and retains shared technical IDs and proper brand names.

Read only the additional references needed for the requested change:

| Change | Reference and contract |
| --- | --- |
| Video or image | [Video manifest](../../../docs/website/video-manifest.md) and [image manifest](../../../docs/website/image-manifest.md). Prefer a relevant documentation video for SK, using the existing `video_player.jsp` INCLUDE with autoplay disabled and a direct YouTube link. EN currently uses EN documentation screenshots where suitable English videos are unavailable. Verify language and content before replacing media. |
| Asset paths | Use the image manifest's local-to-public mapping: language screenshots under `/images/webjetcms-2026/sk/` or `/en/`, shared logos in the parent directory. Record sources and dimensions. New paths require uploading assets before content; retain older server assets that published pages may use. |
| News | [News integration](../../../docs/website/news-feed.md). Keep the locale-specific JSON feed and permanent link to all changes in documentation. Preserve the encoded `app-htmlembed/embed.jsp` macro byte-for-byte unless the script itself must change. The old editor corrupts raw JavaScript whitespace. For an intentional script edit, decode Base64 and the `utf8:` URL encoding, edit and validate the decoded script, then re-encode and verify an exact round trip. Keep untrusted feed data as text, existing URL checks and the failure fallback. |
| Menu or shared template | [Navigation decisions](../../../docs/website/navigation-proposal.md) and the delivery guide's CSS/JS section. Separate confirmed implementation from historical proposals. Menu is outside the article. Archive requested template patches in `docs/website/css/` or `docs/website/js/` with their deployment destination; verify actual DOM and existing styles first. |

## Verify and hand off

Scale checks to the change. Check modified HTML structure, unique IDs, affected anchors, asset existence, meaningful alt text, and the intended language/domain of links. If a news script changed, validate its decoded JavaScript syntax and relevant feed/failure behavior; for an unrelated edit, compare the encoded macro before and after.

Use [preview.py](../../../docs/website/preview.py) for visible content or layout changes:

```sh
python3 docs/website/preview.py --lang sk --port 8765
python3 docs/website/preview.py --lang en --port 8766
```

Run only the needed language instance(s). Inspect affected sections at desktop and narrow mobile widths, including media, CTA and wrapping. The preview uses the public template and translates supported INCLUDE macros only in its response; do not paste rendered preview markup back into the deliverables. Template scripts are removed, so this preview does not verify the live menu, contact submission or the old CMS editor.

Update [verification](../../../docs/website/verification.md) with checks actually performed, their date and limitations when they add useful evidence. Save relevant review screenshots under the language's `previews/` directory. Old verification records are historical, not a substitute for checks of changed behavior. A content-only task does not need a core build or full CMS test suite.

Deliver links to the changed fragments and assets, explain any required CMS metadata or upload steps, and distinguish prepared files from anything actually published. Leave changes uncommitted on the current branch under the repository rules.
