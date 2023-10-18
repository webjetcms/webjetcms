package sk.iway.iwcm.editor.rest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

/**
 * Entita pre ziskani zoznamu parentov pre zadane docid/groupid pri volani /parents/{id} pre Groups a Webpages.
 * Okrem ID parentov vrati aj domenove meno a pripadne kartu (Priecinky, System, Kos).
 */
@Getter
@Setter
public class ParentGroupsResult {
    private boolean found;
    private JsTreeTab tab;
    private String domain;
    private List<Integer> parents;

    /**
     * Metoda pre nastavenie typu Tabu z parent adresarov
     * @param parentGroups List<GroupDetails>
     * @return JsTreeTab
     */
    private void setTabFromParentGroups(List<GroupDetails> parentGroups) {
        final Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
        final String trashDirPath = propSystem.getText("config.trash_dir");
        if (parentGroups.stream().anyMatch(p -> p.getFullPath().equals(trashDirPath))) {
            setTab(JsTreeTab.TRASH);
            return;
        }

        GroupDetails localSystemGroup = GroupsDB.getInstance().getLocalSystemGroup();
        final int localSystemGroupId = localSystemGroup!=null ? localSystemGroup.getGroupId() : -1;

        List<GroupDetails> parentGroupsChecked = new ArrayList<>();
        for (GroupDetails p : parentGroups) {
            if (p.getFullPath().equals("/System") || p.getGroupId()==localSystemGroupId) {
                setTab(JsTreeTab.SYSTEM);
                //remove other parent groups, if it's not root folder
                if (parentGroupsChecked.size()!=parentGroups.size()) {
                    parentGroups.clear();
                    parentGroups.addAll(parentGroupsChecked);
                }
                return;
            }
            parentGroupsChecked.add(p);
        }

        setTab(JsTreeTab.FOLDER);
    }

    public void setParentGroups(GroupDetails group, List<GroupDetails> parentGroups) {
        setFound(true);
        // tab podla toho, v akych adresaroch sa nachadza
        setTabFromParentGroups(parentGroups);

        // vyfiltrovanie kosu a system adresaru z vysledku, kedze sa v JsTree nenachadza
        if (Constants.getBoolean("templatesUseRecursiveSystemFolder")==false) parentGroups = filterSystemAndTrash(parentGroups);

        // IDcka adresarov
        parents = parentGroups.stream().map(GroupDetails::getGroupId).collect(Collectors.toList());
        Collections.reverse(parents);

        // domena z adresaru
        setDomain(group.getDomainName());
    }

    public enum JsTreeTab {
        FOLDER, SYSTEM, TRASH
    }

    /**
     * Metoda pre vyfiltrovanie adresarov (System a Kos) zo zoznamu
     * @param parentGroups List<GroupDetails>
     * @return List<GroupDetails>
     */
    private List<GroupDetails> filterSystemAndTrash(List<GroupDetails> parentGroups) {
        final Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
        final String trashDirPath = propSystem.getText("config.trash_dir");

        return parentGroups.stream().filter(p -> !p.getFullPath().equals(trashDirPath) && !p.getFullPath().equals("/System")).collect(Collectors.toList());
    }
}