# AVIF test fixtures

These synthetic images contain no third-party material. Created with ImageMagick 7.1.2 and libheif 1.21.2:

```sh
magick -size 120x80 xc:none -fill '#e03030' -draw 'rectangle 20,10 99,69' -quality 80 autotest-transparent.avif
magick -size 120x80 xc:red -size 120x80 xc:blue -delay 10 autotest-animated.avif
```

The transparent image contains an opaque red rectangle and transparent margins. The animation contains two frames, red followed by blue. Java metadata tests require no native codec. Processing tests use `AVIF_TEST_IMAGEMAGICK_DIR` when set; with an explicit path, missing AVIF support is a test failure. Without it, native processing tests skip when the server's ImageMagick is unavailable.

The Gradle GitHub workflow uses Ubuntu 26.04 packages (ImageMagick 7, libheif 1.21+ and the AOM encoder/decoder plugins) and sets `AVIF_TEST_IMAGEMAGICK_DIR=/usr/bin`, so native processing tests run in CI. The animated fixture requires sequence decoding support; the older packages in Ubuntu 24.04 are insufficient.

The browser suite has independent page, gallery and image-editor workflows, each followed by a cleanup scenario. Run the complete `avif.js` file against a local WebJET instance; when filtering scenarios, include the matching cleanup. Configuration is restored before deleting the workflow's own test data.

`autotest-exif.avif` is an 8×8 red image with a synthetic EXIF Artist tag (`WebJET test`). It verifies that direct calls to `GalleryDB.stripExif` remove AVIF metadata only when `galleryStripExif` is enabled. Generate it with:

```python
from pathlib import Path
from tempfile import TemporaryDirectory
import struct
import subprocess

with TemporaryDirectory() as folder:
    source = Path(folder) / 'tagged.jpg'
    artist = b'WebJET test\0'
    exif = (b'Exif\0\0II' + struct.pack('<HI', 42, 8) + struct.pack('<H', 1)
            + struct.pack('<HHII', 0x013b, 2, len(artist), 26) + struct.pack('<I', 0) + artist)
    subprocess.run(['magick', '-size', '8x8', 'xc:red', str(source)], check=True)
    data = source.read_bytes()
    source.write_bytes(data[:2] + b'\xff\xe1' + struct.pack('>H', len(exif) + 2) + exif + data[2:])
    subprocess.run(['magick', str(source), 'autotest-exif.avif'], check=True)
```
