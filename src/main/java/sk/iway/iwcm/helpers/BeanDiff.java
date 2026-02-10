package sk.iway.iwcm.helpers;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ArrayUtils;

import sk.iway.iwcm.DBPool;


/**
 *  BeanDiff.java
 *
 *  Compares 2 beans of equal classes, returning differences found in the process.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 29.3.2010 16:45:51
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BeanDiff
{

	private Object original;
	private Object actual;

	private static final String[] supportedClasses = {"String", "Double", "Integer", "Float", "Byte", "Boolean", "int", "double", "boolean", "float", "byte"};
	private List<String> whitelist;
	private List<String> blacklist;
	private boolean whitelistOn = false;
	private boolean blacklistOn = false;

	public Map<String, PropertyDiff> diff()
	{
		Map<String, PropertyDiff> changes = new HashMap<>();

		if (actual != null)
		{
			PropertyDescriptor[] descriptors;
			if (original != null)
				descriptors = PropertyUtils.getPropertyDescriptors(original.getClass());
			else
				descriptors = PropertyUtils.getPropertyDescriptors(actual.getClass());

			for (PropertyDescriptor descriptor : descriptors)
			{
				try{
					String property = descriptor.getName();
					if (whitelistOn && !whitelist.contains(property)) continue;
					if (blacklistOn && blacklist.contains(property)) continue;
					if (descriptor.getPropertyType() != null && !ArrayUtils.contains(supportedClasses, descriptor.getPropertyType().getSimpleName()))
						continue;

					Object originalValue = original != null ? PropertyUtils.getProperty(original, property) : null;
					Object newValue = PropertyUtils.getProperty(actual, property);
					if (originalValue == null && descriptor.getPropertyType() == String.class)
						originalValue = "";
					if (newValue == null && descriptor.getPropertyType() == String.class)
						newValue = "";
					if (originalValue == null) originalValue = "NULL";
					if (newValue == null) newValue = "NULL";

					if (!originalValue.equals(newValue) || original == null)
					{
						PropertyDiff diff = new PropertyDiff();
						diff.valueBefore = originalValue;
						diff.valueAfter = newValue;
						changes.put(property, diff);
					}

				}
				catch (Exception e) {/*in order to satisfy compiler*/}
			}
		}

		return changes;
	}

	public BeanDiff setOriginal(Object original)
	{
		this.original = original;
		return this;
	}

	public BeanDiff setNew(Object newBean)
	{
		this.actual = newBean;
		return this;
	}

	/**
	 * Nastavi novy objekt typu JPA a nahra aktualnu verziu z DB na porovnanie
	 * Nie je potrebne nasledne volat setOriginal
	 * @param actual
	 * @param id
	 * @return
	 */
	public BeanDiff setNewLoadJpaOriginal(Object actual, int id)
	{
		this.actual = actual;

		//ak je to JPA vieme ziskat original, musime mat ale novy EntityManager a nie ten z ThreadLocal, tam by sme totiz mohli vidiet uz zmeneny bean co je actual
		EntityManagerFactory factory = DBPool.getEntityManagerFactory("iwcm");
		EntityManager em = factory.createEntityManager();

		this.original = em.find(actual.getClass(), id);

		em.close();

		return this;
	}

	public BeanDiff whitelist(String... properties)
	{
		whitelistOn = true;
		whitelist = Arrays.asList(properties);
		return this;
	}

	public BeanDiff blacklist(String... properties)
	{
		blacklistOn = true;
		blacklist = Arrays.asList(properties);
		return this;
	}

	public boolean hasOriginal()
	{
		return original != null;
	}

	public String getNewClassName() {
		return actual.getClass().getName();
	}
}
