# Click map

The click map shows where visitors click. Open **Statistics → Click map**, select a period, then select a page name. The list contains accessible pages in the current domain with recorded clicks.

## Enable measurement

Select **Statistics + click map** in the template or template group settings. The tracker is inserted automatically. **Statistics** records ordinary page traffic; **Disabled** stops page measurement. **Inherit setting** uses the parent setting, with the template taking precedence over its template group. Globally disabling statistics takes precedence.

The `statMode=none` configuration setting disables all measurement. Setting `statEnableClickTracking=false` disables click recording even when enabled by a template; ordinary statistics continue according to the selected mode.

Click recording respects the statistical cookie consent settings. Without permission, clicks are not recorded. Ordinary statistics retain their existing anonymization rules.

In an existing installation, add a cookie definition for the click map if it is missing:

- **Cookie name:** `wj_hm_*`
- **Category:** Statistical
- **Type:** `http`
- **Lifetime:** 24 hours
- **Purpose:** Temporarily stores page clicks until they are delivered to generate the click map.

The definition covers individual click cookies with random suffixes. The cookie banner offers categories based on registered cookie definitions. After adding it, open the consent settings on the website and allow statistical cookies. New installations include this definition by default.

## Preview

The `statHeatMapMainContainerSelector` configuration setting specifies the CSS selector of measured containers. Its default is `.container`, as used by Bootstrap. Existing custom values remain valid. An empty value enables measurement across the whole page, grouped by exact window width.

When a selector is set, only clicks inside the nearest matching container are recorded. Clicks outside its bounds are ignored, including content overflowing the container. If the selector matches no elements or is invalid, the page does not record clicks. Replace older selectors using jQuery extensions with standard CSS selectors.

Fixed-width containers centered on the page are grouped by their measured width. For example, windows of 1223 and 1369 px with the same 1140 px container form one **Container 1140 px** group. The X coordinate is measured from the container's left edge, including its padding; Y remains relative to the top of the page. A fixed width is recognized when the container reaches a CSS `max-width` or `width` declared in pixels.

If the browser cannot read a loaded stylesheet, such as CSS from another domain without access permission, the whole page uses exact window widths. This also applies to a separate external font stylesheet. Exact window widths are also used when pixel and relative declarations are combined for the property. Widths derived from `vw`, CSS variables or calculations, and containers with an active CSS animation, are not grouped together either.

Fluid containers and containers that are not centered on the page keep their original coordinates and exact window width. Mobile widths of 390 and 430 px therefore remain in separate **Window 390 px** and **Window 430 px** groups. When a selector is set, clicks outside containers are ignored in this mode too.

The width selector shows click counts and defaults to the most popular group. Reloading the preview within 30 seconds can show counts from the previous request. Container coordinates are kept separate from original page coordinates, even when the groups have the same numeric width.

A container group's preview opens at the most frequently recorded actual window width in that group; ties use the smaller width. This preserves the corresponding breakpoint. The preview adds the container's horizontal offset to X. If no matching fixed-width centered container is found, the page appears without the click overlay and shows a warning. Stored data remains intact.

The preview scrolls independently. When content moves beneath a sticky header, the header also covers the click markers on that content. **Display** changes only the preview's scale on screen; the internal page width remains unchanged. You can adjust map opacity or temporarily hide the map. Reload the preview if the page or part of the map failed to load.

The preview opens on the published page version at the end of the selected period. If the history is incomplete, a notice explains whether the preview uses current content or is unavailable.

When the page changed during the period, **Page version** defaults to **All versions**. You can also use this filter when the only available version covers part of the selected period. The filter lists up to 100 of the most recent published versions in the selected period. To see an older version, select an earlier date range. Select a version by its ID and publication time to see its content. The map and the counts for each width include all clicks on the days that version was live within the selected period. If a new version went live at noon, both versions show all clicks for that day. **All versions** counts each click once.

If no clicks were recorded on those days, you can still preview its saved content at any width recorded in that period. Each width shows zero clicks.

Publication times are derived from the available history metadata. Scheduled publication times are approximate because history does not retain the exact time the publishing task ran.

The template, header, footer, menus, applications and images use current resources. This is not a historical snapshot of the entire website. Content changes, moving elements and different window heights can affect coordinate accuracy. The absolute Y coordinate is not recalculated: changing the height of content above a click can shift the map even at the same container width.

## Top hotspots

Use <button class="btn btn-sm btn-primary" type="button" title="Hotspots" aria-label="Hotspots"><i class="ti ti-list-numbers" aria-hidden="true"></i></button> to show or hide up to five of the busiest non-overlapping areas, with click counts and percentages for the selected page, period, version and map width. Click a row to scroll the preview to that area and highlight it with a numbered marker. Areas group nearby recorded clicks. The panel is hidden while comparing versions.

## Delivery and limitations

Clicks are temporarily stored in a cookie and delivered with the next ordinary server request. Each click does not trigger a separate request. The final clicks before leaving the site may remain undelivered. Pages served entirely by a cache in front of the server may not forward the cookie for processing.

The browser keeps at most 16 pending click cookies, up to 2 KiB in total, with a lifetime of 24 hours. Exceeding either limit discards the oldest undelivered clicks. Administrators can reduce storage for completed months by [aggregating historical clicks](../../../admin/settings/cronjob/README.md#aggregate-historical-clicks). The task stores the total clicks in each group on the row with the lowest ID and keeps that row’s coordinates. Use the database cleanup tool to remove data for a period.

The `statHeatMapCellSize` setting controls cell size for both rendering and aggregation (default `5`, meaning 5 × 5 CSS pixels). Set it to `4` for 4 × 4 or `10` for 10 × 10. Larger cells reduce positional detail; reducing the size later cannot restore coordinates of clicks already merged.

The map does not record mouse movement or individual sessions.

## Existing data

The existing monthly `stat_clicks_YYYY_M` tables are extended with the new fields and continue to store new clicks. The schema upgrade retains existing records. Clicks without a recorded window width are assigned a desktop width of **1920 px** and appear under that width in the selector.

Original coordinates remain unchanged. They may have been measured relative to a page container whose original position can no longer be determined. Older maps at 1920 px are therefore approximate; this assigned width does not establish the original window size.

Clicks recorded before container groups were introduced are not recalculated retrospectively. They remain separate from container groups. New records store the coordinate mode in `coordinate_mode`, the group width in `viewport_width`, and the actual window width in `source_viewport_width`. Pending cookies in the older format continue to be processed as page coordinates during their lifetime.

The domain of older records is assigned from the source page's current domain. If the page or its domain cannot be resolved, the records remain stored but are not displayed in the click map. Historical domain changes are not reconstructed.
