# Recording presentation videos

The `src/test/webapp/video` folder contains repeatable browser control scenarios intended for creating product videos. They are separated from regression tests because the order of steps and visual composition are part of the resulting video.

## Naming a scenario

Use the format `<PR-ID>-<branch>.js` for both the file name and the name `Scenario`. Remove the leading prefix `feature/` or `hotfix/` from the branch name. For example, pull request 293 from branch `feature/config-jstree-view` would use the name:

```text
293-config-jstree-view.js
```

Use `Feature("video.<scenario-name>")` to make the source of the scenario easy to find in reports.

## Scenario creation

- If the feature can be demonstrated without changing the data, keep the scenario read-only.
- Use stable CSS selectors or `data` attributes that are already regression tested.
- Synchronize steps using `waitFor*`, application state, or `DT.waitForLoader()`. Do not use hard wait to synchronize the application.
- For important clicks, use `I.videoClick(locator)`. The rendered cursor moves along a variable natural path with a larger initial arc, a small correction before the target, and smooth acceleration and deceleration. Adds a short visual lead before the click. An optional second parameter determines the strength of the curvature:

  ```javascript
  I.videoClick(locator);      // Predvolená hodnota z prostredia, záložná hodnota je 1.
  I.videoClick(locator, 0);   // Priama dráha.
  I.videoClick(locator, 0.5); // Mierne zakrivenie.
  I.videoClick(locator, 1.5); // Výraznejšie zakrivenie.
  ```

  The force must be a finite non-negative number. Values ​​from `0` to `2` are recommended for natural movement. Larger values ​​may be excessive and will automatically be limited as the path approaches the edge of the browser area. Calls without a second parameter will use `CODECEPT_VIDEO_CURVE_STRENGTH`. The `video` and `video:current` scripts in the `package.json` file use the default value `0.3` unless the variable is unset or empty. A value specified before `npm run` overrides it for a single upload; an explicitly specified parameter always takes precedence over the value from the environment.

- Keep manual shots in the accompanying shot plan instead of simulating unreliable browser action.

Keep the spoken text and shot plan in two separate metadata scenarios before the main scenario. `ElevenLabs` must contain a single call `I.generateAudio` and a tag `@audio`. `Shot plan` continues to use `I.say` and has no tag. Neither of these must log in the user, open a browser, or perform application steps. Do not use a global login `Before` ; insert the `login` object until the main scenario marked `@video`.

```javascript
Feature("video.293-config-jstree-view");

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(`
<text hovoreného slova>
`);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    I.say(`
<časový plán záberov>
`);
});

Scenario("293-config-jstree-view", ({ I, login }) => {
    login("admin");
    // Kroky nahrávania videa.
}).tag("@video");
```

## Recording

In the `src/test/webapp` folder, run:

```shell
npm run video -- video/293-config-jstree-view.js
npm run video:current
```

By default, both commands will create a high-quality WebM file with a resolution of `1920 × 1080`, named after the scenario, in a folder `docs/feature-video` in the root folder of the repository, for example `293-config-jstree-view.webm`. The video recording helper increases Chrome's frame quality to 100 and replaces the default Playwright target bitrate of 1 Mbps with 50 Mbps, using a CRF of 0 and a maximum quantizer of 4 to preserve UI detail. Repeated successful runs of the same scenario will overwrite the previous successful file. A failed recording will be saved separately with a `.failed.webm` extension, for example `293-config-jstree-view.failed.webm`, and will not overwrite the last successful recording.

The `docs/feature-video` folder is local and ignored by `.gitignore`, so the generated media is not added to Git. The final MP3 and WebM files and the working files are created in this folder and survive the `build/test` cleanup. Playwright first records to the `.video-raw` subfolder. When finished, its UUID file is atomically renamed to a stable name and the empty working folder is removed. In case of an error, the raw recording is kept for diagnostics.

The standard commands use the default resolution `package.json` @@ and zoom the page content to a ratio of `24/17`, i.e. approximately `141,18 %`. The video assistant writes this value as the default zoom to a temporary Chromium profile before starting the browser. This is the same mechanism that the Chrome menu zoom uses. Therefore, the application already works with a logical viewport `1360 × 765` during initialization, while the result is rendered directly into the Full HD video. Texts and controls remain well readable without additional image magnification in the video editor.

Both commands will show the browser by default. This is useful when debugging a script or using an external screen recording tool. You can hide it for a single run with `CODECEPT_SHOW=false`.

The actual bitrate depends on the content of the image. The set profile uses more CPU and creates significantly larger files, so run video scenarios serially and check the smoothness of the motion on the recording computer.

Only the page that is active at the end of the script will be saved in the resulting video. Prepare important transitions between multiple tabs or actions outside the browser as manual shots.

When using an external recorder, disable capturing the system cursor. The video script renders both the cursor itself and the click effect, so capturing both cursors would create an annoying duplication.

WebM contains the page area in the browser without the spoken word. Combine the generated MP3 file with the spoken text with the recording in a video editor.

WebM is the native container of the VP8 encoder bundled with Playwright. Renaming the file to `.mp4` or `.mov` will not convert it. If your video editor requires a different format, convert to high-quality WebM using a full installation of FFmpeg. Conversion may improve compatibility with the editor, but it cannot add details that were not captured in the source footage.

## Recording settings

The default values ​​`CODECEPT_VIDEO_WIDTH`, `CODECEPT_VIDEO_HEIGHT`, `CODECEPT_VIDEO_ZOOM`, `CODECEPT_VIDEO_CURVE_STRENGTH`, `CODECEPT_URL`, and `CODECEPT_SHOW` are used only when the corresponding variable is not set or empty. Values ​​specified before `npm run video` or `npm run video:current` take precedence.

The resolution can be changed using the variables `CODECEPT_VIDEO_WIDTH` and `CODECEPT_VIDEO_HEIGHT`. The default values ​​are listed directly in the scripts `video` and `video:current` in the file `package.json`, where you can adjust them for all uploads.

The zoom of the page content is set by `CODECEPT_VIDEO_ZOOM`. The default value `1.411764705882353` represents the exact ratio `24/17` and the logical area `1360 × 765`. The value `1` disables zoom. Percentage notation is also supported, for example `140%`. The value is set as the native default zoom in the temporary Chromium profile; the zoom is no longer set additionally via CSS. The resolution of the resulting video and the size of the synthetic cursor are not changed. If you want to record directly in `1360 × 768` resolution instead of Full HD video with zoom, set the width to `1360`, the height to `768`, and the zoom to `1`.

```shell
CODECEPT_VIDEO_WIDTH=1360 CODECEPT_VIDEO_HEIGHT=768 CODECEPT_VIDEO_ZOOM=1 npm run video -- video/293-config-jstree-view.js
```

You can change the target instance for a single upload using `CODECEPT_URL` ; the default is `http://iwcm.interway.sk`.

```shell
CODECEPT_VIDEO_CURVE_STRENGTH=0.5 CODECEPT_URL=http://custom.webjetcms.test CODECEPT_SHOW=false npm run video:current
```

The time before clicking can be set from 0 to 2000 milliseconds using `CODECEPT_VIDEO_CLICK_DELAY`. Each call to `I.videoClick` will leave 500 milliseconds after the click for easier editing. You can increase this value up to 2000 milliseconds using `CODECEPT_VIDEO_POST_CLICK_DELAY`.

The cursor movement changes between clicks, but the generator uses the scenario name as a basis, so repeated recordings remain reproducible. Setting `CODECEPT_VIDEO_CURSOR_SEED` to a different value will create a different but repeatable variation of the movement.

## Spoken word generation

The sound generation uses ElevenLabs' paid API, so only run it consciously and for a specific file. In the `src/test/webapp` folder, specify exactly one existing JavaScript file from the `video` folder:

```shell
npm run audio video/293-config-jstree-view.js
```

The command will only run the script marked `@audio` via a separate CodeceptJS configuration. It will not open a browser, log in the user, or run the video script or shot schedule. It will save the result in the format `mp3_44100_128` as `docs/feature-video/293-config-jstree-view.mp3` in the root folder of the repository.

### ElevenLabs API key

1. Log in to ElevenLabs and open **Developers > API Keys**.
2. Create a restricted key, grant it only the `text_to_speech` permission, and set a credit limit.
3. Copy the key immediately after it is created. ElevenLabs will only display its full value once.
4. Keep it as a secret outside the repository and set it in the environment variable `ELEVENLABS_API_KEY`.

Detailed instructions are in the [official ElevenLabs authorization documentation](https://elevenlabs.io/docs/help-center/technical/how-do-i-authorize-myself-using-an-api-key). The project does not automatically load the `.env` files. Therefore, do not put the API key in `.env` expecting it to be used automatically, in a JavaScript script, helper parameter, or command line argument.

```shell
export ELEVENLABS_API_KEY="<váš-api-kľúč>"
npm run audio video/293-config-jstree-view.js
```

### Model and voice

The default model is Eleven v3 with identifier `eleven_v3`. The default voice is `Luki Zajo` with identifier `Zai7B4Aol2bJtneyq0L1`. You can change the model and voice with environment variables for the entire run:

```shell
ELEVENLABS_MODEL_ID=eleven_multilingual_v2 npm run audio video/293-config-jstree-view.js
ELEVENLABS_VOICE_ID="<voice-id>" npm run audio video/293-config-jstree-view.js
```

Or set them for just one helper call:

```javascript
I.generateAudio(`
<text hovoreného slova>
`, {
    modelId: "eleven_multilingual_v2",
    voiceId: "<voice-id>",
});
```

An explicit parameter `modelId` or `voiceId` takes precedence over a non-empty environment variable, which takes precedence over the default value. The API key can only be specified via `ELEVENLABS_API_KEY`. The assistant does not send `voice_settings`, so ElevenLabs will use the saved or default voice settings. The available models are described in the [models documentation](https://elevenlabs.io/docs/overview/models) and the [Text to Speech API](https://elevenlabs.io/docs/api-reference/text-to-speech/convert) request format.

`Luki Zajo` is a voice from the community library. Its use via the API depends on the availability of the voice and the account plan and may not be available in the free plan. In this case, use a plan that allows API access to voices from the [Voice Library](https://elevenlabs.io/docs/eleven-creative/voices/voice-library), or set `ELEVENLABS_VOICE_ID` to a voice available for your account. Saving the voice to **My Voices** is optional and does not unlock API access in the free plan. For a list of voices suitable for Slovak, see the [Slovak Text to Speech](https://elevenlabs.io/text-to-speech/slovak) page.

Before calling the API, the helper verifies that it can create a temporary file in the target folder. It then reads the entire response, validates the audio format, and atomically replaces the resulting MP3 file with the complete temporary file. In the event of an API, network, timeout, or write error, the last successful file is retained. The request is not automatically repeated to prevent an unclear network error from causing a second credit charge.
