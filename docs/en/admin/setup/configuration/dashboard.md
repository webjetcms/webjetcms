# Configure the splash screen and environment label

You can set the welcome panel background and environment label via **Settings → Configuration**. Refresh the page after changing it. The label is displayed before the page name in the common administration header v9 and on the login page. On mobile, it is behind the menu button. The procedure for editing the values ​​is described in [Configuration](README.md).

## Configuration variables

| Variable | Default value | Meaning |
| --- | --- | --- |
| `dashboardHeroBackgroundImage` | `/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg` | The background image for the welcome panel. An empty or invalid value will hide the image. |
| `dashboardEnvironmentName` | `{ENVIRONMENT_NAME}` | Label text. Supports macros and custom text. Displays up to 8 characters in uppercase; the full name remains in the tooltip. An empty value will also hide the label from the tab title. |
| `dashboardEnvironmentIcon` | `car` | Icon by environment or name of the Tabler icon, e.g. `rocket` or `ti-database`. A value of `none` or an empty value hides the icon. If the name does not exist, the icon will not be displayed. |
| `dashboardEnvironmentColor` | `car` | Color from the palette according to the environment and style. Custom color `#RGB` or `#RRGGBB` automatically gets black or white text and icon with a contrast of at least 4.5:1. |
| `dashboardEnvironmentStyle` | `car` | `subtle` = subtle, `strong` = strong. The value `auto` will use a strong style for PROD, and a soft style for other environments. |
| `dashboardEnvironmentDescription` | empty | Additional description after the full name in the tooltip. Supports macros. The tooltip works when hovered over with the mouse and when focused with the keyboard; Escape hides it. |
| `adminNewDeviceDetectionEnabled` | `true` | Enables administrator browser detection and blocking, dashboard notifications, and new device emails. |
| `adminNewDeviceMaxAgeDays` | `90` | The number of days since the last successful login that the browser is considered known to the account. It also determines the validity of the cookie. |

The name is also added to the browser tab title and accessible header name, for example `[TEST] Webové stránky | WebJET CMS`. The icon is optional, the text remains mandatory. In production, you can disable the label with an empty value of `dashboardEnvironmentName`.

The background image can use a local path starting with `/` or a full HTTP(S) URL without credentials. Addresses starting with `//`, addresses with backslashes or control characters, and other protocols are not allowed. The maximum length of an address is 1024 characters.

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

The function notifies the administrator of a successful login from a browser that has not used their account within the set period. The first time they log in after enabling the function, they will also receive an email notification. The user procedure is described in [System notifications](../../../redactor/admin/welcome.md#system-notifications).

The cookie `wjdevice` contains a random identifier; the table `user_login_devices` stores its hash and the time of the last login for the user. The record has an automatically generated `device_id` ; the browser searches for `user_id` and the cookie hash. A separate `domain_id` is not used because each user has their own `user_id`. The cookie has a path `/`, attributes `HttpOnly`, `SameSite=Lax` and, for HTTPS, also `Secure`. Each completed login will renew the cookie and remember the account for another `adminNewDeviceMaxAgeDays` days. Normal requests during an open session or unsuccessful logins do not extend the period. Logout will preserve the cookie. Invalid or negative configuration will use 90 days; the upper limit of cookie validity is 24,855 days.

The email is added to the existing queue via `SendMail.sendLater`. The sender's name and address are determined in the same way as for a forgotten password: first `passwordResetDefaultSenderName` and `passwordResetDefaultSenderEmail`, then `defaultSenderName` and `defaultSenderEmail`, finally the user's alternate data. The email contains the browser, system, IP address, time, server name and a label from `dashboardEnvironmentName`. A sending error is logged without canceling the login or warning in the overview.

The device also stores the last notification: time `create_date`, browser, system, IP address and confirmation or reporting status. The notification expires after confirmation or after 7 days from `create_date`. A regular login only updates `last_seen`. When logging in after expiration, the notification in the same unblocked record is restored; a separate event history is not saved. The link **It wasn't me** in the email uses `device_id` and displays the current device detail. The link **It was me** also contains a random 256-bit token valid for 24 hours; confirmation is performed by the logged-in administration via POST with CSRF protection. Opening the GET address itself does not change the status. The task `sk.iway.iwcm.users.devices.DeviceCleanup`, added by the database update, removes unblocked devices unused for `adminNewDeviceMaxAgeDays` days every day at 03:41 along with their notifications. After that, the detail from the email is no longer available. Set the value `adminNewDeviceMaxAgeDays` the same for all domains; cleaning continues even when detection is disabled.

The device and notification records are managed by `DeviceService` via the `DeviceEntity` entity and the Spring Data repository `DeviceRepository`. The service returns the entity directly; the `@JsonIgnore` annotation hides the internal data when sending it to the frontend. Times are sent in milliseconds and `expiresAt` is calculated from `createDate`, without any additional database column. This layer is not tied to the administrator. `AdminDeviceService` provides integration with administrator login, cookies, and email notifications; `AdminDeviceRestController` provides actions available to the logged in administrator.

Both the notification and active login list confirmation require a six-digit code delivered to the email account. It is valid for 10 minutes and allows 5 attempts; resending is limited to one code per minute per device. The database stores only SHA-256 fingerprints of the code and token, separated by account, device, and purpose. Authentication data is not sent in public JSON. Confirmation, reporting, and resetting the notification will invalidate both authentication data. Resending the code will not reset the number of attempts until the previous validity period expires. After successful two-factor authentication in WebJET CMS, an unblocked device is automatically confirmed. For a new device, an informational email with the option to log in is sent.

Clicking **Block Device** sets the existing field to `reported_at`, invalidates the authentication data, and logs out known sessions for that device through the normal logout mechanism. Blocked records are not removed during cleanup. The **It wasn't me** link in the email opens the detail after logging in; opening the link itself does not lock the device.

The next interactive login of the blocked browser will first verify the login details and any 2FA. Then the `/admin/logon/device/` page will request a six-digit code from the email. The pending identity is in a separate session attribute `adminUser_waitingForDevice`, outside of the `USER_KEY` and Spring Security context. The challenge is valid for 15 minutes, the code for 10 minutes, a limit of 5 attempts and a one-minute sending interval apply. The code is also bound to this challenge and cookie. The correct code will unlock and confirm the device. The old confirmation link cannot unblock it. If the code is not delivered, access remains closed.

The check applies to admin logins via form, access key, OAuth2, and NTLM, including admin logins via the user zone. It does not apply to API tokens, HTTP Basic, or regular visitors. Setting `adminNewDeviceDetectionEnabled=false` also disables blocking checking. Test browser exceptions do not bypass the existing block.

Blocking is tied to the `wjdevice` cookie, not the physical device. After it is deleted or expires, the browser behaves as new; blocking therefore does not replace changing a compromised password or 2FA.

### Exceptions for automated tests

E2E tests often start with a new browser profile without cookies `wjdevice`. Each such login therefore creates a new device. Exceptions for test browsers are stored directly in `AdminDeviceService` in immutable sets `IGNORED_DEVICE_DOMAINS` and `IGNORED_DEVICE_USER_AGENTS`. They cannot be changed through configuration or at runtime, modification requires code change and redeployment.
