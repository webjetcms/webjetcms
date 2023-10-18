package sk.iway.iwcm.database.nestedsets;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.eclipse.persistence.jpa.JpaEntityManager;

import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.database.DataSource;
import sk.iway.iwcm.database.nestedsets.annotations.LeftColumn;
import sk.iway.iwcm.database.nestedsets.annotations.LevelColumn;
import sk.iway.iwcm.database.nestedsets.annotations.RightColumn;
import sk.iway.iwcm.database.nestedsets.annotations.RootColumn;
import sk.iway.iwcm.system.jpa.JpaTools;

@MappedSuperclass
abstract public class CommonNestedSetBean<T extends NodeInfo> extends ActiveRecord implements NodeInfo
{
	public static List<String> TO_STRING_IGNORED_PROPERTIES = Arrays.asList("parent", "ancestors", "children", "descendants");
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;

	@Column(updatable=false)
	@LeftColumn
	private int lft;

	@RightColumn
	@Column(updatable=false)
	private int rgt;

	@LevelColumn
	@Column(updatable=false, name="lvl")
	private int level;

	@RootColumn
	private int rootId;

	@Transient
	protected T parent = null;

	@Transient
	protected boolean createRoot = false;


	@Override
	public int getId()
	{
		return id;
	}

	@Override
	public void setId(int id)
	{
		this.id = id;
	}

	@Override
	@JsonIgnore
	public int getLeftValue()
	{

		return lft;
	}

	@Override
	@JsonIgnore
	public int getRightValue()
	{
		return rgt;
	}

	@Override
//	@JsonIgnore
	public int getLevel()
	{
		return level;
	}

	@Override
	@JsonIgnore
	public int getRootValue()
	{
		return rootId;
	}

	@Override
	public void setLeftValue(int value)
	{
		lft = value;
	}

	@Override
	public void setRightValue(int value)
	{
		rgt = value;
	}

	@Override
	public void setLevel(int level)
	{
		this.level = level;
	}

	@Override
	public void setRootValue(int value)
	{
		rootId = value;
	}


	@Override
	@SuppressWarnings("unchecked")
	public boolean save()
	{
		String dbName = "iwcm";
		if (this.getClass().isAnnotationPresent(DataSource.class)) {

			Annotation annotation = this.getClass().getAnnotation(DataSource.class);
			DataSource dataSource = (DataSource) annotation;
			dbName = dataSource.name();
		}
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(dbName);
		if (this.id>0)
		{

			boolean commitAfterSuccess = false;
			if (!em.getTransaction().isActive())
			{
				commitAfterSuccess = true;
				em.getTransaction().begin();
			}
			try
			{
				em.merge(this);
				if (commitAfterSuccess)
				{
					em.getTransaction().commit();
				}
				return true;
			}
			catch (Exception e) {sk.iway.iwcm.Logger.error(e);}

		}
		else
		{
			if (parent==null && !createRoot) throw new IllegalStateException("'parent' must be specified!");
			if (parent!=null)
			{
				JpaNode<T> parentJpaNode = new JpaNode<T>(parent, new JpaNestedSetManager(em));
				try
				{
					boolean commitAfterSuccess = false;
					if (!em.getTransaction().isActive())
					{
						commitAfterSuccess = true;
						em.getTransaction().begin();
					}
					parentJpaNode.addChild((T)this);
					if (commitAfterSuccess)
					{
						em.getTransaction().commit();
					}
					parentJpaNode = null;
					return true;
				}
				catch (Exception e) { sk.iway.iwcm.Logger.error(e);}
				parent = null;
			}
			else
			{
				JpaNestedSetManager nsm = new JpaNestedSetManager(em);
				boolean commitAfterSuccess = false;
				if (!em.getTransaction().isActive())
				{
					commitAfterSuccess = true;
					em.getTransaction().begin();
				}
				nsm.createRoot(this);
				if (commitAfterSuccess)
				{
					em.getTransaction().commit();
				}
				return true;
			}

		}
		return false;
	}

	@Override
	public boolean delete()
	{
		String dbName = "iwcm";
		if (this.getClass().isAnnotationPresent(DataSource.class)) {

			Annotation annotation = this.getClass().getAnnotation(DataSource.class);
			DataSource dataSource = (DataSource) annotation;
			dbName = dataSource.name();
		}
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager(dbName);
		JpaNestedSetManager nsm = new JpaNestedSetManager(em);
		updateNestedSetProperties();
		@SuppressWarnings({ "unchecked" })
		JpaNode<T> node = new JpaNode<T>((T)this, nsm);
		// MBO: povolime vymazanie celej vetvy - uzdela aj s podstromom
		// if (node.hasChildren()) throw new IllegalStateException("Node cannot be deleted, because has children!");
		try
		{
			boolean commitAfterSuccess = false;
			if (!em.getTransaction().isActive())
			{
				commitAfterSuccess = true;
				em.getTransaction().begin();
			}
			node.delete();
			if (commitAfterSuccess)
			{
				em.getTransaction().commit();
			}
			return true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return false;
	}

	public void setParent(T parent)
	{
		this.parent = parent;
	}

	public void createRoot()
	{
		this.createRoot = true;
	}



	private T unwrapped(Node<T> node)
	{
		if (node!=null)	return node.unwrap();
		return null;
	}

	private List<T> unwrappedList(List<Node<T>> nodeList)
	{
		if (nodeList!=null)
		{
			List<T> result = new ArrayList<>();
			for (Node<T> node : nodeList)
			{
				result.add(node.unwrap());
			}
			return result;
		}
		return null;
	}

	private JpaEntityManager getEm()
	{
		String dbName = "iwcm";
		if (this.getClass().isAnnotationPresent(DataSource.class)) {

			Annotation annotation = this.getClass().getAnnotation(DataSource.class);
			DataSource dataSource = (DataSource) annotation;
			dbName = dataSource.name();
		}
		return JpaTools.getEclipseLinkEntityManager(dbName);
	}
	private JpaNestedSetManager getNsm()
	{
		return new JpaNestedSetManager(getEm());
	}

	/**
	 * update kritickych hodnot na cerstve z DB
	 */
	@SuppressWarnings("unchecked")
	private void updateNestedSetProperties()
	{
		updateNestedSetProperties((T)this);

	}
	private void updateNestedSetProperties(T bean)
	{
		StringBuilder jpql = new StringBuilder();
		jpql.append("SELECT NEW sk.iway.iwcm.database.nestedsets.CommonNestedSetBean.SimpleNestedSetItem(t.lft, t.rgt, t.level) FROM ").append(this.getClass().getSimpleName()).append(" t WHERE t.id=?1");
		TypedQuery<SimpleNestedSetItem> typedQuery = getEm().createQuery(jpql.toString() , SimpleNestedSetItem.class);
		typedQuery.setParameter(1, bean.getId());
		SimpleNestedSetItem item=typedQuery.getSingleResult();
		if (item==null) throw new IllegalStateException("Entity not exist.");
		bean.setLeftValue(item.left);
		bean.setRightValue(item.right);
		bean.setLevel(item.level);
		item=null;

	}

	@JsonIgnore
	public T getParent()
	{
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Node<T> node = new JpaNode(this, getNsm());
		return unwrapped(node.getParent());
	}

	@JsonIgnore
	public List<T> getAncestors()
	{
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Node<T> node = new JpaNode(this, getNsm());
		return unwrappedList(node.getAncestors());
	}

	@JsonIgnore
	public List<T> getChildren()
	{
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Node<T> node = new JpaNode(this, getNsm());
		return unwrappedList(node.getChildren());
	}

	@JsonIgnore
	public List<T> getDescendants()
	{
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Node<T> node = new JpaNode(this, getNsm());
		return unwrappedList(node.getDescendants());
	}

	public T addChild(T child)
	{
		updateNestedSetProperties();
		JpaNestedSetManager nsm = getNsm();
		nsm.getEntityManager().getTransaction().begin();
		try
		{
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Node<T> node = new JpaNode(this, nsm);
			return unwrapped(node.addChild(child));
		}
		finally
		{
			nsm.getEntityManager().getTransaction().commit();
			updateNestedSetProperties();
		}

	}


	public boolean isAncestorOf(T maybeDescentan)
	{
		updateNestedSetProperties();
		updateNestedSetProperties(maybeDescentan);
		JpaNestedSetManager nsm = getNsm();
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Node<T> node = new JpaNode(this, nsm);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Node<T> maybeDescentanNode = new JpaNode(maybeDescentan, nsm);
		return maybeDescentanNode.isDescendantOf(node);
	}

	public boolean isDescentantOf(T maybeAncestor)
	{
		updateNestedSetProperties();
		updateNestedSetProperties(maybeAncestor);
		JpaNestedSetManager nsm = getNsm();
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Node<T> node = new JpaNode(this, nsm);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Node<T> maybeAncestorNode = new JpaNode(maybeAncestor, nsm);
		return node.isDescendantOf(maybeAncestorNode);
	}

	public void moveToNewParent(T parent)
	{
		updateNestedSetProperties();
		updateNestedSetProperties(parent);
		JpaNestedSetManager nsm = getNsm();
		nsm.getEntityManager().getTransaction().begin();
		//@Experimental
		nsm.getEntityManager().lock(parent, LockModeType.PESSIMISTIC_READ);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Node<T> node = new JpaNode(this, nsm);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Node<T> parentNode = new JpaNode(parent, nsm);

		node.moveAsLastChildOf(parentNode);
		nsm.getEntityManager().getTransaction().commit();
		updateNestedSetProperties();
		updateNestedSetProperties(parent);
	}

	public static class SimpleNestedSetItem
	{
		protected int left;
		protected int right;
		protected int level;

		public SimpleNestedSetItem(int left, int right, int level)
		{
			super();
			this.left = left;
			this.right = right;
			this.level = level;
		}


	}

}
