package sk.iway.iwcm.sync.inport;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Cislovany prvok v zozname. Pouzijeme, ked chceme iterovat prvky zoznamu, ale
 * potrebujeme vediet aj poradove cislo.
 * 
 * <pre>
 * String[] wordArray = { "zero", "one", "two" };
 * for (Numbered&lt;String&gt; numWord : Numbered.array(wordArray))
 * {
 * 	System.out.println("" + numWord.number + " = " + numWord.item);
 * }
 * List&lt;String&gt; wordList = Arrays.asList(wordArray);
 * for (Numbered&lt;String&gt; numWord : Numbered.list(wordList))
 * {
 * 	System.out.println("" + numWord.number + " = " + numWord.item);
 * }
 * </pre>
 * 
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2012
 * @author $Author: jeeff vbur $
 * @version $Revision: 1.3 $
 * @created Date: 29.6.2012 11:56:44
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class Numbered<T>
{
	public final int number;
	public final T item;

	Numbered(int number, T item)
	{
		this.number = number;
		this.item = item;
	}

	/**
	 * Prisposobi dany zoznam na ocislovanie v prikaze "for". 
	 * 
	 * @param list
	 * @return
	 */
	public static <T> Iterable<Numbered<T>> list(final List<T> list)
	{
		return new Iterable<Numbered<T>>()
		{
			final List<T> itemList = list;

			@Override
			public Iterator<Numbered<T>> iterator()
			{
				return new Iterator<Numbered<T>>()
				{
					private int number = 0;
					final Iterator<T> itemIterator = itemList.iterator();

					@Override
					public boolean hasNext()
					{
						return itemIterator.hasNext();
					}

					@Override
					public Numbered<T> next()
					{
						return new Numbered<T>(number++, itemIterator.next());
					}

					/**
					 * Vymaze polozku; v cislovani sa vsak pokracuje podla povodneho poradia.
					 * Ak povodny zoznam nepodporuje vymazavanie, ani tento nebude.
					 */
					@Override
					public void remove()
					{
						itemIterator.remove();
					}

				};
			}

		};
	}

	/**
	 * Prisposobi dane pole na ocislovanie v prikaze "for". 
	 * 
	 * @param array
	 * @return
	 */
	public static <T> Iterable<Numbered<T>> array(T[] array)
	{
		return list(Arrays.asList(array));
	}

}
