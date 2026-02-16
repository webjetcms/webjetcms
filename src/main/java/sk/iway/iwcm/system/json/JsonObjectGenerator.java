package sk.iway.iwcm.system.json;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.utils.Pair;

/**
 * Zjednodusuje generovanie zlozitejsich Json objektov
 *
 *
 * @author mbocko
 *
 */
public class JsonObjectGenerator
{

	private ObjectFormaterFactory formatFactory;

	public JsonObjectGenerator setObjectFormaterFactory(ObjectFormaterFactory factory)
	{
		this.formatFactory = factory;
		return this;
	}


	public JsonObjectGenerator addObject(JSONObject json, String objectName, Object bean, String attrs)
	{
		if (bean!=null)
		{
			if (bean instanceof Collection)
			{
				return addArrayOfObjects(json, objectName, (Collection<?>)bean, attrs);
			}
			JSONObject jsonObj = new JSONObject();
			if (Tools.isEmpty(attrs))
			{
				//chcem vlozit objekt ako celok
				try
				{
					putToObjectRecursive(json, objectName, formatFactory.format(bean));
				}
				catch (JSONException e){	sk.iway.iwcm.Logger.error(e);}
				return this;
			}
			Pair<String,String>[] attributes = parseTokens(attrs);
			for (Pair<String,String>attribute : attributes)
			{

				if (attribute.first.contains("{"))
				{
					String attrName = attribute.first.substring(0,attribute.first.indexOf("{"));
					try
					{
						Object propertyValue = PropertyUtils.getProperty(bean, attrName);
						if (propertyValue!=null)
						{
							if (propertyValue instanceof Collection)
							{
								String collectionItemAttributes = attribute.first.substring(attribute.first.indexOf("{")+1,attribute.first.lastIndexOf("}"));
								addArrayOfObjects(jsonObj, attrName, (Collection<?>)propertyValue, collectionItemAttributes);
							}
							else
							{
								String itemAttributes = attribute.first.substring(attribute.first.indexOf("{")+1,attribute.first.lastIndexOf("}"));
								addObject(jsonObj, attrName, propertyValue, itemAttributes);
							}
						}
					}
					catch (IllegalAccessException e) {sk.iway.iwcm.Logger.error(e); }
					catch (InvocationTargetException e) {sk.iway.iwcm.Logger.error(e); }
					catch (NoSuchMethodException e) {sk.iway.iwcm.Logger.error(e); }
				}
				else
				{
					try
					{
						Object propertyValue = PropertyUtils.getProperty(bean, attribute.first);
						if (propertyValue!=null)
						{

							jsonObj.put(attribute.second, formatFactory.format(propertyValue));
						}
					}
					catch (IllegalAccessException e) {sk.iway.iwcm.Logger.error(e); }
					catch (InvocationTargetException e) {sk.iway.iwcm.Logger.error(e); }
					catch (NoSuchMethodException e) {sk.iway.iwcm.Logger.error(e); }
					catch (JSONException e) {sk.iway.iwcm.Logger.error(e);}
				}
			}
			try
			{
				putToObjectRecursive(json, objectName, jsonObj);
			}
			catch (JSONException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		return this;
	}

	private void putToObjectRecursive(JSONObject json, String objectName, Object objectToInsert) throws JSONException
	{
		if (objectName.indexOf(".")!=-1)
		{
			String key = objectName.substring(0, objectName.indexOf("."));
			JSONObject parentJson = null;
			try
			{
				parentJson = json.getJSONObject(key);
			}
			catch(JSONException e)
			{}
			if (parentJson==null)
			{
				parentJson = new JSONObject();
				json.put(key, parentJson);
			}
			putToObjectRecursive(parentJson, Tools.replace(objectName, key+".", ""), objectToInsert);
		}
		else
		{
			json.put(objectName, objectToInsert);
		}
	}

	public JsonObjectGenerator addArrayOfObjects(JSONObject json, String arrayName, Collection<?> collection, String attrs)
	{
		if (collection!=null && attrs!=null)
		{
			Pair<String,String>[] attributes = parseTokens(attrs);
			JSONArray jsonArray = new JSONArray();
			Iterator<?> it = collection.iterator();
			while (it.hasNext())
			{
				Object item = it.next();
				putToArray(jsonArray, item, attributes);
			}
			try
			{
				//json.put(arrayName, jsonArray);
				putToObjectRecursive(json, arrayName, jsonArray);
			}
			catch (JSONException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}

		}
		return this;
	}

	@SuppressWarnings("unchecked")
	private void putToArray(JSONArray array, Object bean, Pair<String,String>...attributes)
	{
		JSONObject jsonObj = new JSONObject();
		for (Pair<String,String> attribute : attributes)
		{
			String attrName = attribute.first;
			if (attribute.first.contains("{"))
			{
				attrName = attribute.first.substring(0,attribute.first.indexOf("{"));

				try
				{
					Object propertyValue = PropertyUtils.getProperty(bean, attrName);
					if (propertyValue!=null)
					{
						if (propertyValue instanceof Collection)
						{
							String collectionAttributes = attribute.first.substring(attribute.first.indexOf("{")+1,attribute.first.lastIndexOf("}"));
							addArrayOfObjects(jsonObj, attrName, (Collection<?>)propertyValue, collectionAttributes);
						}
						else
						{
							String itemAttributes = attribute.first.substring(attribute.first.indexOf("{")+1,attribute.first.lastIndexOf("}"));
							addObject(jsonObj, attrName, propertyValue, itemAttributes);
						}


					}
				}
				catch (IllegalAccessException e) {sk.iway.iwcm.Logger.error(e); }
				catch (InvocationTargetException e) {sk.iway.iwcm.Logger.error(e); }
				catch (NoSuchMethodException e) {sk.iway.iwcm.Logger.error(e); }
			}
			else
			{

				try
				{
					Object propertyValue = PropertyUtils.getProperty(bean, attribute.first);
					if (propertyValue!=null)
					{
						jsonObj.put(attribute.second,  formatFactory.format(propertyValue));
					}
				}
				catch (IllegalAccessException e) {sk.iway.iwcm.Logger.error(e); }
				catch (InvocationTargetException e) {sk.iway.iwcm.Logger.error(e); }
				catch (NoSuchMethodException e) {sk.iway.iwcm.Logger.error(e); }
				catch (JSONException e) {sk.iway.iwcm.Logger.error(e);}

			}
		}
		array.put(jsonObj);

	}

	@SuppressWarnings("unchecked")
	private Pair<String,String>[] parseTokens(String toParse)
	{
		if (toParse==null) return null;
		char[] charArray = toParse.toCharArray();
		List<Pair<String,String>> resultList = new ArrayList<Pair<String,String>>();
		StringBuilder beanPropertyName = new StringBuilder();
		StringBuilder jsonPropertyName = null;
		//String jsonPropNameString = "";
		int deepStatus = 0;
		boolean propertyName=false;
		for (int i =0; i<charArray.length; i++)
		{
			char ch = charArray[i];

			if (ch == ',' && deepStatus==0)
			{
				//resultList.add(beanPropertyName.toString());
				if (jsonPropertyName!=null) resultList.add(Pair.of(beanPropertyName.toString(), jsonPropertyName.toString()));
				else resultList.add(Pair.of(beanPropertyName.toString(), beanPropertyName.toString()));
				beanPropertyName = new StringBuilder();
				jsonPropertyName=null;
			}
			else if (ch == ' ') continue;
			else
			{
				if (ch == '{') deepStatus++;
				else if (ch == '}') deepStatus--;

				if (ch == '(' && deepStatus==0)
				{
					propertyName=true;
					continue;
				}
				else if (ch == ')' && deepStatus==0)
				{
					propertyName=false;
					//jsonPropNameString = jsonPropertyName.toString();
					//skoncila definicia json pola, ale ak som nahodou na konci stringu, musim korektne ukoncit cyklus
					if (i==charArray.length-1)
					{
						//ak som na konci stringu
						if (jsonPropertyName!=null) resultList.add(Pair.of(beanPropertyName.toString(), jsonPropertyName.toString()));
						else resultList.add(Pair.of(beanPropertyName.toString(), beanPropertyName.toString()));
					}
					continue;
				}

				if (propertyName)
				{
					if (jsonPropertyName==null) jsonPropertyName=new StringBuilder();
					jsonPropertyName.append(ch);
				}
				else
				{
					beanPropertyName.append(ch);
				}
			}
			if (i==charArray.length-1)
			{
				//ak som na konci stringu
				if (jsonPropertyName!=null) resultList.add(Pair.of(beanPropertyName.toString(), jsonPropertyName.toString()));
				else resultList.add(Pair.of(beanPropertyName.toString(), beanPropertyName.toString()));
			}
		}
		return resultList.toArray(new Pair[]{});
	}

}
