package sk.iway.iwcm.system.fulltext.cdb;

import java.io.File;

import com.strangegizmo.cdb.Cdb;

import org.apache.commons.pool.PoolableObjectFactory;

import sk.iway.iwcm.system.fulltext.lucene.LuceneUtils;

/**
 *  CdbPool.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: jeeff thaber $
 *@version      $Revision: 1.3 $
 *@created      Date: 17.5.2011 10:27:51
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@SuppressWarnings("rawtypes")
public class CdbFactory implements PoolableObjectFactory
{
	public enum Type {
		Lemmas,Synonyms
	}


	private String language;
	private Type type;

	/**
	 *
	 */
	public CdbFactory(String language,Type type)
	{
		this.language = language;
		this.type = type;
	}

	public boolean validateObject(Object obj)
	{
		return false;
	}

	public void passivateObject(Object obj) throws Exception
	{
	}

	public Object makeObject() throws Exception
	{
		return new Cdb(LuceneUtils.LUCENE_INDEX + File.separatorChar + type.toString().toLowerCase() + File.separatorChar
					+ language + ".cdb");
	}

	public void destroyObject(Object obj) throws Exception
	{
		Cdb cdb = (Cdb) obj;
		cdb.close();
	}

	public void activateObject(Object obj) throws Exception
	{
	}

}
