# Mapování DTO objektů

Při požadavku na mapování objektů lze využít knihovnu [mapstruct](https://mapstruct.org).

Příkladem je mapování objektu [DocDetails](../../../../src/main/java/sk/iway/iwcm/doc/DocDetails.java) na [DocHistoryDto](../../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDto.java).

Je třeba připravit DTO objekt a `interface` třídu pro `mapper`. Vytvořený interface je implementován automaticky během kompilace, v `build.gradle` je zapnut nastavením:

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

## DTO objekt

Při implementaci nejprve připravte DTO objekt, například. [DocHistoryDto](../../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDto.java):

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

## Mapování

!>**Upozornění:** mapovací třída musí obsahovat v názvu výraz `Mapper` z důvodu kompilace na `Jenkins` serveru, například. [DocHistoryDtoMapper](../../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDtoMapper.java):

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

Mapování zapisujete jako metody pro jeden objekt (`docToHistoryDto`) nebo jako seznam objektů (`toHistoryDtos`). Název metody je nepodstatný, zvolte výstižný název.

Atribut `INSTANCE` je používán k získání instance `mapper` objektu, vždy jej implementujte.

Atributy mezi objekty jsou konvertovány automaticky, pokud se shodují nepotřebujete nastavit anotaci `@Mapping`. Pokud se neshodují jména atributů, použijte anotaci:

```java
@Mapping(source = "historyId", target = "id")
```

Pokud potřebujete provést specifickou konverzi mezi atributy můžete implementovat metodu pro konverzi a nastavit její použití pomocí atributu `qualifiedByName`. V ukázce je implementace metody `stringDateToLong` která konvertuje datum v `String` objektu na `timestamp`.

Kompletní dokumentaci k možnostem mapování naleznete na stránce [mapstruct.org](https://mapstruct.org/documentation/stable/reference/html/#defining-mapper).

Anotace `@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)` informuje `mapstruct` aby neexistující atributy ignoroval.

## Použití mapování

Ve třídě, kde potřebujete mapovat původní objekt na objekt DTO, použijte atribut `INSTANCE` a následné volání příslušné metody:

```java
//povodny list DocDetails objektov
List<DocDetails> list = historyDB.getHistory(docId, false, false);
//premapovanie na DTO objekty
List<DocDetailsDto> dtolist = DocHistoryDtoMapper.INSTANCE.toHistoryDtos(list);
```
