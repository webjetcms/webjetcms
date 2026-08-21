package sk.iway.iwcm.system.cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sk.iway.iwcm.Tools;

/**
 * Encoded request to run a cron task on a configured cluster node or node group.
 */
final class CronTaskClusterCommand
{
	private static final String PREFIX = "crontab-";

	private final String configuredNode;
	private final long taskId;

	private CronTaskClusterCommand(String configuredNode, long taskId)
	{
		this.configuredNode = normalizeNode(configuredNode);
		this.taskId = taskId;
	}

	static CronTaskClusterCommand create(String configuredNode, long taskId)
	{
		if (taskId < 1) throw new IllegalArgumentException("Cron task ID must be greater than zero");
		return new CronTaskClusterCommand(configuredNode, taskId);
	}

	static CronTaskClusterCommand parse(String command)
	{
		if (isCommand(command) == false) return null;

		int separatorIndex = command.lastIndexOf('-');
		if (separatorIndex <= PREFIX.length()) return null;

		long parsedTaskId = Tools.getLongValue(command.substring(separatorIndex + 1), -1);
		if (parsedTaskId < 1) return null;

		String parsedNode = command.substring(PREFIX.length(), separatorIndex);
		if (Tools.isEmpty(parsedNode)) return null;

		return new CronTaskClusterCommand(parsedNode, parsedTaskId);
	}

	static boolean isCommand(String command)
	{
		return command != null && command.startsWith(PREFIX);
	}

	String encode()
	{
		return PREFIX + configuredNode + "-" + taskId;
	}

	List<String> resolveTargetNodes(String clusterNames)
	{
		List<String> targetNodes = new ArrayList<>();

		if (isNodeGroup(configuredNode))
		{
			if ("auto".equalsIgnoreCase(clusterNames)) targetNodes.add("auto");
			else targetNodes.addAll(Arrays.asList(Tools.getTokens(clusterNames, ",")));
		}
		else
		{
			targetNodes.add(configuredNode);
		}

		return targetNodes;
	}

	String getConfiguredNode()
	{
		return configuredNode;
	}

	long getTaskId()
	{
		return taskId;
	}

	private static String normalizeNode(String configuredNode)
	{
		if (Tools.isEmpty(configuredNode)) return "all";
		return configuredNode;
	}

	private static boolean isNodeGroup(String configuredNode)
	{
		return "all".equals(configuredNode) || "all-admin".equals(configuredNode) || "all-public".equals(configuredNode);
	}
}
