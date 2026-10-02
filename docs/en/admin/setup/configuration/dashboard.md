# Configuring the home screen and environment badge

Set the welcome panel background and environment badge via **Settings → Configuration**, then reload the page. The badge appears before the page title throughout administration v9 and on the login page. On mobile it follows the menu button. See [Configuration](README.md) for the editing procedure.

## Configuration variables

| Variable | Default value | Meaning |
| --- | --- | --- |
| `dashboardHeroBackgroundImage` | `/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg` | The background image for the welcome panel. An empty or invalid value will hide the image. |
| `dashboardEnvironmentName` | `{ENVIRONMENT_NAME}` | Label text with macro support. Displays up to 8 uppercase characters; the complete name remains in the tooltip. An empty value hides the badge and browser-title prefix, including on production. |
| `dashboardEnvironmentIcon` | `auto` | Automatic icon, a Tabler name such as `rocket` or `ti-database`, or `none`/empty to hide it. Unknown icon names display no icon. |
| `dashboardEnvironmentColor` | `auto` | Environment palette or a custom `#RGB`/`#RRGGBB` background. Custom colors receive black or white text and icons with contrast of at least 4.5:1. |
| `dashboardEnvironmentStyle` | `auto` | `subtle`, `strong`, or `auto` (strong for PROD, subtle otherwise). |
| `dashboardEnvironmentDescription` | empty | Description appended to the full name in the hover/focus tooltip. Supports macros. Escape dismisses the tooltip. |

The image can use a local path starting with `/` or a full HTTP(S) URL without credentials. Addresses starting with `//`, addresses with backslashes or control characters, and other protocols are not allowed. The maximum length of an address is 1024 characters.

## Automatic environment labeling

The macro `{ENVIRONMENT_NAME}` determines the environment based on the name of the server through which you access the administration. Changing the selected content domain does not change the designation.

The detection is not case-sensitive. It searches for entire parts of the name separated by a period, hyphen, or underscore, including the node number, such as `uat01` or `web-prod-02`. For multiple matches, the order is **PROD → UAT → CIT → INT → TEST → DEMO → LOCAL → DEV**.

| Environment | Recognized server name tokens | Automatic icon | Subtle background / text | Strong background / text |
| --- | --- | --- | --- | --- |
| PROD | `prod`, `prd`, `production`, `live` | `ti-alert-triangle` | `#FFD6D7` / `#790011` | `#C4001F` / `#FFFFFF` |
| TEST | `test`, `testing`, `qa` | `ti-test-pipe` | `#FFE0CC` / `#7A2E00` | `#C24E00` / `#FFFFFF` |
| CIT | `cit` | `ti-user-check` | `#EADFFF` / `#5200A3` | `#6E00DC` / `#FFFFFF` |
| INT | `int`, `integration`, `sit` | `ti-plug-connected` | `#DCE4FF` / `#0037A6` | `#0049BE` / `#FFFFFF` |
| UAT | `uat`, `aut`, `acc`, `acceptance`, `stage`, `staging`, `preprod`, `preproduction` | none | `#C8F0F4` / `#004F59` | `#00717F` / `#FFFFFF` |
| DEMO | `demo` | `ti-eye` | `#FFF0B3` / `#5C4300` | `#F6BE3F` / `#13151B` |
| LOCAL | `local`, `localhost`, IPv4 loopback `127.*.*.*` or IPv6 `::1` | `ti-device-laptop` | `#E6E8EE` / `#353944` | `#353944` / `#FFFFFF` |
| DEV | Other names, for example `iwcm.interway.sk` | `ti-code` | `#CFF5E4` / `#00533D` | `#007E69` / `#FFFFFF` |

Pre-production also includes `pre-prod`, `pre-production` and their dot/underscore variants; these do not match PROD. Partial words such as `int` in `interway` do not match INT. TEST is separate from UAT: `test`, `testing` and `qa` now select TEST.

Automatic appearance uses a recognized environment name at the start of the configured label, followed by a slash, space, hyphen or end of text. Custom names use the server-detected environment. Explicit icons and colors override automatic selection. Invalid colors fall back to the environment default.

## Node names and custom labels

`dashboardEnvironmentName={ENVIRONMENT_NAME}/{CLUSTER_NAME}` expands the current node's `clusterMyNodeName`. A name such as `UAT/node-1` is shown in full in the tooltip and truncated to 8 uppercase characters in the badge. An empty node name does not leave a trailing slash.

For a short label and a separate description, set `dashboardEnvironmentName=TEST` and `dashboardEnvironmentDescription=Test environment, node {CLUSTER_NAME}`. The label also prefixes the browser tab title and accessible header name, for example `[TEST] Web pages | WebJET CMS`. Configuration remains the only settings interface; no dashboard settings dialog is provided.
