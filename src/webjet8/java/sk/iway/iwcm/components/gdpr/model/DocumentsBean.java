package sk.iway.iwcm.components.gdpr.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.components.gdpr.GdprModule;
import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;

@Entity
@Table(name = "documents")
public class DocumentsBean extends ActiveRecord implements GdprModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_documents")
    @Column(name = "doc_id")
    private int id;
    private String title;
    private String data;
    @Column(name = "data_asc")
    @javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
    private String dataAsc;
    @Column(name = "group_id")
    private int groupId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getDataAsc() {
        return dataAsc;
    }

    public void setDataAsc(String dataAsc) {
        this.dataAsc = dataAsc;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getLinkDelete(HttpServletRequest request)
    {
        return "jstreeDeleteDocByDocIdCustom("+getGroupId()+","+getId()+",this)";
    }

    public String getLinkView(HttpServletRequest request)
    {
        return DocDB.getInstance().getDocLink(getId(),request);
    }

    public String getName()
    {
        return title;
    }

    @Override
    public String getLink() {
        return "/admin/webpages?docid=" + getId();
    }

    @Override
    public String getText(List<GdprRegExpBean> regexps) {
        List<String> texts = new ArrayList<>();
        texts.add(title);
        texts.add(data);
        texts.add(dataAsc);
        return GdprModule.getText(regexps, texts);
    }
}
