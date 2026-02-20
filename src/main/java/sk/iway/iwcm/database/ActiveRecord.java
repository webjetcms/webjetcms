package sk.iway.iwcm.database;

/**
 *  ActiveRecord.java
 * 	
 *		Basic subclass for easier and uniform work with
 *		JPA beans. Supposed to work together with JpaDB class,
 *		namely subclasses of JpaDB.
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 28.2.2011 11:21:12
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public abstract class ActiveRecord extends ActiveRecordBase
{
	public abstract int getId();
	public abstract void setId(int id);
	
}