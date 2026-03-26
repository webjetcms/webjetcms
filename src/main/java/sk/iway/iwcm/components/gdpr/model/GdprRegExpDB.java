package sk.iway.iwcm.components.gdpr.model;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Query;

import org.eclipse.persistence.jpa.JpaEntityManager;

import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *  GdprRegExpDB.java
 *
 *	DAO class for manipulating with GdprRegExpBean
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 09.05.2018 09:25:32
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class GdprRegExpDB extends JpaDB<GdprRegExpBean>
{
    private static GdprRegExpDB INSTANCE=null;

    public static GdprRegExpDB getInstance()
    {
        if(INSTANCE == null)
            INSTANCE = new GdprRegExpDB();
        return INSTANCE;
    }

	public GdprRegExpDB()
	{
		super(GdprRegExpBean.class);
	}

		
	public List<GdprRegExpBean> findByRegexpName(String regexpName)
	{
		return JpaTools.findByMatchingProperty(GdprRegExpBean.class, "regexpName", regexpName);
	}
	
	public GdprRegExpBean findFirstByRegexpName(String regexpName)
	{
		return JpaTools.findFirstByMatchingProperty(GdprRegExpBean.class, "regexpName", regexpName);
	}	
	public List<GdprRegExpBean> findByRegexpValue(String regexpValue)
	{
		return JpaTools.findByMatchingProperty(GdprRegExpBean.class, "regexpValue", regexpValue);
	}
	
	public GdprRegExpBean findFirstByRegexpValue(String regexpValue)
	{
		return JpaTools.findFirstByMatchingProperty(GdprRegExpBean.class, "regexpValue", regexpValue);
	}	
	public List<GdprRegExpBean> findByUserId(int userId)
	{
		return JpaTools.findByMatchingProperty(GdprRegExpBean.class, "userId", userId);
	}
	
	public GdprRegExpBean findFirstByUserId(int userId)
	{
		return JpaTools.findFirstByMatchingProperty(GdprRegExpBean.class, "userId", userId);
	}	
	public List<GdprRegExpBean> findByDateInsert(Date dateInsert)
	{
		return JpaTools.findByMatchingProperty(GdprRegExpBean.class, "dateInsert", dateInsert);
	}
	
	public GdprRegExpBean findFirstByDateInsert(Date dateInsert)
	{
		return JpaTools.findFirstByMatchingProperty(GdprRegExpBean.class, "dateInsert", dateInsert);
	}

    public static List<String> getAllRegexpString()
    {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        em.getTransaction().begin();
        Query query = null;

        query = em.createQuery("SELECT g.regexpValue FROM GdprRegExpBean g ", Long.class);

        return JpaDB.getResultList(query);
    }
}