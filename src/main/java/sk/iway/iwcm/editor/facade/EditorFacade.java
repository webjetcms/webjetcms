package sk.iway.iwcm.editor.facade;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.editor.service.MediaService;
import sk.iway.iwcm.editor.service.MultigroupService;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.spirit.model.Media;

/**
 * Facade pre editaciu web stranok. Dolezite su metody getDocForEditor pre ziskanie DocDetails pre editor a save pre jeho ulozenie.
 */
@Service
@RequestScope
public class EditorFacade {

    private EditorService editorService;
    private MultigroupService multigroupService;
    private MediaService mediaService;
    private HttpServletRequest request;

    //nastavene na true ak je potrebne vyvolat obnovenie stromovej struktury/datatabulky po ulozeni
	private boolean forceReload = false;

	//set to true if you want to directly edit multigroup page (eg. change sort order or groupId), used from jstree calls
	private boolean ignoreMultigroupMapping = false;

    @Autowired
    public EditorFacade(EditorService editorService, MultigroupService multigroupService, MediaService mediaService, HttpServletRequest request) {
        this.editorService = editorService;
        this.multigroupService = multigroupService;
        this.mediaService = mediaService;
        this.request = request;
    }

    /**
	 * Ulozenie web stranky, vykonava nasledovne operacie:
	 *
	 * - multigroup mapping - web stranka moze byt vo viacerych adresaroch, ak sa edituje child, tak sa prepne na editaciu master verzie a nasledne sa rozkopiruje do dalsich priecinkov
	 * - vykona samotne ulozenie stranky, vid metoda saveEditedDoc
	 * - po ulozeni aktualizuje TemplatesDB ak sa jedna o stranku zo System adresara
	 * - ak nastala zmena vyzadujuca reload nastavi atribut forceReload=true
	 * - ulozi Media novej web stranky (tie sa ukladaju do DB pre novu stranku s -user_id namiesto web stranky)
	 *
	 * @param entity
	 * @return
	 */
	public DocDetails save(DocDetails entity) {
		//najskor over multigroup mapping
		int docIdOriginal = entity.getDocId();
		int originalSortPriority = entity.getSortPriority();
		String originalVirtualPath = entity.getVirtualPath();
		if (isIgnoreMultigroupMapping()==false) {
			if (docIdOriginal > 0) {
				int masterId = multigroupService.getMasterDocId(docIdOriginal);
				if(masterId > 0) {
					entity.setDocId(masterId);

					DocDetails master = DocDB.getInstance().getDoc(masterId, -1, true);

					boolean multiGroupkeepSortPriority = Constants.getBoolean("multiGroupKeepSortPriority");
					//ak chcem zachovat sort priority a ukladam slave clanok, potrebujem zachovat sort priority mastra
					if(multiGroupkeepSortPriority && entity.getDocId()>0)
					{
						entity.setSortPriority(master.getSortPriority());
					}
					if (masterId != docIdOriginal) {
						//set original master virtualPath because we are saving MASTER doc (see entity.setDocId(masterId)) and want to keep URL on master
						entity.setVirtualPath(master.getVirtualPath());
					}
				}
			}
		}

		//prenesme nazad hodnoty z editorFields do DocDetails
		entity.getEditorFields().toDocDetails(entity);

		int historyId = editorService.saveEditedDoc(entity);
		Logger.debug(EditorFacade.class, "Page saved, historyId=" + historyId);

        multigroupService.setDefaultDocId(entity.getGroupId(), entity.getDocId());

		//ak je to stranka zo /System adresara tak refreshni TemplatesDB, v novom totiz mame lokalne System adresare
		GroupDetails group = entity.getGroup();
		if (group != null && group.getFullPath().startsWith("/System/"))
			TemplatesDB.getInstance(true);

		//ak sa jedna o novu stranku je potrebne zmenit mediam hodnotu -user_id na novo ulozene docId
		if(docIdOriginal < 1) {
			mediaService.assignDocIdToMedia(entity.getDocId());
		}

		//save attrs - MUST BE BEFORE multigroupHandleSlaves
		if (entity.getEditorFields().getAttrs() != null) {
			editorService.saveAttrs(entity, entity.getEditorFields().getAttrs(), Constants.getBoolean("attrAlwaysCleanOnSave"));
		}

		if (isIgnoreMultigroupMapping()==false) {
			boolean multigroupRedirectSlavesToMaster = Constants.getBoolean("multigroupRedirectSlavesToMaster");
			//Write multigroup mapping
			if (entity.getEditorFields().getGroupCopyDetails() != null) {
				entity.setSortPriority(originalSortPriority);
				entity.setVirtualPath(originalVirtualPath);
				multigroupService.multigroupHandleSlaves(entity, docIdOriginal, entity.getEditorFields().getGroupCopyDetails(), multigroupRedirectSlavesToMaster);
			}
		}

		//vrat aktualny zaznam z DB
		DocDetails savedCopy = editorService.getDoc(entity.getDocId(), historyId);
		DocEditorFields def = new DocEditorFields();
		def.fromDocDetails(savedCopy, true);
		savedCopy.setEditorFields(def);
		savedCopy.setHistoryId(historyId);

		//ak sme editovali multigroup stranku nastav nazad povodne docId
		if (docIdOriginal > 0) savedCopy.setDocId(docIdOriginal);

		return savedCopy;
	}

	/**
	 * Ulozi entitu ako B variantu web stranky pre app AB testovanie
	 * @param entity
	 * @return
	 */
	public DocDetails saveAsBVariant(DocDetails entity) {
		String virtualPath = entity.getVirtualPath();
		if (virtualPath.contains(Constants.getString("ABTestingName"))) {
			editorService.addNotify(new NotifyBean("components.abtesting.dialog_title", editorService.getProp().getText("editor.save_as_abtest.notify.isbvariant", entity.getTitle(), String.valueOf(entity.getId())), NotifyType.INFO, 5000));
			return null;
		}

		//nastav virtual path
		int i = virtualPath.indexOf(".");
		String variantName = Constants.getString("ABTestingName")+"b";
		if (i != -1) {
			virtualPath = virtualPath.substring(0, i) + "-" + variantName + virtualPath.substring(i);
		} else {
			virtualPath = virtualPath + variantName + ".html";
		}

		//over, ci uz nahodou neexistuje stranka so zadanou URL
		String domain = GroupsDB.getInstance().getDomain(entity.getGroupId());
		int actualDocId = DocDB.getDocIdFromURL(virtualPath, domain);
		if (actualDocId>0) {
			editorService.addNotify(new NotifyBean("components.abtesting.dialog_title", editorService.getProp().getText("editor.save_as_abtest.notify.exists", entity.getTitle(), String.valueOf(entity.getId()), String.valueOf(actualDocId)), NotifyType.INFO, 5000));
			return null;
		}

		//sprav kopiu entity
		entity.setDocId(-1);
		entity.setVirtualPath(virtualPath);
		entity.setGenerateUrlFromTitle(false);
		entity.setUrlInheritGroup(false);
		entity.setShowInMenu(false);
		entity.setLoggedShowInMenu(false);
		entity.setSearchable(false);

		return save(entity);
	}

    /**
	 * Pripravi entitu na editaciu, na rozdiel od standardneho ziskania getOne riesi:
	 *
	 * - multigroup mapovanie - ak editujem slave vrati udaje master verzie
	 * - ak sa jedna o novu stranku (doc_id==-1) pripravi stranke udaje podla adresara (sablona, poradie usporiadania...)
	 * - dokaze nacitat stranku aj z historie, pokial je zadane historyId
	 *
	 * @param docId - id web stranky, alebo -1 pre novu stranku
	 * @param historyId - id z historie, alebo -1 pre vratenie aktualnej verzie stranky
	 * @param groupId - id adresara
	 * @return
	 */
    public DocDetails getDocForEditor(int docId, int historyId, int groupId) {

        GroupsDB groupsDB = GroupsDB.getInstance();
		if (groupId < 1 && docId > 0) {
			//try to autodetect current groupId
			DocDetails current = DocDB.getInstance().getBasicDocDetails(docId, false);
			if (current != null && current.getGroupId()>0) groupId = current.getGroupId();
		}

		GroupDetails group = groupsDB.getGroup(groupId);
		//lebo pre zadane docId group ignorujeme (moze to byt 999997)
		if (docId < 0 && group == null) return null;

		DocDetails editedDoc;

		// vymazem docasne ulozene media (uzivatel neulozil novu stranku po
		mediaService.deleteTempMedia();

        if (docId == -1 && group.getNewPageDocIdTemplate() > 0) {
			//je potrebne pouzit sablonu namiesto -1
			docId = -group.getNewPageDocIdTemplate();
		}

        //sablona nemoze byt -1 (to je blank page)
		if (docId < 0 && historyId < 0) {
			editedDoc = editorService.prepareNewDocForEditor(docId, group);
		} else {
			editedDoc = editorService.prepareDocForEditor(docId, historyId, ignoreMultigroupMapping);

			if(editedDoc == null) return null;
		}

		if (Constants.getBoolean("editorAutoFillPublishStart") && editedDoc.getPublishStartDate()==null) {
			editedDoc.setPublishStartDate(new Date(Tools.getNow()));
		}

		if (request != null) {
			editedDoc.setHistoryId(historyId);

			String domainName = DocDB.getDomain(request);
			GroupDetails editorGroup = groupsDB.getGroup(editedDoc.getGroupId());

			if (editorGroup != null && Tools.isNotEmpty(editorGroup.getDomainName())) domainName = editorGroup.getDomainName();
			request.getSession().setAttribute("preview.editorDomainName", domainName);
		}

		if (request != null && ContextFilter.isRunning(request)) {
			//do editoru nahrame texty s pridanymi linkami
			editedDoc.setData(ContextFilter.addContextPath(request.getContextPath(), editedDoc.getData()));
		}

        return editedDoc;
    }

	/**
	 * Vytvori web stranku
	 * @param group - adresar v ktorom ma byt vytvorena
	 * @param title - volitelny titulok stranky, ak je NULL vytvori sa podla mena adresara
	 * @param available - urci, ci stranka na byt ihned zobrazitelna (true), alebo nie (false)
	 * @return
	 */
	public DocDetails createEmptyWebPage(GroupDetails group, String title, boolean available) {
		DocDetails doc;
        if (group.getDefaultDocId()>0 && title==null) doc = getDocForEditor(group.getDefaultDocId(), -1, group.getGroupId());
        else doc = getDocForEditor(-1, -1, group.getGroupId());

        if (Tools.isEmpty(title)) title = group.getGroupName();

        doc.setTitle(title);
        doc.setNavbar(title);

		save(doc);

        return doc;
	}

	public boolean delete(DocDetails doc) {
		return editorService.deleteWebpage(doc, true);
	}

    public boolean isForceReload() {
        return forceReload || editorService.isForceReload();
    }

    /**
	 * Ak nastalo schvalovanie vrati zoznam schvalovatelov
	 * @return
	 */
	public List<UserDetails> getApprovers() {
		return editorService.getApprovers();
	}

	/**
	 * Ak ma web stranka publikovanie v buducnosti nastavi sa sem timestamp zaciatku publikovania
	 * @return
	 */
	public Long getPublihStart() {
		return editorService.getPublihStart();
	}

	/**
	 * Ak bola stranka uspesne vypublikovana na verejne zobrazenie vrati true
	 * @return
	 */
	public boolean isPageSavedToPublic() {
		return editorService.isPageSavedToPublic();
	}

	/**
	 * Ak bola stranka korektne ulozena ako rozpracovana verzia vrati true
	 * @return
	 */
	public boolean isPageSavedAsWorkVersion() {
		return editorService.isPageSavedAsWorkVersion();
	}

    /**
     * Vrati zoznam moznych notifikacii pre pouzivatela
     * @return
     */
    public List<NotifyBean> getNotify() {
        return editorService.getNotify();
    }

	/**
	 * Vrati zoznam vsetkych medii priradenych k zadanej web stranke
	 * @param docId - ID stranky
	 * @return
	 */
	public List<Media> getAllMedia(Integer docId) {
		return mediaService.getAllMedia(docId, "documents");
	}

	/**
	 * Duplikuje media pri duplikovani web stranky
	 * @param oldDocId
	 * @param newDocId
	 * @return
	 */
	public void duplicateMedia(Integer oldDocId, Integer newDocId) {
		// zduplikuj aj media
        List<Media> mediaList = getAllMedia(oldDocId);
        if (mediaList != null && mediaList.isEmpty()==false) {
            for (Media m : mediaList) {
                Logger.debug(getClass(), "Duplikujem medium: "+m.getMediaId()+" "+m.getMediaTitleSk());
				m.setId(null);
				m.setMediaFkId(newDocId);
				m.save();
            }
        }
	}

	/**
	 * Overi, ci pouzivatel ma pravo na editaciu zadanej web stranky
	 * @param user
	 * @param doc
	 * @param isDelete - ak je true kontroluje sa pravo deletePage, inak sa kontroluje addPage/pageSave
	 * @return
	 */
	public boolean isPageEditable(Identity user, DocDetails doc, boolean isDelete) {
		return editorService.isPageEditable(user, doc, isDelete);
	}

	public boolean isIgnoreMultigroupMapping() {
		return ignoreMultigroupMapping;
	}

	/**
	 * set to true if you want to directly edit multigroup page (eg. change sort order or groupId)
	 * @param ignoreMultigroupMapping
	 */
	public void setIgnoreMultigroupMapping(boolean ignoreMultigroupMapping) {
		this.ignoreMultigroupMapping = ignoreMultigroupMapping;
	}
}
