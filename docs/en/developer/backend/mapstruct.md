# Mapping DTO objects

When requesting object mapping, the [mapstruct](https://mapstruct.org) library can be used.

An example is the mapping of the object [DocDetails](../../../../src/main/java/sk/iway/iwcm/doc/DocDetails.java) to [DocHistoryDto](../../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDto.java).

It is necessary to prepare a DTO object and ```interface``` class for ```mapper```. The created interface is implemented automatically during compilation, in ```build.gradle``` it is enabled by setting:

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

When implementing, first prepare a DTO object, e.g. [DocHistoryDto](../../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDto.java):

```java
package sk.iway.iwcm.editor.rest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class DocHistoryDto {

    @DataTableColumn(inputType = DataTableColumnType.ID)
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

!>**Warning:** The mapping class must contain the expression ```Mapper``` in the name due to compilation on the ```Jenkins``` server, e.g. [DocHistoryDtoMapper](../../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDtoMapper.java):

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

You write mappings as methods for a single object (```docToHistoryDto```) or as a list of objects (```toHistoryDtos```). The name of the method is not important, choose a descriptive name.

The ```INSTANCE``` attribute is used to get an instance of the ```mapper``` object, always implement it.

Attributes between objects are converted automatically, if they match you do not need to set the ```@Mapping``` annotation. If the attribute names do not match use the annotation:

```java
@Mapping(source = "historyId", target = "id")
```

If you need to perform a specific conversion between attributes, you can implement a method for the conversion and set its usage using the ```qualifiedByName``` attribute. The example shows the implementation of the ```stringDateToLong``` method, which converts a date in an ```String``` object to ```timestamp```.

Complete documentation on mapping options can be found at [mapstruct.org](https://mapstruct.org/documentation/stable/reference/html/#defining-mapper).

The annotation ```@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)``` tells ```mapstruct``` to ignore non-existent attributes.

## Using mapping

In the class where you need to map the original object to a DTO object, use the ```INSTANCE``` attribute and then call the appropriate method:

```java
//povodny list DocDetails objektov
List<DocDetails> list = historyDB.getHistory(docId, false, false);
//premapovanie na DTO objekty
List<DocDetailsDto> dtolist = DocHistoryDtoMapper.INSTANCE.toHistoryDtos(list);
```

