---
name: wj-create-feature-video
description: "Create WebJET CMS feature-video assets from a pull request or branch: a customer-focused Slovak ElevenLabs voiceover, a synchronized shot plan, and a repeatable CodeceptJS/Playwright browser walkthrough with optional video recording and a visible cursor. Use when preparing a YouTube demo, release video, ElevenLabs narration, PR walkthrough, or automated product-video scenario."
---

# Create a WebJET CMS Feature Video

Prepare a short product story and the repeatable browser actions needed to record
it. Keep the narration understandable for customers while keeping the E2E
scenario deterministic for developers.

Read [references/production-reference.md](references/production-reference.md)
before implementing or changing a video scenario.

## Produce These Outputs

1. A Slovak narration block that can be copied directly into ElevenLabs.
2. A concise shot plan mapping narration beats to browser states and manual
   shots.
3. A CodeceptJS scenario in `src/test/webapp/video` when the repository contains
   browser-visible changes.
4. Exact commands for previewing and recording the scenario, plus the output
   location.

Do not force an E2E scenario when a change has no useful browser-visible state.
Explain that limitation and keep the narration and manual shot plan useful.

## 1. Establish the Source and Name

- Inspect the pull request, its diff, documentation, changelog, screenshots, and
  existing E2E coverage. Prefer visible behavior and customer value over commit
  wording.
- Resolve and verify the exact documentation URL in the target language. Do not
  use a generic documentation home page when a feature-specific page exists.
- Determine the pull request ID and source branch. If either cannot be discovered
  safely, ask for it.
- Remove only a leading `feature/` or `hotfix/` from the branch name. Sanitize the
  remainder to a lowercase hyphenated slug.
- Name both the JavaScript file and the `Scenario` as
  `<PR-ID>-<branch-slug>`. Example: PR 293 from
  `feature/config-jstree-view` becomes `293-config-jstree-view.js` and
  `Scenario("293-config-jstree-view", ...)`.
- Use `Feature("video.<scenario-name>")`.

## 2. Frame the Customer Story

Identify one primary promise and at most three supporting benefits. For a
nontechnical WebJET CMS audience, explain what becomes easier, faster, clearer,
or safer. Avoid implementation details unless they are visible and necessary to
understand the feature.

Default to these parameters unless the user specifies different ones:

- Slovak language.
- WebJET CMS customers and users, typically nontechnical.
- 16:9 landscape video lasting about 80 to 90 seconds.
- A closing invitation to open the relevant WebJET CMS documentation.

## 3. Write the ElevenLabs Narration

- Lead with the user problem or benefit, then demonstrate the improvement, and
  finish with the documentation call to action.
- Use natural spoken Slovak, short sentences, and one idea per sentence.
- Keep `WebJET CMS` written exactly this way. Do not replace it with a phonetic
  spelling.
- Return plain text only inside the copy block. Do not put headings, shot notes,
  SSML, pause tags, bracketed directions, or artificial pause markers into the
  narration.
- Do not make claims that cannot be verified from the pull request or docs.
- Keep settings outside the narration block so ElevenLabs cannot read them.
- Estimate the spoken duration from the word count while drafting. For the
  default 80-to-90-second format, start around 170 to 195 Slovak words, then use
  the selected ElevenLabs voice preview as the authoritative duration when it is
  available. Revise the text to fit and report both the word count and estimated
  or measured duration.

Recommend ElevenLabs Text to Speech with a Slovak-capable voice. Verify the
current official model guidance when web access is available. For a calm product
narration, use Eleven Multilingual v2 as the starting point unless a newer model
is demonstrably better for the selected voice. Start around Stability 50,
Similarity 75, Style 0, Speaker Boost on, and Speed 0.95 to 1.0, then adjust after
a short pronunciation test.

## 4. Design the Shot Plan

- Map every narration beat to a stable screen or action.
- Start from a clean, useful application state and finish on the state that best
  supports the call to action.
- Prefer a few legible states over many rapid clicks. The editor can extend,
  shorten, or reorder shots later.
- Mark browser-external actions, unreliable third-party pages, and title/outro
  cards as `MANUAL` in the shot plan. Do not hide manual gaps inside brittle E2E
  code.
- Avoid sensitive data and customer-specific identifiers in the frame.

## 5. Implement the Walkthrough

- Preserve unrelated working-tree changes and never create a commit.
- Reuse selectors and waits from existing regression tests where possible.
- Prefer a read-only walkthrough. If mutation is essential, create isolated test
  data and clean it up.
- Use `I.videoClick(locator)` for important clicks so the rendered cursor follows
  a gentle curved path with ease-in-out timing, reaches the target before the
  click effect, and the recording keeps at least 500 ms of editing room after
  it. This presentation timing is not application synchronization.
- Synchronize with `waitFor*`, URL or application state, and
  `DT.waitForLoader()`. Never use a fixed wait to synchronize application state.
- Keep test comments in English and organize them by shot.
- Do not include narration text as unused JavaScript data. Deliver it separately
  in the response.

## 6. Validate and Hand Off

Run proportionate checks:

1. Parse changed JavaScript and JSON files.
2. Run CodeceptJS `dry-run` for the new video scenario.
3. Run the actual scenario when the configured WebJET CMS instance and test
   credentials are available.
4. Confirm a passed WebM is retained in `build/test/videos` and visually inspect
   at least representative frames for resolution, cursor visibility, and
   sensitive data.

In the final response, provide the copy-ready narration first, then its word
count and duration, ElevenLabs settings, shot plan, the verified documentation
URL, changed file paths, recording command, output path, validation result, and
any manual shots. Keep technical caveats outside the narration.
