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

Keep the spoken text and shot plan in separate metadata scenarios before the main scenario. For a plan with type `head`, also include the `ElevenLabs Head` scenario described below between them. `ElevenLabs` must contain a single call `I.generateAudio` and a tag `@audio`. `Shot plan` continues to use `I.say` and has no tag. Neither of these may log in the user, open a browser, or perform application steps. Do not use a global login `Before` ; insert the `login` object until the main scenario marked `@video`.

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

## Thumbnail for YouTube

You create the thumbnail with a separate script labeled only `@title`. Prepare a screen from the video in it and call `I.videoTitle(text, style)`. The helper captures the current browser view including the iframe, adds a caption, and saves the JPG. Script 308 shares the preparation of temporary content with the video and closes the editor without saving changes after creating the image.

```shell
cd src/test/webapp
npm run video:title video/308-pb-redesign.js
npm run video:title -- video/308-pb-redesign.js --text "Page Builder po novom" --style glow
npm run video:title -- video/308-pb-redesign.js --style clean
npm run video:title -- video/308-pb-redesign.js --style bold
```

| Style | Appearance |
| --- | --- |
| `glow` (default) | Dark background, white title with a blue-violet glow, and a slightly rotated screenshot. |
| `clean` | Light background, blue title, and a flat screenshot with a soft shadow. |
| `bold` | Blue background, large white letters with a strong shadow, yellow accent and a filmed screenshot. |

All styles use the SVG logo directly from `src/main/webapp/admin/v9/src/images/logo-cms.svg`. In the light style `clean`, the white logo has a dark background. The composition is rendered at a higher resolution and is downscaled before export to smooth out the curved lines of the screenshot.

The output has a resolution of **1920 × 1080**, an aspect ratio of **16:9**, a format of **JPG**, and a size of under **2 MB**. It is saved in `docs/feature-video/<scenario-name>-title-<style>.jpg`, for example `308-pb-redesign-title-glow.jpg`. Each style has its own file; another successful generation will only replace the same style. In case of an error, the previous image will be preserved. You can upload the image as a custom thumbnail in YouTube Studio. The current requirements are in [YouTube Help](https://support.google.com/youtube/answer/72431).

```javascript
Scenario("YouTube thumbnail", async ({ I, login }) => {
    login("admin");
    await I.amOnPage("/admin/v9/webpages/web-pages-list/");
    await I.waitForVisible("#datatableInit", 20);
    await I.videoTitle("Page Builder\nNew experience", "glow");
}).tag("@title");
```

The text supports diacritics and `\n` for manual line breaks. The font size will adjust to the available space. A short title works best; the limit is 160 characters, and text that does not fit even after being reduced will result in an error. The values ​​`--text` and `--style` override the script's default values. Alternatively, use the variables `VIDEO_TITLE_TEXT` and `VIDEO_TITLE_STYLE` ; command parameters take precedence over variables.

The command will only run `@title`, without recording WebM and without generating audio or a speaking character. `VIDEO_SHOT` is ignored for thumbnails. Optional `--dry-run` will list the steps without opening a browser. The script must wait for the desired application state before calling the helper; you can change the underlying screenshot by changing its preparation steps. There is no need to record the entire video first.

Existing `I.videoTitle(shot)` and text-only calls in normal recording continue to display the cutaway title. In `video:title` mode, `I.videoTitle(text)` is sufficient and `glow` is used. Text with the specified style generates a thumbnail even outside of this mode.

## Overview of texts and shot plan

You can read the script with the common object `const videoPlan` in a clear format without starting the recording. In the folder `src/test/webapp` run:

```shell
npm run video:plan video/308-pb-redesign.js
```

The command prints a summary with the language, number of shots, estimated length, word count, and character count. This is followed by a `NARRATION` section with the entire spoken text in shot order, and a `SHOT PLAN` section with the timeline, shot types, IDs, titles, notes, and text for each shot. The times are based on `durationSeconds` and indicate an estimate of the edited video. The language is specified by `videoPlan.language`, the default is `sk`.

No API key or running WebJET CMS instance is required. The command only reads the object statically: it does not run CodeceptJS, the browser, audio generation, or the `prepare` and `shot` functions. The `Scenario("Shot plan")` declaration can remain in the file, but the command does not need or execute it. It also works for a work-in-progress plan without an audio script or ready-made images for `head`. Older handwritten text in `I.say` without the `const videoPlan` object is not read by this command.

You can save the summary to a text file. The `--silent` parameter suppresses the initial npm output:

```shell
npm run --silent video:plan video/308-pb-redesign.js > /tmp/308-pb-redesign-plan.txt
```

## Recording

In the `src/test/webapp` folder, run:

```shell
npm run video -- video/293-config-jstree-view.js
npm run video:current
```

By default, both commands will create a high-quality WebM file with a resolution of `1920 × 1080`, named after the scenario, in a folder `docs/feature-video` in the root folder of the repository, for example `293-config-jstree-view.webm`. The video recording helper increases Chrome's frame quality to 100 and replaces the default Playwright target bitrate of 1 Mbps with 50 Mbps, using a CRF of 0 and a maximum quantizer of 4 to preserve UI detail. Repeated successful runs of the same scenario will overwrite the previous successful file. A failed recording will be saved separately with a `.failed.webm` extension, for example `293-config-jstree-view.failed.webm`, and will not overwrite the last successful recording.

The `docs/feature-video` folder is local and ignored by `.gitignore`, so the generated media is not added to Git. The final MP3, MP4, and WebM files, as well as the working files, are created in this folder and survive the `build/test` cleanup. Playwright first records to the `.video-raw` subfolder. When finished, its UUID file is atomically renamed to a stable name and the empty working folder is removed. In case of an error, the raw recording is retained for diagnostics.

The standard commands use the default resolution `package.json` @@ and zoom the page content to a ratio of `24/17`, i.e. approximately `141,18 %`. The video assistant writes this value as the default zoom to a temporary Chromium profile before starting the browser. This is the same mechanism that the Chrome menu zoom uses. Therefore, the application already works with a logical viewport `1360 × 765` during initialization, while the result is rendered directly into the Full HD video. Texts and controls remain well readable without additional image magnification in the video editor.

Both commands will show the browser by default. This is useful when debugging a script or using an external screen recording tool. You can hide it for a single run with `CODECEPT_SHOW=false`.

The actual bitrate depends on the content of the image. The set profile uses more CPU and creates significantly larger files, so run video scenarios serially and check the smoothness of the motion on the recording computer.

Only the page that is active at the end of the script will be saved in the resulting video. Prepare important transitions between multiple tabs or actions outside the browser as manual shots.

When using an external recorder, disable capturing the system cursor. The video script renders both the cursor itself and the click effect, so capturing both cursors would create an annoying duplication.

WebM contains the page area in the browser without the spoken word. Combine the generated MP3 file with the spoken text with the recording in a video editor.

WebM is the native container of the VP8 encoder bundled with Playwright. Renaming the file to `.mp4` or `.mov` will not convert it. If your video editor requires a different format, convert to high-quality WebM using a full installation of FFmpeg. Conversion may improve compatibility with the editor, but it cannot add details that were not captured in the source footage.

### Re-recording a single image

For scenarios with a common `videoPlan` and a runner `recordVideoPlan`, you can only load one shot using the `VIDEO_SHOT` variable. Enter the exact `id` shot from the plan, for example `outro`:

```shell
VIDEO_SHOT=outro npm run video video/308-pb-redesign.js
```

The result is saved as `docs/feature-video/308-pb-redesign-outro.webm` ; a failed run as `308-pb-redesign-outro.failed.webm`. Rerunning will only replace the retake of the same frame and the same state. The entire video and the last successful retake in case of failure will be preserved. The variable also works with `npm run video:current`.

The common `setup` is executed once. Then only the setup, titles, action, and cleanup of the selected shot are executed. After the two-second SHOT title is removed, there are **3 seconds of clean image before the action** for editing transitions; there is a two-second buffer after the action. These buffers also apply when recording the entire plan and do not change the estimated shot lengths. The other shots are not triggered. Titles and transcripts retain the original shot number and total for the entire plan; the retake does not change its timeline. Selecting `manual` or `head` will only display the existing warning table after the common setup.

A standalone WebM starts directly with the `Shot N/total: id` subtitle, including its display. When saving, the login, SETUP, and preparation before this subtitle are removed. Trimming uses the actual frame positions of Chromium and FFmpeg bundled with Playwright with the same VP8 quality profile. The entire recording without `VIDEO_SHOT` remains, including SETUP. If a frame fails before the subtitle, its `.failed.webm` leaves the entire preparation for diagnosis. In case of a trim error, the raw video is preserved and the original output is not replaced.

An unset or empty value means the entire plan; surrounding spaces are trimmed. The ID must use lowercase letters, numbers, and possible hyphens, for example `text-editing`. An invalid format is rejected when loading the configuration. An unknown ID will terminate the run before the common setup and print the available ID. The validity of the entire plan and all automatic callbacks is also verified during retake.

`VIDEO_SHOT` only affects the browser upload. `audio`, `head` and `video:plan` will still process the entire plan.

### All images into separate files

```shell
npm run video:shots video/308-pb-redesign.js
```

The command loads `videoPlan` without executing its code and sequentially executes each shot via the existing command `video` with its own `VIDEO_SHOT`. The outputs are, for example, `308-pb-redesign-hierarchy.webm` and `308-pb-redesign-preview.webm` in `docs/feature-video`. Each starts with its SHOT title without preparation. The order, numbering, and recording settings are preserved. Any inherited value of `VIDEO_SHOT` is replaced for each shot with its ID.

Each shot has its own browser process and common setup, just like a manual retake. If a shot fails, the batch continues with the next one; at the end, it prints the failure ID and returns a non-zero return code. Interrupting the process stops the batch. The `manual` and `head` types create separate warning tables that need to be replaced during editing; the command does not generate paid audio or video with a talking character.

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

The command will only run the scenario marked `@audio` via a separate CodeceptJS configuration. It will not open a browser, log the user in, or run the video scenario or shot plan. The result in the format `mp3_44100_128` will be saved to `docs/feature-video` in the root folder of the repository. The files are named `293-config-jstree-view-sk-1.mp3`, `293-config-jstree-view-sk-2.mp3`, etc. depending on the selected language and number of parts. The sequence number is also present for a single file. Older scenarios with a text string instead of a plan will produce `293-config-jstree-view-1.mp3` without a language designation.

The assistant gradually joins the entire texts of the shots in the order `shots`. If adding another shot would exceed the model limit, it starts a new part. It also counts spaces and two line breaks between shots. For `eleven_v3`, the limit is 5,000 characters; one shot is never split into files. If a shot itself exceeds the limit, the pre-run check prints its ID and the number of characters. Such a shot should be split into smaller shots in the plan. The texts `manual` and `head` remain part of the narration, empty texts do not create empty files. The older text string is considered one indivisible part.

Before calling ElevenLabs, the console prints each part number, its frames, character count, file name, and the full text under the label `[ElevenLabs audio] Text to generate:`. The parts are generated sequentially; each request has a timeout of 10 minutes. `I.generateAudio` returns an array of file paths in the correct order and registers them as artifacts `audio-1`, `audio-2`, etc. At the end, it prints a single summary of the credits for the entire run.

### ElevenLabs API key

1. Log in to ElevenLabs and open **Developers > API Keys**.
2. Create a restricted key, enable `text_to_speech` and set a credit limit. For credit overview, also enable `user_read` (read user data/subscription); for talking videos, also enable `image_video_generation`.
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

Before calling the API, the helper validates the texts and creates temporary files for all parts. For each request, it reads the entire response, validates the audio format, and atomically replaces the corresponding MP3 file with the complete temporary file. An error stops further requests and lists the part number and file name. Already completed parts remain available; previous files of failed or unexecuted parts are not changed. The request is not automatically repeated so that an unclear network error does not cause a second credit charge. When editing, use the files written by the current run: old files without numbers or redundant parts from a previous longer plan are not automatically deleted.


## Talking videos (`head`)

The common `videoPlan` is a static JavaScript object. The order of its `shots` determines the numbering, timeline, and spoken word order. Each shot contains a unique `id`, a type `auto`, `manual`, or `head`, a name `title`, a positive integer estimate `durationSeconds`, and localized `text-sk`, or `text-cs`, and `text-en`. Automatic steps belong to inline functions `shot` and optionally `prepare`.

Type `head` creates a separate clip with a speaking character. In its place, a two-second board `WARNING: head video` appears in the main recording with a number, title, all localized text, and notes. The setup, action, and cleanup for this shot are skipped. The shot remains part of the timeline and the numbered MP3s from `npm run audio` ; the numbering and total count in SETUP and titles include all shot types. For example, the seventh shot out of sixteen has both `SETUP shot 7/16` and `Shot 7/16: ...`. Replace the board with the generated MP4 when editing.

```javascript
const videoPlan = {
    language: "sk",
    shots: [{
        id: "intro",
        type: "head",
        title: "Introduce the benefit",
        durationSeconds: 8,
        "text-sk": "Predstavujeme vám novinky vo WebJET CMS.",
        notes: "Insert the generated talking-head clip."
    }]
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
```

These scenarios include the main scenario `@video` using `recordVideoPlan`. `ElevenLabs Head` must contain a single call to `I.generateHead` and only a `@head` tag. Its runner uses the same static source code checking as audio; it does not execute callbacks when reading metadata.

From the `src/test/webapp` folder, run:

```shell
npm run head video/308-pb-redesign.js
```

It will only launch `@head`, without a browser or login. The default settings are the **Jack / Home Vlog Style** image prepared for 16:9 ratio in `video/assets/head/jack-home-vlog-style.png`, the `creatify-aurora` model, and the explicit resolution `720p`. TTS uses the same defaults as the audio: `eleven_v3` and Luki Zajo, including the variables `ELEVENLABS_MODEL_ID` and `ELEVENLABS_VOICE_ID`. Other Aurora settings remain default. Availability of the Image & Video API requires a supported paid ElevenLabs plan (currently Pro or higher).

Common settings can be overridden with a helper parameter:

```javascript
I.generateHead(videoPlan, {
    language: "en",
    imagePath: "assets/my-presenter.png",
    modelId: "creatify-aurora",
    resolution: "720p",
    audio: { modelId: "eleven_v3", voiceId: "<voice-id>" }
});
```

A single shot can contain `head: { imagePath, modelId, resolution, audio: { modelId, voiceId } }`. Each of its values ​​overrides the common setting; `language` is set only for the entire run. Relative `imagePath` is evaluated against the scenario file. PNG, JPEG and WebP up to 25 MB are supported; you determine the aspect ratio with a reference image. Supported resolutions are `480p` and `720p`. Another `modelId` must support the same Lip Sync input; Aurora is verified. The public API does not select the avatar and scene by name, so the saved image is used.

The entire plan, translations, non-empty texts `head`, settings, images, and the ability to create all outputs are checked before API calls. A plan without `head` will end informatively without API calls. Footage is processed sequentially: standalone TTS, sending image and MP3 as `inline_base64` to `POST /v1/flows/video`, checking `GET /v1/flows/video/{id}`, and downloading MP4. The API key is not sent to the results repository. Health checks have intervals of 10, 20, 40, and then 60 seconds; the limit is 30 minutes per video.

Outputs are in ignored `docs/feature-video`: `<scenario>-<shot-id>-<language>.mp3` and `.mp4`. Intro of scenario 308 creates `308-pb-redesign-intro-sk.mp4`. Each run generates a new one and consumes credits. Paid requests are not automatically repeated. An error stops further footage and indicates its ID when the task is created. Completed files are replaced atomically; in case of a video error, the last successful MP4 and the already generated new MP3 remain. The length of the clip is determined by the audio, `durationSeconds` is only an estimate for editing. Automatic assembly of the final movie is not part of the generation.

Details of inputs and states are provided in the [ElevenLabs Video API](https://elevenlabs.io/docs/api-reference/flows/video/create).

## Consumption and remaining credits

After `audio` and `head`, a summary of credits is printed. Before and after generation, [subscription](https://elevenlabs.io/docs/api-reference/user/subscription/get/) is loaded. The remaining credits of the current limit are `max(0, character_limit - character_count)`.

- **Audio:** the actual billed consumption comes from the `character-cost` header of the TTS response. If missing, the statement uses the indicated approximate account consumption difference. The number of characters in the input text is not used as an estimate.
- **Head:** approximate usage is the difference `character_count` after and before the entire run. Includes both TTS and video of all processed footage. Concurrent account activity and delayed billing may affect it.
- The summary is also printed after a partial failure. In the case of an incomplete or uncertain remote task, it is marked as in progress. A load error, missing data, or `user_read` authorization will display `unavailable` with a reason. Changing the billing period will invalidate the account usage difference; directly billed TTS usage and the valid current balance remain applicable.

A reporting error will not invalidate the generated media or cover up the original generation error. The TTS header is described in the [ElevenLabs API introduction](https://elevenlabs.io/docs/api-reference/introduction).

## Verification of helpers without paid generation

```shell
npm run audio:test
npm run head:test
npm run video:test
CODECEPT_AUDIO_FILE="$PWD/video/308-pb-redesign.js" npx codeceptjs dry-run -c codecept.audio.conf.js --steps --grep '@audio'
CODECEPT_HEAD_FILE="$PWD/video/308-pb-redesign.js" npx codeceptjs dry-run -c codecept.head.conf.js --steps --grep '@head'
CODECEPT_VIDEO=true npx codeceptjs dry-run -c codecept.video.conf.js --steps -p autoLogin video/308-pb-redesign.js
```

Dry-run does not perform any generation or API credits and does not require an API key. Regular video recording, audio and head have separate triggers. Verify real-world generation consciously for a specific scenario; check the image, audio and lip sync when finished.
