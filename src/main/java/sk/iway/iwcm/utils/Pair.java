package sk.iway.iwcm.utils;

/**
 *  Pair.java
 *
 *  An ordered 2-tuple, containing objects passed as arguments
 *  to constructor. Can be used in methods that seemingly ought
 *  to return 2 values. Those methods can return an instance
 *  of {@link Pair} class, without the need to construct a new class.
 *
 *	@see PairMaker
 * @see Triplet
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.10.2010 15:31:15
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class Pair<T, U>
{
	public final T first;
	public final U second;

	public Pair(T first, U second)
	{
		this.first = first;
		this.second = second;
	}

	public static <T, U> Pair<T, U> of(T first, U second)
	{
		return new Pair<>(first, second);
	}

	public T getFirst()
	{
		return first;
	}

	public U getSecond()
	{
		return second;
	}

	@Override
	public String toString()
	{
		return String.format("Pair: [%s, %s]", first, second);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		@SuppressWarnings("unchecked")
		Pair<T, U> other = (Pair<T, U>) obj;
		if (first == null)
		{
			if (other.first != null)
				return false;
		}
		else if (!first.equals(other.first))
			return false;
		if (second == null)
		{
			if (other.second != null)
				return false;
		}
		else if (!second.equals(other.second))
			return false;
		return true;
	}
}