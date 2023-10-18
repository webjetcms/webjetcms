package sk.iway.iwcm.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sk.iway.iwcm.test.Assertions.collectionsEqual;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.doc.DocDetails;

/**
 *  AssertionsJUnit.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 4.12.2009 15:13:41
 *@modified     $Date: 2009/12/11 15:46:32 $
 */
public class AssertionsJUnit
{

	@Test
	public void setEqualityOfOrdinaryTypes()
	{
		List<Integer> first = new ArrayList<Integer>();
		List<Integer> second = new ArrayList<Integer>();
		Random generator = new Random();

		for (int i=0; i< 10; i++)
		{
			int value = generator.nextInt();
			first.add(value);
			second.add(0, value);
		}

		assertTrue(collectionsEqual(first, second));
	}

	@Test
	public void setUnequality()
	{
		List<String> first = Arrays.asList("One", "Two", "Three");
		List<String> second = Arrays.asList("One", "Two");

		assertFalse(collectionsEqual(first, second));
	}

	@Test
	public void setEqualityWithCustomEqual()
	{
		List<String> first = Arrays.asList("Name1", "Name2");
		List<DocDetails> second = new ArrayList<DocDetails>();
		DocDetails firstDocument = new DocDetails();
		firstDocument.setTitle("Name1");
		DocDetails secondDocument = new DocDetails();
		secondDocument.setTitle("Name2");
		second.add(firstDocument);
		second.add(secondDocument);

		assertTrue(collectionsEqual(first, second, new Equal<String, DocDetails>()
		{
			@Override
			public boolean areEqual(String string, DocDetails doc)
			{
				return doc.getTitle().equals(string);
			}
		}));
	}
}
