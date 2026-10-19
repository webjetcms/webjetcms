# Central news list

The generator at `docs.webjetcms.sk` creates a list of recent YouTube videos and Facebook posts. Customer installations read the common files `wjnews.sk.json`, `wjnews.cs.json` and `wjnews.en.json` ; they do not need their own access or token for social networks. JSON preserves the original format for older versions of WebJET CMS. The news of the installed version comes from local translation keys and the dashboard adds them when viewed.

## Deploying and setting up the task

1. Create an archive with the command `ant -f ant/build.xml finalwar` and deploy `build/updatezip/webjetcms.war` to the docs server. Target `finalwar` also includes the generator classes from package `sk.iway.aceintegration.socialmedia` ; customer JARs and update ZIP archives do not include them. Restart the application after deployment.
2. Allow the application server process to write to `/json/` and `/WEB-INF/social-media/` in the root folder of the web application. The private folder must not be accessible via the web server. Input JSON files do not need to be prepared or copied during [deployment](deployment.md#zoznam-noviniek-na-docs-serveri).
3. In **Settings → Configuration** create the necessary variables from the table below. The default values ​​are defined directly in the generator package, so they may not be in the configuration list before the first save.
4. In **Settings → Automated Tasks**, create the task `sk.iway.aceintegration.socialmedia.SocialMediaFeedGenerator`. Leave the **Task Parameters** blank, set the desired times, and select one specific node in the **Runs on node** field. The task is not automatically registered during installation.
5. Run it manually on the selected node and check the outputs according to the [Verification](#verification) section. Timing and manual run control are described in [Automated Tasks](../../admin/settings/cronjob/README.md).

The paths in the configuration are relative to the root of the web application. The private data and translation cache is in `/WEB-INF/social-media/feed-cache.json` ; image previews are stored in `social-media/images/` under the output folder.

| Variable | Default value | Meaning |
| --- | --- | --- |
| `socialMediaFeedOutputDirectory` | `/json/` | Public output folder. |
| `socialMediaYoutubeChannelId` | `UCtS48g2dGJ5WVeh89PAOeLA` | A YouTube channel from which a public Atom feed is read. Does not require an API key. |
| `socialMediaFacebookPageId` | `237316516345904` | WebJET CMS page ID for Graph API, verified via `GET /me?fields=id,name` with Page token. |
| `socialMediaFacebookAccessToken` | empty | Page access token. When saving, turn on **Encrypt**. The token is only entered on the docs server. |
| `socialMediaFacebookApiVersion` | `v26.0` | The Graph API version used when reading posts. |

These variables are controlled only by the central generator. The refresh interval and loading shutdown in the customer administration are controlled by the [dashboard configuration](../../admin/setup/configuration/dashboard.md#news-from-social-networks).

## Facebook access and translations

For your own page, prepare a Meta application and a **Page access token** with `pages_read_engagement` permission. Obtaining a token and selecting a managed page also requires `pages_show_list` ; check the availability of permissions for a specific application and administrator account according to the [Meta permissions reference](https://developers.facebook.com/docs/permissions/). The generator does not publish anything and does not require write permission.

1. In [Meta for Developers](https://developers.facebook.com/apps/), create or select an app to access your own Facebook page. The account you are providing access to must have access to the app and permission to manage the page.
2. Follow the [Page access token acquisition procedure](https://developers.facebook.com/docs/pages/access-tokens/) to allow page reading and select the WebJET CMS page. Use the page token; the App access token or User access token itself is not part of the generator. The scope of permissions and required app approval depend on the deployment method and account roles.
3. With the Page token selected, verify `GET /me?fields=id,name` in Graph API Explorer. The response must indicate the correct page. Set its `id` to `socialMediaFacebookPageId` ; do not automatically consider the ID from the public address `profile.php` as a verified ID for the Graph API. Then verify the read `/{page-id}/published_posts`. When preparing for permanent operation, check the validity of the token and the renewal procedure according to the Meta documentation. Changing account or application permissions may invalidate access.
4. Save the token to the encrypted docs server configuration as per the following paragraph and run the generator manually. Verify that the public JSON contains items with `source: "facebook"` ; successful execution of the task alone does not mean success for Facebook, as the task may update other resources if it fails.

Posts are read via `GET /v26.0/{page-id}/published_posts` according to the [official reference](https://developers.facebook.com/docs/graph-api/reference/page/published_posts/). The token is sent in the authorization header. Do not include it in task parameters, URLs, source files, or public JSON. Store it in `socialMediaFacebookAccessToken` with the **Encrypt** option enabled; do not use the temporary setting. The generator will not use a token stored without encryption. After obtaining or renewing the token, verify the actual loading of your own page.

YouTube will update without a token. If there are already successfully loaded Facebook posts, they will be preserved even with a missing or invalid token. Change the API version only after verifying compatibility according to the [Graph API changelog](https://developers.facebook.com/docs/graph-api/changelog/).

Texts are considered Slovak. Translations into Czech and English are done using `TranslationService` ; set [translator](../../admin/setup/translation.md) on the docs server. The cache distinguishes between post ID, content, and target language, so unchanged texts do not consume additional translations. In case of an error, the last available translation or Slovak text is used and the next run repeats the translation. Post identity, link, date, and image remain the same in all languages.

## Format and generation process

The JSON root remains an object with a field `news`. Each item has text fields `title`, `perex`, and `link`. The new optional fields are `id`, `source`, `publishedAt`, `image`, and `perexLong`. Example of one item:

```json
{
  "title": "Nové možnosti štýlov v editore",
  "perex": "Pozrite si ukážku práce so štýlmi.",
  "perexLong": "Pozrite si ukážku práce so štýlmi. Vo videu nastavíme vzhľad nadpisov a odsekov priamo v editore stránky.",
  "link": "https://www.youtube.com/watch?v=VIDEO_ID",
  "id": "youtube:VIDEO_ID",
  "source": "youtube",
  "publishedAt": "2026-10-18T09:00:00Z",
  "image": "social-media/images/1d26cf12a6e34c59a10f82d9139af543.jpg"
}
```

The generator will include up to three of the most recent entries from each network, sorted by newest. From the twelve Facebook posts retrieved, it will first exclude videos, Reels, and shared links to YouTube videos, and then select up to three entries. It will not load any more pages of results. If no Facebook posts remain after filtering, it will include four YouTube videos, if the channel offers them. The public file contains only social posts; news releases or the content of the old public JSON are not added to it.

It recognizes videos based on [attachment data from the Graph API](https://developers.facebook.com/docs/graph-api/reference/story-attachment/), including embedded attachments, and known video URLs. Text and photo posts remain even if their text mentions the video; similarity of titles or images is not considered. A video with a recognizable link can be excluded from the cache even without a link. An older saved post without attachment data and with a regular link of `/posts/id` can only be filtered out after a successful Facebook restore. In the event of a complete outage without usable items, the public file will remain unchanged until a successful restore.

`perex` is a short text snippet with a maximum of 180 display characters to fit in the old news view. The generator will use the first complete sentence if it fits within the limit; otherwise, it will truncate the text at a word boundary and add a hyphen.

`perexLong` provides the current dashboard with a longer excerpt of up to 1,000 characters (Unicode code points), without a limit on the first sentence. When truncated, it ends at a word boundary with a ellipsis. Cards will prioritize this text; if it is missing or empty, they will use `perex`. The number of lines displayed adapts to the available card height: a shorter title makes room for more text, and the link remains bottom-aligned. The card reserves space for at least three lines of excerpt with an image and eight without it; if the image fails, the space for the text increases. The text itself is truncated by CSS with an ellipsis, the number of lines is recalculated when the dimensions change. The new field is ignored by the older client.

The generator removes URLs, hashtags, and bullet or other block tags from both excerpts, and merges lines into continuous text. The list content can remain as plain text. If the text begins with a repeated post title, it will be removed. The check will also be performed after translation; the same length limits apply to all languages. A post containing only the title has an empty excerpt to avoid displaying the same text twice. Safe character encoding can make JSON writing longer.

`image` contains the path relative to the JSON folder, for example `social-media/images/1d26cf12a6e34c59a10f82d9139af543.jpg`. Dashboard will fill it in based on the address of the loaded file created from `overviewJsonUrl`. So, with a value of `https://docs.webjetcms.sk/json/new/`, the image will be loaded from `/json/new/social-media/images/`. When moving the output folder, also transfer its subfolder with images. The same path is also used by the small preview in the collapsed rotation, which is loaded only when the corresponding item is displayed. If the preview fails, the text will remain without the image. The separate variable `socialMediaFeedPublicBaseUrl` is no longer used. The generator will also convert its own cached absolute links to relative paths; the dashboard still supports absolute HTTPS links in older lists.

The older client will only display the list when there are **at least four items**. If the sources provide less, the generator will store the actual number without filling in replacement records. The current dashboard will also display a shorter list; in the old client, external news will not be displayed in this case. The private cache also stores the fourth YouTube video for running without Facebook posts.

The old client ignores the new attributes and uses the text and field order. The generator cleans up social texts so that they do not insert HTML, images or executable links through the historical Markdown renderer. The preview belongs only to `image`. The new dashboard also supports the original items without the new attributes and will not display a link to the changelog of the currently installed version a second time. Social posts about the same version remain.

If one network fails, its last successful data is preserved and the other can be updated. In the event of a complete failure without any saved social items, existing public files remain unchanged. However, a successful response with an empty list can also produce an empty output. In the event of a preview error, the card can be text. The generator prepares and validates all language outputs before publishing; it replaces individual files atomically. Corrupted input or incomplete write must not replace valid public JSON. Concurrency is prevented by a lock; schedule the task on a single node anyway.

## Verification

- Open public `https://docs.webjetcms.sk/json/wjnews.sk.json`, `wjnews.cs.json` and `wjnews.en.json`. Verify valid JSON, actual number of social items, order and unchanged fields `title`, `perex`, `link`. Verify same IDs and links in all languages ​​and when running without Facebook, four YouTube videos if available.
- Verify that the relative paths `image` point to the folder with the JSON loaded and the images are accessible via HTTPS from the administration on the other domain. Also check the custom subfolder set up in `overviewJsonUrl` and blocking public access to `/WEB-INF/social-media/`.
- For public JSON responses and images under `/json/social-media/images/`, set up CORS so that they can be read by the customer administration. For public data without login details, `Access-Control-Allow-Origin: *` can be used. Images are loaded in `crossorigin="anonymous"` mode, so they need this header as well. Also check the source permission in the customer administration CSP (`connect-src` for JSON and `img-src` for previews).
- In the current WebJET CMS, expand **What's New** and verify that the local version news is first and the external tabs are below them. On an older version, verify the text list from the same URL.
- After setting up the Facebook token, verify live loading of posts. When rerunning, check that posts are not duplicated and that unchanged texts are not retranslated.
- Check the audit trail of the automated task execution and the log for source or translation errors. Public outputs must not contain a token, private cache, or incomplete JSON.

## Load diagnostics

The audit log of the task execution only confirms its invocation. Look for the result in the `SocialMediaFeedGenerator` class log. If the request is rejected, the log contains the HTTP status, and if there is an error, the Facebook Graph API also contains the numeric `code` and `error_subcode`, if the response provides them. The token, authorization headers, or response text are not written to the diagnostics. Verify the codes according to [Graph API error handling](https://developers.facebook.com/docs/graph-api/guides/error-handling/).

In case of a non-existent field error, the log will fill in its name if it is a field required by the generator. If it already rejects `published_posts`, verify the Page token and page ID according to the previous procedure. Correct any mismatch in the configuration; the generator does not automatically switch to another page.

The missing Facebook token message means that you need to check the `socialMediaFacebookAccessToken` configuration and its encryption. If one source fails, the task continues with the other and uses the saved posts from the last successful run. If both sources fail and the cache is empty, it will not create or overwrite the public files.
