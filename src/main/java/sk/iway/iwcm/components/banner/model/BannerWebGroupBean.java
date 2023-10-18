package sk.iway.iwcm.components.banner.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.adminlog.EntityListenersType;

@Entity
@Table(name="banner_gr")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_BANNER_CREATE)
public class BannerWebGroupBean extends ActiveRecordRepository implements Serializable {

	@JsonIgnore
    private static final long serialVersionUID = -1L;

    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_banner_gr")
	@Column(name="id")
	private Long id;

	@Column(name="group_id")
	private Integer groupId;

	@ManyToOne
	@JsonBackReference(value="bannerBeanGr")
	@JoinColumn(name="banner_id")
	private BannerBean bannerBeanGr;

	public BannerWebGroupBean() {}

	public BannerWebGroupBean(String path) {
        GroupDetails group = GroupsDB.getInstance().getCreateGroup(path);
        if (group != null) groupId = group.getGroupId();
    }

	@Override
	public void setId(Long id)
	{
		this.id = id;
	}

	@Override
	public Long getId()
	{
		return id;
	}

	public int getGroupId()
	{
		return groupId;
	}

	public void setGroupId(int groupId)
	{
		this.groupId = groupId;
	}

	@JsonProperty(access = Access.READ_ONLY)
    public String getFullPath() {
		GroupDetails grp = GroupsDB.getInstance().getGroup(groupId);
		if (grp != null) return grp.getFullPath();
		return "";
	}

	@JsonIgnore
	public BannerBean getBanner() {
		return bannerBeanGr;
	}

	public void setBanner(BannerBean bannerBeanGr) {
		this.bannerBeanGr = bannerBeanGr;
	}

}
