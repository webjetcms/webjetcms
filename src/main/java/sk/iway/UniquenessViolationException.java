package sk.iway;

/**
 *  UniquenessViolationException.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 18.4.2011 13:43:02
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class UniquenessViolationException extends Exception
{
	private static final long serialVersionUID = 5245854094850574560L;
	
	private final String field;
	private final String value;
	
	public UniquenessViolationException(String field, String value)
	{
		super(String.format("Field %s with value %s is already taken", field, value));
		this.field = field;
		this.value = value;
	}

	public String getField()
	{
		return field;
	}

	public String getValue()
	{
		return value;
	}
}