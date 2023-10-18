package sk.iway.iwcm.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.doc.DocDetails;

/**
 *  PairJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.10.2010 16:35:18
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class PairJUnit
{
	@Test
	public void creation()
	{
		Pair<Integer, Boolean> pair = new Pair<Integer, Boolean>(5, true);

		assertThat(pair.first, is(5));
		assertThat(pair.second, is(true));
	}

	@Test
	public void triplet()
	{
		Triplet<DocDetails, String, Integer> docPathDocId = new Triplet<DocDetails, String, Integer>(new DocDetails(), "/sk", 1);
		assertThat(docPathDocId.first, is(not(nullValue())));
		assertThat(docPathDocId.second, is("/sk"));
		assertThat(docPathDocId.third, is(1));
	}

	@Test
	public void twoIdenticalPairsShouldBeEqual()
	{
		Pair<Integer, Boolean> pair = new Pair<Integer, Boolean>(5, true);
		Pair<Integer, Boolean> pair2 = new Pair<Integer, Boolean>(5, true);

		assertThat(pair, is(equalTo(pair2)));
	}
}