package sk.iway.iwcm.stat;

/**
 *  UpdateInsertSqlPair.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 8.7.2010 10:59:34
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
class UpdateInsertSqlPair
{
	String firstSql;
	String followingSql;
	
	UpdateInsertSqlPair(String firstSql, String followingSql)
	{
		this.firstSql = firstSql;
		this.followingSql = followingSql;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((followingSql == null) ? 0 : followingSql.hashCode());
		result = prime * result + ((firstSql == null) ? 0 : firstSql.hashCode());
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
		UpdateInsertSqlPair other = (UpdateInsertSqlPair) obj;
		if (followingSql == null)
		{
			if (other.followingSql != null)
				return false;
		}
		else if (!followingSql.equals(other.followingSql))
			return false;
		
		if (firstSql == null){
			if (other.firstSql != null)
				return false;
		}
		else if (!firstSql.equals(other.firstSql))
			return false;
		
		return true;
	}
	
	@Override
	public String toString()
	{
		return new StringBuilder().append("First: \n").append(firstSql).append("\nFollowed by: ").append(followingSql).toString();
	}
}
