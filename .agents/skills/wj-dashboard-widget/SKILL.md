---
name: wj-dashboard-widget
description: Create or modify widgets on the WebJET CMS administration home dashboard, including their settings, data providers, permissions, responsive variants, and tests. Use for dashboard widgets, not AppStore page components, PageBuilder blocks, or general DataTables applications.
---

# Dashboard Widgets

Build widgets through the shared dashboard framework. Read the approved [implementation plan](../../../../docs/sk/developer/admin/dashboard-widgets-plan.md) for the catalogue and product scope, and the repository design guidance before changing presentation.

## Workflow

1. Identify the existing data source and its server-side authorization and domain rules. A hidden card is not an authorization boundary.
2. Choose only useful size variants, configuration fields, and the correct singleton/multiple-instance policy.
3. Register the widget through the framework; use its shared settings, persistence, movement, collapse, removal, and loading/error handling.
4. Verify real data, empty results, denied access, invalid configuration, responsive rendering, and cleanup. Update this skill when implementation establishes a reusable rule.

Read the [widget contract](references/widget-contract.md) before implementing a widget. It describes the registration, settings API, and lifecycle in the framework source. Keep it aligned with verified code as the framework evolves.

## Invariants

- Users have mixed responsibilities. Availability follows permissions and active-domain data, not role presets. Enforce access on the server for every data provider.
- The layout is shared across domains and devices; record filters such as selected forms or folders belong to the active domain. Keep unavailable instances in the stored profile.
- New and reset profiles include every available widget type in the curated default layout. Preserve existing personal layouts until the user explicitly resets them through the catalogue.
- Persist ordered instances and named sizes, never pixel positions. Supported sizes are `1x1`, `2x2`, `2x3`, `3x2`, `3x3`, and full width with natural height. A type advertises only its useful variants.
- The grid has six logical desktop columns, four below 1200 px, two below 768 px, and one below 360 px. Keep DOM, keyboard, and mobile order aligned; do not enable dense packing.
- Widget preferences must not remove active system alerts or the mandatory session-management widget. A collapsed session widget must retain access to session management.
- Use shared server persistence. Never put personal layout in localStorage or silently migrate browser-global legacy bookmarks into an account.
- Render untrusted titles, values, and configuration as text. Resolve shortcut targets from the authorized menu; do not accept arbitrary executable URLs.
- Preserve focus and provide a keyboard/touch alternative to drag. Use existing Bootstrap controls, WebJET tokens, Tabler icons, and translated labels.
- Lists are previews with a link to the complete module, without nested scrolling. Distinguish no results from unavailable or failed data. Never invent unread/processed states for forms.
- Label metric, period, and comparison. Default statistics to the last seven completed days. Newsletter openings are recorded observations, not complete recipient counts.
- Use the existing AmCharts infrastructure through `window.initAmcharts()` and `ChartTools` from `libs/chart/chart-tools.js`. Provide a text/table equivalent and release chart roots on refresh, collapse, removal, and aborted asynchronous initialization.
- Update only affected widgets. Do not replace the overview root or delete system alerts when settings change. Dispose request work, listeners, charts, and timers when an instance is removed or replaced.
- Poll only visible, actively sending newsletter widgets, every 30 seconds. Other widgets load on entry, relevant configuration/domain changes, and manual refresh.
- Validate configuration before persistence: at most 32 instances and 2000 characters per storage record, with rejection instead of truncation. On save failure restore the last confirmed state.

## Completion

Use meaningful unit and browser tests for configuration, authorization, rendering, and persistence. Build admin assets from source; do not edit generated bundles. Java changes require restarting the local application before integration verification. Leave all changes uncommitted.

Keep API details in the contract reference and link to maintained pilot implementations rather than duplicating their source code. Validate this skill with the skill-creator `quick_validate.py` after changes.
