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

@Entity
@Table(name = "document_forum")
public class ForumBean extends ActiveRecord implements GdprModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_document_forum")
    @Column(name = "forum_id")
    private int id;
    @Column(name = "doc_id")
    private int docId;
    private String subject;
    private String question;
    @Column(name = "domain_id")
    private int domainId;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getLink() {
        return "/apps/forum/admin/";
    }

    @Override
    public String getText(List<GdprRegExpBean> regexps) {
        List<String> texts = new ArrayList<>();
        texts.add(subject);
        texts.add(question);
        return GdprModule.getText(regexps, texts);
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public int getDomainId() {
        return domainId;
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }

    public String getLinkDelete(HttpServletRequest request)
    {
        return "deleteP("+id+","+docId+",this);";
    }

    public String getLinkView(HttpServletRequest request)
    {
        return DocDB.getInstance().getDocLink(getDocId(),request);
    }

    public String getName()
    {
        return subject;
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }
}
