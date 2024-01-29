package sk.iway.iwcm.components.forum.jpa;

public class DocForumIdAndParentId {
    private Long id;
    private Integer parentId;

    public DocForumIdAndParentId(Long id, Integer parentId) {
        this.id = id;
        this.parentId = parentId;
    }

    public Long getId() {
        return id;
    }

    public Integer getParentId() {
        return parentId;
    }
}
