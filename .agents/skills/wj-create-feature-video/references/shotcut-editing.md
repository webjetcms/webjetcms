# Write an editable Shotcut project and render it with melt

## Locate or install Shotcut

Shotcut ships `melt`, `ffmpeg` and `ffprobe` together. First check the installation directory for the host below and any tools already on `PATH`. Use a working installation as is. If the tools are missing, install the build for the host OS and CPU. For a direct download, use the release archive and matching SHA-256 checksum from the [official downloads page](https://shotcut.org/download/).

- **macOS.** Look for `Shotcut.app` in `/Applications` and `~/Applications`. With Homebrew, run `brew install --cask shotcut`. Otherwise mount the official DMG with `hdiutil attach -nobrowse "$dmg"`, copy `Shotcut.app` from the mounted volume into an Applications directory using `ditto`, then detach that volume. Set `bin` to the app’s `Contents/MacOS` directory, e.g. `bin=/Applications/Shotcut.app/Contents/MacOS`. [Homebrew cask](https://formulae.brew.sh/cask/shotcut)
- **Windows.** Check the Shotcut installation folder, normally `$env:ProgramFiles\Shotcut`. Install with `winget install --id Meltytech.Shotcut --exact --source winget`, or extract the official portable ZIP with `Expand-Archive -LiteralPath $zip -DestinationPath $toolsDir` in a user-writable directory. In PowerShell, set `$bin` to the folder containing `melt.exe`, e.g. `$bin = Join-Path $env:ProgramFiles 'Shotcut'`. [WinGet package](https://github.com/microsoft/winget-pkgs/tree/master/manifests/m/Meltytech/Shotcut)
- **Linux.** For a compatible host, download the official portable tar archive and extract it into an existing user-writable tools directory with `tar -xf "$archive" -C "$tools_dir"`. Set `bin` to the extracted `Shotcut.app` directory. Its top-level launchers set the library and plugin paths. If using distro packages on Debian or Ubuntu, install `shotcut melt ffmpeg frei0r-plugins` with `apt install` and use the tools on `PATH`. For an existing Flatpak use `flatpak run --command=melt org.shotcut.Shotcut` and substitute `ffmpeg` or `ffprobe` for the other tools. [Shotcut command-line launchers](https://forum.shotcut.org/pub/export-at-the-command-line)

For the commands below, prepend the selected tool directory to the current shell’s path with `export PATH="$bin:$PATH"` (POSIX) or `$env:Path = "$bin;$env:Path"` (PowerShell). For Flatpak, use its launchers in a directory accessible to the app.

Run `melt -version`. On a fresh installation, run this one-second render in a new temporary directory to check the text filter and MP4 encoders before assembling the project.

```sh
melt -profile atsc_1080p_25 color:black out=24 -filter dynamictext:WebJET fgcolour=white -consumer avformat:shotcut-check.mp4 f=mp4 vcodec=libx264 pix_fmt=yuv420p acodec=aac frequency=48000 channels=2 real_time=-1 -silent
ffprobe -v error -count_frames -show_entries stream=codec_name,nb_read_frames,width,height -of compact shotcut-check.mp4
ffmpeg -v error -i shotcut-check.mp4 -frames:v 1 shotcut-check.png
```

Expect 25 H.264 frames at 1920 × 1080, an AAC stream and the word `WebJET` visible in the PNG. If melt reports a missing filter or encoder, check the selected launcher and its registered services with `melt -query`. [MLT command-line reference](https://www.mltframework.org/docs/melt/)

## Set up the project

Set the MLT `<profile>` to 1920 × 1080, 25 fps, progressive, square pixels for the standard WebJET project. Write the timeline directly to `docs/feature-video/<production>/<film>.mlt`. Copy the browser recordings, approved MP3 parts and other media used on the timeline into a `media/` subdirectory. Shotcut projects reference these files. Keep paths relative to the MLT file and transfer the whole directory. For an alternative cut, use a separate production directory. Reuse the original recordings, retakes and approved narration. A previous edit can supply cue times, and its MP4 can serve as the comparison copy.

## Assemble against the approved narration

1. Place each approved MP3 at normal speed on an audio track. Use the waveform, the actual words and any measured word times to locate the action cues. Preserve the approved lead-in and pauses. For an excerpt, subtract the narration's timeline start from the excerpt start to find its source in-point.
2. Place the recorded interface on a video track and mute its incidental audio. Remove SETUP/SHOT slates, preparation and cleanup. Match the spoken cues to frames in the encoded recordings. Browser event logs can help locate clicks, but setup removal and retakes change their offsets. Cut across waits for a response or a page load. Use **Time Remap** between cues to preserve cursor movement and give the viewer time to read the result.
3. Show the whole interface for window changes, menus and results. For a named menu choice, hold the cursor over the actual row long enough to read it. Add **Size, Position & Rotate** to the picture clip for a spoken detail or text entry. Set a full-frame key, zoom towards the control, hold it through the cue and return to full frame. Keep the camera independent of the time remap and preserve the cursor path. Check adjacent frames at cuts for transient blanks or loading screens.
4. For a complete film, add the opening notice and the shared outro as described below. A middle excerpt uses the picture and sound present in that interval.

## Editable MLT

Use the installed Shotcut filter metadata for filter names and animated properties. An existing project from that version can serve as an XML example. Create the `main_bin` playlist with `xml_retain=1`. Put a black producer in the `background` playlist and use it as the first track of the final tractor. Mark the tractor with `shotcut=1`. Give the narration playlist `shotcut:audio=1`, hide that track's video and use an MLT `mix` transition for its audio. Mark the picture playlist with `shotcut:video=1` and set `audio_index=-1` on silent browser footage. Give both tracks readable `shotcut:name` values. Shotcut documents its [MLT XML annotations](https://shotcut.org/notes/mltxml-annotations/).

### Time Remap and clip length

A `timeremap` link belongs on the media `chain`. In Shotcut 26.11.15 the Time Remap control edits `map`. Its keys are output frames on the chain and its values are source times in seconds. For example, `173=5.16;232=5.32` holds a real menu hover over 59 frame intervals. Use `image_mode=nearest` for sharp text.

When a remap extends past the source duration, set the chain's `length` to cover the last output frame. MLT counts `length` in frames and treats `out` as inclusive. In the AI chat video a 148-frame recording had to fill 234 frames before the outro.

```xml
<chain id="manual" out="233">
  <property name="resource">media/prirucka.mp4</property>
  <property name="mlt_service">avformat</property>
  <property name="audio_index">-1</property>
  <property name="length">234</property>
  <link>
    <property name="mlt_service">timeremap</property>
    <property name="map">0=0.50;233=5.48</property>
    <property name="image_mode">nearest</property>
  </link>
</chain>
```

Use `<entry producer="manual" in="0" out="233"/>` on the picture playlist. With only the two `out` attributes, melt can end the shot at frame 147 and start the following clip 86 frames early. A full-length background track can still give the export the expected total duration.

### Camera and comparison overlays

For the camera, use MLT filter `affine` with `shotcut:filter=affineSizePosition`. Its animated `transition.rect` contains the left, top, width, height and opacity. On a clip starting at frame zero, frames 83 and 147 bracket the move. Frames 95 and 134 hold the AI icon at 120%. The pilot uses additional keys between these positions to match the original acceleration and deceleration.

```xml
<filter in="0" out="299">
  <property name="mlt_service">affine</property>
  <property name="shotcut:filter">affineSizePosition</property>
  <property name="transition.rect">0=0 0 1920 1080 1;83=0 0 1920 1080 1;95=-278 -42 2304 1296 1;134=-278 -42 2304 1296 1;147=0 0 1920 1080 1;299=0 0 1920 1080 1</property>
  <property name="transition.fill">1</property>
  <property name="transition.distort">0</property>
</filter>
```

Here the 120% view keeps source point (1390, 210) fixed. For scale `s` around point `(x, y)`, set the rectangle to `((1-s)*x, (1-s)*y, 1920*s, 1080*s)`. At 25 fps, a 12-second excerpt contains 300 frames. If the voice track starts at 0.32 s, an excerpt starting at 18.00 s uses `<entry producer="voice" in="442" out="741"/>`.

When the narration asks the viewer to check a value against an attachment, place an image of the original page in unused space beside the recorded result. The AI chat edit shows the full PDF first. An `affine` filter then moves the PDF under a fixed **Crop: Rectangle** (`qtcrop` with transparent padding). A marker on a separate track points to the matching row in the real answer at each spoken cue. Composite the overlay playlists over the picture with `frei0r.cairoblend`, using the picture track as `a_track` and each overlay as `b_track`. Check that the source text, the reply and the controls stay readable through each move.

## AI disclosure and WebJET outro

For a complete film, show `Tento obsah bol vytvorený pomocou AI` within the first second and keep it fully readable for at least eight seconds, excluding fades. Limit optional fades to 250 ms each. On a video track above the picture, use a transparent clip with an editable **Text: Simple** filter. Use white sans-serif text (start at 32 px for 1080p), a dark near-opaque padded backing and roughly 4% margins at bottom right. Keep it fixed across opening cuts and camera moves. Check the wording, diacritics, contrast and safe margins in the encoded opening at 640 × 360 using a frame extracted with FFmpeg.

After the last spoken sentence and a short closing hold, append a copy of `src/test/webapp/video/assets/outro.mp4` as its own clip. Probe the current asset, use its entire video at original speed and preserve any embedded audio. If the browser track is muted, put the outro on an audible track. If the scenario has a documentation shot named `outro`, keep it before the brand animation. Include the animation once in both the full timeline and the full export. Check the join and the last frame against the source.

## Export and verify

Run the verified tools from the project directory. The examples below use POSIX shell syntax. In PowerShell, assign `$film` to the project name and enter each command on one line. Use 48 kHz stereo AAC by default. To reproduce the pilot with an exact 12.000-second container, its 96 kHz AAC setting aligns the final frame with a packet boundary.

```sh
film=WebJET-demo
melt -profile atsc_1080p_25 "$film.mlt" \
  -consumer "avformat:$film.mp4" f=mp4 vcodec=libx264 crf=16 preset=medium \
  pix_fmt=yuv420p acodec=aac ab=192k frequency=48000 channels=2 \
  movflags=+faststart rescale=lanczos real_time=-1 -progress
```

To preview a join, add `in=<first-frame> out=<last-frame>` immediately after the MLT filename and choose a separate output filename. For the full export, check the decoded video frame count against the timeline. Measure the video, audio and container durations separately because AAC packet boundaries can extend the container slightly. Decode both streams through the end.

```sh
ffprobe -v error -count_frames -show_entries \
  'format=duration:stream=codec_name,width,height,r_frame_rate,nb_read_frames,channels,sample_rate,duration' \
  -of compact "$film.mp4"
ffmpeg -v error -xerror -i "$film.mp4" -map 0:v:0 -map 0:a:0 -f null -
```

If the macOS sandbox reports Qt's `Incompatible processor ... neon` error, request approval to rerun the same bundled melt outside the sandbox.

- Parse the delivered XML. Resolve each source media path relative to the project file. Follow the final tractor’s track references to the picture and narration playlists and confirm that zoom keyframes belong to the picture clip’s native filter.
- Inspect frames extracted from the MP4 before, during and after each zoom, including menu changes. Compare matched times with the reference export when available. At the planned outro cut, inspect the last feature frame and compare the next frame with the start of the source animation. Compare the final animation frame as well. This catches an early cut or a black tail even when the total frame count matches.
- Check for clipping and listen for synchronization. When comparing waveforms, decode both complete audio streams to the same PCM format before slicing matching windows. Compare at the intended source in-point and in the matching reference interval. Measure offset at the start, middle and end for drift.
- Link the verified MP4 and editable MLT in the handoff. Keep the referenced media with the project.
