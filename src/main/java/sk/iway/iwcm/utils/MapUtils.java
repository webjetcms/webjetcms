package sk.iway.iwcm.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;

/**
 *  MapUtils.java
 *
 *  Library class designed to ease off creating and
 *  populating {@link Map}s.
 *
 *  Unless specified otherwise, all methods in this class
 *  return instances of {@link LinkedHashMap}
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.10.2010 15:33:36
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@SuppressWarnings("unchecked")
public final class MapUtils
{
	private MapUtils(){}

	public static <K, V, T> Map<K, V> toMap(Collection<T> source, PairMaker<K, V, T> pairMaker)
	{
		if (source == null || source.isEmpty())
			return Collections.EMPTY_MAP;

		Map<K, V> map = new LinkedHashMap<K, V>();

		for (T member : source)
		{
			Pair<K, V> pair = pairMaker.makePair(member);
			map.put(pair.first, pair.second);
		}

		return map;
	}

	public static <K, V, T> Map<K, V> toMap(T[] source, PairMaker<K, V, T> pairMaker)
	{
		if (source == null || source.length == 0)
			return Collections.EMPTY_MAP;

		return toMap(Arrays.asList(source), pairMaker);
	}

	public static <K, V> Map<K,V> toMap(List<K> keys, List<V> values)
	{
		if (keys == null || keys.isEmpty() || values == null || values.isEmpty())
			return Collections.EMPTY_MAP;

		if (keys.size() != values.size())
			throw new IllegalArgumentException("Cannot create map. More keys than values or vice versa. "+keys+" =>"+values);

		Map<K, V> map = new LinkedHashMap<K, V>();

		for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++)
			map.put(keys.get(keyIndex), values.get(keyIndex));

		return map;
	}

	public static <K, V> Map<K,V> toMap(K[] keys, V[] values)
	{
		return toMap(Arrays.asList(keys), Arrays.asList(values));
	}

	public static <K, V> Map<K, V> toMapWithPropertyAsKey(Collection<V> source, String property)
	{
		if (source == null || source.size() == 0)
			return Collections.EMPTY_MAP;

		try
		{
			Map<K, V> map = new LinkedHashMap<K, V>();

			for (V element : source){
				K key = (K)PropertyUtils.getProperty(element, property);
				map.put(key, element);
			}

			return map;
		}catch (IllegalAccessException e){}
		catch (InvocationTargetException e){}
		catch (NoSuchMethodException e){}
		throw unknownPropertyException(property);
	}

	private static IllegalArgumentException unknownPropertyException(String property)
	{
		throw new IllegalArgumentException("Property "+property+" is not contained in passed objects");
	}

	public static <K> Map<K,K> asMap(K...keysAndValues)
	{
		if (keysAndValues == null)
			return Collections.EMPTY_MAP;
		if (keysAndValues.length % 2 != 0)
			throw new IllegalArgumentException("Cannot create map. Number of elements is not divisible by 2, cannot create key => value pairs");

		Map<K, K> map = new LinkedHashMap<K, K>();
		for (int i = 0; i < keysAndValues.length; i+= 2)
			map.put(keysAndValues[i], keysAndValues[i+1]);

		return map;
	}

	public static <K, V> Map<K, V> merge(Map<K, V> important, Map<K, V> defaults)
	{
		if (defaults == null)
			defaults = new HashMap<K, V>();
		Map<K, V> merged = new LinkedHashMap<K, V>(defaults);

		Set<Map.Entry<K, V>> set = important.entrySet();
		for(Map.Entry<K, V> me : set)
			merged.put(me.getKey(), me.getValue());

		return merged;
	}
}