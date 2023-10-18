package sk.iway.iwcm.editor.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.MultigroupMappingDB;

/**
 * Web stranky je mozne ukladat do viacerych adresarov, deje sa to pomocou tabulky multigroup_mapping,
 * kde je urceny vztah medzi hlavnou (master) strankou a kopiami (slave) strankami.
 *
 * Technicky sa vzdy primarne pracuje s master strankou, ale po jej ulozeni sa jej obsah nakopiruje
 * aj do slave stranok (v tabulke documents).
 */
@Service
@RequestScope
public class MultigroupService {

    private DocDB docDB;
    private GroupsDB groupsDB;
    private HttpServletRequest request;
	private EditorService editorService;

    @Autowired
    public MultigroupService(HttpServletRequest httpServletRequest, EditorService editorService) {
        this.docDB = DocDB.getInstance();
        this.groupsDB = GroupsDB.getInstance();
        this.request = httpServletRequest;
		this.editorService = editorService;
    }

    /**
	 * Vyriesi zapis slave mapovani pri ukladani web stranky
	 * @param editedDoc
	 * @param docIdOriginal - docid of originally edited doc (aka slave edited docId)
	 * @param otherGroups
	 * @param redirect
	 * @return true, ak je potrebne refreshnut lave menu (pribudla/zmenila sa niekde polozka)
	 */
	public boolean multigroupHandleSlaves(DocDetails editedDoc, int docIdOriginal, List<GroupDetails> otherGroups, boolean redirect) {

		boolean refreshMenu = false;

		Map<Integer, Integer> groupMapping = new HashMap<>();
		List<Integer> toDelete = new ArrayList<>();

		if (otherGroups != null) {
			for(GroupDetails group : otherGroups) {
				int groupId = group.getGroupId();
				if(groupId > 0)
					groupMapping.put(groupId, -1);
			}
		}
		for(Integer docId : MultigroupMappingDB.getSlaveDocIds(editedDoc.getDocId())) {
			DocDetails doc1 = docDB.getBasicDocDetails(docId, true);
			if(groupMapping.get(doc1.getGroupId()) != null) {
				groupMapping.put(doc1.getGroupId(), docId);
			} else {
				// nejaky adresar bol odstraneny, refreshni lavy strom
				refreshMenu = true;
				toDelete.add(docId);
			}
		}

		//multikategorie (praca so slave clankami)
		DocDetails masterDoc = docDB.getDoc(editedDoc.getDocId(), -1, false);
		int masterId = MultigroupMappingDB.getMasterDocId(editedDoc.getDocId());
        if(masterId < 1) masterId = editedDoc.getDocId();

		//zmaz zapisane hodnoty mapovania (nie stranky)
		MultigroupMappingDB.deleteSlaves(editedDoc.getDocId());

		if(redirect) { masterDoc.setExternalLink(masterDoc.getVirtualPath()); }

		boolean multiGroupkeepSortPriority = Constants.getBoolean("multiGroupKeepSortPriority");
		int formSortPriority = editedDoc.getSortPriority();

		for(Map.Entry<Integer, Integer> me : groupMapping.entrySet()) {
			Integer groupId = me.getKey();
			Integer docId = me.getValue();
			if(docId != null) {
				Logger.debug(MultigroupService.class, "Saving slave doc: "+docId+" to group "+groupId);
				if(docId < 0) {
					masterDoc.setVirtualPath("");
				} else {
					masterDoc.setVirtualPath(docDB.getBasicDocDetails(docId, true).getVirtualPath());
				}

				if (docIdOriginal == docId.intValue()) {
					//keep virtual path for edited doc (slave)
					masterDoc.setVirtualPath(editedDoc.getVirtualPath());
				}

				if(Boolean.TRUE.equals(editedDoc.getGenerateUrlFromTitle())) {
					masterDoc.setVirtualPath("");
					masterDoc.setEditorVirtualPath("");
				}

				//ak chcem zachovat sort priority
				if(multiGroupkeepSortPriority) {
					if(docIdOriginal != docId.intValue()) {
						//ak to nie je masterDocId zachovaj sort prioritu
						multiGroupKeepSortPriority(masterDoc, docId);
					} else {
						masterDoc.setSortPriority(formSortPriority);
					}
				}

				masterDoc.setDocId(docId);
				masterDoc.setGroupId(groupId.intValue());
				DocDB.saveDoc(masterDoc);
				setDefaultDocId(masterDoc.getGroupId(), masterDoc.getDocId());
				//POZOR: teraz uz mame v masterDoc.getDocId hodnotu ulozeneho SLAVE
				MultigroupMappingDB.newMultigroupMapping(masterDoc.getDocId(), editedDoc.getDocId(), redirect);
				docDB.updateInternalCaches(masterDoc.getDocId());

				if(docId < 0) {
					// nejaky adresar bol pridany, refreshni lavy strom
					refreshMenu = true;
				}

				//handle AtrDB, always clean because we receive ID of master attributes
				editorService.saveAttrs(masterDoc, editedDoc.getEditorFields().getAttrs(), true);
			}
		}

		// odstranime DocDetails pre zmazane slave mappingy
		for(Integer docId : toDelete) { DocDB.deleteDoc(docId, request); }

		//ak sme nieco zmazali refreshneme DocDB
		if(toDelete.isEmpty()==false)	{
			DocDB.getInstance(true);
		} else {
			docDB.forceRefreshMasterSlaveMappings(); //refreshnem master-slave mapy
		}

		return refreshMenu;
	}

    /**
	 * Skontroluje a nastavi default docid adresara (ak je neplatne alebo nenastavene)
	 * @param groupId
	 * @param docId
	 */
	public void setDefaultDocId(int groupId, int docId) {
		//ak je to prva stranka v adresari a adresar nema defaultDoc, nastav
		GroupDetails group = groupsDB.getGroup(groupId);
		if (group != null) {
			DocDetails doc = docDB.getBasicDocDetails(group.getDefaultDocId(), false);

			if ((group.getDefaultDocId() < 1 && group.getNavbar().indexOf("<a") == -1) || doc == null) {
				group.setDefaultDocId(docId);
				groupsDB.setGroup(group);
			}
		}
	}

	/**
	 * Slave strankam nastavi prioritu na existujucu hodnotu v DB.
	 * Pouziva sa, ked maju slave stranky rozdielnu sort prioritu.
	 * @param doc
	 * @param slaveDocId
	 */
    private void multiGroupKeepSortPriority(DocDetails doc, int slaveDocId) {
		DocDetails slaveDoc = docDB.getBasicDocDetails(slaveDocId, false);
		if(slaveDoc != null) {
			doc.setSortPriority(slaveDoc.getSortPriority());
		}
	}

    /**
     * Pre zadane docId vrati ID master dokumentu, alebo -1 ak web starnka nie je vo viacerych adresaroch
     * @param docId
     * @return
     */
    public int getMasterDocId(int docId) {
        return MultigroupMappingDB.getMasterDocId(docId);
    }
}
