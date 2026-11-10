# Click map

The click map shows where visitors click on a page. Open **Statistics → Click map**, select a period, then select a page name. The list contains accessible pages in the current domain with recorded clicks.

## Enable measurement

Select **Statistics + click map** in the template or template group. The tracker is inserted automatically. **Statistics** measures traffic only, **Disabled** stops measurement, and **Inherit setting** uses the parent setting. The template takes precedence over its group.

Globally, `statMode=none` disables all measurement; `statEnableClickTracking=false` disables only the click map, even when enabled in a template.

Clicks are recorded only with statistical cookie consent. In existing installations, add the cookie definition if missing:

- **Cookie name:** `wj_hm_*`
- **Category:** Statistical
- **Type:** `http`
- **Lifetime:** 24 hours
- **Purpose:** Temporarily stores clicks until they are delivered to generate the map.

Then allow statistical cookies on the website. New installations already include the definition.

## Preview

The preview shows the click map and top hotspots. **Map width** lists groups with click counts and defaults to the most popular. Fixed, centered containers are grouped by container width; others use exact window width. Groups remain separate even at the same numeric width. Refreshed counts may lag by up to 30 seconds.

The CSS selector `statHeatMapMainContainerSelector` (default `.container`) limits recording to clicks inside matching containers. An empty value measures the whole page; an invalid selector or missing container prevents recording. Use standard CSS syntax without jQuery extensions.

If a fixed container width cannot be reliably determined, window width is used. If a matching container is missing from the preview, a warning appears and the map is hidden; stored data is preserved.

- **Display** changes the preview scale, not the page width.
- Scroll with the mouse wheel. Sticky headers also cover click markers beneath them.
- **Map opacity** adjusts the color overlay; **Show map** hides or displays it.
- Use **Reload preview** after a loading error.
- The button in the upper-right corner toggles full-screen view.

Colors from blue to red indicate increasing click density. The scale adapts to the selected data and stays unchanged while scrolling. Color does not represent an area's percentage of clicks.

## Page versions

The preview defaults to published content at the end of the period. If unavailable, a notice explains whether current content is shown or no preview is available. Use **Page version** to select a specific version or **All versions**. The list contains up to 100 of the latest published versions in the period; select an earlier period for older versions.

Clicks are assigned by whole days. Versions published on the same day can therefore include the same clicks; **All versions** counts each click once. Versions without clicks remain available with a zero count. Scheduled publication times are approximate.

Templates, headers, footers, menus, applications and images use current resources. Changes to layout or content height can shift the map relative to the original clicks.

## Compare two versions

If the period contains at least two published versions, click **Compare versions**. Select different versions in **Version A** and **Version B**. Previews share width, scale, opacity and map visibility, but scroll independently.

Each version has its own click count and color scale; matching colors do not mean equal counts. Counts are not adjusted for traffic or how long a version was published. Versions without clicks appear without the overlay. Click **Compare versions** again to restore the original preview.

## Clicks in an area

Enable **Select area** and drag a rectangle. The count and percentage of clicks for the selected period, version and width appear below the preview. You can scroll with the mouse wheel while selecting. Area selection is unavailable when comparing versions.

Disable **Select area** or click **Clear area** to remove the selection. Changing the version or width, or reloading the preview, also clears it.

## Top hotspots

Use <button class="btn btn-sm btn-primary" type="button" title="Hotspots" aria-label="Hotspots"><i class="ti ti-list-numbers" aria-hidden="true"></i></button> to show or hide up to five of the busiest non-overlapping areas, with click counts and percentages for the current selection. Click a row to scroll to the highlighted area. Hiding the panel gives the preview more space. The panel is hidden when comparing versions.

## Delivery and limitations

Clicks wait in cookies for the next server request. Final clicks before leaving, or clicks on cached pages, may remain undelivered. The limit is 16 cookies, totaling 2 KiB, with a 24-hour lifetime; exceeding it discards the oldest pending clicks. The map does not record mouse movement or sessions.

Administrators can [aggregate historical clicks](../../../admin/settings/cronjob/README.md#aggregate-historical-clicks) or remove data with the database cleanup tool. `statHeatMapCellSize` controls cell size for rendering and aggregation (default `5`, meaning 5 × 5 CSS pixels). Larger cells reduce precision; reducing the size later cannot restore merged coordinates.

## Existing data

Older records are preserved. Clicks without a window width appear under **1920 px**; their positions are approximate. They are not recalculated or merged with container groups.

The domain is assigned from the source page's current domain. Records whose page or domain cannot be found remain stored but are not displayed. Historical domain changes are not reconstructed.
