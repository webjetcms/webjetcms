package sk.iway.iwcm.users;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Transient;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import org.eclipse.persistence.jpa.JpaEntityManager;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 * Zastupuje SKUPINU prav, to znamena kolekciu prav zoskupenych pod nejakym nazvom. NEMAZAT - pri pouziti verzie z WJ8 nastane zacyklenie springfox (aj ked su triedy naoko rovnake)
 */
@Entity
@Table(name="user_perm_groups")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_USER_PERM_GROUP_UPDATE)
public class PermissionGroupBean extends ActiveRecordRepository implements Serializable
{
	private static final long serialVersionUID = -1L;

	public PermissionGroupBean(){
		//constructor
	}

	@Id
	@GeneratedValue(generator="WJGen_user_perm_groups")
	@TableGenerator(name="WJGen_user_perm_groups",pkColumnValue="user_perm_groups")
	@Column(name="group_id")
	@DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

	@Column(name="group_title")
	@NotBlank
	@DataTableColumn(
		inputType = DataTableColumnType.OPEN_EDITOR,
		title = "[[#{users.permission_group.name}]]",
		tab = "basicInfo"
	)
	String title;

	//***This columns are redirect through editorFields***
	@Column(name="writable_folders")
	String writableFolders;

	@Column(name="editable_groups")
	String editableGroups;

	@Column(name="editable_pages")
	String editablePages;

	//***

	@JsonManagedReference(value="group")
	@OneToMany(fetch = FetchType.LAZY, cascade=CascadeType.ALL, targetEntity=PermissionInPermissionGroupBean.class, mappedBy="group")
	private Set<PermissionInPermissionGroupBean> permissions = null;

    @DataTableColumnNested
	@Transient
	private PermissionGroupEditorFields editorFields = null; //NOSONAR

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this. id = id;
	}

	@JsonIgnore
	public int getUserPermGroupId() {
		if (id == null) return 0;
		return id.intValue();
	}

	@JsonIgnore
	public void setUserPermGroupId(int id) {
		this.id = Long.valueOf(id);
	}

	public void deletePermission(String permission)
	{
		if (permissions == null) return;
		for (Iterator<PermissionInPermissionGroupBean> iterator = permissions.iterator(); iterator.hasNext();)
		{
			PermissionInPermissionGroupBean pg = iterator.next();
			if (permission.equalsIgnoreCase(pg.getPermission()))
			{
				pg.delete();
				JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
				em.remove(pg);

				iterator.remove();
			}
		}
	}

	public void addPermission(String permission)
	{
		if (permissions == null) permissions = new HashSet<>();
		for (PermissionInPermissionGroupBean perm : permissions)
			if (permission.equalsIgnoreCase(perm.getPermission()))
				return;

		PermissionInPermissionGroupBean newPerm = new PermissionInPermissionGroupBean();
		newPerm.setPermission(permission);
		permissions.add(newPerm);
		newPerm.group = this;
	}

	public List<String> getPermissionNames()
	{
		List<String> perms = new ArrayList<>();
		if (permissions == null) return perms;

		for (PermissionInPermissionGroupBean perm : permissions)
			perms.add(perm.permission);
		return perms;
	}

	public Set<String> getPermissionSet()
	{
		Set<String> perms = new HashSet<>();
		if (permissions == null) return perms;

		for (PermissionInPermissionGroupBean perm : permissions)
			perms.add(perm.permission);
		return perms;
	}

	public String getWritableFolders() {
		return writableFolders;
	}

	public void setWritableFolders(String writableFolders) {
		this.writableFolders = writableFolders;
	}

	public String getEditableGroups() {
		return editableGroups;
	}

	public void setEditableGroups(String editableGroups) {
		this.editableGroups = editableGroups;
	}

	public String getEditablePages() {
		return editablePages;
	}

	public void setEditablePages(String editablePages) {
		this.editablePages = editablePages;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PermissionGroupBean that = (PermissionGroupBean) o;

		if (!Objects.equals(id, that.id) || !Objects.equals(title, that.title)) return false;

		return true;
	}
}
