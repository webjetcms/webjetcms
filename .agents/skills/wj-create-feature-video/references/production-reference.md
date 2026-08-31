# WebJET Feature Video Production Reference

## Repository Recording Profile

Video scenarios live in `src/test/webapp/video`. The standard commands are:

```shell
cd src/test/webapp
npm run video -- video/<scenario-name>.js
npm run video:current
```

The first command records a selected video file. The second records the
scenario marked with `@current`. Both show the browser for authoring or an
external screen recorder. Recordings are retained as WebM files in
`build/test/videos`.

Disable native cursor capture in an external recorder. The scenario already
renders a cursor and click effect, and capturing the system cursor as well can
produce a distracting duplicate.

The default frame and viewport are 1920 x 1080. Override them with
`CODECEPT_VIDEO_WIDTH` and `CODECEPT_VIDEO_HEIGHT`. Use Chromium unless the
feature specifically demonstrates another browser.

`CODECEPT_VIDEO_CURSOR=true` installs a Shadow DOM overlay in every document.
The overlay follows Playwright mouse events and shows a ring on mouse down. Each
`I.videoClick(locator)` moves along a gentle cubic Bezier S-curve, alternates the
curve direction, and uses ease-in-out acceleration and braking. The duration
adapts to the travel distance. The optional
`CODECEPT_VIDEO_CLICK_DELAY` controls the short lead-in before a click. Every
video click also leaves at least 500 milliseconds after the action for easier
editing; increase it up to 2000 milliseconds with
`CODECEPT_VIDEO_POST_CLICK_DELAY` when necessary. These are presentation delays,
not application synchronization mechanisms.

The completed recording is saved atomically as
`build/test/videos/<scenario-name>.webm`. A later run of the same scenario
replaces that file. The video-specific Playwright helper also removes temporary
raw UUID recordings and the legacy UUID-prefixed artifact for the current
scenario. Only the active page at the end becomes the final recording; keep
meaningful multi-tab transitions as manual shots.

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
CODECEPT_VIDEO=true CODECEPT_VIDEO_CURSOR=true npx codeceptjs dry-run -c codecept.video.conf.js --steps -p autoLogin --grep "<scenario-name>"
```

Run the actual `npm run video` command only when the target environment and
credentials are available. A generated video is finalized when its browser
context closes, so do not interrupt the process immediately after the scenario.

## Technology Upgrade Path

Always inspect the installed `codeceptjs` and `playwright` versions before
changing recording infrastructure. The repository originally introduced this
workflow on CodeceptJS 3.6.10 and Playwright 1.49.1, which support WebM recording
but not Playwright's newer screencast cursor.

After upgrading to a compatible current stack, evaluate the native CodeceptJS
`screencast` plugin and Playwright action annotations. Playwright 1.59 introduced
`page.screencast`; Playwright 1.61 added a synthetic pointer to action overlays.
Do not enable helper video and the screencast plugin together, because that
creates two recordings.

Official references:

- [Playwright videos](https://playwright.dev/docs/videos)
- [Playwright screencast API](https://playwright.dev/docs/api/class-screencast)
- [Playwright BrowserContext addInitScript](https://playwright.dev/docs/api/class-browsercontext#browser-context-add-init-script)
- [CodeceptJS Playwright integration](https://codecept.io/playwright/)
- [CodeceptJS screencast plugin](https://codecept.io/plugins/screencast)
- [ElevenLabs Text to Speech](https://elevenlabs.io/docs/speech-synthesis/voice-settings)
- [ElevenLabs models](https://elevenlabs.io/docs/overview/models)
