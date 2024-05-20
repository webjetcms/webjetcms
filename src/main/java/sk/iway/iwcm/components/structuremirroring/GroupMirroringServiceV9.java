package sk.iway.iwcm.components.structuremirroring;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.system.translation.TranslationService;
//Riesi vytvaranie/zmeny synchronizovanych adresarov podla sync_id - EUSTREAMNW-84
public class GroupMirroringServiceV9 {

   public void handleGroupSave(GroupDetails group, WebjetEventType type) {

      int testGroupId = group.getGroupId();
      if (testGroupId<1) testGroupId = group.getParentGroupId();
      if (MirroringService.isEnabled(testGroupId)==false) return;

      Logger.debug(GroupMirroringServiceV9.class, "handleGroupSave GROUP type=" + type + ", group=" + group + " thread="+Thread.currentThread().getName());

      GroupsDB groupsDB = GroupsDB.getInstance();
      Prop prop = Prop.getInstance();
      if ("Nový podadresár".equals(group.getGroupName()) || prop.getText("editor.dir.new_dir").equals(group.getGroupName())) return;

      if (group.getSyncId() < 1 && group.getGroupId()>0) {
         group.setSyncId(getSyncId(group.getGroupId()));
      }

      if (type == WebjetEventType.ON_START) {
         if (group.getSyncId()<1) {
            //jedna sa o novy adresar, vygenerujeme mu syncId
            group.setSyncId(PkeyGenerator.getNextValue("structuremirroring"));
         }
      } else if (type == WebjetEventType.AFTER_SAVE) {
         if (group.getSyncId()>1) {
            //najdi k tomu mirror verzie
            List<GroupDetails> syncedGroups = getGroupsBySyncId(group.getSyncId(), group.getGroupId());
            List<GroupDetails> mappedGroupsList = MirroringService.getMappingForGroup(group.getParentGroupId());
            List<GroupDetails> mappedGroupsListNotExisting = new ArrayList<>();

            if (mappedGroupsList.size()>syncedGroups.size()) {
               //there is new mapping group created in allready synced groups, we must create missing one
               for (GroupDetails mappedGroup : mappedGroupsList) {
                  boolean containGroup = false;
                  for (GroupDetails syncedGroup : syncedGroups) {
                     if (mappedGroup.getGroupId()==syncedGroup.getParentGroupId()) {
                        //ok, this group is allready synced
                        containGroup = true;
                        break;
                     }
                  }
                  if (containGroup==false) mappedGroupsListNotExisting.add(mappedGroup);
               }
               mappedGroupsList = mappedGroupsListNotExisting;
            }

            if (syncedGroups.isEmpty() || mappedGroupsListNotExisting.size()>0) {
               //este neexistuje, musime vytvorit novu grupu (kopiu)
               //vytvor kopie adresara v ostatnych mapovanych adresaroch

               //ak nie je pre tento adresar ziadne mapovanie, skonci, asi sa jedna o adresar mimo nastaveneho mapovania
               if (mappedGroupsList.size()<1) return;

               for (GroupDetails mappedGroup : mappedGroupsList) {
                  String translatedGroupName = TranslationService.translate(group.getGroupName(), getLanguage(group) , getLanguage(mappedGroup));
                  GroupDetails existing = groupsDB.getGroup(translatedGroupName, mappedGroup.getGroupId());

                  if (existing!=null) {
                     //ak uz existuje, tak ho nevytvaram
                     if (existing.getSyncId()==group.getSyncId()) {
                        continue;
                     }
                     if (existing.getSyncId()<1) {
                        //ak nema syncId, tak ho nastavim
                        existing.setSyncId(group.getSyncId());
                        if (existing.getParentGroupId()>0) {
                           //nastav rovnaky sort priority (poziadavka zo zadania)
                           existing.setSortPriority(group.getSortPriority());
                        }

                        groupsDB.setGroup(existing, false);

                        MirroringService.forceReloadTree();
                        continue;
                     }
                  }

                  GroupDetails mirror = groupsDB.getNewGroupDetails(translatedGroupName, mappedGroup.getGroupId());
                  if (Constants.getBoolean("structureMirroringDisabledOnCreate")) mirror.setMenuType(GroupDetails.MENU_TYPE_HIDDEN);
                  else {
                     mirror.setMenuType(group.getMenuType());
                     mirror.setLoggedMenuType(group.getLoggedMenuType());
                  }
                  mirror.setParentGroupId(mappedGroup.getGroupId());
                  mirror.setDefaultDocId(0);
                  mirror.setSyncId(group.getSyncId());
                  //nastav rovnaky sort priority (poziadavka zo zadania)
                  mirror.setSortPriority(group.getSortPriority());

                  //nastav prava
                  mirror.setForceTheUseOfGroupTemplate(group.isForceTheUseOfGroupTemplate());
                  mirror.setHiddenInAdmin(group.isHiddenInAdmin());
                  mirror.setHtmlHead(group.getHtmlHead());
                  mirror.setInstallName(group.getInstallName());
                  mirror.setInternal(group.isInternal());
                  mirror.setLoggedShowInNavbar(group.getLoggedShowInNavbar());
                  mirror.setLoggedShowInSitemap(group.getLoggedShowInSitemap());
                  mirror.setLoggedShowInNavbar(group.getLoggedShowInNavbar());
                  mirror.setLoggedShowInSitemap(group.getLoggedShowInSitemap());
                  mirror.setPasswordProtected(group.getPasswordProtected());
                  mirror.setShowInNavbar(group.getShowInNavbar());
                  mirror.setShowInSitemap(group.getShowInSitemap());

                  //set optional fields
                  mirror.setFieldA( group.getFieldA() );
                  mirror.setFieldB( group.getFieldB() );
                  mirror.setFieldC( group.getFieldC() );
                  mirror.setFieldD( group.getFieldD() );

                  groupsDB.setGroup(mirror, false);
                  MirroringService.forceReloadTree();
               }
            }

            if (syncedGroups.size()>0) {
               //adresare uz existuju, over, ze tam je vsetko dobre nastavene
               //POZOR na zacyklenie

               //overenie zmeny parent adresara
               GroupDetails parentGroup = groupsDB.getGroup(group.getParentGroupId());
               if (parentGroup != null) {
                  List<GroupDetails> syncedParentGroups = getGroupsBySyncId(parentGroup.getSyncId(), parentGroup.getGroupId());
                  //ok mame zoznam parent adresarov, over ci su mapovane spravne
                  for (GroupDetails syncedGroup : syncedGroups) {
                     GroupDetails syncedCorrectParentGroup = MirroringService.selectMappedGroup(syncedGroup, syncedParentGroups);
                     if (syncedCorrectParentGroup != null) {
                        //porovnaj IDecko voci aktualnemu parentu
                        if (syncedGroup.getParentGroupId()!=syncedCorrectParentGroup.getGroupId()) {
                           Logger.debug(GroupMirroringServiceV9.class, "NESEDI PARENT GROUP, syncedGroup="+syncedGroup.toString()+" syncedCorrectParentGroup="+syncedCorrectParentGroup.toString());
                           GroupDetails groupToSave = groupsDB.getGroup(syncedGroup.getGroupId());
                           groupToSave.setParentGroupId(syncedCorrectParentGroup.getGroupId());
                           groupsDB.setGroup(groupToSave, false);
                           MirroringService.forceReloadTree();
                        }
                     }
                  }
               }

               //overenie sort priority
               for (GroupDetails syncedGroup : syncedGroups) {
                  if (syncedGroup.getSortPriority()!=group.getSortPriority()) {
                     Logger.debug(GroupMirroringServiceV9.class, "NESEDI SORT PRIORITY, syncedGroup="+syncedGroup.toString()+" sort="+syncedGroup.getSortPriority()+" group="+group.toString()+" sort="+group.getSortPriority());
                     GroupDetails groupToSave = groupsDB.getGroup(syncedGroup.getGroupId());
                     if (groupToSave.getParentGroupId()>0) {
                        //ak to nie je root grupa nastav sort priority (root chceme mat usporiadane ako chceme my)
                        groupToSave.setSortPriority(group.getSortPriority());
                        groupsDB.setGroup(groupToSave, false);
                        MirroringService.forceReloadTree();
                     }
                  }
               }

               //overenie zmeny defaultDocId
               int defaultDocSyncId = DocMirroringServiceV9.getSyncId(group.getDefaultDocId());
               if (defaultDocSyncId>0) {
                  List<DocDetails> syncedDocs = DocMirroringServiceV9.getDocBySyncId(defaultDocSyncId, group.getDefaultDocId());
                  for (GroupDetails syncedGroup : syncedGroups) {
                     for (DocDetails syncedDoc : syncedDocs) {
                        GroupDetails syncedDocGroup = groupsDB.getGroup(syncedDoc.getGroupId());
                        if (syncedDocGroup != null) {
                           //ak zacina cesta adresara web stranky na cestu aktualneho sync adresara je to spravna vetva a mozeme stranku pouzit
                           if (syncedDocGroup.getFullPath().startsWith(syncedGroup.getFullPath())) {
                              if (syncedGroup.getDefaultDocId() != syncedDoc.getDocId()) {
                                 Logger.debug(GroupMirroringServiceV9.class, "NESEDI DEFAULT_DOC_ID, syncedGroup="+syncedGroup.toString()+" OLD defaultDocId="+group.getDefaultDocId()+" NEW defaultDocId="+syncedDoc.getDocId()+" group.defaultDocId="+group.getDefaultDocId());
                                 GroupDetails groupToSave = groupsDB.getGroup(syncedGroup.getGroupId());
                                 groupToSave.setDefaultDocId(syncedDoc.getDocId());
                                 groupsDB.setGroup(groupToSave, false);
                                 MirroringService.forceReloadTree();
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      } else if (type == WebjetEventType.ON_DELETE) {
         //musi byt ON_DELETE, pretoze AFTER je uz v kosi
         if (group.getSyncId()>1) {
            //najdi k tomu mirror verzie
            List<GroupDetails> syncedGroups = getGroupsBySyncId(group.getSyncId(), group.getGroupId());
            for (GroupDetails syncedGroup : syncedGroups) {
               Logger.debug(GroupMirroringServiceV9.class, "MAZEM, syncedGroup="+syncedGroup.toString()+" group="+group.toString());
               GroupsDB.deleteGroup(syncedGroup.getGroupId(), true, false, false);
               MirroringService.forceReloadTree();
            }
         }
      }

   }

   /**
    * Vrati group details objekty podla zadaneho syncId, ak je zadane nenulove skipGroupId tak v zozname nebude zadany adresar
    * @param syncId
    * @param skipGroupId - ak je >0 v zozname sa nebude nachadzat zadany adresar
    * @return
    */
   public static List<GroupDetails> getGroupsBySyncId(int syncId, int skipGroupId)
	{
      StringBuilder sql = new StringBuilder();
      sql.append("SELECT * FROM groups WHERE sync_id=?");
      if (skipGroupId>0) sql.append(" AND group_id!=? ");
      sql.append(" ORDER BY group_id ASC");

      ComplexQuery cq = new ComplexQuery();
      cq.setSql(sql.toString());
      if (skipGroupId>0) cq.setParams(syncId, skipGroupId);
      else cq.setParams(syncId);

      GroupsDB groupsDB = GroupsDB.getInstance();

		List<GroupDetails> groups = cq.list(new Mapper<GroupDetails>()
		{
			public GroupDetails map(ResultSet rs) throws SQLException
			{
            GroupDetails group = GroupsDB.fillFieldsByResultSet(rs);
            group.setFullPath(groupsDB.getPath(group.getGroupId()));
				return group;
			}

		});

      //filter groups which is not synced anymore
      List<GroupDetails> filtered = new ArrayList<>();
      for (GroupDetails group : groups) {
         List<GroupDetails> parents = groupsDB.getParentGroups(group.getGroupId(), true);
         for (GroupDetails parent : parents) {
            int[] rootIds = MirroringService.getRootIds(parent.getGroupId());
            if (rootIds != null) {
               filtered.add(group);
               break;
            }
         }
      }

      return filtered;
	}

   public static String getLanguage(GroupDetails group) {

      //najskor dohladaj posla nastaveneho jazyka v parent adresaroch
      GroupDetails parent = group;
      int failsafe = 0;
      GroupsDB groupsDB = GroupsDB.getInstance();
      while (parent != null && failsafe++ < 100) {
         if (Tools.isNotEmpty(parent.getLng())) return parent.getLng();

         parent = groupsDB.getGroup(parent.getParentGroupId());
      }

      //nenaslo sa, tak skus podla sablony
      if (group != null) {
         TemplateDetails temp = TemplatesDB.getInstance().getTemplate(group.getTempId());
         if (temp != null) return temp.getLng();
      }

      return null;
   }

   public static int getSyncId(int groupId) {
      int syncId = new SimpleQuery().forInt("SELECT sync_id FROM groups WHERE group_id=?", groupId);
      return syncId;
   }

}
