package sk.iway.iwcm.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.users.UserDetails;

/**
 *  ComplexQueryJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff thaber $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.6.2010 14:35:26
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ComplexQueryJUnit
{
	@Test
	public void test() throws Exception
	{
		List<UserDetails> result = new ComplexQuery().setSql("select * from users where login = ? ").setParams("admin").list(new Mapper<UserDetails>()
		{
			@Override
			public UserDetails map(ResultSet rs) throws SQLException
			{
				UserDetails userDetails = new UserDetails();
				userDetails.setLogin(rs.getString("login"));
				return userDetails;
			}
		});
		assertEquals(1, result.size());
		assertEquals("admin", result.get(0).getLogin());
	}
}
