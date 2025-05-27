package sk.iway.iwcm.components.structuremirroring;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.editor.service.GroupsService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.NullAwareBeanUtils;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.system.translation.TranslationService;

/**
 * Zabezpecuje zrkadlenie web stranky podla sync_id v databaze
 * EUSTREAMNW-84
 */
public class DocMirroringServiceV9 {

   public void handleDocSave(DocDetails doc, WebjetEventType type) {

      if (MirroringService.isEnabled(doc.getGroupId())==false) return;

      Logger.debug(DocMirroringServiceV9.class, "handleDocSave DOC type=" + type + ", doc=" + doc + " thread="+Thread.currentThread().getName());

      GroupsDB groupsDB = GroupsDB.getInstance();
      DocDB docDB = DocDB.getInstance();
      Prop prop = Prop.getInstance();
      if ("Nový podadresár".equals(doc.getTitle()) || prop.getText("editor.dir.new_dir").equals(doc.getTitle())) return;

      //editor form nema nastavene syncId, takze musime nastavit podla existujucej stranky v DB
      if (doc.getDocId()>0) {
         doc.setSyncId(getSyncId(doc.getDocId()));
      }

      if (type == WebjetEventType.ON_START) {
         if (doc.getSyncId()<1) {
            //jedna sa o novy adresar, vygenerujeme mu syncId
            doc.setSyncId(PkeyGenerator.getNextValue("structuremirroring"));
         }
      } else if (type == WebjetEventType.AFTER_SAVE) {

         int autoTranslatorUserId = getAutoTranslatorUserId();

         if (doc.getSyncId()>1) {
            //najdi k tomu mirror verziu
            List<DocDetails> syncedDocs = getDocBySyncId(doc.getSyncId(), doc.getDocId());
            List<GroupDetails> mappedGroupsList = MirroringService.getMappingForGroup(doc.getGroupId());
            List<GroupDetails> mappedGroupsListNotExisting = new ArrayList<>();

            if (mappedGroupsList.size()>syncedDocs.size()) {
               //there is new mapping group created in allready synced groups, we must create missing one
               for (GroupDetails mappedGroup : mappedGroupsList) {
                  boolean containGroup = false;
                  for (DocDetails syncedDoc : syncedDocs) {
                     if (mappedGroup.getGroupId()==syncedDoc.getGroupId()) {
                        //ok, this group is allready synced
                        containGroup = true;
                        break;
                     }
                  }
                  if (containGroup==false) mappedGroupsListNotExisting.add(mappedGroup);
               }
               mappedGroupsList = mappedGroupsListNotExisting;
            }

            if (syncedDocs.isEmpty() || mappedGroupsListNotExisting.isEmpty()==false) {
               //este neexistuje mirror doc, musime vytvorit novy (kopiu)
               //este neexistuje, musime vytvorit novu grupu (kopiu)

               if (MirroringService.isEnabled(doc.getGroupId())==false) return;

               GroupDetails group = groupsDB.getGroup(doc.getGroupId());
               //vytvor kopie adresara v ostatnych mapovanych adresaroch

               TranslationService translator = new TranslationService(GroupMirroringServiceV9.getLanguage(group), null);

               for (GroupDetails mappedGroup : mappedGroupsList) {
                  translator.setToLanguage(GroupMirroringServiceV9.getLanguage(mappedGroup));
                  String translatedTitle = translator.translate(doc.getTitle());

                  DocDetails existing = docDB.getDocByTitle(translatedTitle, mappedGroup.getGroupId());
                  if (existing != null) {
                     if (existing.getSyncId()==doc.getSyncId()) {
                        //uz existuje, takze toto je len mirror
                        continue;
                     }
                     //uz existuje, nema nastavene syncId, takze ho nastavime
                     if (existing.getSyncId()<1) {
                        existing.setSyncId(doc.getSyncId());
                        existing.setSortPriority(doc.getSortPriority());
                        DocDB.saveDoc(existing, false);
                        continue;
                     }
                  }

                  DocDetails mirror = new DocDetails();
                  NullAwareBeanUtils.copyProperties(doc, mirror);
                  mirror.setDocId(-1);
                  if (Constants.getBoolean("structureMirroringDisabledOnCreate")) mirror.setAvailable(false);
                  mirror.setGroupId(mappedGroup.getGroupId());
                  mirror.setSyncId(doc.getSyncId());

                  if (autoTranslatorUserId > 0) mirror.setAuthorId(autoTranslatorUserId);

                  tranlateDoc(doc, mirror, translator);

                  //CloneStructure - keepVirtualPath
                  RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
                  boolean keepVirtualPath = "true".equals(rb.getParameter("keepVirtualPath"));
                  if(keepVirtualPath) {
                     int srcGroupId = Tools.getIntValue(rb.getParameter("srcGroupId"), -1);
                     int destGroupId = Tools.getIntValue(rb.getParameter("destGroupId"), -1);
                     if (srcGroupId > 0 && destGroupId > 0) {
                        //get source group URL prefixes
                        String srcGroupPath = DocDB.getGroupDiskPath(groupsDB.getGroupsAll(), srcGroupId);

                        //get destination group URL prefixes
                        String destGroupPath = DocDB.getGroupDiskPath(groupsDB.getGroupsAll(), destGroupId);

                        //remove source prefix path and replace it with destination prefix path, if not found, append it
                        String virtualPath = doc.getVirtualPath();
                        if(virtualPath.startsWith(srcGroupPath)) {
                           virtualPath = virtualPath.substring(srcGroupPath.length());
                        }
                        virtualPath = destGroupPath + virtualPath;
                        //fix double slashes
                        virtualPath = Tools.replace(virtualPath, "//", "/");
                        mirror.setVirtualPath(virtualPath);
                     }
                  }

                  //Save created mirror DOC
                  DocDB.saveDoc(mirror, false);

                  //nastav hlavnu stranku adresara
                  if (mappedGroup.getDefaultDocId()<1) {
                     GroupDetails groupToSave = groupsDB.getGroup(mappedGroup.getGroupId());
                     groupToSave.setDefaultDocId(mirror.getDocId());
                     groupsDB.setGroup(groupToSave, false);
                  }
                  MirroringService.forceReloadTree();
               }
            }

            if (syncedDocs.size()>0) {
               //uz existuje, skontroluj ostatne kopie ci sa nepresunuli a podobne

               //overenie zmeny parent adresara
               GroupDetails group = groupsDB.getGroup(doc.getGroupId());
               if (group != null) {
                  List<GroupDetails> syncedGroups = GroupMirroringServiceV9.getGroupsBySyncId(group.getSyncId(), group.getGroupId());
                  //ok mame zoznam parent adresarov, over ci su mapovane spravne
                  for (DocDetails syncedDoc : syncedDocs) {
                     GroupDetails syncedGroup = groupsDB.getGroup(syncedDoc.getGroupId());
                     GroupDetails syncedCorrectGroup = MirroringService.selectMappedGroup(syncedGroup, syncedGroups);
                     if (syncedCorrectGroup != null) {
                        //porovnaj IDecko voci aktualnemu parentu
                        if (syncedGroup.getGroupId()!=syncedCorrectGroup.getGroupId()) {
                           Logger.debug(DocMirroringServiceV9.class, "NESEDI PARENT GROUP, syncedGroup="+syncedGroup.toString()+" syncedCorrectGroup="+syncedCorrectGroup.toString());
                           //presun web stranku do spravneho adresara
                           DocDetails docToSave = docDB.getDoc(syncedDoc.getDocId());
                           docToSave.setGroupId(syncedCorrectGroup.getGroupId());
                           docToSave.setAuthorId(doc.getAuthorId());
                           DocDB.saveDoc(docToSave, false);
                           MirroringService.forceReloadTree();
                        }
                     }
                  }
               }

               //overenie sort priority
               for (DocDetails syncedDoc : syncedDocs) {
                  if (syncedDoc.getSortPriority()!=doc.getSortPriority()) {
                     Logger.debug(DocMirroringServiceV9.class, "NESEDI SORT PRIORITY, syncedDoc="+syncedDoc.getTitle()+" "+syncedDoc.getDocId()+" sort="+syncedDoc.getSortPriority()+" doc="+doc.getTitle()+" "+doc.getDocId()+" sort="+doc.getSortPriority());
                     DocDetails docToSave = docDB.getDoc(syncedDoc.getDocId());
                     docToSave.setSortPriority(doc.getSortPriority());
                     docToSave.setAuthorId(doc.getAuthorId());
                     DocDB.saveDoc(docToSave, false);
                     MirroringService.forceReloadTree();
                  }
               }

               //Set same, but translated data text to syncedDoc's
               for (DocDetails syncedDoc : syncedDocs) {
                  //Indicate if we will translate body (data) of doc
                  boolean doTranslate = false;

                  if(autoTranslatorUserId > 0) { //auto translator exist in DB
                     //Check that he is the one who made the change (do autoTranslate)
                     if(autoTranslatorUserId == syncedDoc.getAuthorId())
                        doTranslate = true;
                  } else { //auto translator DOES NOT exist in DB
                     //Decide by available status, doTranslate only if available = false
                     doTranslate = !syncedDoc.isAvailable();
                  }

                  if(doTranslate) {
                     //Get mapped group of syncedDoc
                     GroupDetails mappedGroup = groupsDB.getGroup(syncedDoc.getGroupId());
                     //Translate data of changed doc

                     TranslationService translator = new TranslationService(GroupMirroringServiceV9.getLanguage(group), GroupMirroringServiceV9.getLanguage(mappedGroup));

                     DocDetails docToSave = docDB.getDoc(syncedDoc.getDocId());
                     if (autoTranslatorUserId>0) docToSave.setAuthorId(autoTranslatorUserId);
                     tranlateDoc(doc, docToSave, translator);
                     DocDB.saveDoc(docToSave, false);

                     if(GroupsService.canSyncTitle(docToSave.getDocId(), mappedGroup.getGroupId()))
                     {
                        DocDB.changeGroupTitle(docToSave.getGroupId(), docToSave.getDocId(), docToSave.getTitle(), true);
                     }

                     MirroringService.forceReloadTree();
                  }
               }
            }
         }
      } else if (type == WebjetEventType.ON_DELETE) {
         //musi byt ON_DELETE, pretoze AFTER je uz v kosi
         if (doc.getSyncId()>1) {
            //najdi k tomu mirror verzie
            List<DocDetails> syncedDocs = getDocBySyncId(doc.getSyncId(), doc.getDocId());
            for (DocDetails syncedDoc : syncedDocs) {
               Logger.debug(DocMirroringServiceV9.class, "MAZEM, syncedDoc="+syncedDoc.getTitle()+" "+syncedDoc.getDocId()+" doc="+doc.getTitle()+" "+doc.getDocId());

               EditorService editorService = Tools.getSpringBean("editorService", EditorService.class);
               editorService.deleteWebpage(syncedDoc, false);

               //DeleteServlet.deleteDoc(null, syncedDoc.getDocId(), false);
               MirroringService.forceReloadTree();
            }
         }
      }
   }

   /**
    * Translate doc fields like title, navbar, data
    * @param source - source doc (translated from)
    * @param doc - target doc
    * @param translator
    */
   private static void tranlateDoc(DocDetails source, DocDetails doc, TranslationService translator) {

      doc.setTitle(translator.translate(source.getTitle()));

      doc.setNavbar(translator.translate(source.getNavbar()));

      // translate perex
      doc.setHtmlData(translator.translate(source.getHtmlData()));

      //regenerate URL based on title
      doc.setVirtualPath("");

      //translate doc body
      doc.setData(translator.translate(source.getData()));
   }

   /**
    * Gets the userId for autotranslations (login defined in confing structureMirroringAutoTranslatorLogin)
    * @return
    */
   private static int getAutoTranslatorUserId() {
      //Id of auto translator user
      int autoTranslatorId = -1;

      //Check that there is a login set up for the autoTranslator user
      if(Tools.isNotEmpty( Constants.getString("structureMirroringAutoTranslatorLogin") )) {
         try {
            autoTranslatorId = (new SimpleQuery()).forInt("SELECT user_id FROM users WHERE login=?", Constants.getString("structureMirroringAutoTranslatorLogin"));
         } catch(IllegalStateException ex) {
            //User does not exist in DB
         }
      }

      return autoTranslatorId;
   }

   /**
    * Ziska zoznam DocDetails podla zadaneho syncId
    * @param syncId
    * @param skipDocId - ak je zadane docId toto bude v zozname preskocene (napr. ostatne stranky okrem aktualnej)
    * @return
    */
   public static List<DocDetails> getDocBySyncId(int syncId, int skipDocId)
	{
      StringBuilder sql = new StringBuilder();
      sql.append("SELECT ").append(DocDB.getDocumentFields()).append(" FROM documents d WHERE d.sync_id=?");
      if (skipDocId>0) sql.append(" AND d.doc_id!=? ");
      sql.append(" ORDER BY d.doc_id ASC");

      ComplexQuery cq = new ComplexQuery();
      cq.setSql(sql.toString());
      if (skipDocId>0) cq.setParams(syncId, skipDocId);
      else cq.setParams(syncId);

		List<DocDetails> docs = cq.list(new Mapper<DocDetails>()
		{
			public DocDetails map(ResultSet rs) throws SQLException
			{
            try {
				   return DocDB.getDocDetails(rs, true, true);
            } catch (Exception ex) {
               Logger.error(DocMirroringServiceV9.class, ex);
            }
            return null;
			}

		});

      GroupsDB groupsDB = GroupsDB.getInstance();

      //filter groups which is not synced anymore
      List<DocDetails> filtered = new ArrayList<>();
      for (DocDetails doc : docs) {
         List<GroupDetails> parents = groupsDB.getParentGroups(doc.getGroupId(), true);
         for (GroupDetails parent : parents) {
            int[] rootIds = MirroringService.getRootIds(parent.getGroupId());
            if (rootIds != null) {
               filtered.add(doc);
               break;
            }
         }
      }

      return filtered;
	}

   public static int getSyncId(int docId) {
      int syncId = new SimpleQuery().forInt("SELECT sync_id FROM documents WHERE doc_id=?", docId);
      return syncId;
   }

   /**
    * Vrati list inych jazykov ako je aktualne zadana stranka pre zobrazenie prepinaca jazykov
    * - ak je stranka v inom jazyky dostupna vrati jej URL
    * - ak nie je vrati URL homepage ineho jazyka (linka na hlavnu stranku hlavneho adresara daneho jazyka)
    * @param currentDoc
    * @return
    */
   public static List<LabelValueDetails> getOtherLanguages(DocDetails currentDoc) {
      List<LabelValueDetails> languages = new ArrayList<>();
      if (currentDoc == null) return languages;

      List<DocDetails> syncedDocs = getDocBySyncId(currentDoc.getSyncId(), 0);
      GroupsDB groupsDB = GroupsDB.getInstance();
      DocDB docDB = DocDB.getInstance();

      //najdi root adresar nastavenej konfiguracie pre currentDoc
      //je to taky, ktoreho parent uz nema nastavene syncId
      List<GroupDetails> parentDirs = groupsDB.getParentGroups(currentDoc.getGroupId(), true);
      GroupDetails mirroringRootGroup = null;
      for (GroupDetails parent : parentDirs) {
         if (parent.getSyncId()>0) mirroringRootGroup = parent;
      }

      //root sa nenasiel, asi sme v inom ako zrkadlenom adresari
      if (mirroringRootGroup==null) return languages;

      //vygeneruj zoznam prepinaca jazykov podla ROOT adresarov,
      //ak existuje v syncedDocs stranka pre dany adresar, tak sprav odkaz na nu
      //ak neexistuje (je vypnuta) tak sprav odkaz na hlavnu stranku z daneho root adresara
      int[] rootIds = MirroringService.getRootIds(mirroringRootGroup.getGroupId());
      for (int rootId : rootIds) {
         GroupDetails group = groupsDB.getGroup(rootId);

         if (group != null) {
            LabelValueDetails link = new LabelValueDetails();
            link.setLabel(group.getNavbarName());

            DocDetails groupDefaultDoc = docDB.getBasicDocDetails(group.getDefaultDocId(), false);
            if (groupDefaultDoc==null || groupDefaultDoc.isAvailable()==false) continue;
            link.setValue(docDB.getDocLink(groupDefaultDoc.getDocId(), groupDefaultDoc.getExternalLink(), null));

            //skus overit, ci mame linku na stranku v tomto adresari namiesto odkazu na root stranku
            for (DocDetails syncedDoc : syncedDocs) {
               if (syncedDoc.isAvailable()==false) continue;
               GroupDetails syncedDocGroup = groupsDB.getGroup(syncedDoc.getGroupId());
               if (syncedDocGroup != null) {
                  //ak zacina cesta syncedGroup na cestu aktualneho adresara je to spravna vetva
                  if (syncedDocGroup.getFullPath().startsWith(group.getFullPath())) {
                     link.setValue(docDB.getDocLink(syncedDoc.getDocId(), syncedDoc.getExternalLink(), null));
                  }
               }
            }

            languages.add(link);
         }
      }

      return languages;
   }
}