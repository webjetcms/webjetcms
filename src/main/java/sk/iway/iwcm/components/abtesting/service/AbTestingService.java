package sk.iway.iwcm.components.abtesting.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;

import sk.iway.iwcm.components.abtesting.ABTesting;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.rest.GetAllItemsDocOptions;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

public class AbTestingService {

    private AbTestingService() {}

    public static DatatablePageImpl<DocDetails> getAllItems(GetAllItemsDocOptions options) {
        Page<DocDetails> page = null;

        //Check perms with combination with ABTesting version
		if(options.getCurrentUser().isEnabledItem("cmp_abtesting")) {
			Set<DocDetails> mainWebPages = new HashSet<>();
			List<DocDetails> allBasicDoc = DocDB.getInstance().getBasicDocDetailsAll();
			if(allBasicDoc != null) {
				List<String> allDomains = GroupsDB.getInstance().getAllDomainsList();
				DocDB docDB = DocDB.getInstance();
				for(DocDetails dd : allBasicDoc)
					if(!ABTesting.getAllVariantsDocIds(dd, allDomains, docDB).isEmpty()) mainWebPages.add(dd);
			}

			//Editor fields need to be nullified, or status icons will be stacking
			for(DocDetails dd : mainWebPages) dd.setEditorFields(null);

			page = new DatatablePageImpl<>(new ArrayList<>(mainWebPages));
		} else {
			//User has no right to request data in ABTesting mode
			page = new DatatablePageImpl<>(new ArrayList<>());
		}

        return WebpagesService.preparePage(page, options);
    }
}