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
Recordings are retained as WebM files in `build/test/videos`.

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

`CODECEPT_VIDEO_CURSOR=true` installs a Shadow DOM overlay in every document.
The overlay follows Playwright mouse events and shows a ring on mouse down. Each
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

A completed successful recording is saved atomically as
`build/test/videos/<scenario-name>.webm`. A failed run is saved separately as
`build/test/videos/<scenario-name>.failed.webm`, so it does not replace the last
successful recording. A later run with the same result replaces the respective
file. The video-specific Playwright helper also removes temporary raw UUID
recordings and the legacy UUID-prefixed artifact for the current scenario. Only
the active page at the end becomes the final recording; keep meaningful
multi-tab transitions as manual shots.

## ElevenLabs Audio Profile

Create an ElevenLabs API key under **Developers > API Keys**. Use a restricted
key with only the `text_to_speech` scope and set a credit limit. Copy the key
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
`build/test/videos/<scenario-name>.mp3`; a temporary file replaces the previous
MP3 only after the complete response is available. An API, network, timeout, or
disk error therefore leaves the last successful MP3 unchanged.

## Scenario Template

```javascript
Feature("video.<scenario-name>");

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(`
<copy-ready Slovak narration across multiple lines>
`);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    I.say(`
<timed shot plan across multiple lines, including MANUAL shots>
`);
});

Scenario("<scenario-name>", ({ I, DT, login }) => {
    login("admin");
    I.amOnPage("<admin-url>");
    I.waitForElement("<initial-state>", 20);
    DT.waitForLoader();

    // Shot 1: describe the visible customer benefit.
    I.videoClick("<stable-selector>");
    I.waitForElement("<result-state>", 20);
    DT.waitForLoader();
}).tag("@video");
```

Use only the injected objects needed by the scenario. Selectors based on IDs,
roles, or stable `data-*` attributes are preferable to visual position or
translated text.

To use `npm run video:current`, add `.tag("@current")` to the same main scenario
after `.tag("@video")`. Never tag `Shot plan`; `ElevenLabs` must have only the
`@audio` tag.

To select a different model or voice for one narration, keep the options outside
the spoken text:

```javascript
Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(`
<copy-ready Slovak narration across multiple lines>
`, {
        modelId: "eleven_multilingual_v2",
        voiceId: "<voice-id>",
    });
}).tag("@audio");
```

## Validation Commands

```shell
node --check helpers/audio_helper.js
node --check helpers/audio_runner.js
node --check helpers/video_helper.js
node --check video/<scenario-name>.js
node -e "JSON.parse(require('fs').readFileSync('package.json', 'utf8'))"
CODECEPT_AUDIO_FILE="$(pwd)/video/<scenario-name>.js" npx codeceptjs dry-run -c codecept.audio.conf.js --steps --grep '@audio'
CODECEPT_VIDEO=true CODECEPT_VIDEO_ZOOM=1.411764705882353 CODECEPT_VIDEO_CURSOR=true npx codeceptjs dry-run -c codecept.video.conf.js --steps -p autoLogin video/<scenario-name>.js
npm run audio:test
npm run video:test
npm run video video/<scenario-name>.js
```

`CODECEPT_AUDIO_FILE` is an internal runner/validation input, not a public
authoring interface. Without it the audio configuration discovers no tests.
The dry-run does not execute `I.generateAudio` and does not require an API key.
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
