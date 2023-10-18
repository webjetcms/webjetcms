package sk.iway.iwcm.database.nestedsets;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.eclipse.persistence.expressions.Expression;

/**
 * A <tt>NestedSetManager</tt> is used to read and manipulate the nested set tree structure of
 * classes that implement {@link NodeInfo} using and where each instance thus has a position in a
 * nested set tree.
 *
 */
public interface NestedSetManager {
	/**
	 * Clears the NestedSetManager, removing all managed nodes from the <tt>NestedSetManager</tt>.
	 * Any entities wrapped by such nodes are not detached from the underlying <tt>EntityManager</tt>.
	 *
	 * @return void
	 */
	void clear();

	/**
	 * Creates a root node for the given NodeInfo instance.
	 *
	 * @param <T>
	 * @param root
	 * @return The created node instance.
	 */
	<T extends NodeInfo> Node<T> createRoot(T root);

	/**
	 * Fetches a complete tree, returning the root node of the tree.
	 *
	 * @param <T>
	 * @param clazz
	 * @param rootId
	 * @return The root node of the tree.
	 */
	<T extends NodeInfo> Node<T> fetchTree(Class<T> clazz, Expression filtrationCriteria, T parent, int rootId);

	/**
	 * Fetches the complete tree, returning the root node of the tree.
	 *
	 * @param <T>
	 * @param clazz
	 * @param rootId
	 * @return The root node of the tree.
	 */
	<T extends NodeInfo> Node<T> fetchTree(Class<T> clazz, Expression filtrationCriteria, T parent);

	/**
	 * Fetches a complete tree and returns the tree as a list.
	 *
	 * @param <T>
	 * @param clazz
	 * @return The tree in form of a list, starting with the root node.
	 */
	<T extends NodeInfo> List<Node<T>> fetchTreeAsList(Class<T> clazz, Expression filtrationCriteria, T parent);

	/**
	 * Fetches a complete tree and returns the tree as a list.
	 *
	 * @param <T>
	 * @param clazz
	 * @param rootId
	 * @return The tree in form of a list, starting with the root node.
	 */
	<T extends NodeInfo> List<Node<T>> fetchTreeAsList(Class<T> clazz, Expression filtrationCriteria, T parent, int rootId);

	/**
	 * Gets the EntityManager used by this NestedSetManager.
	 *
	 * @return The EntityManager.
	 */
	EntityManager getEntityManager();

	/**
	 * Gets the node that represents the given NodeInfo instance in the tree.
	 *
	 * @param <T>
	 * @param nodeInfo
	 * @return The node.
	 */
	<T extends NodeInfo> Node<T> getNode(T nodeInfo);

	/**
	 * Gets a collection of all nodes currently managed by the NestedSetManager.
	 *
	 * @return The collection of nodes.
	 */
	Collection<Node<?>> getNodes();

	public <T extends NodeInfo> void update(T nodeInfo);


	public <T extends NodeInfo> void update(Node<T> node);

	public Configuration getConfig(Class<?> clazz);
	
	
}