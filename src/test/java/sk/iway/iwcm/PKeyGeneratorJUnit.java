package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sourceforge.stripes.mock.MockServletContext;
import sk.iway.iwcm.database.SimpleQuery;

/**
 *  PKeyGeneratorJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 29.7.2010 16:30:30
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class PKeyGeneratorJUnit
{
	private static final String TEST_TABLE = "_test";

	@BeforeEach
	public void setup()
	{
		Constants.setServletContext(new MockServletContext(""));
		Constants.setInt("pkeyGenOffset", 0);
		new SimpleQuery().execute("CREATE TABLE _test (_test_id BIGINT PRIMARY KEY);");
		new SimpleQuery().execute("INSERT INTO _test VALUES(?) ", Integer.MAX_VALUE - 2);
		//value now = MAX_VALUE - 1
		PkeyGenerator.getNextValue(TEST_TABLE);
	}

	@AfterEach
	public void tearDown()
	{
		new SimpleQuery().execute("DELETE FROM pkey_generator WHERE table_name = ?", TEST_TABLE);
		new SimpleQuery().execute("DROP TABLE _test");
	}

	@Test
	public void getInt()
	{
		int value = PkeyGenerator.getNextValue(TEST_TABLE);
		assertTrue(value == Integer.MAX_VALUE-3);
	}

	@Test
	public void getLong()
	{
		long value = PkeyGenerator.getNextValueAsLong(TEST_TABLE);
		//value now == MAX_VALUE
		value = PkeyGenerator.getNextValueAsLong(TEST_TABLE);
		//value now = MAX_VALUE + 1
		assertTrue(value == (long)(Integer.MAX_VALUE) + 1);
	}
}