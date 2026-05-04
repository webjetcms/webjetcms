package sk.iway.iwcm.utils;

/**
 *  Triplet.java
 *
 *  An ordered 3-tuple, containing objects passed as arguments
 *  to constructor. Can be used in methods that seemingly ought
 *  to return 3 values. Those methods can return an instance
 *  of {@link Triplet} class, without the need to construct a new class.
 *
 *  @see Pair
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.10.2010 16:28:38
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class Triplet<T1, T2, T3>
{
	public final T1 first;
	public final T2 second;
	public final T3 third;

	public Triplet(T1 first, T2 second, T3 third)
	{
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public static <T1, T2, T3> Triplet<T1, T2, T3> of(T1 first, T2 second, T3 third)
	{
		return new Triplet<>(first, second, third);
	}

	@Override
	public String toString()
	{
		return String.format("Triplet: [%s, %s, %s]", first, second, third);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		result = prime * result + ((third == null) ? 0 : third.hashCode());
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
		Triplet<T1, T2, T3> other = (Triplet<T1, T2, T3>) obj;
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
		if (third == null)
		{
			if (other.third != null)
				return false;
		}
		else if (!third.equals(other.third))
			return false;
		return true;
	}
}