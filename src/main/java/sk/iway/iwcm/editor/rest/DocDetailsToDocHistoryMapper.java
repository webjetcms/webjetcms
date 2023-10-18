package sk.iway.iwcm.editor.rest;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;

/**
 * Mapper medzi DocDetails a DocHistory objektom, je to z toho dovodu, ze na FE zobrazujeme stranky na schvalenie
 * ktore su ale z doc_history tabulky, cize DocHistory objekt. Ale datatabulka je nakonfigurovana na DocDetails
 * a len sa v nej prepinanim kariet zobrazuju rozne data. Jednym z nich su aj stranky na schvalenie, potrebujeme
 * ich preto premapovat na DocDetails objekty.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocDetailsToDocHistoryMapper {

    DocDetailsToDocHistoryMapper INSTANCE = Mappers.getMapper(DocDetailsToDocHistoryMapper.class);

    @Mapping(target = "publicable", ignore = true)
    DocDetails docHistoryToDocDetails(DocHistory history);

    List<DocDetails> docHistoryListToDocDetailsList(List<DocHistory> historyList);

    @Mapping(target = "publicable", ignore = true)
    @Mapping(target = "id", ignore = true)
    DocHistory docDetailsToDocHistory(DocDetails detail);

    List<DocHistory> docDetailsListToDocHistoryList(List<DocDetails> detailsList);
}
