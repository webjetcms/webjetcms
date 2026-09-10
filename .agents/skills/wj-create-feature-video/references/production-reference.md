# WebJET Feature Video Production Reference

## Repository Recording Profile

Video scenarios live in `src/test/webapp/video`. The standard commands are:

```shell
cd src/test/webapp
npm run audio video/<scenario-name>.js
npm run video video/<scenario-name>.js
npm run video:current
```

The audio command generates narration only from the selected file's `@audio`
scenario. It requires exactly one existing `.js` file below `video`. The first
video command records only the main walkthrough from a selected video file;
the npm script filters for `@video`, which deliberately excludes its
`ElevenLabs` and `Shot plan` metadata scenarios. Keep the no-video-frames error
enabled because it signals a real recording failure when a tagged walkthrough
does not render. The second command records the scenario marked with `@current`.
Both show the browser by default for authoring or an external screen recorder;
set `CODECEPT_SHOW=false` for a headless run.
Final recordings are retained as WebM files in `docs/feature-video` at the
repository root. This local directory is gitignored, so generated media does
not enter version control, but final MP3 and WebM artifacts survive cleanup of
`build/test`. Playwright records below `docs/feature-video/.video-raw`; after a
successful run its UUID file is atomically renamed to the stable target and the
empty raw directory is removed. A failed finalization retains the raw recording
for diagnosis.

Disable native cursor capture in an external recorder. The scenario already
renders a cursor and click effect, and capturing the system cursor as well can
produce a distracting duplicate.

The standard npm scripts record a 1920 x 1080 frame by default and set the
unzoomed browser viewport to the same size. Caller-provided values override the
defaults, including `CODECEPT_VIDEO_WIDTH`, `CODECEPT_VIDEO_HEIGHT`,
`CODECEPT_VIDEO_ZOOM`, `CODECEPT_VIDEO_CURVE_STRENGTH`, `CODECEPT_URL`, and
`CODECEPT_SHOW`. For these settings, an unset or empty value uses the script
default. `CODECEPT_URL` defaults to `http://iwcm.interway.sk`.
The default `CODECEPT_VIDEO_ZOOM=1.411764705882353` is the exact ratio 24/17.
It applies an approximately 141.18% recording scale by creating the browser
context with a native default page zoom in Playwright's temporary Chromium
profile before the browser starts. This uses Chromium's HostZoomMap, the same
mechanism as the browser zoom menu. It is therefore active before the first
document loads, and applications calculate layout dimensions from the
resulting 1360 x 765 logical viewport instead of being restyled after
initialization. The recorded frame
remains 1920 x 1080 and the synthetic cursor compensates for the zoom to keep
its output size unchanged. Set the zoom to `1` to disable it; a positive number
or percentage is accepted. Use Chromium unless the feature specifically
demonstrates another browser.

The Chromium recording profile is optimized as a high-quality editing source:
screencast frames use JPEG quality 100 and the VP8 encoder uses a 50 Mb/s target
bitrate, CRF 0, and a maximum quantizer of 4. This replaces Playwright's low
1 Mb/s target bitrate, which produces visible artifacts in Full HD recordings.
The actual bitrate is content-dependent and can be lower on static screens.
This profile uses more CPU and produces much larger files, so run video
scenarios serially and inspect motion continuity on the recording machine.

`CODECEPT_VIDEO_CURSOR=true` renders one Shadow DOM cursor overlay in the
top-level page. Mouse listeners inside iframes relay coordinates and clicks to
their parent, accounting for frame offsets, borders and scaling. This also
supports nested and cross-origin frames without leaving a second cursor behind.
Ordinary `I.click` updates the same cursor; `I.videoClick` adds animated movement
and editing holds. The overlay shows a ring on mouse down. Each
`I.videoClick(locator)` uses a varied human-like trajectory: a larger early arc,
a much smaller late correction, and an almost straight approach to the target.
Its minimum-jerk timing smoothly accelerates and brakes, while the shape and
duration vary between clicks. The pseudo-random sequence is seeded by the
scenario name, so re-recording the same scenario remains repeatable. Set
`CODECEPT_VIDEO_CURSOR_SEED` to a different value when a new repeatable motion
variant is wanted. The optional second argument controls the curve strength:
`I.videoClick(locator, 0)` follows a straight line, `1` is the baseline, and
higher finite non-negative values produce a more pronounced curve where
viewport room allows it. Keep normal authoring values between `0` and `2`; very
large values can look exaggerated and eventually saturate at the safe viewport
boundary. When the environment variable is not set, the helper's internal
fallback is `CODECEPT_VIDEO_CURVE_STRENGTH=1`. The `video` and `video:current`
npm scripts provide a default of `0.3` only when the caller has not set the
variable, so calls without the second argument use `0.3` during standard npm
video runs. Change the script default in `package.json` to tune all standard
runs, or override one run with, for example,
`CODECEPT_VIDEO_CURVE_STRENGTH=0.5 npm run video video/<scenario>.js`. An
explicit second argument always takes precedence. The optional
`CODECEPT_VIDEO_CLICK_DELAY` controls the short lead-in before a click. Every
video click also leaves at least 500 milliseconds after the action for easier
editing; increase it up to 2000 milliseconds with
`CODECEPT_VIDEO_POST_CLICK_DELAY` when necessary. These are presentation delays,
not application synchronization mechanisms.

`I.videoTitle(shot)` accepts a resolved shot returned by `getRecordingShots`.
For automatic shots, it renders a black full-window slate with the derived shot
number/id (`Shot N/total: id`) and the first 200 Unicode characters of localized
narration in smaller white text. For manual shots, the heading is
`WARNING: manual steps | Shot N/total: id` and the smaller text contains the full
filming instructions from `notes`, without the 200-character truncation. Missing
or blank notes display a reminder to add filming instructions. Keep notes concise
enough to fit in one frame; they replace the narration excerpt on manual slates.
Head slates show `WARNING: head video | Shot N/total: id`, full localized narration
and notes. All three kinds hold for two seconds after painting, then remove themselves.
The top-level overlay does not change active iframe context, focus or selection
and works with cursor rendering disabled. Text is rendered literally, not HTML.
Legacy `I.videoTitle("Shot 1: description.")` calls remain supported. The shared
runner also calls `I.videoTitle("SETUP shot <index>/<total> <id> (<duration>s)")` before
preparation and includes the same progress in `I.say`. Index and total count
all shots in the full plan, including manual and head entries. Normal slates use
`Shot <index>/<total>: <id>` with exactly the same numbers. A plan with two
manual intro shots and twelve automatic shots starts with two manual warnings,
then `SETUP shot 3/14 ...` followed by `Shot 3/14: ...`.
Warnings use the same full-plan number and total. There is no separate automatic counter.
The SETUP slate marks footage to discard, up to and including the normal slate.
The runner inserts all editing slates; individual callbacks must not duplicate them.
After the normal slate disappears, the runner holds the prepared scene for two
seconds, executes the action, then holds the result for two seconds before
cleanup. These transition handles are extra footage, excluded from the edited
duration estimates; add further presentation holds only when the shot needs them.
Use `I.clickCss` for CSS selectors or ordinary `I.click` for off-camera
preparation, keeping animated `I.videoClick` calls for the shot itself.

`I.videoScroll()` waits for fonts, resets the current top-level page to the top
and scrolls its document smoothly to the bottom at 160 pixels per second. It
returns immediately after font readiness when no scrolling is needed. It does
not scroll a nested iframe or an editor's internal scroll container.
`I.videoDocumentation(url)` returns to the top-level context, opens the URL in
the same tab, waits for `article h1` (20-second timeout), then calls
`I.videoScroll()`. Verify the feature documentation URL and article structure.

Every manual shot stays in recording order as a warning, including manual shots
at the start or end and plans containing only manual shots. One-time `setup`
still runs, but manual entries skip shared `prepare`, inline `prepare`/`shot`
and shared `cleanup`. The warning does not pause automation for human input.
Replace it with separately recorded footage or a card during editing.

Slates are captured editing markers. Cut them and inter-shot preparation/cleanup
out of the final video. They are excluded from the edited timeline. Place
initial-state preparation and readiness waits before the normal shot slate.
Keep waits for action results immediately after those actions inside `shot`.
Shot durations are estimates for the edited voiceover, not application
synchronization or automatic audio/video alignment.

A completed successful recording is saved atomically as
`docs/feature-video/<scenario-name>.webm`. A failed run is saved separately as
`docs/feature-video/<scenario-name>.failed.webm`, so it does not replace the last
successful recording. A later run with the same result replaces the respective
file. The video-specific Playwright helper moves the raw UUID file from its
isolated `.video-raw` run directory, removes that empty directory, and cleans up
the legacy UUID-prefixed artifact for the current scenario. Only the active page
at the end becomes the final recording; each tab has its own video. Keep the
walkthrough in the original recording tab. If a preview must open a second tab,
wait for it, capture its URL, close it, return to the original tab and open the
URL there before scrolling. Scenario 308 demonstrates this flow and closes its
editor before navigating. Mark the intermediate tab work for removal in editing.
A legacy/demo site can also be recorded in the original tab, but needs its own
login on that origin; the default instance session does not authenticate it.

## ElevenLabs Audio Profile

Create an ElevenLabs API key under **Developers > API Keys**. Use a restricted
key with `text_to_speech` and set a credit limit. Add `user_read` for credit
summaries and `image_video_generation` for head generation. Copy the key
when it is created because ElevenLabs displays the complete value only once.
Treat it as a secret: never put it in the repository, a scenario, a helper
argument, or a command-line argument. Export it to the process environment as
`ELEVENLABS_API_KEY`. The repository does not automatically load `.env` files.

The default model is Eleven v3
(`eleven_v3`), and the default voice is Luki Zajo
(`Zai7B4Aol2bJtneyq0L1`). Override either value for a run with
`ELEVENLABS_MODEL_ID` or `ELEVENLABS_VOICE_ID`, or for one narration with the
optional `{ modelId, voiceId }` argument to `I.generateAudio`. Precedence is:
explicit helper argument, non-empty environment variable, repository default.
The API key is accepted only from the environment. The request uses
`mp3_44100_128` and does not send `voice_settings`, leaving ElevenLabs to apply
the voice's stored or default settings.

The Luki Zajo default is a community voice. Community voice API access can
depend on the account plan and may not be available on the free tier. If the
voice is unavailable through the API, use a plan that permits Voice Library API
access or set `ELEVENLABS_VOICE_ID` to a voice ID available to the account.
Saving the voice to **My Voices** is optional and does not unlock API access on
the free tier.

`npm run audio video/<scenario-name>.js` uses an audio-only CodeceptJS
configuration. It does not start a browser or run login hooks, and it selects
only `@audio`. Run it only when audio generation was explicitly requested,
because the API call can consume ElevenLabs credits. There is no automatic
retry, avoiding a second charge after an ambiguous network failure.

A successful response is written atomically as
`docs/feature-video/<scenario-name>-<language>.mp3` for object plans; legacy string
narration retains `<scenario-name>.mp3`. A temporary file replaces the previous
MP3 only after the complete response is available. An API, network, timeout, or
disk error therefore leaves the last successful MP3 unchanged.

## ElevenLabs Head Profile and Credit Reports

`npm run head video/<scenario-name>.js` runs only the synchronous
`Scenario("ElevenLabs Head", ({ I }) => { I.generateHead(videoPlan); }).tag("@head")`.
Place it between audio and Shot plan. The dedicated configuration contains only
HeadHelper, without a browser, login or plugins. Both runners statically inspect
metadata and skip inline callbacks. Dry-runs call neither generation nor credit APIs.

Defaults: `creatify-aurora`, explicit `720p`, shared Jack / Home Vlog Style reference
`video/assets/head/jack-home-vlog-style.png`, framed for 16:9. The API uses an image,
not avatar/scene names. `I.generateHead(videoPlan, options)` accepts `language`,
`imagePath`, `modelId`, `resolution` (`480p` or `720p`) and `audio: { modelId, voiceId }`.
Each shot's `head` object overrides these options except language; merge individual
audio fields. TTS uses the existing defaults and environment precedence. Relative
image paths resolve beside the scenario. Supported images: PNG/JPEG/WebP up to
25 MB; their framing determines aspect ratio. Other models must support the same
Lip Sync contract; Aurora is verified. Image & Video API requires an eligible
Pro-or-higher account.

Validate all plan metadata, selected translations, non-empty head narration,
options, images and output reservations before API calls. No head shots means no
API calls. Process shots sequentially: fresh TTS, then `POST /v1/flows/video` with
`model_id`, `resolution`, and `image`/`audio` references of
`{ type: "inline_base64", content_base64, mime_type }`. Leave Aurora guidance at
its defaults; do not send prompt, aspect ratio or duration settings. Poll
`GET /v1/flows/video/{id}` after 10, 20, 40 and then 60 seconds, with a 30-minute
video deadline. Download `content_url` over HTTPS without the ElevenLabs key.

Every run regenerates all head clips; never automatically retry paid POSTs.
Failures stop later shots and include the generation ID when available. Each
completed MP3/MP4 atomically replaces
`docs/feature-video/<scenario-name>-<shot-id>-<language>.mp3` or `.mp4`. Failed
video generation keeps the previous MP4 and newly generated speech. Scenario 308
uses head only for intro, preserving its narration and duration estimate; its
output is `308-pb-redesign-intro-sk.mp4`. Clip duration follows audio, not
`durationSeconds`. Assembly remains a manual editing step.

Both commands read `GET /v1/user/subscription` before and after generation.
Remaining credits in the current limit are
`max(0, character_limit - character_count)`. Audio uses the exact `character-cost`
TTS response header, falling back to an explicitly approximate account delta.
Never estimate costs from text length. Head reports the whole run's account
delta including TTS and video; concurrent activity and delayed charges can affect
it. Report after partial failure too and label pending/uncertain jobs provisional.
Missing scope/data or failed reads produce `unavailable` with a reason. A changed
billing period or decreased counter invalidates the delta; valid exact TTS costs
and current remaining credits remain usable. Reporting must never invalidate
media or hide a generation error. Subscription reads require `user_read`.

Run `npm run head:test` alongside audio/video tests. Mock coverage includes full
preflight, localization, overrides, inline media, polling/timeouts, failures,
atomic outputs, downloads without key headers and whole-run credit summaries.
For an explicitly authorized integration test, run head on 308 and inspect
16:9/720p framing, sound, lip synchronization and the credit summary.

- [Video API](https://elevenlabs.io/docs/api-reference/flows/video/create)
- [Subscription API](https://elevenlabs.io/docs/api-reference/user/subscription/get/)
- [TTS credit cost](https://elevenlabs.io/docs/api-reference/introduction)

## JavaScript Plan and Scenario Template

Declare the plan as a top-level JavaScript object literal with static metadata
and inline `shot`/`prepare` functions. Comments, unquoted keys and trailing commas
are allowed. The audio preflight reads literal values from the syntax tree and
skips these function bodies without evaluating source code. Do not use getters,
computed keys, spreads, shorthand references or dynamic metadata expressions.
Functions are allowed only as each shot's `shot` and `prepare` properties.
Keep the `shots` array non-empty and ids unique, lowercase and hyphenated,
for example `text-editing`; do not use position-dependent `auto1` names. These
literal-data restrictions apply to metadata, not to the browser steps inside
callback bodies. Static strings can use quotes or template literals without
interpolation.

The plan is not strict JSON. `JSON.stringify(videoPlan)` omits the callbacks;
that export retains metadata but cannot reproduce browser steps on its own.

This template demonstrates editing in a DataTable editor. Adapt its selectors,
shared preparation and cleanup to the feature; features without an editor do
not need DTE callbacks. The introductory head is optional and assumes it was
requested; use `manual` and omit the head scenario for a separately made card.
Keep the same plan/runner structure.

```javascript
Feature("video.<scenario-name>");

const videoPlan = {
    "language": "sk",
    "shots": [
        {
            "id": "intro",
            "type": "head",
            "durationSeconds": 8,
            "title": "Introduce the benefit",
            "text-sk": "<Slovak narration>",
            "notes": "Insert the generated Jack / Home Vlog Style clip."
        },
        {
            "id": "edit-content",
            "type": "auto",
            "durationSeconds": 15,
            "title": "Edit content directly",
            "text-sk": "<Slovak narration matching these actions>",
            prepare: async ({ I, selector }) => {
                await I.clickCss(selector);
                await I.waitForVisible("<shot-ready-state>", 20);
            },
            shot: async ({ I, selector, typeText }) => {
                await I.videoClick(selector);
                await I.pressKey("End");
                await typeText(" <demo text>");
                await I.waitForText("<demo text>", 20, selector);
                await I.wait(3); // Extra presentation hold after the result assertion.
            }
        },
        {
            "id": "documentation",
            "type": "auto",
            "durationSeconds": 10,
            "title": "Find out more in the documentation",
            "text-sk": "<Slovak documentation call to action>",
            shot: async ({ I }) => {
                await I.videoDocumentation("<verified-feature-documentation-url>");
            }
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan);
}).tag("@audio");

Scenario("ElevenLabs Head", ({ I }) => {
    I.generateHead(videoPlan);
}).tag("@head");

Scenario("Shot plan", ({ I }) => {
    const { formatShotPlan } = require("../helpers/feature_video_plan.js");
    I.say(formatShotPlan(videoPlan));
});

Scenario("<scenario-name>", async ({ I, DTE, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const selector = "<editable-content-selector>";
    const typeText = text => I.usePlaywrightTo("type the demonstration text", async ({ page }) => {
        await page.keyboard.type(text, { delay: 70 });
    });
    await recordVideoPlan(I, {
        plan: videoPlan,
        context: { selector, typeText },
        setup: async () => { login("admin"); },
        prepare: async shot => {
            if (shot.id === "documentation") return;
            await I.amOnPage("<editor-url>");
            DTE.waitForEditor();
            // Restore isolated content and enter the content iframe here if needed.
            await I.waitForVisible(selector, 20);
        },
        cleanup: async shot => {
            if (shot.id === "documentation") return;
            await I.switchTo();
            DTE.cancel();
            await I.waitForInvisible("div.DTED.show", 10);
        }
    });
}).tag("@video");
```

`resolveVideoPlan` validates unique ids, `auto`/`manual`/`head` types, positive integer
`durationSeconds`, titles and selected localized text. It derives `number`,
`total`, `startSeconds`, `endSeconds` and `narration` without mutating the editable plan.
`formatShotPlan` includes manual footage, production notes and narration.
`getRecordingShots(plan, language)` retains all shots in array order and validates
each automatic `shot` function and optional `prepare` function before recording.
Manual and head shots need no functions. `recordVideoPlan(I, options)` uses that validation
before its one-time `setup`. For automatic shots it sequences logging, the SETUP
slate, shared `prepare`, optional inline `shot.prepare(context)`, the normal slate,
a two-second hold, `shot.shot(context)`, another two-second hold, and `cleanup`.
For manual/head shots it only logs the warning and shows `I.videoTitle(shot)`
with the filming instructions or full head narration and notes. Neither gets
per-shot lifecycle callbacks or transition holds.

Pass `I` separately, then one options object with `plan`. Optional `context`
contains selectors, injected page objects and reusable functions defined in the
main scenario. Top-level inline callbacks cannot close over those local variables;
destructure the dependencies they use from the context argument. Each inline
callback receives `{ ...context, I, shot }`; `I` and `shot` are supplied by the
runner and cannot be overridden by context values.
Shared `prepare` and `cleanup` callbacks run only for automatic shots and receive
the resolved shot directly; `setup` receives no arguments. The `language` option overrides narration for
recording slates. No separate action or preparation maps are needed.

Keep application-specific actions (such as DTE cancellation and discard
confirmation) in lifecycle callbacks instead of coupling the generic runner to
Page Builder. Call this plain async function directly from the main Scenario,
not as a CodeceptJS actor helper step. It stops on failure. These utilities live
in `helpers/feature_video_plan.js`.

Move whole objects in `shots` to reorder the film. Derived numbering and time
ranges update automatically; inline callbacks move with the metadata. Never sort
by old timestamps or maintain another ordered callback list. Each callback needs an
independent baseline. The `308-pb-redesign.js` example reopens the editor and
installs isolated browser-only content per shot, with extra preparation for the
structure drawer and section library. Its shared lifecycle branches by stable
shot id for the legacy editor, preview and documentation. The legacy editor
logs in on the demo origin, the preview reopens its URL in the recording tab,
and the automatic outro uses `I.videoDocumentation`. Shared Page Builder
preparation skips the legacy editor and documentation; shared cleanup skips
the preview and documentation because neither leaves an editor open. Setup and cleanup are cut out during editing.
The migrated 260, 283, 289 and 293 scenarios provide examples for scoped upload
fixtures, tree selection, nested editors and configuration views. Each shot must
prepare its own prerequisites with ordinary clicks; never invoke another shot
as its setup. When migrating, compare the original narration with joined plan
text after normalizing whitespace and preserve deliberate silence and manual gaps.
For Page Builder footage, also reuse its iframe-aware `waitForPageBuilder`
helper: the installed CodeceptJS FrameLocator has no `waitForFunction`, so it
resolves the Playwright content frame first. The `typeText` helper uses
`page.keyboard.type` for Slovak characters; `I.type` interprets them as key names
in this stack. These are feature-specific helpers passed through `context`.

The template's cleanup follows the current example: switch to the top-level
page, cancel DTE, then wait for the editor to close. Add native popup handling
only if the actual flow requires confirmation; do not call `I.acceptPopup()`
unconditionally when no popup is displayed.

`durationSeconds` does not set speech speed, insert silence or make a callback
last that long. Update estimates after measuring narration. The generator joins
all selected localized texts with paragraph breaks and makes one API request.
It never runs `shot` or `prepare`. Manual and head shots are included and explicit
empty strings mean intentional silence in combined audio. Head generation requires
non-empty head narration.
Missing language fields fail before the paid request; there is no silent fallback.

Add `text-cs` for Czech (language code `cs`, not country code `cz`) and `text-en`
for English. Change `videoPlan.language` to select the default for all derived
outputs. For an audio-only override, use:

```javascript
Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan, {
        language: "en",
        modelId: "eleven_v3",
        voiceId: "<voice-id>"
    });
}).tag("@audio");
```

Object-plan audio artifacts have a language suffix so different languages coexist.
Browser UI language, fixture text and language-dependent locators must be adapted
separately when producing foreign-language footage. Current npm video commands
and the Page Builder example target the Slovak UI: the npm scripts explicitly
set `CODECEPT_LNG=sk`, so prefixing them with another `CODECEPT_LNG` value does
not override it. Adapt the recording command/configuration as well as the
fixture and locators for another UI language.

Use only needed injected objects. Prefer selectors based on IDs, roles or stable
`data-*` attributes. Add `.tag("@current")` only to the main recording scenario
for `npm run video:current`. Never tag `Shot plan`; `ElevenLabs` has only `@audio`.

## Validation Commands

Run these from `src/test/webapp`; choose checks proportionate to the change.
For skill-only edits, validate the skill metadata and parse/preflight its complete
scenario template with `validateAudioScenarioSource` from `helpers/audio_runner.js`.
No new media or paid audio request is needed for an instruction-only review.

```shell
node --check helpers/feature_video_paths.js
node --check helpers/feature_video_plan.js
node --check helpers/audio_helper.js
node --check helpers/audio_runner.js
node --check helpers/video_helper.js
node --check helpers/video_playwright_helper.js
node --check video/<scenario-name>.js
node -e "JSON.parse(require('fs').readFileSync('package.json', 'utf8'))"
CODECEPT_AUDIO_FILE="$(pwd)/video/<scenario-name>.js" npx codeceptjs dry-run -c codecept.audio.conf.js --steps --grep '@audio'
CODECEPT_VIDEO=true CODECEPT_VIDEO_ZOOM=1.411764705882353 CODECEPT_VIDEO_CURSOR=true npx codeceptjs dry-run -c codecept.video.conf.js --steps -p autoLogin video/<scenario-name>.js
npm run audio:test
npm run head:test
npm run video:test
npm run video video/<scenario-name>.js
```

`CODECEPT_AUDIO_FILE` is an internal runner/validation input, not a public
authoring interface. Without it the audio configuration discovers no tests.
The dry-run does not execute `I.generateAudio` and does not require an API key.
For plans with head shots also run
`CODECEPT_HEAD_FILE="$PWD/video/<scenario-name>.js" npx codeceptjs dry-run -c codecept.head.conf.js --steps --grep '@head'`.
It executes neither head generation nor subscription reads.
Run the actual `npm run video` command only when the target environment and
credentials are available. A generated video is finalized when its browser
context closes, so do not interrupt the process immediately after the scenario.

The following command makes a paid API request. Run it only when audio
generation was explicitly requested and `ELEVENLABS_API_KEY` is available:

```shell
npm run audio video/<scenario-name>.js
```

## Technology Upgrade Path

Always inspect the installed `codeceptjs` and `playwright` versions before
changing recording infrastructure. The repository originally introduced this
workflow on CodeceptJS 3.6.10 and Playwright 1.49.1, which support WebM recording
but not Playwright's newer screencast cursor.

The high-quality profile currently applies a runtime adapter inside the
video-only process to Playwright's private Chromium encoder and temporary
profile preparation. The encoder adapter is needed because the public
`recordVideo` API does not expose image quality, bitrate, codec, or format; the
profile adapter sets Chromium's native default page zoom before launch.
Revalidate both parts whenever Playwright or Chromium is upgraded. Current
upstream Playwright also uses a fixed 1 Mb/s VP8/WebM target bitrate, so an
upgrade alone does not resolve recording quality.

After upgrading to a compatible current stack, evaluate the native CodeceptJS
`screencast` plugin and Playwright action annotations. Playwright 1.59 introduced
`page.screencast`; Playwright 1.61 added a synthetic pointer to action overlays.
Do not enable helper video and the screencast plugin together, because that
creates two recordings.

Keep WebM as the captured source unless a full external FFmpeg installation is
part of the production environment. The bundled FFmpeg shipped with the pinned
Playwright version exposes VP8 as its only video encoder. Post-converting to MP4
or MOV changes compatibility, not captured detail, and should always start from
the high-quality WebM rather than an older low-bitrate recording.

Official references:

- [Playwright videos](https://playwright.dev/docs/videos)
- [Playwright screencast API](https://playwright.dev/docs/api/class-screencast)
- [Playwright video encoder source](https://github.com/microsoft/playwright/blob/main/packages/playwright-core/src/server/videoRecorder.ts)
- [Playwright video quality controls request](https://github.com/microsoft/playwright/issues/17217)
- [Playwright BrowserContext addInitScript](https://playwright.dev/docs/api/class-browsercontext#browser-context-add-init-script)
- [CodeceptJS Playwright integration](https://codecept.io/playwright/)
- [CodeceptJS screencast plugin](https://codecept.io/plugins/screencast)
- [ElevenLabs API key authorization](https://elevenlabs.io/docs/help-center/technical/how-do-i-authorize-myself-using-an-api-key)
- [ElevenLabs Text to Speech API](https://elevenlabs.io/docs/api-reference/text-to-speech/convert)
- [ElevenLabs models](https://elevenlabs.io/docs/overview/models)
- [ElevenLabs Slovak voices](https://elevenlabs.io/text-to-speech/slovak)
- [ElevenLabs Voice Library](https://elevenlabs.io/docs/eleven-creative/voices/voice-library)
