package sk.iway.iwcm.database;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *  Mapper.java
 *  Class for transforming ResultSet rows to objects
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff thaber $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.6.2010 14:27:51
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public interface Mapper<T>
{
	/**
	 * Maps a row from ResultSet to an Object
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public T map(ResultSet rs) throws SQLException;
}
