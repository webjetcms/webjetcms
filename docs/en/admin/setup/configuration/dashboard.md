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
