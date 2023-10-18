# Mapovanie DTO objektov

Pri požiadavke na mapovanie objektov je možné využiť knižnicu [mapstruct](https://mapstruct.org).

Príkladom je mapovanie objektu [DocDetails](../../../src/main/java/sk/iway/iwcm/doc/DocDetails.java) na [DocHistoryDto](../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDto.java).

Je potrebné pripraviť DTO objekt a ```interface``` triedu pre ```mapper```. Vytvorený interface je implementovaný automaticky počas kompilácie, v ```build.gradle``` je zapnutý nastavením:

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

Pri implementácii najskôr pripravte DTO objekt, napr. [DocHistoryDto](../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDto.java):

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

## Mapovanie

**POZOR** mapovacia trieda musí obsahovať v názve výraz ```Mapper``` z dôvodu kompilácie na ```Jenkins``` serveri, napr. [DocHistoryDtoMapper](../../../src/main/java/sk/iway/iwcm/editor/rest/DocHistoryDtoMapper.java):

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

Mapovanie zapisujete ako metódy pre jeden objekt (```docToHistoryDto```) alebo ako zoznam objektov (```toHistoryDtos```). Názov metódy je nepodstatný, zvoľte výstižný názov.

Atribút ```INSTANCE``` je používaný na získanie inštancie ```mapper``` objektu, vždy ho implementujte.

Atribúty medzi objektami sú konvertované automaticky, ak sa zhodujú nepotrebujete nastaviť anotáciu ```@Mapping```. Ak sa nezhodujú mená atribútov použite anotáciu:

```java
@Mapping(source = "historyId", target = "id")
```

Ak potrebujete vykonať špecifickú konverziu medzi atribútmi môžete implementovať metódu pre konverziu a nastaviť jej použitie pomocou atribútu ```qualifiedByName```. V ukážke je implementácia metódy ```stringDateToLong``` ktorá konvertuje dátum v ```String``` objekte na ```timestamp```.

Kompletnú dokumentáciu k možnostiam mapovania nájdete na stránke [mapstruct.org](https://mapstruct.org/documentation/stable/reference/html/#defining-mapper).

Anotácia ```@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)``` informuje ```mapstruct``` aby neexistujúce atribúty ignoroval.

## Použitie mapovania

V triede, kde potrebujete mapovať pôvodný objekt na DTO objekt použite atribút ```INSTANCE``` a následné volanie príslušnej metódy:

```java
//povodny list DocDetails objektov
List<DocDetails> list = historyDB.getHistory(docId, false, false);
//premapovanie na DTO objekty
List<DocDetailsDto> dtolist = DocHistoryDtoMapper.INSTANCE.toHistoryDtos(list);
```

