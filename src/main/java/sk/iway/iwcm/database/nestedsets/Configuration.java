package sk.iway.iwcm.database.nestedsets;

/**
 * A configuration for a class managed by a NestedSetManager.
 *
 * @author robo
 */
public class Configuration {
	private String leftFieldName;
	private String rightFieldName;
	private String levelFieldName;
	private String rootIdFieldName;
	private String entityName;

	private boolean hasManyRoots = false;

	private boolean locked = false;

	/**
	 * @return the leftFieldName
	 */
	public String getLeftFieldName() {
		return leftFieldName;
	}

	/**
	 * @param leftFieldName the leftFieldName to set
	 */
	public void setLeftFieldName(String leftFieldName) 
	{
		if (locked) throw new IllegalStateException("Config is locked, cannod be modiffied.");
		this.leftFieldName = leftFieldName;
	}

	/**
	 * @return the rightFieldName
	 */
	public String getRightFieldName() {
		return rightFieldName;
	}

	/**
	 * @param rightFieldName the rightFieldName to set
	 */
	public void setRightFieldName(String rightFieldName) {
		if (locked) throw new IllegalStateException("Config is locked, cannod be modiffied.");
		this.rightFieldName = rightFieldName;
	}

	/**
	 * @return the levelFieldName
	 */
	public String getLevelFieldName() {
		return levelFieldName;
	}

	/**
	 * @param levelFieldName the levelFieldName to set
	 */
	public void setLevelFieldName(String levelFieldName) {
		if (locked) throw new IllegalStateException("Config is locked, cannod be modiffied.");
		this.levelFieldName = levelFieldName;
	}

	/**
	 * @return the rootIdFieldName
	 */
	public String getRootIdFieldName() {
		return rootIdFieldName;
	}

	/**
	 * @param rootIdFieldName the rootIdFieldName to set
	 */
	public void setRootIdFieldName(String rootIdFieldName) {
		if (locked) throw new IllegalStateException("Config is locked, cannod be modiffied.");
		this.rootIdFieldName = rootIdFieldName;
		this.hasManyRoots = true;
	}

	public boolean hasManyRoots() {
		return this.hasManyRoots;
	}

	@Override public String toString() {
		return "[leftFieldName: " + this.leftFieldName + ", rightFieldName:" + this.rightFieldName
				+ ", levelFieldName: " + this.levelFieldName + ", rootIdFieldName:" + this.rootIdFieldName + "]";
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String tableName) {
		if (locked) throw new IllegalStateException("Config is locked, cannod be modiffied.");
		this.entityName = tableName;
	}

	public void lock()
	{
		this.locked=true;
	}
}
