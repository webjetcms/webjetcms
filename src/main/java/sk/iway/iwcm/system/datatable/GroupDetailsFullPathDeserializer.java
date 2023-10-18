package sk.iway.iwcm.system.datatable;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;

/**
 * Deserializuje groupId so zadaneho ID alebo zadanej cesty.
 * Pouziva sa pri importe, ak sa napr. importuju sablony z ineho prostredia a priecinky s danym ID neexistuju.
 * V takom pripade vytvori priecinok podla zadanej cesty (fullPath).
 */
public class GroupDetailsFullPathDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        Integer groupId = null;
        try {
            groupId = Integer.parseInt(text);
        } catch (Exception ex) {

        }
        if (groupId == null) {

            GroupDetails group = GroupsDB.getInstance().getCreateGroup(text);
            if (group != null) {
                groupId = group.getGroupId();
                Prop prop = Prop.getInstance();
                NotifyBean notify = new NotifyBean(prop.getText("templates.import.creategroup.title"), prop.getText("templates.import.creategroup.text", group.getFullPath(), groupId.toString()), NotifyType.INFO);
                DatatableRestControllerV2.addNotify(notify);
            }
        }
        if (groupId == null) groupId = -1;

        return groupId;
    }
}
