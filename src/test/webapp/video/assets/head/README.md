# Default head reference

`jack-home-vlog-style.png` is the shared landscape reference for ElevenLabs
Creatify Aurora. It depicts ElevenLabs stock avatar **Jack**, scene **Home Vlog
Style**, with the room generatively extended to landscape instead of cropping
into his face. The output is 1672 × 941 pixels (16:9 rounded to whole pixels).
Video generation still requests 720p explicitly.

`jack-home-vlog-style-original.png` is the unmodified 896 × 1200 PNG downloaded
on 2026-09-06 from the original `content.png` URL embedded in the selected
style's image source. This is the original source, rather than the website's
resized, compressed Next.js thumbnail or a screenshot.

The landscape version was prepared with the `image_gen` image-edit tool using
that original PNG and visually inspected. Prompt: generatively outpaint left
and right to 16:9 at the highest available resolution, preferably 3840 × 2160;
preserve the complete portrait composition, Jack's identity, hairstyle,
expression, gray sweater, torso, arm and open hand, desk and laptop; avoid
cropping or enlarging the head. Extend the same bright room and wooden desk
with matching daylight and perspective, without extra people, limbs, text,
watermarks or borders. The tool returned 1672 × 941 pixels. The added room is
AI-generated; this landscape composition is not an original ElevenLabs scene.

Source UI: https://elevenlabs.io/app/image-video?modality=lipsync
Stock asset identifiers: `iRidgDgAdcwGvTxbA280` / `4f77rfes0WZTXT0bcXx7`.
No signed source URL, account token or API key is stored here.

Override `imagePath` in `I.generateHead` or a shot's `head` object to choose a
different reference. Relative paths resolve against the scenario file.
