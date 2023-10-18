package sk.iway.iwcm.components.gdpr.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.components.gdpr.GdprModule;
import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.i18n.Prop;

@Entity
@Table(name = "questions_answers")
public class QuestionsAnswersBean extends ActiveRecord implements GdprModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_questions_answers")
    @Column(name = "qa_id")
    private int id;

    @Column(name = "group_name")
    private String groupName;
    private String question;
    private String answer;
    private String hash;
    @Column(name = "domain_id")
    private int domainId;
    @Transient
    Prop prop;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getLink() {
        return "/components/qa/admin_answer.jsp?id=" + id + "&hash=" + hash;
    }

    @Override
    public String getText(List<GdprRegExpBean> regexps) {
        List<String> texts = new ArrayList<>();
        texts.add(groupName);
        texts.add(question);
        texts.add(answer);
        return GdprModule.getText(regexps, texts);
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getDomainId() {
        return domainId;
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }

    public String getLinkDelete(HttpServletRequest request)
    {
        return "deleteOK('"+getProp(request).getText("components.qa.delete_confirm")+"',this,'/qa.add.do?action=delete&qaId="+id+"')";
    }

    public String getLinkView(HttpServletRequest request)
    {
        return getLink();
    }
    public String getName()
    {
        return question;
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
