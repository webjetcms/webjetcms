# Default head reference

`jack-home-vlog-style.png` is the shared landscape reference for ElevenLabs
Creatify Aurora. It depicts ElevenLabs stock avatar **Jack**, scene **Home Vlog
Style**, selected by the user in the ElevenLabs Image & Video interface on
2026-09-06. It is 1672 × 941 pixels (approximately 16:9). Video generation requests
720p explicitly; reference dimensions are not the output resolution.

Prepared with the `image_gen` image-edit tool from a screenshot of the selected
stock reference, then visually inspected. Prompt: crop the same photograph to a
16:9 landscape composition around the face and upper shoulders, preserve Jack's
identity and existing background, remove black margins, and add no background,
objects, text or borders. This is an AI-prepared reference, not the original
ElevenLabs download. The original screenshot showed a portrait image.

Source UI: https://elevenlabs.io/app/image-video?modality=lipsync
Stock asset identifiers: `iRidgDgAdcwGvTxbA280` / `4f77rfes0WZTXT0bcXx7`.
No signed source URL, account token or API key is stored here.

Override `imagePath` in `I.generateHead` or a shot's `head` object to choose a
different reference. Relative paths resolve against the scenario file.
