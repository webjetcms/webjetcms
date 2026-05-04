package sk.iway.iwcm.components.crud;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;

/**
 * @author mbocko
 *
 *
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UniversalComponentHelper
{
	private HttpServletRequest req;
	private int filterCounter=0;

	private UniversalComponentHelper(HttpServletRequest request)
	{
		this.req = request;
	}


	/**
	 * metoda vrati instanciu tejto triedy
	 *
	 * @param request
	 * @return
	 */
	public static UniversalComponentHelper setComponent(HttpServletRequest request)
	{
		return new UniversalComponentHelper(request);
	}

	/**
	 * cesta k tejto komponente
	 *
	 * @param listPath
	 * @return
	 */
	public UniversalComponentHelper addListPath(String listPath)
	{
		this.req.setAttribute("universal_component_list", listPath);
		return this;
	}

	/**
	 * cesta k edit dialogu
	 * @param dialogPath
	 * @return
	 */
	public UniversalComponentHelper addDialogPath(String dialogPath)
	{
		this.req.setAttribute("universal_component_editDialog", dialogPath);
		return this;
	}

	/**
	 * Objekt triedy beanClass
	 *
	 * @param beanClass
	 * @return
	 */
	public UniversalComponentHelper addBeanClass(Class beanClass)
	{
		this.req.setAttribute("universal_component_beanClass", beanClass);
		return this;
	}

	/**
	 * instancia DB pre beanClass - potomok JpaDB
	 *
	 * @param fqn
	 * @return
	 */
	public UniversalComponentHelper addDbClass(String fqn)
	{
		Class dbClass = null;
		try {
			dbClass = Class.forName(fqn);
		} catch (ClassNotFoundException e)
		{
			Logger.debug(getClass(), "DB trieda "+fqn+" sa nenasla");
		}
		return addDbClass(dbClass);
	}

	/**
	 * Vytvori instanciu DB triedy
	 *
	 * @param dbClass
	 * @return
	 */
	public UniversalComponentHelper addDbClass(Class dbClass)
	{
		JpaDB dbInstance = null;
		Constructor constr = null;
		try
		{
			Class[] varargs = null;
			constr = dbClass.getConstructor(varargs);
			if (constr!=null) {
				Object[] params = null;
				dbInstance = (JpaDB)constr.newInstance(params);
			}
		}
		catch (Exception e)
		{

		}
		if (dbInstance==null)
		{
			try
			{
				Class[] varargs = null;
				Method getInstanceMethod = dbClass.getDeclaredMethod("getInstance", varargs);
				if (getInstanceMethod!=null) {
					Object[] params = null;
					getInstanceMethod.invoke(dbInstance, params);
				}
			}
			catch (Exception e)
			{

			}
		}
		return addDbInstance(dbInstance);
	}


	/**
	 * instancia DB pre beanClass - potomok JpaDB
	 *
	 * @param dbInstance
	 * @return
	 */
	public UniversalComponentHelper addDbInstance(JpaDB dbInstance)
	{
		this.req.setAttribute("universal_component_dbInstance", dbInstance);
		return this;
	}

	/**
	 * Prida do requestu filtrovanie v zozname beanov
	 *
	 * @param propertyToFilter property podla ktorej sa bude zoznam filtrovat
	 * @return
	 */
	public UniversalComponentHelper addListFilter(String propertyToFilter)
	{

		return addListFilter(propertyToFilter, null, null, null);
	}

	/**
	 * Prida do requestu filtrovanie v zozname beanov, umoznuje pridat aj selecbox s ponukou na foltrovanie
	 *
	 * @param propertyToFilter property podla ktorej sa bude zoznam filtrovat
	 * @param selectItems kolekcia s polozkami ak sa ma zobrazit select box s moznostami filtrovania
	 * @param selectValue property objektu kolekcie z ktoreho sa berie hodnota selectu
	 * @param selectLabel property objektu kolekcie z ktoreho sa berie popis selectu
	 * @return
	 */
	public UniversalComponentHelper addListFilter(String propertyToFilter, Collection selectItems, String selectValue, String selectLabel)
	{
		int filterNum = ++filterCounter;
		req.setAttribute(("universal_component_filter."+filterNum), propertyToFilter);
		if (selectItems!=null && selectItems.size()>0)
		{
			req.setAttribute(("universal_component_filter."+filterNum+".select"), selectItems);
			if (Tools.isNotEmpty(selectLabel))
				req.setAttribute(("universal_component_filter."+filterNum+".select.label"), selectLabel);
			if (Tools.isNotEmpty(selectValue))
				req.setAttribute(("universal_component_filter."+filterNum+".select.value"), selectValue);
		}
		return this;
	}

	/**
	 * Prida do requestu filtrovanie v zozname beanov, pre proprety ktora je mapovana cez JPA
	 *
	 * @param propertyToFilter property podla ktorej sa bude zoznam filtrovat
	 * @param dbClass JpaDB trieda mapovaneho objektu
	 * @param selectLabel property objektu kolekcie z ktoreho sa berie popis selectu
	 * @return
	 */
	public UniversalComponentHelper addListFilterForMappedEntity(String propertyToFilter, Class dbClass, String selectLabel)
	{

		JpaDB dbInstance = null;
		Constructor constr = null;
		try
		{
			Class[] varargs = null;
			Object[] params = null;
			constr = dbClass.getConstructor(varargs);
			dbInstance = (JpaDB)constr.newInstance(params);
		}
		catch (Exception e)
		{ }
		if (dbInstance==null)
		{
			try
			{
				Class[] varargs = null;
				Object[] params = null;
				Method getInstanceMethod = dbClass.getDeclaredMethod("getInstance", varargs);
				getInstanceMethod.invoke(dbInstance, params);
			}
			catch (Exception e)
			{ }
		}
		if (dbInstance != null) {
			List objects = dbInstance.getAll();
			@SuppressWarnings("unused")
			UniversalComponentHelper result = addListFilter(propertyToFilter, objects, "id", selectLabel);
			req.setAttribute("universal_component_filter."+filterCounter+".select.dbInstance", dbInstance);
		}
		return this;
	}


	/**
	 * Metoda prida mapovanie na zaklade ID a zabezpeci spravne zobrazenie
	 * - pouzije sa ak je metoda na vyber objektu staticka
	 *
	 * @param dbClass trieda DB z ktorej sa vyberie objekt
	 * @param propertyToMap nazov property, ktora reprezentuje referenciu na objekt
	 * @param methodToRetrieveObject Metoda na ziskanie instancie objektu na ktory je mapovana property
	 * @param fieldToDisplay property objektu, ktora sa zobrazi
	 * @return
	 */
	public UniversalComponentHelper addListFieldMapping(Class dbClass, String propertyToMap, String methodToRetrieveObject, String fieldToDisplay)
	{
		req.setAttribute("universal_component_"+propertyToMap+".mappingDbClass", dbClass);
		req.setAttribute("universal_component_"+propertyToMap+".mappingMethodName", methodToRetrieveObject);
		req.setAttribute("universal_component_"+propertyToMap+".mappingObjectFieldName", fieldToDisplay);

		return this;
	}


	/**
	 * * Metoda prida mapovanie na zaklade ID a zabezpeci spravne zobrazenie
	 * - pouzije sa ak je metoda na vyber instancna
	 *
	 * @param dbInstance Instancia DB triedy z ktorej sa vyberie objekt
	 * @param propertyToMap nazov property, ktora reprezentuje referenciu na objekt
	 * @param methodToRetrieveObject Metoda na ziskanie instancie objektu na ktory je mapovana property
	 * @param fieldToDisplay property objektu, ktora sa zobrazi
	 * @return
	 */
	public UniversalComponentHelper addListFieldMapping(Object dbInstance, String propertyToMap, String methodToRetrieveObject, String fieldToDisplay)
	{
		req.setAttribute("universal_component_"+propertyToMap+".mappingDbInstance", dbInstance);
		req.setAttribute("universal_component_"+propertyToMap+".mappingMethodName", methodToRetrieveObject);
		req.setAttribute("universal_component_"+propertyToMap+".mappingObjectFieldName", fieldToDisplay);

		return this;
	}


	/**
	 * Nastavi pre editForm pre konkretnu property nastavenie cez selectbox
	 *
	 * @param propertyToAddSelect nemo property ktoru budeme nastavovat cez select
	 * @param selectItems kolekcia s polozkami na zobrazenie v selecte
	 * @param selectValue property objektu kolekcie z ktoreho sa berie hodnota selectu
	 * @param selectLabel property objektu kolekcie z ktoreho sa berie popis selectu
	 * @return
	 */
	public UniversalComponentHelper addSelectToField(String propertyToAddSelect, Collection selectItems, String selectValue, String selectLabel)
	{
		req.setAttribute("universal_component_"+propertyToAddSelect+".select", selectItems);
		if (Tools.isNotEmpty(selectValue))
			req.setAttribute("universal_component_"+propertyToAddSelect+".select.value", selectValue);
		if (Tools.isNotEmpty(selectLabel))
			req.setAttribute("universal_component_"+propertyToAddSelect+".select.label", selectLabel);
		return this;
	}

	/**
	 * Nastavi pre editForm pre konkretnu property nastavenie cez selectbox
	 *
	 * @param propertyToAddSelect nemo property ktoru budeme nastavovat cez select
	 * @param selectItems kolekcia s polozkami na zobrazenie v selecte
	 * @return
	 */
	public UniversalComponentHelper addSelectToField(String propertyToAddSelect, Collection selectItems)
	{
		return addSelectToField(propertyToAddSelect, selectItems, null, null);
	}

	public UniversalComponentHelper addIwcmTextPrefixToField(String propertyToText, String iwcmTextPrefix)
	{
		req.setAttribute("universal_component_"+propertyToText+"_iwcmtextprefix", iwcmTextPrefix);
		return this;
	}

}
