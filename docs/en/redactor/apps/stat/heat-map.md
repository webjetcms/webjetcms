# Click map

The click map shows where visitors click. Open **Statistics → Click map**, select a period, then select a page name. The list contains accessible pages in the current domain with recorded clicks.

## Enable measurement

Select **Statistics + click map** in the template or template group settings. The tracker is inserted automatically. **Statistics** records ordinary page traffic; **Disabled** stops page measurement. **Inherit setting** uses the parent setting, with the template taking precedence over its template group. Globally disabling statistics takes precedence.

The `statMode=none` configuration setting disables all measurement. Setting `statEnableClickTracking=false` disables click recording even when enabled by a template; ordinary statistics continue according to the selected mode.

Click recording respects the statistical cookie consent settings. Without permission, clicks are not recorded. Ordinary statistics retain their existing anonymization rules.

## Preview

New clicks are recorded separately for each exact window width in CSS pixels. The width selector shows click counts and defaults to the most popular width. The preview scrolls independently. Scaling the preview keeps its internal page width unchanged. You can adjust map opacity, hide the map, and reload the preview.

The preview selects an available published content version as of the end of the period and shows its date. When a historical version cannot be established, the interface explains that current content is shown, or that the preview is unavailable. When the content changed during the period, a notice explains that clicks from multiple versions are combined.

Publication times are derived from the available history metadata. Scheduled publication times are approximate because history does not retain the exact time the publishing task ran.

The template, header, footer, menus, applications and images use current resources. This is not a historical snapshot of the entire website. Content changes, moving elements and different window heights can affect coordinate accuracy.

## Delivery and limitations

Clicks are temporarily stored in a cookie and delivered with the next ordinary server request. Each click does not trigger a separate request. The final clicks before leaving the site may remain undelivered. Pages served entirely by a cache in front of the server may not forward the cookie for processing.

The browser keeps at most 16 pending click cookies, up to 2 KiB in total, with a lifetime of 24 hours. Exceeding either limit discards the oldest undelivered clicks. Records in the monthly database tables are not deleted automatically; use the existing database cleanup tool to remove them.

The map does not record mouse movement or individual sessions.

## Existing data

The existing monthly `stat_clicks_YYYY_M` tables are extended with the new fields and continue to store new clicks. Existing records are retained. Clicks without a recorded window width are assigned a desktop width of **1920 px** and appear under that width in the selector.

Original coordinates remain unchanged. They may have been measured relative to a page container whose original position can no longer be determined. Older maps at 1920 px are therefore approximate; this assigned width does not establish the original window size.

The domain of older records is assigned from the source page's current domain. If the page or its domain cannot be resolved, the records remain stored but are not displayed in the click map. Historical domain changes are not reconstructed.
