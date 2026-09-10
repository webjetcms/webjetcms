---
name: wj-create-feature-video
description: "Create WebJET CMS feature-video assets from a pull request or branch: one JavaScript shot plan with localized narration and inline CodeceptJS/Playwright shot functions, ElevenLabs audio generation, and browser recordings with editing slates and a visible cursor. Use when preparing a YouTube demo, release video, PR walkthrough, or automated product-video scenario."
---

# Create a WebJET CMS Feature Video

Prepare a customer-focused product story and repeatable browser footage. Keep
shot metadata, localized narration, preparation and browser steps in one
JavaScript object; derive the narration, shot plan and walkthrough from it.

Read [references/production-reference.md](references/production-reference.md)
before implementing or changing a video scenario. It contains the exact schema,
scenario template, recording defaults and validation commands. Use
[308-pb-redesign.js](../../../src/test/webapp/video/308-pb-redesign.js) as the
working example of the current architecture; adapt its application-specific
selectors, fixture and lifecycle callbacks to the feature being demonstrated.

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
across all shots, including manual and head entries. Use measured voice duration
when it is available; `durationSeconds` remains an editing estimate until adjusted.

## 3. Keep One JavaScript Shot Plan

- Declare a top-level `const videoPlan = { ... };` before the scenarios. Its
  initializer is a JavaScript object literal, with `shot` and optional `prepare`
  functions directly inside each automatic shot. Keep metadata literal and
  static; comments, unquoted keys and trailing commas are allowed. In metadata,
  do not use computed keys, spreads, getters, shorthand references or dynamic
  expressions. Callback bodies contain ordinary JavaScript browser steps.
  The audio preflight reads metadata from the syntax tree and skips callback
  bodies without executing them. Function values belong only in `shot`/`prepare`.
  This is not strict JSON: serializing it with `JSON.stringify` omits callbacks.
- Use `language: "sk"` and a non-empty `shots` array. Each shot has a unique,
  stable lowercase hyphenated `id` (e.g. `text-editing`), `type` (`"auto"`,
  `"manual"` or `"head"`), positive integer `durationSeconds`, English `title`, localized
  `text-sk`, and optional production `notes`.
- `auto` means browser steps are automated; `manual` means footage/cards are
  supplied during editing. A manual shot produces a warning slate in its plan
  position instead of browser steps. Give each manual shot concise filming
  instructions in `notes`; those instructions appear in full on the slate.
  Old `AUTO 1`, `AUTO 2` labels were shot numbers, not
  different execution types. Never encode position in the type or use an array
  index as the action id.
- Store one narration beat and matching browser action/state per shot. If an
  old shot covers different narration beats, split it into separate stable ids.
- The array order is authoritative for narration, shot-plan numbering, derived
  time ranges and automatic execution. Move the entire shot object to reorder
  it, including its inline callbacks. Do not maintain a separate narration block,
  hard-coded timeline, ordered callback list or title strings.
- Use `text-cs` for Czech and `text-en` for English. Select the default with
  `videoPlan.language`; `I.generateAudio(videoPlan, { language: "en" })` can
  override audio language for one run. Missing translations fail explicitly;
  use `""` only for a deliberately silent shot. Translating narration alone does
  not translate UI selectors, fixture content or the browser login language.
- Put browser-external actions, unreliable third-party pages and final
  title/outro cards in manual shots. Documentation pages can be automatic shots
  using `I.videoDocumentation(url)`. Use `head` only for a requested generated
  presenter; do not silently convert existing manual cards to paid head clips.
  Keep every manual shot in the plan so its audio and duration remain part of
  the story. Avoid sensitive data in footage. For configuration screens, follow
  293: mask password/secret/key cells in the browser after table draws, without
  altering saved values, and inspect the recording.
- Time ranges are derived cumulatively from `durationSeconds`. They describe
  the edited film, excluding setup, cleanup, two-second editing slates and the
  runner's two-second holds before and after each automatic action.
  Never use these durations as application waits. Use ASCII hyphens in ranges.

## 4. Implement the Scenarios

Keep these scenarios in order:

1. `ElevenLabs`: inject only `I`, use a synchronous callback containing one
   `I.generateAudio(videoPlan)` call and tag only `@audio`.
2. For plans with head shots, `ElevenLabs Head`: inject only `I`, call
   `I.generateHead(videoPlan)` once in a synchronous callback and tag only `@head`.
3. `Shot plan`: format the same object with `formatShotPlan(videoPlan)` from
   `helpers/feature_video_plan.js` and print it with `I.say`; leave it untagged.
4. The named main walkthrough: tag `@video`. Add `@current` here only if needed.

Keep metadata callbacks free of browser actions. Do not add global login hooks.
Require shared utilities inside the relevant callback; the audio runner permits
only static declarations and direct Feature/Scenario calls at file scope.
Existing plain-text and JSON audio plans remain supported, but use the unified
JavaScript plan for new productions.

Define `shot: async ({ I, ...dependencies }) => { ... }` next to each automatic
shot's title, narration and notes. Add an inline `prepare` with the same context
argument when that shot needs extra preparation. Manual and head shots need no callbacks.
Keep shared selectors and helper functions in the main async scenario and pass
them through `context`; the runner adds `I` and the current resolved `shot`.
Top-level callbacks cannot access variables declared inside the main scenario;
destructure each required dependency from their context argument.
For example, `shot: async ({ I, services }) => { await I.videoClick(services); }`
uses `context: { services }` from the main scenario.

Call `await recordVideoPlan(I, { plan: videoPlan, context, ... })` from
`helpers/feature_video_plan.js`. Do not copy the recording loop into scenarios
or maintain separate callback maps. Pass `I` first and one options object with
`plan`, optional `context` and `language`, and the lifecycle callbacks needed:

- `setup`: one-time login and shared setup, after automatic callbacks are validated.
- `prepare`: a shared baseline before every automatic shot; receives the resolved shot.
- `cleanup`: cleanup after each successful automatic shot; receives the resolved shot.

For automatic shots, the runner logs
`Recording shot <index>/<total> <id> (<duration>s)` and displays
a two-second `SETUP shot <index>/<total> <id> (<duration>s)` slate. The index and
total use the full plan, including manual and head shots. It then
awaits shared `prepare`, runs `shot.prepare(context)` if present, displays the
normal two-second shot slate, waits two seconds on the prepared scene, runs
`shot.shot(context)`, waits another two seconds on the result and awaits cleanup.
These automatic lead-in/tail holds provide room for transitions during editing;
do not duplicate them in every callback.
The normal slate includes
`Shot <index>/<total>: <id>` and first 200 Unicode characters of localized narration.
SETUP, normal slates and warnings use the same full-plan numbering and total.
The runner owns the editing slates: do not repeat
`I.videoTitle` inside individual `shot` or `prepare` functions.
Cut everything from the SETUP slate through the normal slate out of the final
film, along with cleanup. Neither slate contributes to the edited timeline.

For manual shots, the runner logs a warning and calls `I.videoTitle(shot)` at
their position in the full plan. This shows `WARNING: manual steps | Shot N/total: id`
and the complete `notes` for two seconds. Missing notes produce a reminder to
add filming instructions. The shared `prepare`/`cleanup` and inline callbacks
are skipped for manual shots; one-time `setup` still runs. Replace this warning
slate with the required manual footage during editing. Manual narration and
estimated duration remain in the edited plan and audio; the marker does not
wait for a person to record the scene or contribute to the edited duration.

Shared lifecycle callbacks may branch on the stable `shot.id`: skip editor
preparation and cancellation for a documentation shot or a shot that opens its
own route. Keep those decisions independent of array position. A preview can
close its editor itself, so its shared cleanup must not close it again.

A reordered shot must not depend on a prior shot's dialog, selection, search or
mutation. Reopen/reset the editor with isolated browser-only content when that
is the simplest reliable baseline. Use `I.clickCss` for CSS selectors or ordinary
`I.click` during preparation to avoid cursor animation and editing holds in
footage that will be cut. Discard temporary changes during cleanup. When a shot
uses an iframe, return to the top-level page before closing its editor. Handle
native confirmation dialogs only if the actual flow displays one; do not add
unconditional popup acceptance steps to the current Page Builder cleanup.
For uploads or other persistent fixtures, use narrowly named demo records, seed
what each shot needs and remove only those records afterward. Never run another
shot's on-camera actions as preparation.

Await actor steps inside async callbacks so recording order stays deterministic.
`recordVideoPlan` is an ordinary async module function invoked from a Scenario,
not an `I` helper step; wrapping actor callbacks inside a CodeceptJS helper step
can interfere with its recorder queue. It stops on errors instead of continuing
with another shot.

Reuse selectors and waits from regression tests. Prefer read-only actions or
isolated test data. Synchronize with `waitFor*`, URLs, application state and
`DT.waitForLoader()`, not fixed delays. Presentation holds belong after readiness
checks. Keep initial readiness waits in preparation and result assertions after
the corresponding actions inside `shot`. Use `I.videoClick(locator, curveStrength)`
for important on-camera clicks; the production reference documents cursor defaults
and editing holds. Use `I.videoScroll()` to smoothly scroll the current page
from top to bottom, or `I.videoDocumentation(url)` to open documentation, wait
for `article h1` and scroll in the recording tab. Keep content in the original
recording tab; for previews opened in a new tab, capture the URL, close that tab
and reopen the URL in the original tab, as in 308.
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
including manual and head shots, in array order and makes one request for the complete
narration. It never invokes `shot` or `prepare`. It does not generate separate
MP3s per shot or force speech to match the estimated durations. Run paid
generation only when explicitly requested.

Run proportionate checks:

1. Parse changed JavaScript and run `npm run audio:test`, `npm run head:test` and
   `npm run video:test` after infrastructure changes. Verify reordered plans,
   translation errors, manual-shot narration and warning slates, callback
   validation, and skipped automatic lifecycle callbacks for manual shots.
   Verify the two-second lead-in/tail holds for automatic shots and their absence
   for manual/head entries. Verify that audio validation and generation do not
   execute inline callbacks.
2. Dry-run the audio-only, head-only and complete video configurations; none may call
   ElevenLabs. Check legacy scenarios remain compatible when changing helpers.
3. Run `npm run audio video/<scenario-name>.js` only on explicit request with
   an available API key. Object plans produce `<scenario-name>-<language>.mp3`;
   legacy string narration retains `<scenario-name>.mp3`.
4. Run the tagged recording with `npm run video video/<scenario-name>.js`
   when the configured instance and test credentials are available. Reordering
   checks should include a shot that previously depended on its predecessor.
5. Final MP3/MP4/WebM files live in gitignored `docs/feature-video`. Confirm and
   inspect generated media before handing it off. The production reference
   describes atomic output replacement and retention of failed raw recordings.

When migrating an existing scenario, preserve its narration and intended edited
duration while distributing text across shots. Check the joined localized text
against the original after normalizing whitespace, and preflight every migrated
file. Preserve manual gaps in `notes`; use `""` only for intentional silence.
Run the migrated browser actions with a changed shot order to verify that their
preparation is independent.

For new productions, deliver the derived narration and shot plan, word count,
estimated/measured duration, voice/model, documentation URL, scenario path,
commands, output paths, validation and manual gaps. For workflow refactoring,
explain the schema, reorder/language controls, changed files and verification;
do not repeat the unchanged complete narration. State whether media was actually
generated and any unverified browser behavior.

## Talking Heads and Credits

Use `type: "head"` for an explicitly requested generated presenter. It keeps
its narration, numbering and estimated duration in the shared plan and combined
audio. In the browser recording it shows a two-second `WARNING: head video`
slate with `Shot N/total: id`, full localized narration and notes. All per-shot
preparation, actions and cleanup are skipped; replace the marker with the clip during editing.

`npm run head video/<scenario-name>.js` runs only `@head` without a browser or
login. It validates every head text, option, image and output before paid calls,
then generates separate TTS and a lip-sync video for each head shot in order.
Default reference: Jack / Home Vlog Style, framed for 16:9 in
`video/assets/head/jack-home-vlog-style.png`. Default model/resolution:
`creatify-aurora` / `720p`; leave other Aurora settings at their defaults.
The `imagePath` option selects another reference (relative to the scenario file).
`I.generateHead` also accepts `language`, `modelId`, `resolution` and
`audio: { modelId, voiceId }`. Each shot's `head` object overrides the same
settings except language, with individual audio fields merged. Head text must
not be empty. Audio uses the existing TTS defaults and environment precedence.

Run paid head generation only when explicitly requested. Each invocation
regenerates all head clips; there is no cache or automatic paid retry. Files are
`docs/feature-video/<scenario-name>-<shot-id>-<language>.mp3` and `.mp4`, replaced
atomically on completion. Video duration follows audio, not `durationSeconds`.
Failures stop later shots and include the generation ID when available. Inspect
resolution, framing, sound and lip synchronization before handing off a real run.

Keep the key only in `ELEVENLABS_API_KEY`. Head generation requires
`text_to_speech` and `image_video_generation`; both audio and head credit
summaries additionally need `user_read`. Audio reports `character-cost` from the
TTS response, falling back to an explicitly approximate account delta. Head
reports the whole run's account delta, including TTS and video. Both display
credits remaining in the current limit; reporting is non-fatal, runs even after
partial failure, and labels pending jobs provisional. Dry-runs call neither the
generation nor credit APIs. See the production reference for API and failure details.
