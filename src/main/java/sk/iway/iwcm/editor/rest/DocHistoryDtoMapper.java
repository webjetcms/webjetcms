package sk.iway.iwcm.editor.rest;

import java.util.Date;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocHistoryDtoMapper {

    DocHistoryDtoMapper INSTANCE = Mappers.getMapper(DocHistoryDtoMapper.class);

    @Mapping(source = "historySaveDate", target = "historySaveDate", qualifiedByName = "stringDateToLong")
    @Mapping(source = "publishStartStringExtra", target = "publishStartStringExtra", qualifiedByName = "stringDateToDate")
    @Mapping(source = "historyId", target = "id")
    DocHistoryDto docToHistoryDto(DocDetails doc);
    List<DocHistoryDto> toHistoryDtos(List<DocDetails> docs);

    @Mapping(source = "historySaveDate", target = "historySaveDate", qualifiedByName = "stringDateToLong")
    @Mapping(source = "publishStartStringExtra", target = "publishStartStringExtra", qualifiedByName = "stringDateToDate")
    DocHistoryDto docHistoryToHistoryDto(DocHistory doc);

    @Named("stringDateToLong")
    default Long stringDateToLong(String date) {
        long timestamp = DB.getTimestamp(date);
        //Logger.debug(DocHistoryDtoMapper.class, "stringDateToLong, date="+date+" timestamp="+timestamp);
        return Long.valueOf(timestamp);
    }

    @Named("stringDateToDate")
    default Date stringDateToDate(String date) {
        long timestamp = DB.getTimestamp(date);
        //Logger.debug(DocHistoryDtoMapper.class, "stringDateToDate, date="+date+" timestamp="+timestamp);
        if (timestamp > 1000) return new Date(timestamp);
        return null;
    }
}
