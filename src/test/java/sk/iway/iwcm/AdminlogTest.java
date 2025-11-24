package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.test.BaseWebjetTest;

/**
 *  AdminlogJUnit.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 14.04.2010 11:29:31
 *@modified     $Date: 2009/12/11 15:42:33 $
 */
public class AdminlogTest extends BaseWebjetTest
{
	@Test
	public void typesAutoloading()
	{
		Integer[] types = Adminlog.getTypes();
		assertFalse(ArrayUtils.contains(types, null));
		assertTrue(ArrayUtils.getLength(types)>=122);
	}
}