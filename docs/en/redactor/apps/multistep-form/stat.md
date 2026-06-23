# Form statistics

The **Form Statistics** section provides an overview of responses submitted through a multi-step form. It displays summary numbers, graphical visualizations of responses to individual form items, and advanced form completion statistics.

![](stat-section.png)

## Summary statistics

At the top of the page are basic information cards:

| Card | Description |
| --- | --- |
| **Answers** | Number of forms completed and submitted in the selected period. |
| **Average completion time** | The average time respondents spent filling out the form in the selected period, in `MM:SS` format. |
| **Days since creation** | The number of days that have passed since the form was created. A form created on the current day will display a value of `< 1`. |
| **Last answer** | Date and time of the last response sent in the selected period. |

![](stat-section-header.png)

After clicking the **Show advanced statistics** button, additional tabs will also appear:

| Card | Description |
| --- | --- |
| **Display** | The number of times a visitor viewed a multi-step form. |
| **Attempts** | Number of attempts to complete the form by submitting the last step. |
| **Errors** | The sum of item validation errors and system errors when working with form steps. |
| **Ratio** | The ratio of the number of responses to the total number of form views, as a percentage. |

![](stat-section-header-advanced.png)

## Filtering the period

There is a date filter **From - To** in the top bar of the page. After changing the range and clicking the filter button, responses, form item graphs, and statistics that are tied to saved submissions will be recalculated. If you do not specify a range, the default range of the last 30 days will be used.

| Data | Date filter behavior |
| --- | --- |
| **Responses**, **Average completion time**, **Last response**, item graphs, language mutations, deployed form address, and system errors | They are calculated from the selected period. |
| **Impressions**, **Attempts**, and item validation errors | These are running counters since tracking was deployed; the date filter does not split them back. |
| **Days since creation** | It is counted from the time the form is created, regardless of the selected range. |

!>**Note:** Older historical responses created before the statistics extension may not include language, form deployment address, or error counters.

## Form item charts

Below the summary tabs, graphs are displayed for individual form items that have statistics enabled. For each such item, a separate graph is drawn with the distribution of responses in the selected period.

!>**Warning:** The graph will only be displayed for those form items that have the **Show statistics** option enabled in the [Statistics](./README.md#statistics) tab when editing the item.

Each chart contains a button in the upper right corner.<button class="btn btn-sm btn-outline-secondary chart-more-btn"><span><i class="ti ti-settings"></i></span></button> , which opens a dialog with the **Statistics** tab for configuring the chart. This tab is directly paired with the [Statistics](./README.md#statistics) tab available when editing a form item.

## Editing charts

To change the chart type, its behavior, or its colors, open the dialog via the settings button in the upper right corner of the respective chart. After changing and saving your preferences, the chart will automatically redraw without reloading the page and will use the currently set date range.

The available chart configuration options are the same as the settings in the [Statistics](./README.md#statistics) tab when editing a form item:

- **Chart type** – determines what type of chart you want to use to represent the data.
- **Number of values** – the number of most common values ​​that will be displayed in the graph.
- **Show "Other"** – remaining values ​​beyond the **Number of values** limit will be merged into a single "Other" item.
- **Show "Unanswered"** – unanswered replies will be displayed as a separate "Unanswered" item.
- **Compare loosely** – case and diacritics are ignored when grouping responses (e.g. `Áno` and `ano` are counted as the same response).
- **Select color scheme for graph** – select a color scheme from the available palettes (each contains 5 colors, with a larger number of values, the colors are repeated).

## Advanced statistics

Click the **Show advanced statistics** button to view additional statistical information.

| Chart | Description |
| --- | --- |
| **Form language mutations** | Distribution of submitted responses by language of the page the form was embedded in. The language is determined from the folder or page template. |
| **Address of the deployed form** | An overview of the most common URLs of the pages from which the form was submitted. If the address cannot be determined, it is assigned the value `unknown`. |
| **Number of errors by item** | Breakdown of validation errors by form items. The 5 most common items are displayed, the rest are merged into the value **Other**. |
| **Number of errors by action** | Distribution of system errors by **Get Step** or **Save Step** action in the selected period. |
| **View and send** | Line chart of submissions and system errors by day. |

Advanced charts are fixed and do not have a separate chart settings button. The date filter **affects** the **Number of errors by action** and **Time evolution of system errors** charts, data is obtained from the audit.

![](stat-section-advanced.png)

## How data is collected

- **Form view** is counted when a multi-step form application is viewed on a page.
- **Submit Attempt** is counted when the last step of the form is completed.
- If a step fails validation, validation errors for specific items are counted. Such a failed step will not increase the number of form submission attempts.
- When a saved response is submitted, the duration of completion, the language of the page, and the address of the page from which the form was submitted are recorded.
- If a step fails to load or save, an audit log entry of type **Multi-step form - Users** will be created.
