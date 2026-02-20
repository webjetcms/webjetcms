package sk.iway.iwcm.database.nestedsets;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.database.nestedsets.annotations.LeftColumn;
import sk.iway.iwcm.database.nestedsets.annotations.LevelColumn;
import sk.iway.iwcm.database.nestedsets.annotations.RightColumn;
import sk.iway.iwcm.database.nestedsets.annotations.RootColumn;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *
 */
public class JpaNestedSetManager implements NestedSetManager
{
	private final JpaEntityManager em;
	//private final Map<Key, Node<?>> nodes;
	private final Map<Class<?>, Configuration> configs;

	public JpaNestedSetManager(JpaEntityManager em)
	{
		this.em = em;
		//this.nodes = new HashMap<Key, Node<?>>();
		this.configs = new HashMap<Class<?>, Configuration>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JpaEntityManager getEntityManager()
	{
		return em;//JpaTools.getEclipseLinkEntityManager();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear()
	{
		//this.nodes.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<Node<?>> getNodes()
	{
		return null;//Collections.unmodifiableCollection(this.nodes.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends NodeInfo> List<Node<T>> fetchTreeAsList(Class<T> clazz, Expression filtrationCriteria, T parent)
	{
		return fetchTreeAsList(clazz, filtrationCriteria, parent, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeInfo> List<Node<T>> fetchTreeAsList(Class<T> clazz, Expression filtrationCriteria, T parent, int rootId)
	{
		int left = 1;
		if (parent!=null) left = parent.getLeftValue();
		Configuration config = getConfig(clazz);

		ReadAllQuery dbQuery = new ReadAllQuery(clazz);
		ExpressionBuilder builder = new ExpressionBuilder();

		Expression where = builder.get(config.getLeftFieldName()).greaterThanEqual(left);

		if (filtrationCriteria!=null) where = JpaDB.and(where, filtrationCriteria);//cb.and(where, filtrationCriteria);

		if (parent!=null) where = JpaDB.and(where, builder.get(config.getRightFieldName()).lessThanEqual(parent.getRightValue()));

		dbQuery.setSelectionCriteria(where);
		dbQuery.addAscendingOrdering(config.getLeftFieldName());

		applyRootId(clazz, builder, where, rootId);

		Query q = JpaTools.getEclipseLinkEntityManager().createQuery(dbQuery);

		List<Node<T>> tree = new ArrayList<Node<T>>();
		for (T n : (List<T>)q.getResultList())
		{
			tree.add(getNode(n));
		}

		buildTree(tree, 0);

		return tree;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends NodeInfo> Node<T> fetchTree(Class<T> clazz, Expression filtrationCriteria, T parent, int rootId)
	{
		return fetchTreeAsList(clazz, filtrationCriteria, parent, rootId).get(0);
	}

	@Override
	public <T extends NodeInfo> Node<T> fetchTree(Class<T> clazz, Expression filtrationCriteria, T parent)
	{
		return fetchTree(clazz, filtrationCriteria, parent, 0);
	}

	/**
	 * Establishes all parent/child/ancestor/descendant relationships of all the
	 * nodes in the given list. As a result, invocations on the corresponding
	 * methods on these node instances will not trigger any database queries.
	 *
	 * @param <T>
	 * @param treeList
	 * @param maxLevel
	 */
	public <T extends NodeInfo> void buildTree(List<Node<T>> treeList, int maxLevel)
	{
		Node<T> rootNode = treeList.get(0);
		Stack<JpaNode<T>> stack = new Stack<JpaNode<T>>();
		int level = rootNode.getLevel();
		boolean foundParent;
		for (Node<T> n : treeList)
		{
			JpaNode<T> node = (JpaNode<T>) n;
			node.setOfflineNode(true);

			foundParent = false;
			while(!stack.empty() && !foundParent)
			{
				JpaNode<T> possibleParent = stack.peek();
/*
			      A
			     / \
			    B   G		If user is NOT in B, C (with children D and E) and F will have A as parent (works with multiple levels as well).
			   / \   \											|
			  C  F    H											|
			 / \												|
			D   E												|
			  													|
			      A												|
			     /|\											|
			    C F G	<---------------------------------------|
			   / \   \
			  D   E   H
*/
				if (node.getLeftValue() > possibleParent.getLeftValue() && node.getRightValue() < possibleParent.getRightValue())
				{
					//found parent, set everything
					node.internalSetParent(possibleParent); 				// set parent

					possibleParent.internalAddChild(node); 					// add child to parent

					node.internalSetAncestors(new ArrayList<>(stack)); 		// set ancestors

					for (JpaNode<T> anc : stack) 							// add descendant to all ancestors
					{
						anc.internalAddDescendant(node);
					}
					foundParent = true;
				}else{
					//did not found parent, pop false parent from stack and try again
					stack.pop();
				}
			}

			level = node.getLevel();

			if (node.hasChildren() && (maxLevel == 0 || maxLevel > level))
			{
				stack.push(node);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends NodeInfo> Node<T> createRoot(T root)
	{
		Configuration config = getConfig(root.getClass());

		int maximumRight;
		if (config.hasManyRoots())
		{
			maximumRight = 0;
		} else
		{
			maximumRight = getMaximumRight(root.getClass());
		}
		root.setLeftValue(maximumRight + 1);
		root.setRightValue(maximumRight + 2);
		root.setLevel(0);
		JpaTools.getEclipseLinkEntityManager().persist(root);
		return getNode(root);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends NodeInfo> Node<T> getNode(T nodeInfo)
	{
//		Key key = new Key(nodeInfo.getClass(), nodeInfo.getId());
//		if (this.nodes.containsKey(key))
//		{
//			@SuppressWarnings("unchecked")
//			Node<T> n = (Node<T>) this.nodes.get(key);
//			return n;
//		}
		Node<T> node = new JpaNode<T>(nodeInfo, this);
		if (!node.isValid())
		{
			throw new IllegalArgumentException(
					"The given NodeInfo instance has no position "
							+ "in a tree and is thus not yet a node.");
		}
		//this.nodes.put(key, node);

		return node;
	}

	/**
	 * INTERNAL: Gets the nestedset configuration for the given class.
	 *
	 * @param clazz
	 * @return The configuration.
	 */
	@Override
	public Configuration getConfig(Class<?> clazz)
	{
		if (!this.configs.containsKey(clazz))
		{
			Configuration config = new Configuration();

			Entity entity = clazz.getAnnotation(Entity.class);
			String name = entity.name();
			config.setEntityName((name != null && name.length() > 0) ? name
					: clazz.getSimpleName());

			for (Field field : clazz.getDeclaredFields())
			{
				if (field.getAnnotation(LeftColumn.class) != null)
				{
					config.setLeftFieldName(field.getName());
				} else if (field.getAnnotation(RightColumn.class) != null)
				{
					config.setRightFieldName(field.getName());
				} else if (field.getAnnotation(LevelColumn.class) != null)
				{
					config.setLevelFieldName(field.getName());
				} else if (field.getAnnotation(RootColumn.class) != null)
				{
					config.setRootIdFieldName(field.getName());
				}
			}

			// ak by som nahodou mal preddefinovanu superclassu pre NS
			if (clazz.getSuperclass() != null
					&& clazz.getSuperclass().isAnnotationPresent(
							MappedSuperclass.class))
			{
				for (Field field : clazz.getSuperclass().getDeclaredFields())
				{
					if (Tools.isEmpty(config.getLeftFieldName())
							&& field.getAnnotation(LeftColumn.class) != null)
					{
						config.setLeftFieldName(field.getName());
					} else if (Tools.isEmpty(config.getRightFieldName())
							&& field.getAnnotation(RightColumn.class) != null)
					{
						config.setRightFieldName(field.getName());
					} else if (Tools.isEmpty(config.getLevelFieldName())
							&& field.getAnnotation(LevelColumn.class) != null)
					{
						config.setLevelFieldName(field.getName());
					} else if (Tools.isEmpty(config.getRootIdFieldName())
							&& field.getAnnotation(RootColumn.class) != null)
					{
						config.setRootIdFieldName(field.getName());
					}
				}
			}
			config.lock();
			this.configs.put(clazz, config);
		}

		return this.configs.get(clazz);
	}

	int getMaximumRight(Class<? extends NodeInfo> clazz)
	{
		Configuration config = getConfig(clazz);
		CriteriaBuilder cb = JpaTools.getEclipseLinkEntityManager().getCriteriaBuilder();
		CriteriaQuery<? extends NodeInfo> cq = cb.createQuery(clazz);
		Root<? extends NodeInfo> queryRoot = cq.from(clazz);
		cq.orderBy(cb.desc(queryRoot.get(config.getRightFieldName())));
		List<? extends NodeInfo> highestRows = JpaTools.getEclipseLinkEntityManager().createQuery(cq)
				.setMaxResults(1).getResultList();
		if (highestRows.isEmpty())
		{
			return 0;
		} else
		{
			return highestRows.get(0).getRightValue();
		}
	}


	void applyRootId(Class<?> clazz, ExpressionBuilder builder, Expression expression, int rootId)
	{
		Configuration config = getConfig(clazz);
		if (config.getRootIdFieldName() != null)
		{
			expression = JpaDB.and(expression, builder.get(config.getRootIdFieldName()).equal(rootId));
		}
	}


//	void updateLeftValues(int minLeft, int maxLeft, int delta, int rootId)
//	{
//		for (Node<?> node : this.nodes.values())
//		{
//			if (node.getRootValue() == rootId)
//			{
//				if (node.getLeftValue() >= minLeft
//						&& (maxLeft == 0 || node.getLeftValue() <= maxLeft))
//				{
//					node.setLeftValue(node.getLeftValue() + delta);
//					((JpaNode<?>) node).invalidate();
//				}
//			}
//		}
//	}


//	void updateRightValues(int minRight, int maxRight, int delta, int rootId)
//	{
//		for (Node<?> node : this.nodes.values())
//		{
//			if (node.getRootValue() == rootId)
//			{
//				if (node.getRightValue() >= minRight
//						&& (maxRight == 0 || node.getRightValue() <= maxRight))
//				{
//					node.setRightValue(node.getRightValue() + delta);
//					((JpaNode<?>) node).invalidate();
//				}
//			}
//		}
//	}


//	void updateLevels(int left, int right, int delta, int rootId)
//	{
//		for (Node<?> node : this.nodes.values())
//		{
//			if (node.getRootValue() == rootId)
//			{
//				if (node.getLeftValue() > left && node.getRightValue() < right)
//				{
//					node.setLevel(node.getLevel() + delta);
//					((JpaNode<?>) node).invalidate();
//				}
//			}
//		}
//	}

//	void removeNodes(int left, int right, int rootId)
//	{
//		Set<Key> removed = new HashSet<Key>();
//		for (Node<?> node : this.nodes.values())
//		{
//			if (node.getRootValue() == rootId)
//			{
//				if (node.getLeftValue() >= left
//						&& node.getRightValue() <= right)
//				{
//					removed.add(new Key(node.unwrap().getClass(), node.getId()));
//				}
//			}
//		}
//		for (Key k : removed)
//		{
//			Node<?> n = this.nodes.remove(k);
//			n.setLeftValue(0);
//			n.setRightValue(0);
//			n.setLevel(0);
//			n.setRootValue(0);
//			this.em.detach(n.unwrap());
//		}
//	}
	@Override
	public <T extends NodeInfo> void update(Node<T> node)
	{
		update(node.unwrap());
	}
	@Override
	public <T extends NodeInfo> void update(T nodeInfo)
	{
		if (nodeInfo.getId()>0)
		{
			JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
			try
			{
				em.getTransaction().begin();
				em.merge(nodeInfo);
				em.getTransaction().commit();
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
				em.getTransaction().rollback();
			}

		}
	}


}
