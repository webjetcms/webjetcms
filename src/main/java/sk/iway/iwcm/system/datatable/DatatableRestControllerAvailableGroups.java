package sk.iway.iwcm.system.datatable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.GroupsDB;

/**
 * DatatableRestControllerAvailableGroups is a class that extends DatatableRestControllerV2 and adds the ability to filter
 * entities based on the availableGroups coma separeted IDs of webpage groups.
 * It is used for entities filtered by user perms for webpage groups.
 * WARNING: you must use serverSide: false in datatable options, because it's not possible to filter by availableGroups on server side
 */
@SuppressWarnings("java:S119")
public abstract class DatatableRestControllerAvailableGroups<T, ID extends Serializable> extends DatatableRestControllerV2<T, ID> {

    private String idColumnName = "id";
    private String availableGroupsColumnName = "availableGroups";

    /**
     * DT Constructor with default idColumnName and availableGroupsColumnName
     * @param repo
     * @param idColumnName - column name for entity ID, if null it's "id"
     * @param availableGroupsColumnName - column name for comma separated available groups IDs, if null it's "availableGroups"
     */
    protected DatatableRestControllerAvailableGroups(JpaRepository<T, Long> repo, String idColumnName, String availableGroupsColumnName) {
        super(repo);
        if (idColumnName!=null) this.idColumnName = idColumnName;
        if (availableGroupsColumnName!=null) this.availableGroupsColumnName = availableGroupsColumnName;
    }

    @Override
    public Page<T> getAllItems(Pageable pageable) {
        Page<T> page = new DatatablePageImpl<>(filterByPerms(getRepo().findAll()));
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public void beforeSave(T entity) {
        if (InitServlet.isTypeCloud() && CloudToolsForCore.isControllerDomain()==false) {
            //update availableGroups field in entity from editorFields
            processToEntity(entity, ProcessItemAction.CREATE);
            //in multiweb force to set perms to root groups
            BeanWrapperImpl bw = new BeanWrapperImpl(entity);
            String availableGroups = (String) bw.getPropertyValue(availableGroupsColumnName);
            if (Tools.isEmpty(availableGroups)) {
                bw.setPropertyValue(availableGroupsColumnName, CloudToolsForCore.getRootGroupIds());
                if (bw.isWritableProperty("editorFields")) bw.setPropertyValue("editorFields", null);
                processFromEntity(entity, ProcessItemAction.EDIT);
                //reset value, it will be set from editorFields, othervise it will duplicate IDs (perexGroup)
                if (bw.isWritableProperty("editorFields")) bw.setPropertyValue(availableGroupsColumnName, "");
            }
        }
    }

    /**
     * Filter entities by availableGroups
     * @param all
     * @return
     */
    public List<T> filterByPerms(List<T> all) {
        List<T> filtered = new ArrayList<>();
        for (T entity : all) {
            if (checkItemPerms(entity, null)) {
                filtered.add(entity);
            }
        }
        return filtered;
    }

    @Override
    public boolean checkItemPerms(T entity, Long id) {

        BeanWrapperImpl bw = new BeanWrapperImpl(entity);
        Number entityId = (Number) bw.getPropertyValue(idColumnName);
        String availableGroups = (String) bw.getPropertyValue(availableGroupsColumnName);

        if ((InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) && entityId != null && entityId.longValue()>0) {
            //if it's empty AND it's NOT multiweb then it is available for all domains
            if (InitServlet.isTypeCloud()==false && Tools.isEmpty(availableGroups)) return true;

            if (GroupsDB.isGroupsEditable(getUser(), availableGroups)==false) return false;
            T old = getOneItem(entityId.longValue());
            if (old != null) {
                //check also if original entity is editable, you can't just remove perms and edit entity which not belongs to you
                BeanWrapperImpl bwOld = new BeanWrapperImpl(old);
                availableGroups = (String) bwOld.getPropertyValue(availableGroupsColumnName);
                if (GroupsDB.isGroupsEditable(getUser(), availableGroups)==false) return false;
            }
        }
        return true;
    }
}
