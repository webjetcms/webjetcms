# BackEnd for working with charts

Working with graphs is exclusively the work of the FrontEnd and the BackEnd only provides data for these graphs. Since each graph filters its data using an external filter, various classes have been added that make it easier to work with parameters from these external filters. Data acquisition and preparation for graphs are provided by individual `RestController` files, which also serve the given page on which these graphs are located.

## Chart type

`ChartType` is a class of type `Enum`, which is used for quick and safe identification of the graph type.

Since different graphs have the same data source, which only needs to be specifically adjusted for the given type, such quick identification is suitable.

The `ChartType` class contains the following values:

- `NOT_CHART`, this value is usually preset. Adding this type was necessary because in some cases the data table and the graph shared the same data source and this value can be used to indicate that it is not a graph but, for example, a data table.
- `LINE`, the value represents a **line** type graph
- `BAR_VERTICAL`, the value represents a **column** type chart, with vertical columns
- `BAR_HORIZONTAL`, the value represents a **column** type chart, with horizontal columns
- `PIE_CLASSIC`, the value represents a **pie** type chart
- `PIE_DONUT`, the value represents a variation of the **pie** type chart, which has an empty circle in the middle (not filled in)
- `DOUBLE_PIE`, the value represents a **double pie** type chart (outer and inner chart)
- `WORD_CLOUD`, the value represents a **word cloud** graph that displays words, where the size of the word represents its frequency of occurrence (or other value) in a given dataset
- `TABLE`, the value represents a simple **table** of data (without advanced functionalities such as paging, filtering, etc.)

## Data for charts

The format of the data returned from the BackEnd for charts depends on the chart type to which this data will be assigned:

- `LINE`, the returned data type is a map containing lists of objects, `Map<String, List<T>>`, where the map key is the name `datasetu`, the object in the list of objects (the value of the map element) must contain a numeric variable (Y-axis, value) and a date variable (X-axis, when the value was obtained)
- `BAR_VERTICAL` / `BAR_HORIZONTAL`, the returned data type is a simple list of objects, `List<T>`, where the object must contain a text variable (Y-axis, category) and a numeric variable (X-axis, value for the given category)
- `PIE_CLASSIC` / `PIE_DONUT`, the returned data type is a simple list of objects, `List<T>`, where the object must contain a text variable (category) and a numeric variable (value for the given category)
- `DOUBLE_PIE`, the returned data type is a simple list of objects, `List<T>`, where the object must contain a text variable (category) and two numeric variables (each value for one category)
- `WORD_CLOUD`, in the case of this chart, data is returned in 2 different formats:
  - a simple list of objects, `List<T>`, where the object contains a text variable (word/phrase) and a numeric variable (frequency of occurrence of the word)
  - can be a simple text string, `String`, the graph itself will search for individual words and their frequency of occurrence in this string
- `TABLE`, the returned data type is a simple list of objects, `List<T>`, where the object contains various text or numeric values ​​that you want to display

More detailed information about the process of obtaining/processing data for graphs is specifically commented in the individual `RestControlleroch` that provide this data.

### Statistics section

The main user of graphs are applications in the Statistics section. A common feature for these applications is that the data for graphs is obtained specifically using classes `StatTableDB` or `StatNewDB` and the returned data is `listy` with type `Column`. Since the variable `Column` serves dynamically for different combinations of data, each class must obtain the necessary values ​​from this variable specifically. For example, `BrowserRestController` obtains the value of `visit` as `Column.getIntColumn3()` but `CountryRestController` obtains the same value as `Column.getIntColumn2()`.

## Filtering

The `FilterHeaderDto` class represents the most commonly used external filter values ​​used to filter values ​​for charts. Since almost every page using charts has an external filter, the most commonly used parameters of these filters have been summarized in this class to improve clarity and avoid code duplication.

In addition to the constructor that sets the basic values, the class also contains a method `groupIdToQuery`. Its task is to create and set `query` necessary for backward compatibility from the specified `rootGroupId`. The created `rootGroupIdQuery` represents an SQL command for selecting those records whose `group_id` falls under the specified `rootGroupId` as a child at any level (selecting the entire sub-tree of web pages).

## `StatService`

This class was primarily created to reduce duplicate code and simplify processing of values ​​from external filters. Since every chart on the page uses some external filter, various methods have been added to this class that make it easier to process values ​​from external filters or work with them. A more detailed description of individual functions/methods, their inputs and possible outputs is directly in the `StatService` class in the form of comments.

## Chart color scheme

Charts support different color schemes to customize the appearance of the chart. To select a color scheme, we recommend using a field of type `IMAGE_RADIO`, which must have a class of `image-radio-chart-colorset` for correct setting (see example below)

```java
@Column(name = "color_scheme")
@DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "&nbsp;", tab = "stat", className = "image-radio-horizontal image-radio-chart-colorset")
private String colorScheme;
```

you set the options for this field as

```java
page.addOptions("colorScheme", StatService.getColorSchemeOptions(), "label", "value", true);
```

the important thing is to call the static method `getColorSchemeOptions` from the auxiliary class `StatService`. This method returns a list of available color schemes with the path to the image that displays the given colors in the scheme

```java
public static final String DEFAULT_COLORSET_NAME = "set3";
public static final List<OptionDto> getColorSchemeOptions() {
    List<OptionDto> optionsMap = new ArrayList<>();
    optionsMap.add(new OptionDto("set1", "set1", "/apps/_common/charts/images/overlapping_circles_set1.png"));
    optionsMap.add(new OptionDto("set2", "set2", "/apps/_common/charts/images/overlapping_circles_set2.png"));
    optionsMap.add(new OptionDto("set3", "set3", "/apps/_common/charts/images/overlapping_circles_set3.png"));
    optionsMap.add(new OptionDto("set4", "set4", "/apps/_common/charts/images/overlapping_circles_set4.png"));
    optionsMap.add(new OptionDto("set5", "set5", "/apps/_common/charts/images/overlapping_circles_set5.png"));
    optionsMap.add(new OptionDto("set_blue", "set_blue", "/apps/_common/charts/images/overlapping_circles_set_blue.png"));
    optionsMap.add(new OptionDto("set_green", "set_green", "/apps/_common/charts/images/overlapping_circles_set_green.png"));
    optionsMap.add(new OptionDto("set_red", "set_red", "/apps/_common/charts/images/overlapping_circles_set_red.png"));
    optionsMap.add(new OptionDto("set_yellow", "set_yellow", "/apps/_common/charts/images/overlapping_circles_set_yellow.png"));
    return optionsMap;
}
```