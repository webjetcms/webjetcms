package sk.iway.iwcm.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * ExcelImportJXLTest.java - testuje datumy v ExcelImportJXL.
 *
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2011
 * @author $Author: jeeff vbur $
 * @version $Revision: 1.3 $
 * @created Date: 2.12.2011 16:45:17
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class ExcelImportJXLTest
{

	@Test
	public void testEmptyDate()
	{
		assertEquals(null, ExcelImportJXL.getDateValue(""));
	}

}
