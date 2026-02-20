package sk.iway.iwcm.database.nestedsets;

/**
 * A NodeInfo implementor carries information about its identity and position in
 * a nested set.
 *
 */
public interface NodeInfo {
	int getId();
	int getLeftValue();
	int getRightValue();
	int getLevel();
	int getRootValue();
	void setLeftValue(int value);
	void setRightValue(int value);
	void setLevel(int level);
	void setRootValue(int value);
}
