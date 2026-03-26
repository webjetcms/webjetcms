package sk.iway.iwcm.components.structuremirroring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;

/**
 * Vseobecna trieda pre mirrorovanie struktury. Synchronizuju sa zadane ID adresarov
 * (nemusi sa jednat o ROOT adresare, ak jed ich oznacujeme ako korenove - je to tak z pohladu  mirroringu).
 *
 * Zosynchronizovane adresare/stranky NESMU byt vymazane, musia byt oznacene ako nezobrazovat
 *
 * Synchronizuje sa:
 * - vytvorenie/zmazanie
 * - presunutie v strukture
 * - zmena poradia (sort_priority)
 *
 * EUSTREAMNW-84
 */
public class MirroringService {

   public static final String MIRRORING_MODE_MIRROR = "mirror";
   public static final String MIRRORING_MODE_CLONE = "clone";

   private MirroringService() {
      //tools class
   }

   /**
    * Overi, ci je mirrorovanie vobec zapnuta/nakonfigurovane
    * @return
    */
   public static boolean isEnabled(int groupId) {
      String mirroringConfig = Constants.getString("structureMirroringConfig");

      if (Tools.isEmpty(mirroringConfig)) return false;

      //over, ci niektory z parent adresarov je v konfiguracii
      GroupsDB groupsDB = GroupsDB.getInstance();
      List<GroupDetails> parents = groupsDB.getParentGroups(groupId, true);
      String[] lines = Tools.getTokens(mirroringConfig, "\n");
      for (String line : lines) {
         //odstan poznamku
         String ids = line;
         int i = line.indexOf(":");
         if (i>0) ids = line.substring(0, i);

         int[] mapping = Tools.getTokensInt(ids, ",");
         for (int id : mapping) {
            for (GroupDetails parent : parents) {
               if (parent.getGroupId()==id) return true;
            }
         }
      }
      return false;
   }

   /**
    * Vrati ostatne mapovane adresare pre zadane groupId
    * - v zozname je odstraneny GroupDetails s groupId zhodnym s parametrom groupId
    * - ak je parametr groupId korenovy adresar nastaveny v mapovani vrati ostatne adresare z mapovania
    * - ak sa nejedna o korenovy adresar z mapovania, vrati ostatne adresare podla syncId
    * @param groupId
    * @return
    */
   public static List<GroupDetails> getMappingForGroup(int groupId) {
      GroupsDB groupsDB = GroupsDB.getInstance();
      List<GroupDetails> mapped = new ArrayList<>();
      //najskor over, ci to nie je root grupa podla mappingu

      int[] rootIds = getRootIds(groupId);
      if (rootIds != null) {
         //prebehni ich a pridaj do listu
         for (int rootGroup : rootIds) {
            if (rootGroup == groupId) continue;
            GroupDetails group = groupsDB.getGroup(rootGroup);
            if (group != null) mapped.add(group);
         }
      } else {
         GroupDetails group = groupsDB.getGroup(groupId);
         if (group != null && group.getSyncId() > 0) {
            List<GroupDetails> groupsBySync = GroupMirroringServiceV9.getGroupsBySyncId(group.getSyncId(), group.getGroupId());
            mapped.addAll(groupsBySync);
         }
      }

      //Check if they are enabled for mirroring
      List<GroupDetails> filtered = new ArrayList<>();
      for(GroupDetails group : mapped) {
         if (MirroringService.isEnabled(group.getGroupId())) filtered.add(group);
      }

      return filtered;
   }

   /**
    * Vrati mapovanie pre zadane groupId
    * @param groupId - ID adresara, POZOR jedna sa o jeden z nastavenych adresarov, cize ROOT adresar
    * @return
    */
   public static int[] getRootIds(int groupId) {
      //format zapisu: id,id,id:POZNAMKA\n
      String mirroringConfig = Constants.getString("structureMirroringConfig");
      String[] lines = Tools.getTokens(mirroringConfig, "\n");

      if(MIRRORING_MODE_CLONE.equals( Constants.getString("mirroringMode") )) {
         int srcId = Constants.getInt("cloneActionSrcId");
         int destId = Constants.getInt("cloneActionDestId");

         if(groupId == srcId) {
            return new int[] {srcId, destId};
         } else {
            return getCloneRootIds(groupId, lines);
         }
      } else {
         return getMirrorRootIds(groupId, lines);
      }
   }

   private static int[] getMirrorRootIds(int groupId, String[] lines) {
      //accept only lines that contain wanted id and prevent duplicates
      Set<Integer> toReturn = new HashSet<>();

      for (String line : lines) {
         //odstan poznamku
         String ids = line;
         int i = line.indexOf(":");
         if (i > 0) ids = line.substring(0, i);

         int[] mapping = Tools.getTokensInt(ids, ",");
         for (int id : mapping) {
            if (id > 0 && id == groupId) {
               //nasli sme riadok, v ktorom je zelane id -> push-ni ich do set-u (groupId moze byt aj v inych riadkoch)
               for(int foundId : mapping) {
                  toReturn.add( foundId );
               }
               break;
            }
         }
      }

      return toReturn.size() < 1 ? null : toReturn.stream().mapToInt(Integer::intValue).toArray();
   }

   private static int[] getCloneRootIds(int groupId, String[] lines) {
      int destId = Constants.getInt("cloneActionDestId");

      //Accept only lines that contain wanted id AND DEST id
      Set<Integer> toReturn = new HashSet<>();

      for (String line : lines) {
         //odstan poznamku
         String ids = line;
         int i = line.indexOf(":");
         if (i > 0) ids = line.substring(0, i);

         int[] mapping = Tools.getTokensInt(ids, ",");
         boolean containWanted = false;
         boolean containDest = false;
         for (int id : mapping) {
            if (id > 0 && id == groupId) {
               containWanted = true;
            }

            if (id > 0 && id == destId) {
               containDest = true;
            }

            if(containWanted && containDest) {
               for(int foundId : mapping) {
                  toReturn.add( foundId );
               }
               break;
            }
         }
      }

      return toReturn.size() < 1 ? null : toReturn.stream().mapToInt(Integer::intValue).toArray();
   }

   /**
    * V zozname syncedGroups najde taky, ktory vyhovuje vetve stromu zadanej pomocou group (ked mame mapovanie typu SK,EN,DE,FR)
    * @param group
    * @param syncedGroups
    * @return
    */
   public static GroupDetails selectMappedGroup(GroupDetails group, List<GroupDetails> syncedGroups) {
      //ziskaj zoznam parent adresarov
      GroupsDB groupsDB = GroupsDB.getInstance();
      List<GroupDetails> parentGroups = groupsDB.getParentGroups(group.getGroupId(), true);
      //preiteruj parent grupy, kym nenajdes nejake mapovanie, ideme od najnizsej urovne
      for (GroupDetails rootGroup : parentGroups) {
         //ako prve musime prebehnut jednotlive riadky mapovania a vobec identifikovat riadok v ktorom je nase mapovanie
         //robime to tak, ze iterujeme cez parent grupy a snazime sa najst root mapovanie
         int[] rootIds = getRootIds(rootGroup.getGroupId());
         //rootGroup je najdena hlavna grupa mapovania, preto ju volame rootGroup
         if (rootIds != null) {
            //super, nasli sme riadok mapovania, teraz preiteruj syncedGroups a najdi taku, ktora patri do nasej vetvy
            for (GroupDetails synced : syncedGroups) {
               List<GroupDetails> syncedParentGroups = groupsDB.getParentGroups(synced.getGroupId(), true);
               syncedParentGroups.add(synced);
               for (GroupDetails syncedParent : syncedParentGroups) {
                  if (syncedParent.getGroupId()==rootGroup.getGroupId()) return synced;
               }
            }
         }
      }
      return null;
   }

   /**
    * Vyvolane po zmene konfiguracie, nastavi sync_id pre korenove adresare v konfiguracii
    */
   public static void checkRootGroupsConfig() {
      //preiteruj riadky konfiguracie a over/nastav zadanym adresarom syncid
      //format zapisu: id,id,id:POZNAMKA\n
      String mirroringConfig = Constants.getString("structureMirroringConfig");
      String[] lines = Tools.getTokens(mirroringConfig, "\n");
      GroupsDB groupsDB = GroupsDB.getInstance();
      for (String line : lines) {
         //odstan poznamku
         String ids = line;
         int i = line.indexOf(":");
         if (i>0) ids = line.substring(0, i);

         int[] mapping = Tools.getTokensInt(ids, ",");
         int syncId = 0;
         int defaultDocSyncId = 0;
         StringBuilder groupIdsList = new StringBuilder();
         StringBuilder defaultDocIdsList = new StringBuilder();
         for (int id : mapping) {
            if (id > 0) {
               GroupDetails group = groupsDB.getGroup(id);
               if (group == null) continue;

               if (group.getSyncId()>0) syncId = group.getSyncId();

               if (groupIdsList.length()>0) groupIdsList.append(",");
               groupIdsList.append(String.valueOf(group.getGroupId()));

               //ziskaj hlavnu stranku adresara
               if (group.getDefaultDocId()>0) {
                  int docSyncId = DocMirroringServiceV9.getSyncId(group.getDefaultDocId());
                  if (docSyncId>0) defaultDocSyncId = docSyncId;

                  if (defaultDocIdsList.length()>0) defaultDocIdsList.append(",");
                  defaultDocIdsList.append(String.valueOf(group.getDefaultDocId()));
               }
            }
         }
         if (groupIdsList.length()>0) {
            if (syncId==0) syncId = PkeyGenerator.getNextValue("structuremirroring");
            //nastav syncId vsetkym v riadku
            new SimpleQuery().execute("UPDATE groups SET sync_id=? WHERE group_id IN ("+groupIdsList.toString()+")", syncId);
         }
         //nastav sync aj pre hlavnu stranku adresara
         if (defaultDocIdsList.length()>0) {
            if (defaultDocSyncId==0) defaultDocSyncId = PkeyGenerator.getNextValue("structuremirroring");
            //nastav syncId vsetkym v riadku
            new SimpleQuery().execute("UPDATE documents SET sync_id=? WHERE doc_id IN ("+defaultDocIdsList.toString()+")", defaultDocSyncId);
         }
      }
      //refreshni GroupsDB
      GroupsDB.getInstance(true);
      DocDB.getInstance(true);
   }

   public static void forceReloadTree() {
      RequestBean.setAttribute("forceReloadTree", Boolean.TRUE);
   }

   /**
    * Clear syncId property in database for all groups and documents in tree
    * @param rootGroupId - ID of root group
    */
   public static void clearSyncId(int rootGroupId) {
      if(rootGroupId == 0) {
         //Clear ALL sync_id's
         (new SimpleQuery()).execute("UPDATE groups SET sync_id=0");
         (new SimpleQuery()).execute("UPDATE documents SET sync_id=0");
      } else {
         GroupsDB groupsDB = GroupsDB.getInstance();

         List<GroupDetails> groups = groupsDB.getGroupsTree(rootGroupId, true, true);
         //convert group.getGroupId to comma separated String
         StringBuilder groupIds = new StringBuilder();
         for(GroupDetails group : groups) {
            if(groupIds.isEmpty()==false) groupIds.append(",");
            groupIds.append(String.valueOf(group.getGroupId()));
         }

         if (Tools.isNotEmpty(groupIds)) {
            //update database
            (new SimpleQuery()).execute("UPDATE groups SET sync_id=0 WHERE group_id IN ("+groupIds.toString()+")");
            (new SimpleQuery()).execute("UPDATE documents SET sync_id=0 WHERE group_id IN ("+groupIds.toString()+")");
         }
      }
   }
}
