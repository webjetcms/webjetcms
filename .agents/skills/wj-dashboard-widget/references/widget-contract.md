# Dashboard widget contract

The framework source is in `src/main/webapp/admin/v9/src/js/dashboard/` (paths in this reference are repository-relative). Read `registry.js`, `dashboard.js`, and `model.js` when extending shared behavior.

## Registration

Import `registerWidget` from `./registry` and register each type once. Definitions contain:

- `type`: stable identifier, also allowlisted with its sizes by the settings backend.
- `titleKey`, optional `descriptionKey`: translation keys ending in `.js`; use the `admin.dashboard` namespace.
- `icon`: a Tabler `ti-*` class.
- `sizes`: subset of `1x1`, `2x2`, `2x3`, `3x2`, `3x3`, `fullauto`; `defaultSize` must belong to it.
- `multiple`, `mandatory`: false by default. Mandatory widgets cannot be removed.
- `defaultOptions`, `defaultDomainOptions`: shared and active-domain preferences, respectively.
- `isAvailable(context)`: presentation eligibility; data authorization remains server-side.
- Optional `isVisible(instance, context)` hides acknowledged content without removing the stored instance. A hidden singleton may expose `reveal(instance, context)` so the catalogue can show it again. Do not use this for permission checks or fixed security/release regions.
- Optional `getTitle(instance, context)` for a text-only instance title.
- `render(args)`, optional `renderCollapsed(args)`, optional `configure(args)`.

`render` receives `{container, instance, options, domainOptions, context, signal, refresh, saveOptions}`. Append safe DOM nodes to `container`. `context` contains `data`, `labels`, `config`, `overview`, `dashboard`, `settings`, and `translate(key)`. Use `signal` with asynchronous requests. Return a cleanup function or `{destroy()}` when owning resources; asynchronous returns are supported. The framework aborts and cleans up stale renders, shows failures, and provides retry controls.

`configure` receives the same data context and an abort signal, without refresh/save callbacks. Add labeled controls to its container and return `{read: () => ({options, domainOptions}), destroy?}`. Read may reject invalid input with a localized Error. The framework owns the size picker, modal, save/cancel, focus, and atomic persistence.

Use `saveOptions({options, domainOptions})` for an in-widget preference action. It returns a Promise<boolean>; do not display successful persistence until true. Use `refresh()` to reload only this instance.

`context.dashboard.showAddWidget(type)` opens configuration before a new instance is persisted; canceling leaves the profile unchanged. Use this for the shortcut strip add action.

`context.dashboard.showDialog(title)` opens a shared modal and returns `{body, footer, close, signal, setCleanup}`. Use it for a full preview such as all active sessions. Bind requests to its signal and let the shared modal restore focus and dispose resources.

## Settings API

`GET /admin/rest/dashboard/settings` reads fresh account preferences. `PUT` saves and returns:

```json
{
  "version": 1,
  "configured": true,
  "items": [
    {"id": "security", "type": "sessions", "size": "2x3", "collapsed": false, "options": {}},
    {"id": "stable-id", "type": "shortcut", "size": "1x1", "collapsed": false, "options": {"source": "menu", "href": "/apps/form/admin/"}}
  ],
  "domainOptions": {"stable-id": {}},
  "acknowledgedNewsVersion": null
}
```

The server derives user and active domain. Never send a user ID or arbitrary domain selector. The server validates allowed types, sizes, singleton rules, identifiers, and storage limits. Shared layout records and active-domain filters are committed together; failed saves leave the confirmed UI state unchanged.

`getDashboardDefaults(context)` supplies useful available types for an unconfigured profile: traffic `3x3`, forms/approvals/errors `1x1`, recent pages and referrers `3x3`, publishing `2x3`, newsletter `2x2`, search terms `2x3`, then top pages `3x3`. Sessions, news and search render in fixed regions above the personal grid; shortcuts render in their own strip. Fixed-region display does not rewrite stored sizes/order or collapse preferences. Default shortcuts select authorized pages/forms destinations, falling back to the first authorized menu destination; no placeholder is created when the menu has no destination. Additional types fall back to their registered default size. New optional widget types must not rewrite existing personal layouts. Mandatory types are ensured separately.

The controller starts in view mode. `setEditing(true)` reveals add-widget, movement and settings controls without persistence. Moving cards stays within the personal grid or shortcut strip; fixed regions cannot be moved. Keyboard/touch movement uses the same region restrictions as drag.

`DELETE /admin/rest/dashboard/settings` takes no body and resets the authenticated user's shared layout, widget options in **all domains**, and news acknowledgement. It uses the same per-user database lock and transaction as PUT, returns a clean `configured:false` DTO only after commit, and clears the account's admin-settings cache. It preserves every non-dashboard preference and legacy bookmark. The catalogue explains this scope and asks for confirmation. On success the controller applies current defaults; they remain unconfigured until the first personal edit. On failure retain the previous layout, filters, news acknowledgement and removal undo. Never implement reset as a sequence of partial PUT requests or by clearing localStorage.

## Data and rendering

`widget-utils.js` provides safe DOM, URL, field, table, formatting, and fetch helpers. Do not use arbitrary options as HTML or icon classes. Shortcut options are `{source: "menu" | "url", href, title}`. Omitted `source` preserves legacy menu behavior; menu destinations must resolve against the current authorized menu. Explicit URL mode requires a nonempty title and validates a maximum 1024-character root-relative or HTTP(S) URL, rejecting credentials, backslashes, controls and protocol-relative forms. Link titles are always text. `shortcutUrl` mirrors server validation before persistence.

Data providers are under `/admin/rest/dashboard/data/{type}`; the recent-pages pilot uses `GET /admin/rest/dashboard/recent-pages`. Request only the widget's necessary projection with its abort signal. Every `/admin/rest/` fetch, including GET, must send `X-CSRF-Token: window.csrfToken`: the repository PathFilter enforces this independently of the HTTP method. jQuery's global AJAX setup does not apply to fetch. Return empty states for successful empty results and let actual failures reach the framework. Never convert an access error to a zero count.

The pilot registrations are in `src/main/webapp/admin/v9/src/js/dashboard/widgets.js`: `shortcut` demonstrates authorized menu/custom URL selection, safe links and shared options; `recent-pages` demonstrates two list sizes, an abortable authorized provider and 38-pixel local perex thumbnails through `/thumb` at 76 pixels with lazy loading and a file-icon fallback. Show the real section and edit date supplied by the provider; localized `saveDate` strings remain verbatim rather than being reparsed as JavaScript dates. Their behavior tests are in `src/test/webapp/helpers/dashboard-widgets.test.js`.

Build request parameters explicitly from allowed fields. Shared `options.days` describes the period; a record selection such as `domainOptions.formName` is domain-specific and takes precedence over any similarly named shared value. If a stored selection is no longer accessible, retain its preference and show an unavailable state; do not silently select the first record or clear the user's selection.

When adding a type, update frontend registration, backend settings validation, translations, relevant data authorization, tests, and this reference where a new reusable pattern is introduced. Add an example link only after the referenced implementation exists.

## Projection examples and errors

The provider is `src/main/java/sk/iway/iwcm/components/welcome/DashboardWidgetDataService.java`, routed by `DashboardWidgetDataRestController`. Allowed query parameters include `days` (7, 30, or 90), traffic `metric` (`sessions`, `views`, or `uniqueUsers`), and optional `formName` or positive `campaignId` domain selections. Do not pass layout IDs or client domain IDs as authorization context.

For example, `fetchData("forms", {days: 7, formName: domainOptions.formName}, signal)` returns:

```json
{
  "total": 1,
  "from": 1789776000000,
  "to": 1790380799999,
  "items": [{"id": "42", "title": "Contact", "date": 1790000000000, "url": "/apps/form/admin/detail/?formName=Contact"}],
  "options": [{"id": "Contact", "title": "Contact"}]
}
```

`from`/`to` and item dates are epoch milliseconds; display the returned inclusive interval. `items` is a bounded preview; `total` counts the whole authorized result. Form `options` are the authorized form choices. Load unfiltered form options for the settings dialog and retain a missing saved choice with an unavailable label. Actual submissions use their real submission dates; they have no fabricated unread/processed state. Form periods include today so new submissions are immediately visible; the completed-day rule belongs to analytics widgets. Use the provider's grouped latest-form query instead of one query per form or a correlated scan over all historical submissions.

Traffic adds `previous`, `series`, and `previousSeries`, each point containing `date` and `value`. Previous points retain their real dates and align by index for chart comparison. Unique visitors are distinct across the full period, never a sum of daily unique counts. A zero comparison baseline has no percentage growth. The 404 provider labels its whole-week coverage; historical data that cannot distinguish domain or year is unavailable rather than a misleading zero.

Charts use `src/main/webapp/admin/v9/src/js/dashboard/charts.js`: create a uniquely identified host with `chartHost`, then call `mountChart` with the render signal and a factory for `ChartTools.LineChartForm` or `BarChartForm`. The helper loads the existing bundle with `window.initAmcharts()`, creates the chart through `ChartTools.createAmchart`, and returns paired `ChartTools.destroyChart` cleanup. Pass that cleanup back from the widget renderer. Do not reuse a host ID across asynchronous renders or keep an AmCharts root after abort. `data-widgets.js` demonstrates equal-period lines with real dates retained in tooltips, horizontal referrer bars, and a translated `details` table containing precise values. Dashboard previews omit the full-report scrollbars and zoom controls; the existing WebJET theme and locale remain shared with other modules. `charts.js` reads `--wj-dashboard-chart-primary`, `--wj-dashboard-chart-comparison`, `--wj-dashboard-chart-grid` and `--wj-dashboard-chart-label` from the chart host at creation. Define their colors as SASS tokens exported to CSS custom properties, without hard-coded JavaScript palettes.

Newsletter returns campaign `options`, `selectedId`, `active`, and items with `status`, `sent`, `recipients`, `failed`, `opens`, and `clicks`. The selected campaign is first. Automatic selection prioritizes an active campaign, the next scheduled campaign, then the last completed campaign. Only an active, visible card polls. The implementation in `data-widgets.js` demonstrates an IntersectionObserver and timer with paired cleanup.

The provider uses 400 for invalid parameters, 403 for denied module access, 404 for unavailable record selections, and 503 for unavailable data. `fetchData` converts JSON `reason` into `error.dashboardReason`, which the shell translates. In particular, `domain-unavailable` and `period-unavailable` explain limitations of historical 404 records. Do not catch provider errors and return an empty array or zero.

`utility-widgets.js` demonstrates mandatory session management, shared modal cleanup, version-specific announcement acknowledgement, and explicit search scope. Session removal must inspect the endpoint's `success` result as well as HTTP status; a 200 response can still report failure. A successful response with `pending: true` means the verified remote session is awaiting cluster synchronization; show that state instead of claiming immediate logout. The current session remains first in the compact preview and cannot be removed through this endpoint. Its fixed header region always retains every active session and the management action, including profiles with a previously collapsed session widget. A bounded scroll area in this security region preserves access to longer session lists.

Release announcements arrive through WebJET's Markdown renderer, which may represent paragraph boundaries as `<br><br>`. Preserve those boundaries when extracting safe text. The expanded release shows up to two highlights; acknowledgment of its version collapses it to a visible one-line summary and a more-info toggle, rather than hiding the release. An updated version expands again. Preserve the last confirmed presentation if acknowledgment fails.

## Persistence and verification

`DashboardSettingsService` validates the schema and `DashboardSettingsRepository` writes the existing `user_settings_admin` table atomically. The MariaDB/MySQL table must use InnoDB; the targeted migration is in `src/main/webapp/WEB-INF/sql/autoupdate-webjet9.xml`. Do not bypass the repository guard or introduce a separate preference store. The REST DTO uses ordinary maps/lists, compatible with the application's Jackson 3 response converter.

The shared records are `overview.layout.v1`, `overview.widget.<id>`, and `overview.news`; domain records are `overview.domain.<rootGroupId>.<id>`. Immediate removal undo retains the removed instance's other-domain selections until the next successful edit. A failed save must retain the undo opportunity. Unavailable widget types remain in the stored profile.

Run the frontend framework and widget behavior tests (`src/test/webapp/helpers/dashboard-*.test.js`), relevant `Dashboard*Test` Java tests, and the Codecept scenarios in `src/test/webapp/tests/admin/dashboard-widgets.js`, `src/test/webapp/tests/admin/dashboard-data.js`, and `src/test/webapp/tests/admin/dashboard-catalogue.js`. Match verification to the changed provider, permissions, variants, and resource lifecycle. The data scenarios are read-only; the interaction scenarios restore the test account's effective settings. Test security operations only against sessions created by the test. Never invoke the feedback email scenario as part of a dashboard smoke test.

The real reset scenario in `src/test/webapp/tests/admin/dashboard-reset.js` creates and removes a disposable account. Do not run a destructive reset on an existing account whose other-domain preferences cannot be restored through the active-domain API. The catalogue tests inspect AmCharts root registration after refresh, collapse, size change, removal, and undo; browser assertions also check chart canvas rendering and the table equivalent.
