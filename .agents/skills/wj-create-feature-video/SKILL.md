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

1. A Slovak narration block preserved in an `@audio` metadata scenario and
   ready for ElevenLabs generation.
2. A concise shot plan mapping narration beats to browser states and manual
   shots.
3. A CodeceptJS scenario in `src/test/webapp/video` when the repository contains
   browser-visible changes.
4. Exact commands for generating narration and previewing or recording the
   scenario, plus the output locations.

Whenever a CodeceptJS walkthrough is created, archive outputs 1 and 2 in the
same JavaScript file as the metadata scenarios described in section 5. Keep
returning them in the final response as well.

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
- Name both the JavaScript file and the main recording `Scenario` as
  `<PR-ID>-<branch-slug>`. Example: PR 293 from
  `feature/config-jstree-view` becomes `293-config-jstree-view.js` and
  `Scenario("293-config-jstree-view", ...)`.
- Tag the main recording scenario with `@video`.
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

The repository defaults to Eleven Multilingual v2 (`eleven_multilingual_v2`)
and the Luki Zajo voice (`Zai7B4Aol2bJtneyq0L1`). Do not send
`voice_settings`; use the voice's stored or default ElevenLabs settings.
Override a model or voice for one scenario with the optional
`{ modelId, voiceId }` argument to
`I.generateAudio`, or for one run with `ELEVENLABS_MODEL_ID` and
`ELEVENLABS_VOICE_ID`. An explicit helper argument takes precedence over a
non-empty environment variable, which takes precedence over the repository
default.

Do not put the ElevenLabs API key in source code, a helper argument, or a
command-line argument. The generator reads it only from
`ELEVENLABS_API_KEY`. Do not call the ElevenLabs API or generate speech unless
the user explicitly requests it; preparing or validating a scenario must not
consume paid API credits.

## 4. Design the Shot Plan

- Map every narration beat to a stable screen or action.
- Start from a clean, useful application state and finish on the state that best
  supports the call to action.
- Prefer a few legible states over many rapid clicks. The editor can extend,
  shorten, or reorder shots later.
- Mark browser-external actions, unreliable third-party pages, and title/outro
  cards as `MANUAL` in the shot plan. Do not hide manual gaps inside brittle E2E
  code.
- Use only the ASCII hyphen `-` for time ranges and separators. Never use an en
  dash (`U+2013`) or em dash (`U+2014`) in the shot plan.
- Avoid sensitive data and customer-specific identifiers in the frame.

## 5. Implement the Walkthrough

- Preserve unrelated working-tree changes and never create a commit.
- Before the main recording scenario, add exactly two metadata scenarios named
  `ElevenLabs` and `Shot plan`. Each scenario must inject only `I` and contain a
  single call with a backtick-delimited multiline string. Put the copy-ready
  Slovak narration in `I.generateAudio()` in `ElevenLabs`; put the complete
  timed shot plan, including every `MANUAL` shot, in `I.say()` in `Shot plan`.
- Put a newline immediately after the opening backtick and immediately before
  the closing backtick. Keep the content lines and closing backtick unindented
  so the copied text contains no leading spaces. Follow the exact template in
  the production reference.
- Keep both metadata scenarios free of login, navigation, assertions, and other
  browser actions. Do not use a global `Before` login hook; inject `login` into
  the main recording scenario and call `login("admin")` there instead.
- Tag `ElevenLabs` with `@audio`, leave `Shot plan` untagged, and tag only the
  main recording scenario with `@video`. Keep all three in this order. The
  audio runner uses an audio-only CodeceptJS configuration without a browser or
  login, while the video runner filters for `@video`.
- Reuse selectors and waits from existing regression tests where possible.
- Prefer a read-only walkthrough. If mutation is essential, create isolated test
  data and clean it up.
- Use `I.videoClick(locator, curveStrength)` for important clicks so the rendered
  cursor follows a varied human-like path with a larger early arc, a subtle late
  correction, and smooth minimum-jerk acceleration and braking. Omit the
  optional strength to use the environment default, use `0` for a straight
  path, use `1` for the baseline curve, and use higher values for a more
  pronounced curve. Use a finite non-negative number and normally stay within
  `0` to `2`. The environment default comes from
  `CODECEPT_VIDEO_CURVE_STRENGTH`, falling back to `1`; an explicit second
  argument overrides it. The cursor reaches the target before the click effect,
  and the recording keeps at least 500 ms of editing room after it. This
  presentation timing is not application synchronization.
- Synchronize with `waitFor*`, URL or application state, and
  `DT.waitForLoader()`. Never use a fixed wait to synchronize application state.
- Keep test comments in English and organize them by shot.
- Do not store narration or the shot plan as unused JavaScript constants. Keep
  them in the two metadata scenarios and also deliver them in the response.

## 6. Validate and Hand Off

Run proportionate checks:

1. Parse changed JavaScript and JSON files and run `npm run audio:test` and
   `npm run video:test` after changing the infrastructure.
2. Run CodeceptJS dry-runs for the audio-only and complete video
   configurations. A dry-run must never contact ElevenLabs.
3. Run `npm run audio video/<scenario-name>.js` only when the user explicitly
   requested generation and `ELEVENLABS_API_KEY` is available. The command
   accepts exactly one existing JavaScript file below `video` and generates
   only its `@audio` scenario.
4. Run only the tagged main recording scenario with
   `npm run video video/<scenario-name>.js` when the configured WebJET CMS
   instance and test credentials are available.
5. Confirm the successful MP3 and WebM artifacts in `build/test/videos`. A
   successful audio response is written as `<scenario-name>.mp3` with format
   `mp3_44100_128`; inspect or listen to generated output before handoff.

In the final response, provide the copy-ready narration first, then its word
count and duration, ElevenLabs model and voice, shot plan, the verified
documentation URL, changed file paths, audio and recording commands, output
paths, validation result, and any manual shots. State whether audio generation
was requested and completed; keep technical caveats outside the narration.
