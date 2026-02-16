package sk.iway.iwcm.users;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.Query;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.jpa.JpaTools;


/**
 *  PermissionGroupDB.java
 *
 *  Zakladne CRUD operacie pre skupiny prav
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 16.04.2010 14:09:28
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class PermissionGroupDB extends JpaDB<PermissionGroupBean>
{

	public PermissionGroupDB() {
		super(PermissionGroupBean.class);
	}

	private static final SimpleQuery query = new SimpleQuery();

	//-----------------------------------CRUD OPERACIE-------------------------------

	public static void delete(int id)
	{
		PermissionGroupDB pgdb = new PermissionGroupDB();
		PermissionGroupBean group = pgdb.getById(id);

		if (group != null) {
			Adminlog.add(Adminlog.TYPE_USER_PERM_GROUP_DELETE, "Zmazana skupina prav "+group.title, id, -1);

			group.delete();
			query.execute("DELETE FROM user_perm_groups_perms WHERE perm_group_id = ?", id);
			query.execute("DELETE FROM users_in_perm_groups WHERE perm_group_id = ?", id);
		}
	}

	@Override
	public List<PermissionGroupBean> getAll()
	{
		return PermissionGroupDB.getPermissionGroups(null);
	}

	/**
	 * Vrati vsetky vytvorene skupiny prav, ktorych nazov obsahuje vstupny parameter groupName
	 *
	 * @param groupName nazov skupiny
	 * @return
	 */
	public static List<PermissionGroupBean> getPermissionGroups(String groupName)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

		ExpressionBuilder builder = new ExpressionBuilder();
		ReadAllQuery all = new ReadAllQuery(PermissionGroupBean.class, builder);

		if (Tools.isNotEmpty(groupName))
		{
			Expression expr = builder.get("title").likeIgnoreCase("%" + groupName + "%");
			all.setSelectionCriteria(expr);
		}

		Query query = em.createQuery(all);
		return JpaDB.getResultList(query);
	}

    /**
     * Vrati vsetky skupinu prav, ktorej nazov je rovnaky ako groupName
     *
     * @param groupName nazov skupiny
     * @return
     */
    public static PermissionGroupBean getPermissionGroup(String groupName)
    {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

        ExpressionBuilder builder = new ExpressionBuilder();
        ReadAllQuery all = new ReadAllQuery(PermissionGroupBean.class, builder);

        if (Tools.isNotEmpty(groupName))
        {
            Expression expr = builder.get("title").equal(groupName);
            all.setSelectionCriteria(expr);
        }

        Query query = em.createQuery(all);
        return (PermissionGroupBean) query.getSingleResult();
    }



	//---------------------------OPREACIE NAD PermissionGroupBean-------------------------------

	public static int getUserCount(PermissionGroupBean permGroup)
	{
		return query.forInt("SELECT COUNT(upg.user_id) FROM users_in_perm_groups upg JOIN users u ON (upg.user_id = u.user_id) WHERE u.is_admin="+DB.getBooleanSql(true)+" AND perm_group_id = ?", permGroup.getId());
	}

	public static List<Number> getUserIds(PermissionGroupBean permGroup)
	{
		return query.forListNumber("SELECT user_id FROM users_in_perm_groups WHERE perm_group_id = ?", permGroup.getId());
	}

	public static int savePermission(int permissionGroupId,String newName, String[] newPermissions)
	{
		return savePermission(permissionGroupId, newName, newPermissions, null, null, null);
	}

	@SuppressWarnings("unchecked")
	public static int savePermission(int permissionGroupId,String newName, String[] newPermissions,String editableGroups,String editablePages,String writableFoldersField)
	{
		if (Tools.isEmpty(newName))
			throw new IllegalArgumentException("users.permission_group.errors.empty_name");

//		if (newPermissions == null)
//			throw new IllegalArgumentException("users.permission_group.errors.empty_set");

	   PermissionGroupDB pgdb = new PermissionGroupDB();

		if (permissionGroupId < 1)
		{
			boolean nameAllreadyExists = false;
			for (PermissionGroupBean perm : pgdb.getAll())
			{
				if (perm.getTitle().equalsIgnoreCase(newName))
				{
					nameAllreadyExists = true;
					break;
				}
			}
			if (nameAllreadyExists)
			{
				throw new IllegalArgumentException("users.permission_group.errors.title_allready_exists");
			}
		}

		PermissionGroupBean permGroup;

		if (permissionGroupId < 1)
		{
			permGroup = new PermissionGroupBean();
			Adminlog.add(Adminlog.TYPE_USER_PERM_GROUP_CREATE, "Vytvorena skupina prav: "+newName+". Jej prava: "+Arrays.toString(newPermissions), -1, -1);
		}
		else
		{
			permGroup = pgdb.getById(permissionGroupId);
		}

		if (permGroup == null)
			throw new IllegalArgumentException("PermissionGroup with id: "+permissionGroupId+" does not exist");

		permGroup.setTitle(newName);

		if(editableGroups != null)
			permGroup.setEditableGroups(editableGroups);
		if(editablePages != null)
			permGroup.setEditablePages(editablePages);
		if(writableFoldersField != null)
			permGroup.setWritableFolders(writableFoldersField);

		Collection<String> whichToBeGranted = null;
		Collection<String> whichToBeTaken = null;
		if(newPermissions != null)
		{
			List<String> newPerms = Arrays.asList(newPermissions);
			List<String> oldPerms = permGroup.getPermissionNames();

			whichToBeGranted = CollectionUtils.subtract(newPerms, oldPerms);
			whichToBeTaken = CollectionUtils.subtract(oldPerms, newPerms);

			logChanges(permGroup, whichToBeGranted, whichToBeTaken);
		}


		if(newPermissions != null) {
			takePermissions(permGroup, whichToBeTaken);
			grantPermissions(permGroup, whichToBeGranted);
		}

		pgdb.save(permGroup);

		return permGroup.getUserPermGroupId();
	}

	private static void logChanges(PermissionGroupBean permGroup, Collection<String> whichToBeGranted, Collection<String> whichToBeTaken)
	{
		if (permGroup.getUserPermGroupId() > 0 && (whichToBeGranted.size() > 0 || whichToBeTaken.size() > 0))
		{
			StringBuilder message = new StringBuilder("Zmenena skupina prav: ").append(permGroup.title).append('.').append(". \n").
				append("Pridane prava: ").append(whichToBeGranted).append(", \n").
				append("Odobrane prava: ").append(whichToBeTaken);
			Adminlog.add(Adminlog.TYPE_USER_PERM_GROUP_UPDATE, message.toString(), permGroup.getId().intValue(), -1);
		}
	}

	private static void grantPermissions(PermissionGroupBean permGroup, Collection<String> whichToBeAdded)
	{
		for (String permission : whichToBeAdded)
		{
			//addPermission(permGroup.getId(), permission);
			permGroup.addPermission(permission);
		}
	}

	private static void takePermissions(PermissionGroupBean permGroup, Collection<String> whichToBeTaken)
	{
		for (String permission : whichToBeTaken)
		{
			//takePermission(permGroup.getId(), permission);
			permGroup.deletePermission(permission);
		}
	}

}