package sk.iway.iwcm.doc;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import sk.iway.iwcm.admin.layout.DocDetailsDto;

/**
 * Konvertuje databazovy typ int docId na DocDetailsDto objekt a naopak, priklad pouzitia:
 *
 * @Column(name = "email_doc_id")
 * @Convert(converter = DocDetailsConverter.class)
 * private DocDetailsDto emailDoc;
 *
 * v databaze sa ulozi hodnota docId ale na FE bude dostupny DocDetailsDto objekt
 */
@Converter
public class DocDetailsConverter implements AttributeConverter<DocDetailsDto, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DocDetailsDto doc) {
        //podmienka >0 je aby fungovala anotacia @NotNull na entite
        if (doc != null && doc.getDocId()>0) return Integer.valueOf(doc.getDocId());
        return null;
    }

    @Override
    public DocDetailsDto convertToEntityAttribute(Integer docId) {
        if (docId != null && docId.intValue()>0) {
            DocDB docDB = DocDB.getInstance();
            DocDetails doc = docDB.getBasicDocDetails(docId, false);
            if (doc != null) return new DocDetailsDto(doc);
        }
        return null;
    }

}
