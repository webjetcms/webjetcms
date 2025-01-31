# BackEnd for working with graphs

Working with graphs is solely the job of FrontEnd, and BackEnd only provides the data for those graphs. Since each graph filters its data using an external filter, various classes have been added to make it easier to work with parameters from these external filters. The data acquisition and preparation for the graphs is provided by the individual `RestController` files that also serve the page on which these charts are located.

## Chart type

`ChartType` is a class of type `Enum`, which is used to quickly and safely identify the graph type.

Since different graphs have the same data source, which only needs to be specifically adapted to the type, this quick identification is convenient.

Class `ChartType` contains the following values:
- `NOT_CHART`, this value is the primary preset. The addition of this type was necessary because in some cases the datatable and the chart shared the same data source and this value can be used to say that it is not a chart but for example a datatable.
- `PIE`, the value represents the graph **Pie** type
- `DOUBLE_PIE`, the value represents the graph **double pie crust** type (outer and inner graph)
- `LINE`, the value represents the graph **bar** type
- `BAR`, the value represents the graph **Columnar** type

## Data for charts

The format of the data returned from BackEnd for charts depends on the type of chart to which the data will be assigned:
- `Bar`, the returned data type is a simple list of objects, `java List<T>` where the object must contain a text variable(Y-axis, category) and a numeric variable (X-axis, value to the category)
- `Pie`, the returned data type is a simple list of objects, `java List<T>` where the object must contain a text variable(category) and a numeric variable (value to the category)
- `DOUBLE_PIE`, the returned data type is a simple list of objects, `java List<T>` where the object must contain a text variable(category) and two numeric variables (each value for one category)
- `Line`, the returned data type is a map containing object sheets, `java Map<String, List<T>>`, where the map key represents the name `datasetu`, the object in the object sheet (map item value) must contain a numeric variable (Y-axis, value) and a date variable(X-axis, when the value was obtained)

Further information on the data acquisition/processing process for the graphs is specifically commented on in the individual `RestControlleroch` that provide this data.

### Statistics Section

The main users of the graphs are the applications in the Statistics section. A common feature for these applications is that we get data for graphs specifically using classes `StatTableDB` or `StatNewDB` and the returned data are `Listy` with type `Column`. Since the variable `Column` used dynamically for different combinations of data, each class must get the necessary values from this variable specifically. For example `BrowserRestController` gains value `visit` Like `Column.getIntColumn3()` But `CountryRestController` gets the same value as `Column.getIntColumn2()`.

## Filtering

Class `FilterHeaderDto` represents the most commonly used external filter values that are used to filter values for charts. Since almost every page using graphs has an external filter, the most commonly used parameters for these filters have been aggregated into this class to increase clarity and avoid code duplication.

In addition to the constructor that sets the basic values, the class also contains a method `groupIdToQuery`. Its task is to create and set up `query` required for backward compatibility from the specified `rootGroupId`. Created by `rootGroupIdQuery` is a sql command for selecting those records whose `group_id` falls under the specified `rootGroupId` as a descendant at any level (selecting an entire sub-tree of a website).

## `StatService`

This class was primarily created to reduce code duplication and simplify the processing of values from external filters. Since every chart on the page uses some sort of external filter, various methods have been added to this class to make it easier to process or work with values from external filters. A more detailed description of each function/method, its inputs and possible outputs is directly in `StatService` class in the form of comments.
