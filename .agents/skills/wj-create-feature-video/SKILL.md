---
name: wj-create-feature-video
description: "Create WebJET CMS feature-video assets from a pull request or branch: one JSON shot plan with localized narration, reorderable CodeceptJS/Playwright shot functions, ElevenLabs audio generation, and browser recordings with editing slates and a visible cursor. Use when preparing a YouTube demo, release video, PR walkthrough, or automated product-video scenario."
---

# Create a WebJET CMS Feature Video

Prepare a customer-focused product story and repeatable browser footage. Keep
shot metadata, localized narration and execution order in one JSON-compatible
object; derive the narration, shot plan and walkthrough from it.

Read [references/production-reference.md](references/production-reference.md)
before implementing or changing a video scenario. It contains the exact schema,
scenario template, recording defaults and validation commands.

## 1. Establish the Source and Name

- Inspect the PR or branch diff, documentation, changelog, screenshots and
  existing E2E coverage. Prefer visible behavior and customer value over commit
  wording. Verify the feature-specific documentation URL in the target language.
- Determine the PR ID and branch. Ask only when they cannot be discovered safely.
- Remove a leading `feature/` or `hotfix/` and sanitize the rest to a lowercase
  hyphenated slug. Name the JavaScript file and main scenario
  `<PR-ID>-<branch-slug>`, e.g. `293-config-jstree-view.js`.
- Use `Feature("video.<scenario-name>")` and store the file in
  `src/test/webapp/video`. Preserve unrelated edits and never create a commit.
- If there is no useful browser-visible change, keep a manual plan and narration
  rather than forcing an automated walkthrough.

## 2. Frame the Story and Write Shot Narration

Default to nontechnical WebJET CMS customers, Slovak, 16:9 landscape, about
80 to 90 seconds, and a closing invitation to the feature documentation. Follow
explicit requests for other lengths or languages.

Identify one main promise and up to three supporting benefits. Explain what
becomes easier, faster, clearer or safer. Lead with the problem or benefit, show
the improvement and close with the documentation call to action.

Write the narration directly in each shot's `text-sk`. Use natural spoken
Slovak, short sentences and one idea per sentence. Keep `WebJET CMS` spelled
exactly this way. Do not include headings, SSML, pause tags, bracketed shot
instructions or unsupported claims in spoken text. Keep production directions
in `notes` and ElevenLabs settings outside the text.

Start around 170 to 195 Slovak words for the default duration. Count words
across all shots, including manual ones. Use measured voice duration when it
is available; `durationSeconds` remains an editing estimate until adjusted.

## 3. Keep One JSON Shot Plan

- Declare a top-level `const videoPlan = { ... };` before the scenarios. Its
  initializer must be strict JSON: quoted keys and strings, no comments,
  trailing commas, functions, interpolation or expressions inside the object.
  This lets the audio runner validate the data without executing scenario code.
- Use `language: "sk"` and a `shots` array. Each shot has a stable descriptive
  `id`, `type` (`"auto"` or `"manual"`), positive integer `durationSeconds`,
  English `title`, localized `text-sk`, and optional production `notes`.
- `auto` means browser steps are automated; `manual` means footage/cards are
  supplied during editing. Old `AUTO 1`, `AUTO 2` labels were shot numbers, not
  different execution types. Never encode position in the type or use an array
  index as the action id.
- Store one narration beat and matching browser action/state per shot. If an
  old shot covers different narration beats, split it into separate stable ids.
- The array order is authoritative for narration, shot-plan numbering, derived
  time ranges and automatic execution. Move the entire shot object to reorder
  it; keep its id and callback together by name. Do not maintain a separate
  narration block, hard-coded timeline, ordered callback list or title strings.
- Use `text-cs` for Czech and `text-en` for English. Select the default with
  `videoPlan.language`; `I.generateAudio(videoPlan, { language: "en" })` can
  override audio language for one run. Missing translations fail explicitly;
  use `""` only for a deliberately silent shot. Translating narration alone does
  not translate UI selectors, fixture content or the browser login language.
- Put browser-external actions, unreliable third-party pages and final
  title/outro cards in manual shots. Keep every manual shot in the JSON so its
  audio and duration remain part of the story. Avoid sensitive data in footage.
- Time ranges are derived cumulatively from `durationSeconds`. They describe
  the edited film, excluding setup, cleanup and two-second editing slates.
  Never use these durations as application waits. Use ASCII hyphens in ranges.

## 4. Implement the Three Scenarios

Keep these scenarios in order:

1. `ElevenLabs`: inject only `I`, use a synchronous callback containing one
   `I.generateAudio(videoPlan)` call and tag only `@audio`.
2. `Shot plan`: format the same object with `formatShotPlan(videoPlan)` from
   `helpers/feature_video_plan.js` and print it with `I.say`; leave it untagged.
3. The named main walkthrough: tag `@video`. Add `@current` here only if needed.

Keep metadata callbacks free of browser actions. Do not add global login hooks.
Require shared utilities inside the relevant callback; the audio runner permits
only static declarations and direct Feature/Scenario calls at file scope.
Existing plain-text audio scenarios remain supported, but use JSON for new work.

In the main async scenario, map each automatic id to its own async function and
call `await recordVideoPlan(I, { plan: videoPlan, scenarios: shots, ... })` from
`helpers/feature_video_plan.js`. Do not copy the recording loop into scenarios.
Pass the injected `I` as the first argument. The second argument is an options
object containing `plan`, `scenarios` (callbacks keyed by shot id), and only the
lifecycle callbacks needed:

- `setup`: one-time login and shared setup, after all action ids are validated.
- `prepare`: a shared baseline before every shot.
- `prepareShots`: optional extra preparation functions keyed by shot id.
- `cleanup`: scenario-specific cleanup after each successful shot.

The runner logs `Recording shot <index>/<total> <id> (<duration>s)` and displays
a two-second `SETUP shot <index>/<total> <id>` slate. The index and total count
only automatic shots in their current recording order. It then awaits baseline and id-specific preparation, displays the normal two-second
shot slate, executes its callback and awaits cleanup. The normal slate includes
the derived number/title and first 200 Unicode characters of localized narration.
Cut everything from the SETUP slate through the normal slate out of the final
film, along with cleanup. Neither slate contributes to the edited timeline.

A reordered shot must not depend on a prior shot's dialog, selection, search or
mutation. Reopen/reset the editor with isolated browser-only content when that
is the simplest reliable baseline. Use ordinary `I.click` during preparation to
avoid cursor animation and editing holds in footage that will be cut. Discard
temporary changes during cleanup, including handling native confirmation dialogs.

Await actor steps inside async callbacks so recording order stays deterministic.
`recordVideoPlan` is an ordinary async module function invoked from a Scenario,
not an `I` helper step; wrapping actor callbacks inside a CodeceptJS helper step
can interfere with its recorder queue. It stops on errors instead of continuing
with another shot.

Reuse selectors and waits from regression tests. Prefer read-only actions or
isolated test data. Synchronize with `waitFor*`, URLs, application state and
`DT.waitForLoader()`, not fixed delays. Presentation holds belong after readiness
checks. Use `I.videoClick(locator, curveStrength)` for important on-camera
clicks; the production reference documents cursor defaults and editing holds.
The video helper renders only one cursor in the top-level page and relays mouse
events from iframes, including nested frames; never install a second visible
cursor in the editor. Keep code comments and shot titles in English.

## 5. Generate Audio and Validate

The default ElevenLabs model is `eleven_v3`, voice Luki Zajo
(`Zai7B4Aol2bJtneyq0L1`). Do not send `voice_settings`. Model/voice precedence is
explicit `{ modelId, voiceId }`, non-empty `ELEVENLABS_MODEL_ID` /
`ELEVENLABS_VOICE_ID`, repository default. The API key comes only from
`ELEVENLABS_API_KEY`; never store it in code or command arguments.

`I.generateAudio(videoPlan)` joins only the selected `text-<language>` fields,
including manual shots, in array order and makes one request for the complete
narration. It does not generate separate MP3s per shot or force speech to match
the estimated durations. Run paid generation only when explicitly requested.

Run proportionate checks:

1. Parse changed JavaScript/JSON and run `npm run audio:test` and
   `npm run video:test` after infrastructure changes. Verify reordered plans,
   translation errors, manual-shot narration, callback validation and slates.
2. Dry-run the audio-only and complete video configurations; neither may call
   ElevenLabs. Check legacy scenarios remain compatible when changing helpers.
3. Run `npm run audio video/<scenario-name>.js` only on explicit request with
   an available API key. JSON plans produce `<scenario-name>-<language>.mp3`;
   legacy string narration retains `<scenario-name>.mp3`.
4. Run the tagged recording with `npm run video video/<scenario-name>.js`
   when the configured instance and test credentials are available. Reordering
   checks should include a shot that previously depended on its predecessor.
5. Final MP3/WebM files live in gitignored `docs/feature-video`. Confirm and
   inspect generated media before handing it off. The production reference
   describes atomic output replacement and retention of failed raw recordings.

For new productions, deliver the derived narration and shot plan, word count,
estimated/measured duration, voice/model, documentation URL, scenario path,
commands, output paths, validation and manual gaps. For workflow refactoring,
explain the schema, reorder/language controls, changed files and verification;
do not repeat the unchanged complete narration. State whether media was actually
generated and any unverified browser behavior.
