package cn.bluejoe.elfinder.controller.executor;

import java.util.HashMap;
import java.util.Map;

public class DefaultCommandExecutorFactory implements CommandExecutorFactory
{
	String _classNamePattern = "cn.bluejoe.elfinder.controller.executors.%sCommandExecutor";

	private Map<String, CommandExecutor> _map = new HashMap<String, CommandExecutor>();

	@Override
	public CommandExecutor get(String commandName)
	{
		if (_map.containsKey(commandName))
			return _map.get(commandName);

		try
		{
			String className = String.format(_classNamePattern,
				commandName.substring(0, 1).toUpperCase() + commandName.substring(1));
			return (CommandExecutor) Class.forName(className).getDeclaredConstructor().newInstance();
		}
		catch (Exception e)
		{
			//not found
			return null;
		}
	}

	public String getClassNamePattern()
	{
		return _classNamePattern;
	}

	public Map<String, CommandExecutor> getMap()
	{
		return _map;
	}

	public void setClassNamePattern(String classNamePattern)
	{
		_classNamePattern = classNamePattern;
	}

	public void setMap(Map<String, CommandExecutor> map)
	{
		_map = map;
	}
}
