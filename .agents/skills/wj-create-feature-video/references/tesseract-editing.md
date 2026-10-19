# Tesseract Editing for WebJET Feature Videos

Read this reference when assembling or revising a finished video. Use the
installed Tesseract video skill for CLI installation, schemas and rendering;
its current documentation is authoritative. The details below preserve the
WebJET production decisions and lessons from the PR 332 edit.

## Inputs and Persistent Knowledge

- Keep `videoPlan` in `src/test/webapp/video/<scenario-name>.js` as the source of
  narration order and browser actions. `text-sk` determines what each action
  should accompany; production notes must not reorder the spoken walkthrough.
- Use the actual approved MP3 parts and recorded topic shots. Replacing an MP3
  can change individual sentence lengths even when the text is unchanged.
  Retiming does not require new TTS or another browser recording.
- Reuse the established project folder and preserve a meaningful earlier
  version before a substantial revision. Keep the `.tsrct`, matching MP4,
  previews, optional chapter exports and working files together.
- Generated files belong in gitignored `docs/feature-video/`. A local readme is
  useful for that deliverable, but reusable instructions belong in this skill.
  Preserve media outside a worktree before deleting it when continued access is
  needed; Git does not retain these generated files. Do not force-add renders,
  model caches or local virtual environments to the repository.

## Timing and Picture

1. Inspect the real encoded recordings. Remove SETUP/SHOT slates, fixture
   preparation and cleanup. Source frame positions are authoritative; browser
   test wall-clock logs and estimated `durationSeconds` can differ from them.
2. Keep one continuous topic flow per shot. Preserve the approved narration
   intact unless a speech edit is requested. Use waveform views and, when
   available, local word timestamps to identify sentence/action cues; inspect
   the actual clicks against those cues. Do not invent timing from text alone.
3. Build a mapping from narration cues to source-video positions within each
   topic. Adjust holds and picture speed between cues instead of stretching the
   whole film by one duration ratio. If audio is regenerated, rebuild these
   mappings from the new audio, including later chapter boundaries and exports.
4. Skip loading screens with deliberate frame-aligned cuts. Review both sides
   at adjacent output frames; a fast time remap can still expose an intermediate
   blank editor or unrelated list for one frame.
5. Prefer a clean screen recording without numbered chapter-title overlays.
   Use smooth zooms for important fields, autocomplete thumbnails, menu choices
   and settings. Hold the detail while it is explained, then widen before
   actions outside the crop. Keep the full drag-and-drop path visible. Verify
   Save/Done buttons, dropdowns and dialog headers during each camera move.
   Do not add decorative motion or sound to every click.

## Native Project and Performance

- Inspect and check out the existing `.tsrct` before editing. Preserve layer
  IDs, dynamics and unknown fields. Apply changes with `project commit` or
  `project apply`, then check out again after action batches. Never edit the
  archive directly or rebuild an existing edit from a minimal template.
- Import media before referencing its returned asset ID. Keep footage, narration
  and camera keyframes editable as native layers/groups in one composition.
  Do not flatten the entire previous MP4 to add a new ending.
- The document duration is in seconds; layer ranges and keyframe times are in
  milliseconds. `activeRange` uses the immediate parent's clock; `sourceRange`
  selects source footage. A clip moved on the timeline must still play the
  intended source interval. Keep the full referenced source range available
  when using time-remap keyframes, or later frames can render black.
- In CLI 0.3.0, `setFxLayerTimeRemap` actions serialize as the layer's `playback`
  field on checkout. Use the installed schema instead of inventing fields.
- For repeated seeking through retimed browser footage, long-GOP video can
  make rendering very slow. A high-quality H.264 all-intra derivative (`-g 1`,
  e.g. CRF 16) resolved this in PR 332. Preserve source duration, frame rate,
  originals and the conversion recipe; import the derivative once and reuse it.
  This is a performance remedy, not a requirement to transcode every short clip.
- Mute browser footage when it has no intended sound. Keep approved narration
  as one native Audio layer per MP3 part with explicit timing and gain. A newly
  imported video needs `volume: 1.0` to retain embedded sound in this CLI;
  omitted/null volume disables it. Avoid a second audible copy of the same audio.
- Render through Tesseract. FFmpeg can prepare compatible sources, inspect
  exports and extract individual chapters from the completed film. It must not
  silently replace the Tesseract composition/rendering step. Resolve local
  executable paths; do not copy another worktree's absolute paths or caches.

## Standard Brand Outro

The shared source is `src/test/webapp/video/assets/outro.mp4` relative to the
repository root. Probe and preview the current file; use the video duration
reported by import rather than assuming the container's audio padding is part
of the animation. For the original asset, CLI 0.3.0 reports 11,400 ms of video.
Do not hard-code that duration for a replacement asset.

Import it with `project import-video`, then add a separate native Video layer
after the finished feature segment. Set its source start to zero, preserve the
full source duration and original playback speed, and extend the composition
through its last frame. Retain source sound if present, but do not invent music
for a silent asset. Keep the previous final sentence complete and a short
breathing pause before the animation; no extra heading or narration is needed.

An existing scenario shot called `outro` can be a documentation walkthrough or
feature-specific closing narration. It does not replace the shared brand
animation. Conversely, if the animation is already on the timeline, do not
append a duplicate. Adding this silent editing asset must not force changes to
approved `text-sk`, paid audio generation or the browser scenario.

Append it to the complete final MP4 and editable project. When supplying
individual shots, retain the topic clips and optionally export the animation
once as its own final clip. Do not repeat it after every topic.

## Validation and Handoff

- Review native previews and the actual encoded film: zoom entrances/holds/
  exits, important clicks, chapter joins, the transition into the outro and its
  final frame. A valid project alone does not prove correct output.
- Check dimensions, frame rate, duration, full decoding, unintended black gaps,
  audible waveform, loudness and encoded peaks. A dark supplied outro is not
  automatically a black-frame defect; compare flagged intervals with its source.
- For a narration-only revision, comparing the full rendered waveform with the
  approved MP3 verifies preservation and offset. Compare chapter audio with the
  corresponding final-video interval. These checks do not establish perceived
  synchronization or replace listening; disclose when a full audition was not
  possible. Do not claim successful listening based on metadata or meters.
- Keep the `.tsrct`, exported MP4 and visible previews on the same revision.
  Re-export changed chapter clips when timing changes. Preserve unchanged ones
  when only appending the outro, and update duration/shot notes.
- Show the final MP4 and link the portable project and requested shot clips.
  MP4 is usable in ordinary players/editors. The `.tsrct` retains native layers,
  timing and embedded assets for Tesseract; inspect current editor capabilities
  before promising a desktop GUI or round-trip to another editor.
