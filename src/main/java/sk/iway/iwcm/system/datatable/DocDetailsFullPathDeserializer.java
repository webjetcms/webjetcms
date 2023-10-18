package sk.iway.iwcm.system.datatable;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;

/**
 * Deserializuje docId so zadaneho ID alebo zadanej cesty.
 * Pouziva sa pri importe, ak sa napr. importuju sablony z ineho prostredia a hlavicky/paticky neexistuju.
 * V takom pripade vytvori prazdnu stranku podla zadanej cesty (fullPath) a vrati docId.
 */
public class DocDetailsFullPathDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        Integer docId = null;
        try {
            docId = Integer.parseInt(text);
        } catch (Exception ex) {

        }
        if (docId == null) {
            GroupDetails baseGroup = null;
            String prefix = "/System";
            if (Constants.getBoolean("templatesUseDomainLocalSystemFolder")) baseGroup = GroupsDB.getInstance().getLocalSystemGroup();
            if (baseGroup == null) baseGroup = GroupsDB.getInstance().getGroup(Constants.getInt("headerFooterGroupId"));
            if (baseGroup != null) prefix = baseGroup.getFullPath();
            DocDetails doc = DocDB.getInstance().getCreateDoc(prefix + "/" + text);
            if (doc != null) {
                docId = doc.getDocId();
                Prop prop = Prop.getInstance();
                NotifyBean notify = new NotifyBean(prop.getText("templates.import.createpage.title"), prop.getText("templates.import.createpage.text", doc.getFullPath(), docId.toString()), NotifyType.INFO);
                DatatableRestControllerV2.addNotify(notify);
            }
        }
        if (docId == null) docId = -1;

        return docId;
    }
}
