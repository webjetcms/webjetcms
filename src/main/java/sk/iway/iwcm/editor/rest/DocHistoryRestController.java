package sk.iway.iwcm.editor.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.HistoryDB;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

/**
 * #53385 - REST pre zobrazenie stranok v historii
 * Data poskytuje len READ ONLY
 * Vyuziva mapovanie na DTO objekt
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/web-pages/history")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
public class DocHistoryRestController extends DatatableRestControllerV2<DocHistoryDto, Long>{

    private final DocHistoryRepository docHistoryRepository;

    @Autowired
    public DocHistoryRestController(DocHistoryRepository docHistoryRepository) {
        super(null);
        this.docHistoryRepository = docHistoryRepository;
    }

    @Override
    public Page<DocHistoryDto> getAllItems(Pageable pageable) {

        int docId = Tools.getIntValue(getRequest().getParameter("docId"), -1);
        int docIdOriginal = docId;
        HistoryDB historyDB = new HistoryDB(DBPool.getDBName(getRequest()));
        int masterDocId = MultigroupMappingDB.getMasterDocId(docId);
        docId = masterDocId > 0 ? masterDocId : docId; //ak slave stranka, tak ukazem historiu master stranky
        List<DocDetails> list = historyDB.getHistory(docId, false, false);

        DatatablePageImpl<DocHistoryDto> page = new DatatablePageImpl<>(DocHistoryDtoMapper.INSTANCE.toHistoryDtos(list));

        if (docId != docIdOriginal) {
            //preserve docId from original request, not from multigroup mapping
            for (DocHistoryDto dto : page.getContent()) {
                dto.setDocId(docIdOriginal);
            }
        }

        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public boolean deleteItem(DocHistoryDto entity, long id) {
        //zmazat je mozne len zaznamy v buducnosti
        DocHistory history = docHistoryRepository.getById(entity.getId());
        if (history!=null && Boolean.TRUE.equals(history.getPublicable()) && history.getPublishStartDate().getTime()>Tools.getNow()) {
            if (EditorDB.isPageEditable(getUser(), history.getDocId())) {
                docHistoryRepository.deleteByIdPublicable(history.getId());
                return true;
            }
        }
        return false;
    }

    @Override
    public void beforeSave(DocHistoryDto entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public DocHistoryDto processFromEntity(DocHistoryDto entity, ProcessItemAction action) {
        if (entity.isHistoryActual()) {
            BaseEditorFields ef = new BaseEditorFields();
            ef.addRowClass("is-default-page");
            entity.setEditorFields(ef);
        }
        if (entity.isDisableAfterEnd()) {
            entity.setPublishEndExtra(entity.getPublishEndDate());
        }
        //If history record is disapproved, set it's color to red in table
        if(entity.getHistoryDisapprovedByName() != null && !entity.getHistoryDisapprovedByName().isEmpty()) {
            if(entity.getEditorFields() == null) {
                BaseEditorFields ef = new BaseEditorFields();
                ef.addRowClass("is-disapproved");
                entity.setEditorFields(ef);
            } else
                entity.getEditorFields().addRowClass("is-disapproved");
        }
        return entity;
    }
}
