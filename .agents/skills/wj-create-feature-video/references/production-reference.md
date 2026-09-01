# WebJET Feature Video Production Reference

## Repository Recording Profile

Video scenarios live in `src/test/webapp/video`. The standard commands are:

```shell
cd src/test/webapp
npm run video -- video/<scenario-name>.js
npm run video:current
```

The first command records a selected video file. The second records the
scenario marked with `@current`. Both show the browser by default for authoring
or an external screen recorder; set `CODECEPT_SHOW=false` for a headless run.
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
`CODECEPT_VIDEO_CURVE_STRENGTH=0.5 npm run video -- video/<scenario>.js`. An
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

## Scenario Template

```javascript
Feature("video.<scenario-name>");

Before(({ login }) => {
    login("admin");
});

Scenario("<scenario-name>", ({ I, DT }) => {
    I.amOnPage("<admin-url>");
    I.waitForElement("<initial-state>", 20);
    DT.waitForLoader();

    // Shot 1: describe the visible customer benefit.
    I.videoClick("<stable-selector>");
    I.waitForElement("<result-state>", 20);
    DT.waitForLoader();
});
```

Use only the injected objects needed by the scenario. Selectors based on IDs,
roles, or stable `data-*` attributes are preferable to visual position or
translated text.

## Validation Commands

```shell
node --check helpers/video_helper.js
node --check video/<scenario-name>.js
node -e "JSON.parse(require('fs').readFileSync('package.json', 'utf8'))"
CODECEPT_VIDEO=true CODECEPT_VIDEO_ZOOM=1.411764705882353 CODECEPT_VIDEO_CURSOR=true npx codeceptjs dry-run -c codecept.video.conf.js --steps -p autoLogin --grep "<scenario-name>"
```

Run the actual `npm run video` command only when the target environment and
credentials are available. A generated video is finalized when its browser
context closes, so do not interrupt the process immediately after the scenario.

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
- [ElevenLabs Text to Speech](https://elevenlabs.io/docs/speech-synthesis/voice-settings)
- [ElevenLabs models](https://elevenlabs.io/docs/overview/models)
