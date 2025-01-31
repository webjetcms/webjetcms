# Generating preview images in WebJET

WebJET allows you to generate images of a specified size from any image in the folders on request `/images,/files,/shared,/video,/templates`.

## Basic generation

Let's imagine we have a picture (it doesn't have to be from a gallery, it's just a sample):

`/images/gallery/test-vela-foto/dsc04131.jpg`

![Original Image](original-image.png)

and we need to have it in max size `200x200` points. Just add a prefix before the image URL `/thumb` and add URL parameters `w` a `h` with the required dimension, that is:

`/thumb/images/gallery/test-vela-foto/dsc04131.jpg?w=200&h=200`

![Thumb Image 200x200](thumb-image.png)

The image realistic may be smaller than the required `200x200` points, depending on its aspect ratio. In this case, it was generated as `200x134` points, but always fits into the required dimension.

## Default image

By default, for a non-existent image when using `/thumb` addresses return a standard 404 error. However, if you need to display the default image for such a case, you can use the configuration variable `thumbServletMissingImg`. You can add the name of the folder and the name of the file to be used for this case to the lines. Setup example:

```
/images/gallery/test/|/images/photo3.jpg
/images/|/images/photo1.jpg
```

Setting according to the specified format for non-existent images from the folder `/images/gallery/test/` and its sub-directories displays an image `/images/photo3.jpg`. To call an image from a folder `/images/test/podadresar/` the image will be displayed `/images/photo1.jpg`, because the best match will be just with `/images` directory. When calling `/templates/meno/assets/image.jpg` a standard 404 error is displayed, as no prefix for this folder is defined in the configuration variable.

The found image goes through the process of `/thumb`, so it is generated in the specified dimension from the URL parameters.

## Restrictions

The generation of images is a burden on the server, so it is protected by SPAM protection. The following conf. variables are used:
- `spamProtectionTimeout-ThumbServlet` - time between HTTP requests, set to `-2` to turn off, as there may be multiple images on the page that are generated at the same time.
- `spamProtectionHourlyLimit-ThumbServlet` - the maximum number of images generated from one IP address per hour, set to the value by default `300`.
- `cloudCloneAllowedIps` - comma separated list of starting IP addresses for which the restriction will not be applied, empty by default (not applicable).
