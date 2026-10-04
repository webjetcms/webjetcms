# Dashboard design decisions

These maintained notes explain the current dashboard design. Read them before changing shared behavior; use the [widget contract](widget-contract.md) for interfaces and the [user guide](../../../../docs/sk/redactor/admin/welcome.md) for user-facing behavior. Revisit the affected decision when the implementation changes.

## Keep essential information available during personalization

Users have mixed responsibilities, so widget availability follows permissions rather than predefined job roles. Session management and unresolved system notices remain outside the personal grid: removing or rearranging content must not hide security information. News keeps a version-specific acknowledgement and a visible summary so users can return to the announcement. Shortcuts have their own editing and reset controls because changing the dashboard's reports should preserve navigation choices.

## Preserve a portable personal layout

Store ordered instances and named sizes on the account so the same arrangement works across devices and domains. Record selections belong to the active domain because a form or folder from one domain may not be meaningful in another. Keep temporarily unavailable instances in storage so a permission or domain change does not erase the user's choices.

New optional types enter the catalogue. The curated defaults apply to new or explicitly reset profiles; silently appending every registered type would change existing personal layouts. Layout and domain records share a transaction so a failed save cannot leave a partially updated profile. The existing settings table requires InnoDB on MySQL/MariaDB for this guarantee. Widget changes stay provisional until toolbar Save; removal and undo preserve instance IDs and domain filters. Shortcut and fixed preferences continue to save independently. Reset previews defaults and clears old domain records only in the final atomic request. The last successful save wins across devices; there is no live layout synchronization.

## Reuse the owning modules and keep initial loading small

The dashboard is a preview of existing modules. Reusing their endpoints preserves authorization, metric definitions and data scope without maintaining a second query implementation. Scope follows the owning module: server-wide audit and monitoring data must not be presented as active-domain metrics. Add a projection only when the module's existing API cannot supply the necessary data.

Settings, notices and current-user sessions arrive with the page. Authorized administrator summaries load through the session REST service only when their widget is visible or their dialog tab is active, allowing explicit refresh without paying for unused lists. Expensive previews and chart initialization wait for viewport entry. This keeps security information independent of chart requests and avoids loading reports the user never sees. Live monitoring shares current samples between memory and CPU; it works without historical monitoring enabled. Other refresh policies are specified in the contract.

## Keep reading order and access to content predictable

The grid preserves DOM order across desktop, keyboard navigation and mobile layouts. Gaps are acceptable; dense packing could make visual and keyboard order diverge. Editing controls appear in an explicit edit mode to keep the everyday overview compact, with a keyboard/touch alternative to dragging.

Recent pages, forms, sessions and logged administrators retain their entries in compact cards through native scrolling. Shared scroll containment prevents the administration's outer scrollbar from consuming those gestures. Other previews are bounded and link to the full module. Charts retain exact values in tooltips and screen-reader tables; compact presentation must preserve access to the underlying data.

## Describe the data the system actually records

Forms include today's submissions because those are immediately actionable. Analytics compare completed periods of equal length so a partial day does not distort the comparison. Weekly 404 aggregates keep their actual range and count requests, not distinct addresses. Empty results, unavailable selections and request failures have different meanings and remain distinguishable. Use only states and metrics exposed by the owning module; form processing states or newsletter delivery statistics must not be inferred from incomplete data.

## Fit the existing administration and identify the server

The dashboard retains the administration shell, Asap typography, Tabler icons and shared color tokens. Runtime CSS properties connect card and chart colors to the same palette. Detailed styling belongs in the module SCSS rather than copied prototype dimensions.

The environment label identifies the server used for administration. Deriving it from the request server name keeps it stable when the user switches content domains. Administrators can override its text, icon, color and header image through [configuration](../../../../docs/sk/admin/setup/configuration/dashboard.md).
