# Videos

Add engaging video content from YouTube, Vimeo, Facebook, or uploaded mp4 files to your site. With the ability to adjust the size and playback properties, you can captivate and engage your visitors.

## Application settings

### Video source

In this section you can select from the available video sources:

- YouTube
- Vimeo
- Facebook
- Video on the server

![](editor-source.png)

### YouTube

YouTube video parameters:

- **YouTube page address**, simply insert the link to the website with the video
- Fixed size (in points)
  - Width
  - Height
- Responsive size (in percentage)
  - Width (%)
- Video aspect ratio - select the aspect ratio for responsive video display (e.g. 16:9, 4:3, 1:1, 21:9, 9:16)
- Play video after page loads
- Show video title
- Show YouTube logo
- Show option to go full screen
- Show control icons
- Show similar videos after playing

![](editor-youtube.png)

### Vimeo

Vimeo video parameters:

- **Vimeo page address**, simply insert the link to the video website
- Fixed size (in points)
  - Width
  - Height
- Responsive size (in percentage)
  - Width (%)
- Video aspect ratio - select the aspect ratio for responsive video display (e.g. 16:9, 4:3, 1:1, 21:9, 9:16)
- Play video after page loads
- Show video title
- Show author's text on the video
- Show option to go full screen
- Show author's photo on video
- Enable watermark display on video

![](editor-vimeo.png)

### Facebook

Facebook video parameters:

- **Address of the video page on facebook.com**, simply insert the link to the video web page
- Fixed size (in points)
  - Width
- Responsive size (full width of the block)
- Video aspect ratio - select the aspect ratio for responsive video display (e.g. 16:9, 4:3, 1:1, 21:9, 9:16)
- Play video after page loads
- Show video title
- Show author's text on the video
- Show option to go full screen

![](editor-facebook.png)

### Videos

Server video parameters:

- **Location of video file on server**, selecting video using file explorer (direct entry of file path is also supported)
- Fixed size (in points)
  - Width
  - Height
- Responsive size (in percentage)
  - Width (%)
- Video aspect ratio - select the aspect ratio for responsive video display (e.g. 16:9, 4:3, 1:1, 21:9, 9:16)

![](editor-video.png)

## View the application

![](video.png)

## Configuration

Video display management can be customized with the following configuration variables:

| Variable | Description | Default value |
| --- | --- | --- |
| `videoClasses` | Comma-separated list of available aspect ratios in the editor. Format: `translation_key:css_classes` or just `css_classes`. The first item is the default value. | 16:9, 4:3, 1:1, 21:9, 9:16 (Bootstrap 4+5 classes) |
| `videoWrapperClass` | CSS class for the video wrapper element. For Bootstrap, use `embed-responsive`. | `embed-responsive` |
| `videoItemClass` | CSS class for inner `<iframe> ` video element. For Bootstrap use ` embed-responsive-item`. | `embed-responsive-item` |

Example of changing to custom CSS classes (without Bootstrap):

```txt
videoClasses=components.video_player.ratio-16x9:video-wrapper-16x9,components.video_player.ratio-4x3:video-wrapper-4x3
videoWrapperClass=video-wrapper
videoItemClass=video-item
```

If you need to set a CSS class for both the wrapper and the `iframe` video element itself, you can split the CSS classes with the `|` character, the value before this character will be used for the wrapper and the value after the character will be used for the `iframe` element. Example:

```txt
videoClasses=components.video_player.ratio-16x9:video-wrapper-16x9|video-iframe-16x9,components.video_player.ratio-4x3:video-wrapper-4x3|video-iframe-4x3
videoWrapperClass=video-wrapper
videoItemClass=video-item
```