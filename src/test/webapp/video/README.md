# Video scenarios

This directory contains repeatable browser walkthroughs used to create product
videos. They are separate from regression tests because their order and visual
composition are part of the output.

## Naming

Use `<PR-ID>-<branch>.js` for both the file name and the `Scenario` name. Remove
the leading `feature/` or `hotfix/` prefix from the branch name. For example,
pull request 293 from `feature/config-jstree-view` uses:

```text
293-config-jstree-view.js
```

Use `Feature("video.<scenario-name>")` so the source is easy to find in reports.

## Authoring

- Keep the walkthrough read-only whenever the feature can be demonstrated
  without changing data.
- Use stable CSS or data-attribute selectors already covered by regression
  tests.
- Synchronize with `waitFor*`, application state, or `DT.waitForLoader()`.
  Fixed waits must not be used for application synchronization.
- Use `I.videoClick(locator)` for important clicks. It moves the rendered cursor
  to the target on a gentle S-curve with ease-in-out timing, then adds a short
  visual lead-in before clicking.
- Keep manual shots in the accompanying shot plan instead of simulating an
  unreliable browser action.

## Recording

From `src/test/webapp` run:

```shell
npm run video -- video/293-config-jstree-view.js
npm run video:current
```

Both commands create a high-quality 1920 x 1080 WebM file named after the
scenario in `build/test/videos`, for example `293-config-jstree-view.webm`.
The video-only helper raises Chromium frame quality to 100 and replaces
Playwright's 1 Mb/s VP8 target bitrate with a 50 Mb/s target, CRF 0, and a
maximum quantizer of 4 to preserve UI detail. Running the same scenario again
replaces the previous file. Both commands show the browser, which is useful
while adjusting the walkthrough or when an external screen recorder is
preferred.

The actual bitrate remains content-dependent. This profile uses more CPU and
produces much larger files, so record video scenarios serially and check motion
continuity on the recording machine.

Only the active page at the end of a scenario becomes the final recording.
Keep meaningful multi-tab or browser-external transitions as manual shots.

When using an external recorder, disable its native cursor capture. The video
scenario already renders its own cursor and click effect, so recording both
would produce two cursors.

The WebM contains the browser viewport and no narration. Generate the voiceover
in ElevenLabs and combine it with the recording in the video editor.

WebM is the native container of Playwright's bundled VP8 encoder. Renaming the
file to `.mp4` or `.mov` does not convert it. If an editor requires another
format, convert the high-quality WebM with a full FFmpeg installation. This can
improve editor compatibility but cannot add detail that was not captured in the
source recording.

The dimensions can be overridden with `CODECEPT_VIDEO_WIDTH` and
`CODECEPT_VIDEO_HEIGHT`. The cursor lead-in can be adjusted between 0 and 2000
milliseconds with `CODECEPT_VIDEO_CLICK_DELAY`. Each `I.videoClick` leaves 500
milliseconds after the click for editing; increase it up to 2000 milliseconds
with `CODECEPT_VIDEO_POST_CLICK_DELAY`.
