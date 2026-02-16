package sk.iway.iwcm.users;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;

/**
 * Entita priradenia konkretneho prava k skupine prav
 */
@Entity
@Table(name="user_perm_groups_perms")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_USER_PERM_GROUP_UPDATE)
public class PermissionInPermissionGroupBean extends ActiveRecordRepository implements Serializable
{
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator="WJGen_user_perm_groups_perms")
	@TableGenerator(name="WJGen_user_perm_groups_perms",pkColumnValue="user_perm_groups_perms")
	@Column(name="perm_id")
	private Long id;

	@Column(name="permission")
	String permission;

	@ManyToOne(cascade=CascadeType.ALL)
	@JsonBackReference(value="group")
	@JoinColumn(name="perm_group_id")
	PermissionGroupBean group;

	@Override
	public Long getId()
	{
		return id;
	}

	@Override
	public void setId(Long id)
	{
		this.id = id;
	}

	@JsonIgnore
	public int getPermGroupId()
	{
		if (group == null) return 0;
		return group.getUserPermGroupId();
	}

	public String getPermission()
	{
		return permission;
	}

	public void setPermission(String permission)
	{
		this.permission = permission;
	}

	public int getUserPermGroupsPermId() {
		if (id == null) return 0;
		return id.intValue();
	}

	public void setUserPermGroupsPermId(int id) {
		this.id = Long.valueOf(id);
	}

	public PermissionGroupBean getGroup()
	{
		return group;
	}

	public void setGroup(PermissionGroupBean permGroup)
	{
		this.group = permGroup;
	}
}