package sk.iway.iwcm.components.gdpr.model;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class GdprResult {
    private int id;
    private String link;
    private String name;
    private String text;
    private String linkView;
    private String linkDelete;

    public GdprResult (GdprModel model, List<GdprRegExpBean> regexps, HttpServletRequest request) {
        this.setId(model.getId());
        this.setLink(model.getLink());
        this.setText(model.getText(regexps));
        this.setLinkView(model.getLinkView(request));
        this.setLinkDelete(model.getLinkDelete(request));
        this.setName(model.getName());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLinkDelete() {
        return linkDelete;
    }

    public void setLinkDelete(String linkDelete) {
        this.linkDelete = linkDelete;
    }

    public String getLinkView() {
        return linkView;
    }

    public void setLinkView(String linkView) {
        this.linkView = linkView;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
