# Form statistics

The **Form Statistics** section provides an overview of responses submitted through a multi-step form. It displays both summary numbers and graphical visualizations of responses to individual form items.

![](stat-section.png)

## Summary statistics

There are four information cards at the top of the page:

| Card | Description |
| --- | --- |
| **Answers** | Total number of forms completed and submitted. |
| **Average completion time** | The average time respondents spent filling out the form, in `MM:SS` format. |
| **Days since creation** | The number of days that have passed since the form was created. |
| **Last answer** | Date and time of the last response sent. |

![](stat-section-header.png)

## Form item charts

Below the summary tabs, graphs are displayed for individual form items that have statistics enabled. A separate graph with the distribution of responses is drawn for each such item.

!>**Warning:** The graph will only be displayed for those form items that have the **Show statistics** option enabled in the [Statistics](./README.md#statistics) tab when editing the item.

Each chart contains a button in the upper right corner.<button class="btn btn-sm btn-outline-secondary chart-more-btn"><span><i class="ti ti-settings"></i></span></button> , which opens a dialog with the **Statistics** tab for configuring the chart. This tab is directly paired with the [Statistics](./README.md#statistics) tab available when editing a form item.

## Editing charts

To change the chart type, behavior, or colors, open the dialog via the settings button in the upper right corner of the chart. After changing and saving your preferences, the charts will automatically redraw without having to reload the page.

The available chart configuration options are the same as the settings in the [Statistics](./README.md#statistics) tab when editing a form item:

- **Chart type** – determines what type of chart you want to use to represent the data.
- **Number of values** – the number of most common values ​​that will be displayed in the graph.
- **Show "Other"** – remaining values ​​beyond the **Number of values** limit will be merged into a single "Other" item.
- **Show "Unanswered"** – unanswered replies will be displayed as a separate "Unanswered" item.
- **Compare loosely** – case and diacritics are ignored when grouping responses (e.g. `Áno` and `ano` are counted as the same response).
- **Select color scheme for graph** – select a color scheme from the available palettes (each contains 5 colors, with a larger number of values, the colors are repeated).