package sk.iway.iwcm.system.datatable;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.doc.TemplatesGroupDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;

/**
 * Deserializuje TemplatesGroup ID so zadaneho ID alebo zadanej cesty.
 * Pouziva sa pri importe, ak sa napr. importuju sablony z ineho prostredia a skupina sablon neexistuje.
 * V takom pripade vytvori prazdnu skupinu podla zadaneho nazvu a vrati id.
 */
public class TemplateGroupNameDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        Integer id = null;
        try {
            id = Integer.parseInt(text);
        } catch (Exception ex) {

        }
        if (id == null) {
            TemplatesGroupBean entity = TemplatesGroupDB.getTemplatesGroupByName(text);
            if (entity==null) {
                TemplatesGroupDB tgdb = new TemplatesGroupDB();
                entity = new TemplatesGroupBean();
                entity.setName(text);
                tgdb.save(entity);
            }
            if (entity.getId()!=null) id = entity.getId().intValue();
            Prop prop = Prop.getInstance();
            if (id != null) {
                NotifyBean notify = new NotifyBean(prop.getText("templates.import.createTemplatesGroup.title"), prop.getText("templates.import.createTemplatesGroup.text", entity.getName(), id.toString()), NotifyType.INFO);
                DatatableRestControllerV2.addNotify(notify);
            }
        }
        if (id == null) id = -1;

        return id;
    }
}
