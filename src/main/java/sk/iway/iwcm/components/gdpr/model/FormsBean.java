package sk.iway.iwcm.components.gdpr.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.components.gdpr.GdprModule;
import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.i18n.Prop;

@Entity
@Table(name = "forms")
public class FormsBean extends ActiveRecord implements GdprModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_forms")
    private int id;

    @Column(name = "form_name")
    private String formName;
    private String data;
    private String html;
    @Column(name = "domain_id")
    private int domainId;
    @Column(name = "doc_id")
    private int docId;
    @Transient
    Prop prop;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getLink() {
        return "/admin/formlist.do?showArchived=false&id=" + id;
    }

    @Override
    public String getText(List<GdprRegExpBean> regexps) {
        List<String> texts = new ArrayList<>();
        texts.add(formName);
        texts.add(data);
        texts.add(html);
        return GdprModule.getText(regexps, texts);
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public int getDomainId() {
        return domainId;
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }

    public String getLinkDelete(HttpServletRequest request)
    {
        return "deleteOK('"+getProp(request).getText("formslist.do_you_really_want_to_delete")+"',this,'/admin/formdel.do?formid="+id+"')";
    }

    public String getLinkView(HttpServletRequest request)
    {
        return "/admin/rest/forms-list/html/?id="+id; //GdprLinkGenerator.getWebPageLinkView(request,getDocId());
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public String getName()
    {
        return formName;
    }

    public Prop getProp() {
        return prop;
    }

    public Prop getProp(HttpServletRequest request) {
        if(prop == null)
            prop = Prop.getInstance(request);

        return prop;
    }

    public void setProp(Prop prop) {
        this.prop = prop;
    }
}
