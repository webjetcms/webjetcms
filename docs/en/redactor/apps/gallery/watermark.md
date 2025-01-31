# Watermark settings

Inserting a watermark requires the following server settings:
- Library available on the server [ImageMagick](https://imagemagick.org/script/download.php) specific commands `convert` a `composite`.
- Configuration variable set `imageMagickDir` to the installation directory `ImageMagick` (typically `/usr/bin`).
- Enabled watermark insertion by setting a configuration variable `galleryEnableWatermarking` to the value of `true`.

Only after the server settings have been met will the set watermark start to be inserted into the image.

![](watermark-applied.png)

The following watermark image formats are supported:
- `png` - the image is inserted into the uploaded image without resizing, so you need to set the watermark size appropriately (note that the photo gallery contains both a small and a large image, and both will have the same size watermark inserted).
- `svg` - the watermark is embedded in the image with correct scaling of the size according to the size of the uploaded image. The watermark size is set in the configuration variable `galleryWatermarkSvgSizePercent` (5 per cent by default) and `galleryWatermarkSvgMinHeight` (standard 30 points). The size of the watermark is therefore automatically adjusted to the size of the image. Sample [svg watermark](watermark.svg).

!>**Warning:** note that in the gallery the watermark is not inserted into the original image, this is because of the possibility of re-generating existing images (e.g. resizing or changing the watermark). The original image is available with the prefix `o_` and can therefore be publicly obtained in this way. If you absolutely need to have the watermark in all images, you need to set the automatic application of the watermark after uploading the image by setting the conf. variable `galleryWatermarkApplyOnUpload` to the value of `true`.

## Automatically apply a watermark after uploading an image

To enable automatic watermarking of photos when uploading them to WebJET, set the configuration variable `galleryWatermarkApplyOnUpload` at `true`. The watermark is inserted into the uploaded photo to avoid duplicate insertion when resizing the photo and so on.

Exceptions for which the watermark is not applied when the file is uploaded can be defined in the configuration variable `galleryWatermarkApplyOnUploadExceptions` (set to logo,nowatermark,system by default) where there are expressions that when found in the image path (image name or directory name) the watermark is not applied.

The watermark in this mode is the same for the whole site (it is not possible to specify different watermarks), for a multidomain installation it is possible to set an image for each domain. It is set in the configuration variable `galleryWatermarkApplyOnUploadDir` (default /templates/{INSTALL_NAME}/assets/watermark/) - directory where images are placed for automatic watermarking when uploading an image. The name of the image must be `default.png`, with multidomain it is possible to have a different one for each domain, in the form `domena.png` (e.g. `www.interway.sk.png`).

You set the position of the watermark in the configuration variable `galleryWatermarkGravity` (preset to Center). Options by cardinal directions in English: `NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast`. You can set the water pressure translucency in the variable `galleryWatermarkSaturation` - sets the transparency of the watermark in the resulting image. Number 0-100, 0 means full transparency, 100 means opacity.

## Possible configuration variables

- `galleryEnableWatermarking` (true by default) - Turns off/on watermarking for images. Watermarking can significantly slow down large image imports due to recursive searching for watermark settings.
- `galleryWatermarkSaturation` (default 70) - sets the transparency of the watermark in the resulting image. Number 0-100, 0 means full transparency, 100 means opacity.
- `galleryWatermarkGravity` (default Center) - The position of the watermark in the resulting image. Options by cardinal directions in English: `NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast`.
- `galleryWatermarkApplyOnUpload` (false by default) - When set to `true` activates automatic watermark application when images are uploaded, so the watermark is also applied to the original images in the gallery.
- `galleryWatermarkApplyOnUploadDir` (default /templates/{INSTALL_NAME}/assets/watermark/) - Directory where images are placed for automatic watermarking when an image is uploaded. The name of the image must be `default.png`, with multidomain it is possible to have a different one for each domain, in the form `domena.png` (e.g. `www.interway.sk.png`).
