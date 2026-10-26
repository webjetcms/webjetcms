# Generating preview images in WebJET

WebJET allows you to generate images of a specified size from any image in the `/images,/files,/shared,/video,/templates` folders on request.

## Basic generation

Let's imagine we have an image (it doesn't have to be from the gallery, it's just a sample):

`/images/gallery/test-vela-foto/dsc04131.jpg`

![Original Image](original-image.png)

and we need it to have a max size of `200x200` points. Just add the prefix `/thumb` before the image URL and add the URL parameters `w` and `h` with the desired size, so:

`/thumb/images/gallery/test-vela-foto/dsc04131.jpg?w=200&h=200`

![Thumb Image 200x200](thumb-image.png)

The image may actually be smaller than the requested `200x200` pixels, depending on its aspect ratio. In this case, it was generated as `200x134` pixels, but it will always fit within the requested dimensions.

## AVIF images

Thumbnail generation and AVIF editing require **ImageMagick with AVIF read and write support, including transparency and animations** on the server. Use ImageMagick **7.1.2-34 or later**, `libheif` **1.22.0 or later**, and the AV1 codec. In the `imageMagickDir` configuration, set the folder with the `magick` command. Support must be provided by the server administrator. WebJET assumes a working codec for processing.

Verify the versions in a terminal under the account that WebJET is running under. Enter the same folder in the `image_magick_dir` variable as in the `imageMagickDir` configuration (in the example `/usr/local/bin`):

```sh
magick -version
magick -list format | awk '$1 == "AVIF" { print }'
```

Example of a valid output (abbreviated):

```text
Version: ImageMagick 7.1.2-34 Q16-HDRI ...
     AVIF  HEIC      rw+   AV1 Image File Format (1.23.6)
```

- The version after `ImageMagick` must be **7.1.2-34 or later**.
- The number in parentheses on the line `AVIF` is the version of the `libheif` library used by ImageMagick. It must be **1.22.0 or later**.
- The `AVIF` line must contain `rw`, meaning both read and write support. If the line is missing, this build does not have AVIF support. The `HEIC` column may not be displayed depending on the build.

The editing uses regular SDR output, with no guarantee of preserving HDR and the original 10/12-bit depth. An automatic watermark on upload (`galleryWatermarkApplyOnUpload`) is also applied to the original AVIF and embedded in each frame. Transcoding may change the bytes of the file. The TUI drawing editor sends a single PNG image from the canvas, so saving it will create a static AVIF.

You can set the encoding quality and speed using the existing [ImageMagick custom parameters] mechanism. The default value for `imageMagickCustomParams_avif` is `-quality 80 -define heic:speed=6`. More specific settings for the operation, such as `imageMagickCustomParams_resize_avif`, also apply.

## Default image

By default, a standard 404 error is returned for a non-existent image when using the `/thumb` address. However, if you need to display a default image for such a case, you can use the `thumbServletMissingImg` configuration variable. It can be used to add the folder name and file name to the lines that should be used for this case. Example of the setting:

```txt
/images/gallery/test/|/images/photo3.jpg
/images/|/images/photo1.jpg
```

Setting according to the above format for non-existent images from the folder `/images/gallery/test/` and its sub-directories will display the image `/images/photo3.jpg`. For calling an image from the folder `/images/test/podadresar/`, the image `/images/photo1.jpg` will be displayed, because the best match will be with the `/images` directory. When calling `/templates/meno/assets/image.jpg`, a standard error 404 will be displayed, since no prefix for this folder is defined in the configuration variable.

The found image is processed through `/thumb`, so it is generated in the specified size from the URL parameters.

## Restrictions

Image generation is server-side-loaded, so it is protected by SPAM protection. The following config variables are used:

- `spamProtectionTimeout-ThumbServlet` - ​​time between HTTP requests, set to `-2` to disable, as there may be multiple images on the page that are generated at once.
- `spamProtectionHourlyLimit-ThumbServlet` - ​​maximum number of generated images from one IP address per hour, set to `300` by default.
- `cloudCloneAllowedIps` - ​​comma-separated list of IP address beginnings for which the restriction will not be applied, empty by default (not used).
- `thumbServletAllowedSizes` - ​​list of allowed file sizes for image generation. It is generated in the format `{width}x{height}ip{ip}{noip}c{color}q{quality}`, for example `730x401ip5ncff00ffq90`. Parameters that are not specified are not used, for example `430x405` or `730x404ip5`. We recommend using the `learn` mode for initial setup and then setting the `check` mode. If you use `ip=1/ip=2` without the `h/w` parameter, use the same value for the permission, for example for `/thumb/images/image.jpg?ip=1&w=610` set `610x610ip1`.
- `thumbServletAllowedSizeMode` - ​​Sets the allowed dimensions mode for image generation. Possible values:
  - `deny` - ​​disables generation of new images if no administrator is logged in (existing images will be displayed as they are already generated on disk)
  - `allow` - ​​allows all dimensions, or does not check the list of allowed options
  - `learn` - ​​adds a value to the list `thumbServletAllowedSizes` (if it is not already there) - learning mode of existing values
  - `check` - ​​allows only specified values, if an administrator is logged in, automatically adds a new dimension to the list
  - `strict` - ​​allows generating an image only for the specified values ​​(checks the list of allowed values)

The **Thumbnail** tab in the image properties is controlled by the `thumbServletAllowedSizeMode` setting:

| Mode | Specified permitted dimensions | Empty list |
| --- | --- | --- |
| `strict` | Select an allowed size or **Do not use thumbnail** | Just **Do not use thumbnail** |
| `learn`, `check`, `allow` | Select a size, **Do not use thumbnail** or **Enter custom size…** | Original fields for entering dimensions |
| `deny` | The card is hidden. | The card is hidden. |

The options are read from `thumbServletAllowedSizes` (separated by a comma or newline), duplicates are removed, and sorted numerically by width and height. For example, `96x96ip2` is displayed as **96px - Fixed Height (ip2)**. The mode name uses the same translation as the **Mode** field. A value without `ip` is displayed as maximum dimensions `(ip0)`. The background color is displayed in hexadecimal; POI off and quality are added to the description.

The selected combination is saved in the image URL and CSS class `fixedSize`. The quality is written with the suffix `-q90`, for example `fixedSize-730-401-5-ff00ff-true-q90`. When reopened, the corresponding option is selected. If the existing size is not in the list, in `strict` mode, a warning is displayed and an allowed size or **Do not use thumbnail** must be selected. In other modes, the custom setting with the original values ​​is opened. In `deny` mode, confirming the dialog will keep the original thumbnail.

The **Do not use thumbnail** option has an empty value and uses the original image: it removes the `/thumb/` prefix, thumbnail parameters, and `fixedSize` class.

First set the `learn` mode to learn existing values ​​and then set the `check` mode in which the values ​​are checked, but if the administrator is logged in, the new value is automatically added to the list of allowed values. WebJET automatically sets the `learn` mode during an update and then after at least a month and another restart it switches to the `check` mode.

In case of multi-node installation you can set mode `deny` and generate preview images only on admin nodes. The difference between `deny` and `check` is that `deny` does not even check the list of allowed options and is therefore faster to process. Also if the admin is logged in the image is generated.

It is important to note that the `thumbServletAllowedSizeMode` mode is only checked if the requested size is not yet generated in the `/WEB-INF/imgcache` folder, if the file already exists it is displayed regardless of the setting of this configuration variable. This is for performance reasons, as checking the allowed options is more demanding.
