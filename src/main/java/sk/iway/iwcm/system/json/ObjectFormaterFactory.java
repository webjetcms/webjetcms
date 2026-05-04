package sk.iway.iwcm.system.json;

import java.util.HashMap;
import java.util.Map;

public class ObjectFormaterFactory
{
	private Map<Class<?>, ObjectFormater> formaters = new HashMap<Class<?>, ObjectFormater>();
	
	private static final ObjectFormater DEFAULT_FORMATER = new ObjectFormater() {
		
		@Override
		public Object format(Object object)
		{
			return object;
		}
	};
	
	public void setFormater(Class<?> className, ObjectFormater formatter)
	{
		formaters.put(className, formatter);
	}
	
	public Object format(Object object)
	{
		Class<?> clazz = object.getClass();
		if (formaters.containsKey(clazz))
		{
			return formaters.get(clazz).format(object);
		}
		return DEFAULT_FORMATER.format(object);
	}

}
