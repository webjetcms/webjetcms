# Watermark settings

Embedding a watermark requires the following server settings:

- [ImageMagick](https://imagemagick.org/script/download.php) library available on the server, specifically the ```convert``` and ```composite``` commands.
- Set configuration variable ```imageMagickDir``` to installation directory ```ImageMagick``` (typically ```/usr/bin```).
- Enabled watermark insertion by setting the configuration variable ```galleryEnableWatermarking``` to the value ```true```.

Only after the server settings are met will the set watermark begin to be inserted into the image.

![](watermark-applied.png)

The following watermark image formats are supported:

- ```png``` - ​​the image is inserted into the uploaded image without changing the size, therefore it is necessary to set the watermark size appropriately (note that the photo gallery contains both a small and a large image and a watermark of the same size will be inserted into both).
- ```svg``` - ​​the watermark is inserted into the image with the correct scaling of the size according to the size of the uploaded image. The watermark size is set in the configuration variable ```galleryWatermarkSvgSizePercent``` (default 5 percent) and ```galleryWatermarkSvgMinHeight``` (default 30 points). The watermark size is therefore automatically adjusted to the image size. Sample [svg watermark](watermark.svg).

!>**Warning:** Please note that the watermark is not inserted into the original image in the gallery, this is due to the possibility of regenerating existing images (e.g. changing the size or changing the watermark). The original image is available with the prefix ```o_``` and therefore it is possible to obtain it publicly like this. If you absolutely need to have a watermark in all images, you need to set the automatic application of the watermark after uploading the image by setting the conf. variable `galleryWatermarkApplyOnUpload` to the value `true`.

## Automatic watermarking after image upload

You can enable automatic watermarking of photos when uploading them to WebJET by setting the configuration variable ```galleryWatermarkApplyOnUpload``` to ```true```. The watermark is inserted into the uploaded photo to prevent duplication of its insertion when resizing the photo, etc.

Exceptions for which the watermark is not applied when uploading a file can be defined in the configuration variable ```galleryWatermarkApplyOnUploadExceptions``` (default set to logo,nowatermark,system) where there are expressions that, when found in the path to the image (image name or directory name), the watermark is not applied.

In this mode, the watermark is inserted the same for the entire website (it is not possible to specify different watermarks), for multidomain installation it is possible to set an image for each domain. It is set in the configuration variable ```galleryWatermarkApplyOnUploadDir``` (by default /templates/{INSTALL_NAME}/assets/watermark/) - the directory where images for automatic watermarking are placed when uploading an image. The image name must be ```default.png```, for multidomain it is possible to have a different one for each domain, in the form ```domena.png``` (e.g. ```www.interway.sk.png```).

You set the watermark position in the configuration variable ```galleryWatermarkGravity``` (default is Center). Options according to the cardinal points in English: ```NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast```. You can set the watermark transparency in the variable ```galleryWatermarkSaturation``` - sets the transparency of the watermark in the resulting image. Number 0-100, 0 means full transparency, 100 opacity.

## Possible configuration variables

- ```galleryEnableWatermarking``` (default true) - Disables/enables watermarking for images. Watermarking can significantly slow down large image imports due to recursively searching for watermark settings.
- ```galleryWatermarkSaturation``` (default 70) - sets the transparency of the watermark in the resulting image. Number 0-100, 0 means full transparency, 100 opacity.
- ```galleryWatermarkGravity``` (default Center) - Position of the watermark in the resulting image. Options according to the cardinal points in English: ```NorthWest, North, NorthEast, West, Center, East, SouthWest, South, SouthEast```.
- ```galleryWatermarkApplyOnUpload``` (default false) - When set to ```true```, it activates automatic watermarking when uploading images, so the watermark is also applied to the original images in the gallery.
- ```galleryWatermarkApplyOnUploadDir``` (default /templates/{INSTALL_NAME}/assets/watermark/) - Directory where images are placed for automatic watermarking when uploading an image. The image name must be ```default.png```, with multidomain it is possible to have a different one for each domain, in the form ```domena.png``` (e.g. ```www.interway.sk.png```).


