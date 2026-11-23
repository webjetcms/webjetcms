# Configure the splash screen and environment label

You can set the welcome panel background and environment label via **Settings → Configuration**. Refresh the page after changing it. The label is displayed before the page name in the common administration header v9 and on the login page. On mobile, it is behind the menu button. The procedure for editing the values ​​is described in [Configuration](README.md).

## Configuration variables

| Variable | Default value | Meaning |
| --- | --- | --- |
| `dashboardHeroBackgroundImage` | `linear-gradient(111.7deg, #031D46 0%, #073780 76.92%)` | Welcome bar background: CSS gradient or image path. Empty or invalid value will use the default blue gradient. |
| `dashboardSocialRefreshHours` | `8` | Number of hours the saved external news list is valid. A value of `0` or a negative value disables its loading and display of saved items. Expanding the panel with the function enabled always verifies the current list. |
| `overviewJsonUrl` | `https://docs.webjetcms.sk/json/` | The base URL for the `wjnews.sk.json`, `wjnews.cs.json` and `wjnews.en.json` files. You can set your own HTTP(S) server or a local path to the folder with these files. |
| `dashboardEnvironmentName` | `{ENVIRONMENT_NAME}` | Label text. Supports macros and custom text. Displays up to 8 characters in uppercase; the full name remains in the tooltip. An empty value will also hide the label from the tab title. |
| `dashboardEnvironmentIcon` | `car` | Icon by environment or name of the Tabler icon, e.g. `rocket` or `ti-database`. A value of `none` or an empty value hides the icon. If the name does not exist, the icon will not be displayed. |
| `dashboardEnvironmentColor` | `car` | Color from the palette according to the environment and style. Custom color `#RGB` or `#RRGGBB` automatically gets black or white text and icon with a contrast of at least 4.5:1. |
| `dashboardEnvironmentStyle` | `car` | `subtle` = subtle, `strong` = strong. The value `auto` will use a strong style for PROD, and a soft style for other environments. |
| `dashboardEnvironmentDescription` | empty | Additional description after the full name in the tooltip. Supports macros. The tooltip works when hovered over with the mouse and when focused with the keyboard; Escape hides it. |
| `adminLoginLocationApiKey` | empty | Detecting login orientation location by IP address: empty value will use free API IPWHOIS, `DISABLED` will disable detection, API key will use paid API. The key is visible to visitors of the login page. Details are described in [Login orientation location](#login-orientation-location). |
| `adminNewDeviceDetectionEnabled` | `true` | Enables recognition and blocking of admin browsers, notifications on the overview and emails about new devices. A value of `false` also disables checking for blocked devices. Location detection is handled separately by `adminLoginLocationApiKey`. |
| `adminNewDeviceMaxAgeDays` | `90` | The number of days since the last successful login that the browser is considered known to the account. Also determines the cookie expiration and the period for cleaning unused, unblocked devices. An invalid or non-positive value will use 90 days. |

The name is also added to the browser tab title and accessible header name, for example `[TEST] Webové stránky | WebJET CMS`. The icon is optional, the text remains mandatory. In production, you can disable the label with an empty value of `dashboardEnvironmentName`.

You can change the background directly with the value `dashboardHeroBackgroundImage`, for example:

- `linear-gradient(135deg, #102A43 0%, #486581 100%)` – custom colors and gradient direction. Radial, tapered, and repeating CSS gradients are also supported.
- `/images/company/header.jpg` – image from the website.
- `https://cdn.example.com/header.jpg` – image from full HTTP(S) address.

The image will replace the gradient and cover the entire panel, maintaining the aspect ratio (`cover`), aligned to the center and bottom edge. Enter the path directly, without the `url(...)` wrapper. The HTTP(S) address must not contain login information. Addresses starting with `//`, addresses with backslashes or control characters, and other protocols are not allowed. The maximum length of the address is 1024 characters.

## News from social networks

The **What's New** panel combines the news of the installed version with posts from YouTube and Facebook of WebJET CMS. The browser reads the common JSON from the server configured in `overviewJsonUrl` ; the customer server does not contact the social networks. Data generation on the docs server is described in [Central News List](../../../developer/install/social-media-feed.md).

JSON must be publicly accessible without login, even with a custom local path: the request does not send login cookies or the `Referer` header.

The relative path of the image in the field `image` is filled in according to the JSON folder. For example, with `overviewJsonUrl=https://docs.webjetcms.sk/json/new/`, `social-media/images/obrazok.jpg` is loaded from `https://docs.webjetcms.sk/json/new/social-media/images/obrazok.jpg`. Therefore, when moving the list, also transfer the subfolder with images. Absolute HTTPS addresses of images from older lists remain supported.

When opening the home screen, the list stored in `localStorage` is used. If it is missing or has reached the expiration date set in `dashboardSocialRefreshHours` (default 8 hours), it is refreshed in the background. The list is also verified before this period expires each time it is expanded. The cache is stored under the key `webjet.externalNews.v1` and also contains the URL of the loaded list. When the URL or language is changed, the corresponding list is loaded and this item is overwritten; the Czech administration uses the file `wjnews.cs.json`. In the collapsed rotation, only the preview of the currently displayed news item is loaded; other images are not loaded in advance. Card images start loading after the panel is expanded. The preview in the rotation does not change the reading status.

The **New** label remains displayed until the user expands the unread content. Expanding confirms both local news and the social items currently being viewed. A successful save to account settings includes a read date of `readAt` ; for social posts, their ID fingerprints are also stored under the key `dashboard.externalNews.<odtlačok základnej URL>`. Switching languages ​​or editing the translation of the same post will not create a new notification. The new ID will create one regardless of the publication date. Data received after collapse or in a hidden browser tab will not be marked as read; if the panel is still expanded, it will be marked when returning to the tab.

In the event of a loading error, external tabs, rotation items, and their notification will be hidden even if the browser has older data stored. The panel will not display a loading or error message; local version news and social media links will remain. The stored cache will not be cleared by the error.

The value `dashboardSocialRefreshHours=0` disables JSON and image requests, external tabs, their rotation, and unread item notifications. This also applies to data that the browser has already saved. News from the installed version and links to both social channels will remain available. Refresh the administration page after changing the configuration.

## Automatic environment labeling

The macro `{ENVIRONMENT_NAME}` determines the environment based on the name of the server through which you access the administration. Changing the selected content domain does not change the designation.

Detection is not case sensitive. It searches for entire parts of the name separated by a period, hyphen, or underscore, including the node number, such as `uat01` or `web-prod-02`. For multiple matches, the order is **PROD → UAT → CIT → INT → TEST → DEMO → LOCAL → DEV**.

| Environment | Recognized parts of the server name | Automatic icon | Subtle color/text | Bold color/text |
| --- | --- | --- | --- | --- |
| PROD | `prod`, `fart`, `production`, `live` | `ti-alert-triangle` | `#FFD6D7` / `#790011` | `#C4001F` / `#FFFFFF` |
| TEST | `test`, `testing`, `qa` | `ti-test-pipe` | `#FFE0CC` / `#7A2E00` | `#C24E00` / `#FFFFFF` |
| CIT | `feeling` | `ti-user-check` | `#EADFFF` / `#5200A3` | `#6E00DC` / `#FFFFFF` |
| INT | `int`, `integration`, `sit` | `ti-plug-connected` | `#DCE4FF` / `#0037A6` | `#0049BE` / `#FFFFFF` |
| UAT | `uat`, `aut`, `acc`, `acceptance`, `stage`, `staging`, `preprod`, `preproduction` | without icon | `#C8F0F4` / `#004F59` | `#00717F` / `#FFFFFF` |
| DEMO | `demo` | `ti-eye` | `#FFF0B3` / `#5C4300` | `#F6BE3F` / `#13151B` |
| LOCAL | `local`, `localhost`, IPv4 loopback `127.*.*.*` or IPv6 `::1` | `ti-device-laptop` | `#E6E8EE` / `#353944` | `#353944` / `#FFFFFF` |
| DEV | Other names, for example `iwcm.interway.sk` | `ti-code` | `#CFF5E4` / `#00533D` | `#007E69` / `#FFFFFF` |

Pre-production also includes `pre-prod`, `pre-production`, and their dot or underscore variants. These parts of the name are not considered PROD. A part of the word, such as `int` in `interway`, does not match the INT environment. TEST is a separate environment; the names `test`, `testing`, and `qa` no longer indicate UAT.

The automatic appearance will preferentially use the initial environment tag in the configured text, followed by a slash, space, hyphen, or end of text. For other custom text, it is based on server detection. A manually set icon or color takes precedence. If the color is invalid, the automatic selection will be used.

## Node name and custom label

For example, `dashboardEnvironmentName={ENVIRONMENT_NAME}/{CLUSTER_NAME}` will complete the name of the current node from `clusterMyNodeName`. The full name, for example `UAT/node-1`, is in the tooltip; the label will display the first 8 characters in uppercase. For an empty node name, the trailing slash is removed.

For a short label and a longer description, set `dashboardEnvironmentName=TEST` and `dashboardEnvironmentDescription=Testovacie prostredie, uzol {CLUSTER_NAME}`. The environment setup uses the existing configuration; a separate dialog is not available on the splash screen.

## Detection of new devices

**You can disable detection by setting `adminNewDeviceDetectionEnabled=false`** in **Settings → Configuration**. This disables the detection of new browsers, notifications on the overview, emails about new devices, and checking for already blocked devices. The default value is `true`. Location detection has a [separate setting](#orientational-location-logins).

When detection is enabled, WebJET CMS notifies the administrator of a successful login from an unknown browser. The notification will also be received the first time the feature is enabled. The procedure for confirming or blocking the device is described in [System notifications](../../../redactor/admin/welcome.md#system-notifications). Detection does not apply to regular visitors, API tokens, or HTTP Basic.

The browser is recognized using the `wjdevice` cookie. The `adminNewDeviceMaxAgeDays` variable determines the retention period from the last successful login, by default **90 days**. Set it the same for all domains. After this period, unused, unblocked devices, even with notifications, are automatically deleted; cleaning continues even when detection is disabled. Blocked devices are not deleted.

**Verify email delivery and correctness of administrator email addresses.** The sender is set the same as for password recovery: `passwordResetDefaultSenderName` and `passwordResetDefaultSenderEmail`, or general `defaultSenderName` and `defaultSenderEmail`. Unblocking the browser requires a code delivered to the email account, even if the user uses 2FA. If you have problems with access, use the `/admin/logon/` form in the given browser and complete email verification.

After deleting or expiring cookies, the browser is considered new and its previous blocking will not apply. Therefore, if you suspect account misuse, you should change your password and enable 2FA.

### Approximate login location

Location detection works independently of device detection. **To disable external calls, set `adminLoginLocationApiKey=DISABLED`** ; already saved locations will be retained.

The variable `adminLoginLocationApiKey` has the following options:

- Empty value (default): free API service [IPWHOIS](https://ipwhois.io/documentation).
- `DISABLED`: location detection disabled.
- API key: paid API. **The key is visible to visitors to the login page** because the service is called by their browser.

The location is indicative and may correspond to a company headquarters or VPN gateway. It is not used for authentication or blocking devices. Its unavailability does not prevent login.

The service provider sees the public IP address and domain of the CMS. If your organization's policies do not allow such external calls, disable discovery; the provider's [privacy policy](https://ipwhois.io/privacy) lists the terms of data processing. For restricted CSP, enable the endpoint `https://ipwho.is` or `https://ipwhois.pro` used in `connect-src`.

### Release news

Write the text `admin.overview.changelog` as a Markdown title with a version and a list of points. Each point begins with a bold title, followed by a dash and a description. In the file `.properties`, use the separator `\\n` ; the translation key editor can include regular line breaks.

```markdown
## WebJET CMS 2026.18

- **Schvaľovanie zmien priečinkov** – Zmeny priečinkov môžu prejsť viacúrovňovým schvaľovaním.
- **Testovanie prístupnosti** – Kontroly sú súčasťou automatizovaných testov.
```

The number in the **What's New** link is derived from the list. Older text in paragraphs remains readable. In `user_settings_admin`, a record `dashboard.news.<jazyk>` is kept for each user and language with a fingerprint of the text read (`fingerprint`) and the date read (`readAt`). It is saved when the visible panel is expanded. Changing the text will restore the **New** label to the same version; the notification does not expire after a timeout. The Help menu reads the same local news status, but opening it does not change it. Saving or resetting the dashboard layout does not change this record.

The older entry with `firstSeen` only indicated the first view and did not have read information. Therefore, after the update, local news items will appear as unread until the user expands them once. Existing stored read social post IDs remain valid.
