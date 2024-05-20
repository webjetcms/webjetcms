# Mapping of DTO objects

If you want to map objects, you can use the library [mapstruct](https://mapstruct.org).

An example is object mapping [DocDetails](../../../src/main/java/sk/iway/iwcm/doc/DocDetails.java) at [DocHistoryDto](../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDto.java).

It is necessary to prepare a DTO object and `interface` class for `mapper`. The created interface is implemented automatically during compilation, in `build.gradle` is enabled by setting:

```javascript
ext {
    webjetVersion = "8.8-SNAPSHOT";
    mapstructVersion = "1.4.2.Final"
}
dependencies {
    ...
    //mapstruct, upgradnuty z WJ8 na 1.4.2
    implementation "org.mapstruct:mapstruct:${mapstructVersion}"
    annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
}
```

## DTO object

When implementing, first prepare a DTO object, e.g. [DocHistoryDto](../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDto.java):

```java
package sk.iway.iwcm.editor.rest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class DocHistoryDto {

    @DataTableColumn(inputType = DataTableColumnType.ID, renderFormat = "dt-format-selector", title = "editor.cell.id")
    private Long id; //historyId

    @DataTableColumn(inputType = DataTableColumnType.TEXT_NUMBER, title = "components.forum.docid")
    private int docId;

    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title = "history.date")
    private Long historySaveDate;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "history.changedBy")
    private String authorName;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "history.approvedBy")
    private String historyApprovedByName;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "history.disapprovedBy")
    private String historyDisapprovedByName;

}
```

## Mapping

**ATTENTION** the mapping class must contain the expression in the name `Mapper` because of the compilation on `Jenkins` server, e.g. [DocHistoryDtoMapper](../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDtoMapper.java):
```java
package sk.iway.iwcm.editor.rest;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DocDetails;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocHistoryDtoMapper {

    DocHistoryDtoMapper INSTANCE = Mappers.getMapper(DocHistoryDtoMapper.class);

    @Mapping(source = "historySaveDate", target = "historySaveDate", qualifiedByName = "stringDateToLong")
    @Mapping(source = "historyId", target = "id")
    DocHistoryDto docToHistoryDto(DocDetails doc);
    List<DocHistoryDto> toHistoryDtos(List<DocDetails> docs);

    @Named("stringDateToLong")
    default Long stringDateToLong(String date) {
        long timestamp = DB.getTimestamp(date);
        Logger.debug(DocHistoryDtoMapper.class, "stringDateToLong, date="+date+" timestamp="+timestamp);
        return Long.valueOf(timestamp);
    }
}
```

You write the mappings as methods for a single object (`docToHistoryDto`) or as a list of objects (`toHistoryDtos`). The name of the method is irrelevant, choose a concise name.

Attribute `INSTANCE` is used to get an instance of `mapper` object, always implement it.

Attributes between objects are converted automatically, if they match you don't need to set annotation `@Mapping`. If the attribute names do not match, use annotation:

```java
@Mapping(source = "historyId", target = "id")
```

If you need to perform a specific conversion between attributes, you can implement a method for the conversion and set its use using the attribute `qualifiedByName`. In the sample is the implementation of the method `stringDateToLong` which converts the date in `String` objects on `timestamp`.

Full documentation on mapping options can be found at [mapstruct.org](https://mapstruct.org/documentation/stable/reference/html/#defining-mapper).

Annotation `@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)` informs `mapstruct` to ignore non-existent attributes.

## Using mapping

In the class where you need to map the original object to a DTO object, use the attribute `INSTANCE` and then calling the appropriate method:

```java
//povodny list DocDetails objektov
List<DocDetails> list = historyDB.getHistory(docId, false, false);
//premapovanie na DTO objekty
List<DocDetailsDto> dtolist = DocHistoryDtoMapper.INSTANCE.toHistoryDtos(list);
```
