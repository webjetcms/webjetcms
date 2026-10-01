---
name: wj-dashboard-widget
description: Create or modify widgets on the WebJET CMS administration home dashboard, including their settings, data providers, permissions, responsive variants, and tests. Use for dashboard widgets, not AppStore page components, PageBuilder blocks, or general DataTables applications.
---

# Dashboard Widgets

Build widgets through the shared dashboard framework. The [user guide](../../../docs/sk/redactor/admin/welcome.md) describes the dashboard's user-facing behavior. Follow the repository [design guidance](../../../.github/agents/AGENTS-design.md) before changing presentation.

## Workflow

1. Identify the existing data source and its server-side authorization and domain rules. A hidden card is not an authorization boundary.
2. Choose only useful size variants, configuration fields, and the correct singleton/multiple-instance policy.
3. Register the widget through the framework; use its shared settings, persistence, movement, removal, and loading/error handling.
4. Verify real data, empty results, denied access, invalid configuration, responsive rendering, and cleanup. Update this skill when implementation establishes a reusable rule.

Read the [widget contract](references/widget-contract.md) before implementing a widget. It describes the registration, settings API, and lifecycle in the framework source. Keep it aligned with verified code as the framework evolves.

Read the [design decisions](references/design-decisions.md) when changing shared architecture, personalization or presentation. They explain the reasons for the current behavior; widget registrations and the contract define the supported interfaces and variants.

## Invariants

- Users have mixed responsibilities. Availability follows permissions and active-domain data, not role presets. Enforce access on the server for every data provider.
- Reuse existing module REST endpoints, services and repositories for data access and authorization. If a module endpoint already supplies the required authorized data, call it directly with bounded pagination instead of adding a dashboard-specific backend provider. Prefer minimal response mapping over new SQL or module APIs.
- The layout is shared across domains and devices; record filters such as selected forms or folders belong to the active domain. Keep unavailable instances in the stored profile.
- New and reset profiles include only the explicit, permission-filtered list in `getDashboardDefaults(context)`, ending with Newsletter in the personal grid. Do not append other registered types automatically; they remain available in the catalogue. Default shortcuts require an authorized menu destination. Preserve existing personal layouts until the user explicitly resets them from the edit toolbar.
- Shift-clicking the widget reset generates one instance per authorized widget type and supported size and persists the layout atomically. Preserve shortcuts; sessions, news and search remain singletons. Grid types support multiple instances so every variant can be resized, removed and restored independently. Normal reset still uses the curated defaults.
- Persist ordered instances and named sizes, never pixel positions. Supported sizes are `1x1`, `2x2`, `2x3`, `3x2`, `3x3`, and full width with natural height. A type advertises only its useful variants.
- The grid has six logical desktop columns, four below 1200 px, two below 768 px, and one below 360 px. Keep DOM, keyboard, and mobile order aligned; do not enable dense packing.
- Keep active system alerts and session management visible outside the personal grid. Sessions, release news and search occupy fixed regions; shortcuts occupy an independent section under the greeting inside the welcome panel. Each section owns its edit controls; only widgets have a reset, which preserves shortcuts and their migration metadata. Shortcut removal and edits provide an eight-second undo notification. Legacy `collapsed` fields are ignored; every widget renders its full content at the selected size. The release-news summary toggle is independent.
- Use shared server persistence, not localStorage, for personal layouts. On dashboard load, automatically migrate legacy `localStorage["bookmarks"]` into the signed-in account once, replacing only shortcuts. Preserve the original source until a successful server save; a persisted account marker prevents repeated imports.
- Render untrusted titles, values, and configuration as text. Menu shortcuts resolve from authorized entries. Explicit custom-URL shortcuts accept only root-relative local URLs or HTTP(S) URLs without credentials, backslashes, control characters or protocol-relative forms; validate them on the client and server.
- Show arrangement and widget controls only in the explicit edit mode. Preserve focus and provide a keyboard/touch alternative to drag. Use existing Bootstrap controls, WebJET tokens, Tabler icons, and translated labels. Initialize dynamic selects through `WJ.initSelectPicker()` and destroy their picker instances when the dialog closes.
- Lists are bounded previews with a link to the complete module. Recent pages, forms, active sessions and logged administrators use native internal scrolling to preserve their entries in compact cards; use the shared `containNativeScroll` helper with abort cleanup so the administration scrollbar does not consume their gestures. Other previews use natural content height. Distinguish no results from unavailable or failed data. Never invent unread/processed states for forms.
- Label metric, period, and comparison. Default statistics to the last seven completed days. Newsletter previews use the campaign module’s existing status and sent/recipient counts.
- Use the existing AmCharts infrastructure through `window.initAmcharts()` and `ChartTools` from `libs/chart/chart-tools.js`. Provide a text/table equivalent and release chart roots on refresh, resizing, removal, and aborted asynchronous initialization.
- Grid renderers start only when their card enters the viewport. Keep data requests inside the renderer so the shared controller can defer them. Fixed utilities and shortcuts render immediately. Scrolling back to a loaded card does not refresh it; configuration/domain changes and explicit refresh wait for visibility again. Abort pending observation when the render is replaced or removed.
- Embed lightweight settings, system notices, sessions and logged administrators through `DashboardListener` and the existing Thymeleaf `overviewData` bootstrap. These data have no read REST endpoints or client fallbacks. Only include logged administrators when the user has `welcomeShowLoggedAdmins` permission. Keep confirmed settings in the page data after saving/resetting, and remove successfully logged-out sessions from the supplied list before rendering it again. Expensive previews remain lazy.
- Update only affected widgets. Do not replace the overview root or delete system alerts when settings change. Dispose request work, listeners, charts, and timers when an instance is removed or replaced.
- Live memory and CPU widgets share one request to `/admin/rest/monitoring/actual` every 5 seconds while visible. Pause polling for a hidden document or when no visible subscriber remains, avoid overlapping requests, and dispose requests/timers on removal. Newsletter polls only while visible and actively sending, every 30 seconds. Other widgets load on entry, relevant configuration/domain changes, and manual refresh.
- Use `WJ.notifySuccess` with a 10-second timeout for successful preference changes and `WJ.confirm` for reset confirmation; preserve tooltip cleanup and focus restoration.
- Validate configuration before persistence: at most 48 instances and 2000 characters per storage record, with rejection instead of truncation. On save failure restore the last confirmed state.

## Completion

Use meaningful unit and browser tests for configuration, authorization, rendering, and persistence. Build admin assets from source; do not edit generated bundles. Java changes require restarting the local application before integration verification. Leave all changes uncommitted.

Keep API details in the contract reference and link to maintained pilot implementations rather than duplicating their source code. Validate this skill with the skill-creator `quick_validate.py` after changes.
