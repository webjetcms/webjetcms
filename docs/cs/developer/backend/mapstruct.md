# Mapování objektů DTO

Pokud chcete mapovat objekty, můžete použít knihovnu [mapstruct](https://mapstruct.org).

Příkladem je mapování objektů [DocDetails](../../../src/main/java/sk/iway/iwcm/doc/DocDetails.java) na adrese [DocHistoryDto](../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDto.java).

Je nutné připravit objekt DTO a `interface` třída pro `mapper`. Vytvořené rozhraní je automaticky implementováno během kompilace, v programu `build.gradle` je povoleno nastavením:

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

## Objekt DTO

Při implementaci nejprve připravte objekt DTO, např. [DocHistoryDto](../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDto.java):

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

## Mapování

**Varování:** třída mapování musí v názvu obsahovat výraz `Mapper` kvůli kompilaci na `Jenkins` server, např. [DocHistoryDtoMapper](../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDtoMapper.java):

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

Mapování zapíšete jako metody pro jeden objekt (`docToHistoryDto`) nebo jako seznam objektů (`toHistoryDtos`). Na názvu metody nezáleží, zvolte výstižný název.

Atribut `INSTANCE` se používá k získání instance `mapper` objekt, vždy jej implementujte.

Atributy mezi objekty se převádějí automaticky, pokud se shodují, nemusíte nastavovat anotaci. `@Mapping`. Pokud se názvy atributů neshodují, použijte anotaci:

```java
@Mapping(source = "historyId", target = "id")
```

Pokud potřebujete provést konkrétní převod mezi atributy, můžete implementovat metodu pro převod a nastavit její použití pomocí atributu `qualifiedByName`. V ukázce je implementace metody `stringDateToLong` který převede datum v `String` objekty na `timestamp`.

Úplnou dokumentaci k možnostem mapování naleznete na adrese [mapstruct.org](https://mapstruct.org/documentation/stable/reference/html/#defining-mapper).

Anotace `@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)` informuje `mapstruct` ignorovat neexistující atributy.

## Použití mapování

Ve třídě, kde potřebujete namapovat původní objekt na objekt DTO, použijte atribut `INSTANCE` a následné volání příslušné metody:

```java
//povodny list DocDetails objektov
List<DocDetails> list = historyDB.getHistory(docId, false, false);
//premapovanie na DTO objekty
List<DocDetailsDto> dtolist = DocHistoryDtoMapper.INSTANCE.toHistoryDtos(list);
```
