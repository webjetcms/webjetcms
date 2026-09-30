# Configuring the home screen

You can set the welcome panel background and the environment label on the [home screen](../../../redactor/admin/welcome.md) via **Settings → Configuration**. Find the variable name, set the value, and refresh the home screen. The procedure for editing the values ​​is described in [Configuration](README.md).

## Configuration variables

| Variable | Default value | Meaning |
| --- | --- | --- |
| `dashboardHeroBackgroundImage` | `/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg` | The background image for the welcome panel. An empty or invalid value will hide the image. |
| `dashboardEnvironmentName` | `{ENVIRONMENT_NAME}` | Environment label text. Supports macros and custom text. An empty value hides the label. |
| `dashboardEnvironmentIcon` | `car` | Icon by environment or custom Tabler class `ti-*`, for example `ti-database`. |
| `dashboardEnvironmentColor` | `car` | Color according to the environment or a custom color in `#RGB` or `#RRGGBB` format. The text color is automatically selected based on contrast. |

The image can use a local path starting with `/` or a full HTTP(S) URL without credentials. Addresses starting with `//`, addresses with backslashes or control characters, and other protocols are not allowed. The maximum length of an address is 1024 characters.

## Automatic environment labeling

The macro `{ENVIRONMENT_NAME}` determines the environment based on the name of the server through which you access the administration. Changing the selected content domain does not change the designation.

The detection is not case-sensitive. It searches for entire parts of the name separated by a period, hyphen, or underscore, including the node number, such as `uat01` or `web-prod-02`. For multiple matches, the order is **PROD → UAT → INT → DEV**.

| Environment | Recognized parts of the server name | Automatic icon | Automatic color |
| --- | --- | --- | --- |
| PROD | `prod`, `fart`, `production`, `live` | `ti-server` | green `#D6F5EF` |
| UAT | `uat`, `aut`, `acc`, `acceptance`, `stage`, `staging`, `test`, `testing`, `qa`, `reprod`, `preproduction` | `ti-clipboard-check` | yellow `#FFF2C9` |
| INT | `int`, `integration`, `sit` | `ti-git-merge` | orange `#FFE0B2` |
| DEV | Other names, such as `localhost`, IP address or `iwcm.interway.sk` | `ti-code` | red `#FFD9DE` |

Pre-production also includes `pre-prod`, `pre-production` and their variants with a dot or underscore. These parts of the name are not considered PROD. A part of the word, for example `int` in `interway`, does not match the INT environment.

The automatic icon and color will preferentially use the leading `PROD`, `UAT`, `INT`, or `DEV` in the label text followed by a slash, space, hyphen, or end of text. For other custom text, they will be based on the environment detected by the server. A valid manually set icon or color takes precedence, an invalid value will use the automatic selection.

## Node name and custom label

To also display the cluster node name, set `dashboardEnvironmentName` to `{ENVIRONMENT_NAME}/{CLUSTER_NAME}`. The `{CLUSTER_NAME}` macro uses the `clusterMyNodeName` value of the current node. For example, the result might be `UAT/node-1`. If the node name is empty, the trailing slash in the label is removed, leaving `UAT`.

You can also use fixed text, such as `UAT/Školenie`, or a custom label `Školiaci server`. To hide the label, set `dashboardEnvironmentName` to empty, or `dashboardHeroBackgroundImage` to empty the background image.
