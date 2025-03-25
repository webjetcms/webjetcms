package sk.iway.iwcm.editor.rest;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-03-25T15:55:09+0100",
    comments = "version: 1.6.1, compiler: Eclipse JDT (IDE) 3.41.0.z20250213-2037, environment: Java 21.0.6 (Eclipse Adoptium)"
)
public class DocHistoryDtoMapperImpl implements DocHistoryDtoMapper {

    @Override
    public DocHistoryDto docToHistoryDto(DocDetails doc) {
        if ( doc == null ) {
            return null;
        }

        DocHistoryDto docHistoryDto = new DocHistoryDto();

        docHistoryDto.setHistorySaveDate( stringDateToLong( doc.getHistorySaveDate() ) );
        docHistoryDto.setPublishStartStringExtra( stringDateToDate( doc.getPublishStartStringExtra() ) );
        docHistoryDto.setId( (long) doc.getHistoryId() );
        docHistoryDto.setAuthorName( doc.getAuthorName() );
        docHistoryDto.setDisableAfterEnd( doc.isDisableAfterEnd() );
        docHistoryDto.setDocId( doc.getDocId() );
        docHistoryDto.setEditorFields( doc.getEditorFields() );
        docHistoryDto.setFullPath( doc.getFullPath() );
        docHistoryDto.setHistoryActual( doc.isHistoryActual() );
        docHistoryDto.setHistoryApprovedByName( doc.getHistoryApprovedByName() );
        docHistoryDto.setHistoryDisapprovedByName( doc.getHistoryDisapprovedByName() );
        docHistoryDto.setPublishEndDate( doc.getPublishEndDate() );
        docHistoryDto.setPublishStartDate( doc.getPublishStartDate() );
        docHistoryDto.setTitle( doc.getTitle() );

        return docHistoryDto;
    }

    @Override
    public List<DocHistoryDto> toHistoryDtos(List<DocDetails> docs) {
        if ( docs == null ) {
            return null;
        }

        List<DocHistoryDto> list = new ArrayList<DocHistoryDto>( docs.size() );
        for ( DocDetails docDetails : docs ) {
            list.add( docToHistoryDto( docDetails ) );
        }

        return list;
    }

    @Override
    public DocHistoryDto docHistoryToHistoryDto(DocHistory doc) {
        if ( doc == null ) {
            return null;
        }

        DocHistoryDto docHistoryDto = new DocHistoryDto();

        docHistoryDto.setHistorySaveDate( stringDateToLong( doc.getHistorySaveDate() ) );
        docHistoryDto.setPublishStartStringExtra( stringDateToDate( doc.getPublishStartStringExtra() ) );
        docHistoryDto.setAuthorName( doc.getAuthorName() );
        docHistoryDto.setDisableAfterEnd( doc.isDisableAfterEnd() );
        docHistoryDto.setDocId( doc.getDocId() );
        docHistoryDto.setEditorFields( doc.getEditorFields() );
        docHistoryDto.setFullPath( doc.getFullPath() );
        docHistoryDto.setHistoryActual( doc.isHistoryActual() );
        docHistoryDto.setHistoryApprovedByName( doc.getHistoryApprovedByName() );
        docHistoryDto.setHistoryDisapprovedByName( doc.getHistoryDisapprovedByName() );
        docHistoryDto.setId( doc.getId() );
        docHistoryDto.setPublishEndDate( doc.getPublishEndDate() );
        docHistoryDto.setPublishStartDate( doc.getPublishStartDate() );
        docHistoryDto.setTitle( doc.getTitle() );

        return docHistoryDto;
    }
}
