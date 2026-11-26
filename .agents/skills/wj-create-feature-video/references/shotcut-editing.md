# Write an editable Shotcut project and render it with melt

## Set up the project

On macOS, check the installed Shotcut bundle and run `/Applications/Shotcut.app/Contents/MacOS/melt -version`. The same directory contains FFmpeg and ffprobe. If Shotcut is missing, use the [official macOS distribution](https://shotcut.org/download/) and follow the system installation approval.

Set the MLT `<profile>` to 1920 × 1080, 25 fps, progressive, square pixels for the standard WebJET project. Write the timeline directly to `docs/feature-video/<production>/<film>.mlt`. Copy the browser recordings, approved MP3 parts and other media used on the timeline into a `media/` subdirectory. Shotcut projects reference these files. Keep paths relative to the MLT file and transfer the whole directory. A previous final MP4 is a comparison source.

## Assemble against the approved narration

1. Place each approved MP3 at normal speed on an audio track. Use the waveform, the actual words and any measured word times to locate the action cues. Preserve the approved lead-in and pauses. For an excerpt, subtract the narration's timeline start from the excerpt start to find its source in-point.
2. Place the recorded interface on a video track and mute its incidental audio. Remove SETUP/SHOT slates, preparation and cleanup. Keep the clicks, their results and the visible cursor in order. Adjust picture holds and speeds between cues. Use Shotcut's **Time Remap** filter for variable speed. Its MLT representation on a media `chain` is a `timeremap` link. Shotcut 26.11.15 reads `map` with output frame numbers as keys and source seconds as values. In the Pozvánka pilot, `173=5.16;232=5.32` plays 0.16 seconds of real hover footage over 59 output frames before the click. Use `image_mode=nearest` to keep interface text readable.
3. Show the whole interface for window changes, menus and results. For a named menu choice, hold the cursor over the actual row long enough to read it. Add **Size, Position & Rotate** to the picture clip for a spoken detail or text entry. Set a full-frame key, zoom towards the control, hold it through the cue and return to full frame. Keep the camera independent of the time remap and preserve the cursor path. Check adjacent frames at cuts for transient blanks or loading screens.
4. For a complete film, add the opening notice and the shared outro as described below. A middle excerpt uses the picture and sound present in that interval.

## Editable MLT

Use the installed Shotcut filter metadata for filter names and animated properties. An existing project from that version can serve as an XML example. Create the `main_bin` playlist with `xml_retain=1`. Put a black producer in the `background` playlist and use it as the first track of the final tractor. Mark the tractor with `shotcut=1`. Give the narration playlist `shotcut:audio=1`, hide that track's video and use an MLT `mix` transition for its audio. Mark the picture playlist with `shotcut:video=1` and set `audio_index=-1` on silent browser footage. Give both tracks readable `shotcut:name` values. Shotcut documents its [MLT XML annotations](https://shotcut.org/notes/mltxml-annotations/).

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

Here the 120% view keeps source point (1390, 210) fixed. For scale `s` around point `(x, y)`, set the rectangle to `((1-s)*x, (1-s)*y, 1920*s, 1080*s)`. At 25 fps, the pilot covers reference frames 450 through 749. Its 300 frames are numbered 0 through 299. MLT entry `out` is inclusive. Speech begins at 0.32 s, so the 18.00 s excerpt starts 17.68 s into the approved MP3. On the audio playlist use `<entry producer="voice" in="442" out="741"/>` in project-frame units. Recalculate these values for each edit.

## AI disclosure and WebJET outro

For a complete film, show `Tento obsah bol vytvorený pomocou AI` within the first second and keep it fully readable for at least eight seconds, excluding fades. Limit optional fades to 250 ms each. On a video track above the picture, use a transparent clip with an editable **Text: Simple** filter. Use white sans-serif text (start at 32 px for 1080p), a dark near-opaque padded backing and roughly 4% margins at bottom right. Keep it fixed across opening cuts and camera moves. Check the wording, diacritics, contrast and safe margins in the encoded opening at 640 × 360 using a frame extracted with FFmpeg.

After the last spoken sentence and a short closing hold, append a copy of `src/test/webapp/video/assets/outro.mp4` as its own clip. Probe the current asset, use its entire video at original speed and preserve any embedded audio. If the browser track is muted, put the outro on an audible track. If the scenario has a documentation shot named `outro`, keep it before the brand animation. Include the animation once in both the full timeline and the full export. Check the join and the last frame against the source.

## Export and verify

Run the bundled `melt` from the project directory. Set `film` to the project filename without its extension. The standard export uses 48 kHz stereo AAC. For the 12-second Pozvánka pilot, use `frequency=96000` to end on an AAC packet boundary.

```sh
bin=/Applications/Shotcut.app/Contents/MacOS
film=WebJET-demo
"$bin/melt" -profile atsc_1080p_25 "$film.mlt" \
  -consumer "avformat:$film.mp4" f=mp4 vcodec=libx264 crf=16 preset=medium \
  pix_fmt=yuv420p acodec=aac ab=192k frequency=48000 channels=2 \
  movflags=+faststart rescale=lanczos real_time=-1 -progress
```

Check the decoded video frame count against the timeline and measure the audio, video and container durations separately. The 25 fps pilot has 300 frames and lasts 12.000 s. Decode both streams through the end.

```sh
"$bin/ffprobe" -v error -count_frames -show_entries \
  'format=duration:stream=codec_name,width,height,r_frame_rate,nb_read_frames,channels,sample_rate,duration' \
  -of compact "$film.mp4"
"$bin/ffmpeg" -v error -xerror -i "$film.mp4" -map 0:v:0 -map 0:a:0 -f null -
```

If the macOS sandbox reports Qt's `Incompatible processor ... neon` error, request approval to rerun the same bundled melt outside the sandbox.

- Parse the delivered XML. Resolve each source media path relative to the project file. Follow the final tractor’s track references to the picture and narration playlists and confirm that zoom keyframes belong to the picture clip’s native filter.
- Inspect frames extracted from the MP4 before, during and after each zoom, including menu changes. Compare matched times with the reference export when available. Check for unintended black gaps and compare a dark branded ending with its source.
- Check for clipping and listen for synchronization. When comparing waveforms, decode both complete audio streams to the same PCM format before slicing matching windows. Compare at the intended source in-point and in the matching reference interval. Measure offset at the start, middle and end for drift.
- Link the verified MP4 and editable MLT in the handoff. Keep the referenced media with the project.
