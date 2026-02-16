package sk.iway.iwcm.users;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.Table;

import org.eclipse.persistence.jpa.JpaEntityManager;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *  PasswordsHistoryDB.java
 *
 *	DAO class for manipulating with PasswordsHistoryBean
 *  #23471 - Password security
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 10.01.2018 14:52:01
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class PasswordsHistoryDB extends JpaDB<PasswordsHistoryBean>
{
    private static PasswordsHistoryDB INSTANCE = null;
    private String TABLE = "";
    private SimpleQuery sq = new SimpleQuery();

	public static PasswordsHistoryDB getInstance()
    {
        if(INSTANCE == null)
            INSTANCE  = new PasswordsHistoryDB();
        return INSTANCE;
    }

    public PasswordsHistoryDB()
    {
         super(PasswordsHistoryBean.class);
        if (PasswordsHistoryBean.class.isAnnotationPresent(Table.class))
        {
            TABLE = PasswordsHistoryBean.class.getAnnotation(Table.class).name();
        }
    }

    public List<PasswordsHistoryBean> findByUserId(int userId)
	{
		return JpaTools.findByMatchingProperty(PasswordsHistoryBean.class, "userId", userId);
	}

	public PasswordsHistoryBean findFirstByUserId(int userId)
	{
		return JpaTools.findFirstByMatchingProperty(PasswordsHistoryBean.class, "userId", userId);
	}
	public List<PasswordsHistoryBean> findByPassword(String password)
	{
		return JpaTools.findByMatchingProperty(PasswordsHistoryBean.class, "password", password);
	}

	public PasswordsHistoryBean findFirstByPassword(String password)
	{
		return JpaTools.findFirstByMatchingProperty(PasswordsHistoryBean.class, "password", password);
	}
	public List<PasswordsHistoryBean> findBySalt(String salt)
	{
		return JpaTools.findByMatchingProperty(PasswordsHistoryBean.class, "salt", salt);
	}

	public PasswordsHistoryBean findFirstBySalt(String salt)
	{
		return JpaTools.findFirstByMatchingProperty(PasswordsHistoryBean.class, "salt", salt);
	}
	public List<PasswordsHistoryBean> findBySaveDate(Date saveDate)
	{
		return JpaTools.findByMatchingProperty(PasswordsHistoryBean.class, "saveDate", saveDate);
	}

	public PasswordsHistoryBean findFirstBySaveDate(Date saveDate)
	{
		return JpaTools.findFirstByMatchingProperty(PasswordsHistoryBean.class, "saveDate", saveDate);
	}

	protected static boolean existsPassword(int userId, String hash)
	{
      if (Constants.getBoolean("passwordHistoryEnabled")==false) return false;

		long count = 0;
		if(userId > 0 && Tools.isNotEmpty(hash))
		{
	        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
	        em.getTransaction().begin();

	        Query query = em.createQuery("SELECT COUNT(p) FROM PasswordsHistoryBean p WHERE p.userId = :userId AND p.password = :password",Long.class);

	        query.setParameter("userId",userId).setParameter("password",hash);//.executeUpdate();
	        count = (Long)query.getSingleResult();
	        em.getTransaction().commit();
		}
		return count > 0;
	}

	public boolean existsPassword(String newUserPassword, int userId)
    {
       if (Constants.getBoolean("passwordHistoryEnabled")==false) return false;

       List<PasswordsHistoryBean> passwordList = findByUserId(userId);
       if(passwordList == null || passwordList.size() <= 1)
       {
           return false;
       }

        Collections.sort(passwordList, new OrderByNew());
        passwordList.remove(0);//prvy je aktualne heslo a to nechceme validovat

       try
       {
           for (PasswordsHistoryBean passwordBean : passwordList)
           {
               if(Tools.isEmpty(passwordBean.getSalt()))
               {
                   sk.iway.Password pass = new sk.iway.Password();
                   if(passwordBean.getPassword().equals(pass.encrypt(newUserPassword)))
                   {
                       return true;
                   }
               }
               else
               {
                   if(passwordBean.getPassword().equals(PasswordSecurity.calculateHash(newUserPassword, passwordBean.getSalt())))
                   {
                       return true;
                   }
               }
           }
       }
       catch(Exception exc)
       {
           sk.iway.iwcm.Logger.error(exc);
           return false;
       }
       return false;
    }

    public int getCount(int userId)
    {
        return sq.forInt("SELECT count(*) from " + TABLE + " WHERE user_id = ? ",userId);
    }

    public void deleteOld(int userId)
    {

        if(getCount(userId) > Constants.getInt("passwordHistoryLength")+1)//porovname posledne 3(historicke) + 1(atualne)
        {
            int id = sq.forInt("SELECT MIN(passwords_history_id) from " + TABLE +" where user_id = ?",userId);
            sq.execute("DELETE FROM "+TABLE+" WHERE passwords_history_id = ?",id);
        }
    }

    public void deleteAllByUserId(int userId)
    {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        em.getTransaction().begin();

        Query query = em.createQuery("DELETE FROM PasswordsHistoryBean p WHERE p.userId = :userId OR p.userId = :one");

        query.setParameter("userId",userId).setParameter("one",-1).executeUpdate();
        em.getTransaction().commit();
    }

    public class OrderByNew implements Comparator<PasswordsHistoryBean> {
        @Override
        public int compare(PasswordsHistoryBean object1, PasswordsHistoryBean object2) {
            Integer num1 = object1.getId();
            Integer num2 = object2.getId();

            return num2.compareTo(num1);
        }
    }
}