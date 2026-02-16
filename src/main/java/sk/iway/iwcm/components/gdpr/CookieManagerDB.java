package sk.iway.iwcm.components.gdpr;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.system.jpa.JpaTools.Condition;

/**
 *  CookieManagerDB.java
 *
 *	DAO class for manipulating with CookieManagerBean
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 19.05.2018 10:43:50
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class CookieManagerDB extends JpaDB<CookieManagerBean>
{
	public CookieManagerDB()
	{
		super(CookieManagerBean.class);
	}

	public List<CookieManagerBean> findByNameAndProvider(String cookieName, String provider)
	{
		List<Condition> conditions = new ArrayList<JpaTools.Condition>();
		if(Tools.isNotEmpty(cookieName))
			conditions.add(CookieManagerDB.filterSubstringIgnoringCase("cookie_name", cookieName));
		if(Tools.isNotEmpty(provider))
			conditions.add(CookieManagerDB.filterSubstringIgnoringCase("provider", provider));
		return JpaTools.findBy(CookieManagerBean.class,  conditions.toArray(new Condition[]{}));
	}
		
	public List<CookieManagerBean> findByDomainId(int domainId)
	{
		return JpaTools.findByMatchingProperty(CookieManagerBean.class, "domainId", domainId);
	}
	
	public CookieManagerBean findFirstByDomainId(int domainId)
	{
		return JpaTools.findFirstByMatchingProperty(CookieManagerBean.class, "domainId", domainId);
	}	
	public List<CookieManagerBean> findByUserId(int userId)
	{
		return JpaTools.findByMatchingProperty(CookieManagerBean.class, "userId", userId);
	}
	
	public CookieManagerBean findFirstByUserId(int userId)
	{
		return JpaTools.findFirstByMatchingProperty(CookieManagerBean.class, "userId", userId);
	}	
	public List<CookieManagerBean> findBySaveDate(Date saveDate)
	{
		return JpaTools.findByMatchingProperty(CookieManagerBean.class, "saveDate", saveDate);
	}
	
	public CookieManagerBean findFirstBySaveDate(Date saveDate)
	{
		return JpaTools.findFirstByMatchingProperty(CookieManagerBean.class, "saveDate", saveDate);
	}	
	public List<CookieManagerBean> findByCookieName(String cookieName)
	{
		return JpaTools.findByMatchingProperty(CookieManagerBean.class, "cookieName", cookieName);
	}
	
	public CookieManagerBean findFirstByCookieName(String cookieName)
	{
		return JpaTools.findFirstByMatchingProperty(CookieManagerBean.class, "cookieName", cookieName);
	}	
	public List<CookieManagerBean> findByDescription(String description)
	{
		return JpaTools.findByMatchingProperty(CookieManagerBean.class, "description", description);
	}
	
	public CookieManagerBean findFirstByDescription(String description)
	{
		return JpaTools.findFirstByMatchingProperty(CookieManagerBean.class, "description", description);
	}	
	public List<CookieManagerBean> findByProvider(String provider)
	{
		return JpaTools.findByMatchingProperty(CookieManagerBean.class, "provider", provider);
	}
	
	public CookieManagerBean findFirstByProvider(String provider)
	{
		return JpaTools.findFirstByMatchingProperty(CookieManagerBean.class, "provider", provider);
	}	
	public List<CookieManagerBean> findByPurpouse(String purpouse)
	{
		return JpaTools.findByMatchingProperty(CookieManagerBean.class, "purpouse", purpouse);
	}
	
	public CookieManagerBean findFirstByPurpouse(String purpouse)
	{
		return JpaTools.findFirstByMatchingProperty(CookieManagerBean.class, "purpouse", purpouse);
	}	
	public List<CookieManagerBean> findByValidity(String validity)
	{
		return JpaTools.findByMatchingProperty(CookieManagerBean.class, "validity", validity);
	}
	
	public CookieManagerBean findFirstByValidity(String validity)
	{
		return JpaTools.findFirstByMatchingProperty(CookieManagerBean.class, "validity", validity);
	}	
	public List<CookieManagerBean> findByType(String type)
	{
		return JpaTools.findByMatchingProperty(CookieManagerBean.class, "type", type);
	}
	
	public CookieManagerBean findFirstByType(String type)
	{
		return JpaTools.findFirstByMatchingProperty(CookieManagerBean.class, "type", type);
	}	
	public List<CookieManagerBean> findByClassification(String classification)
	{
		return JpaTools.findByMatchingProperty(CookieManagerBean.class, "classification", classification);
	}
	
	public CookieManagerBean findFirstByClassification(String classification)
	{
		return JpaTools.findFirstByMatchingProperty(CookieManagerBean.class, "classification", classification);
	}
}