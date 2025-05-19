package sk.iway.iwcm;

import java.util.Calendar;
import java.util.Date;

/**
 *  BirthNumber.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: thaber $
 *@version      $Revision: 1.1 $
 *@created      Date: Aug 5, 2009 11:12:18 AM
 *@modified     $Date: 2009/08/19 10:56:16 $
 */
public class BirthNumber
{
	private boolean valid = false;
	private int year = -1;
	private int month = -1;
	private int day = -1;
	private int ext = -1;
	private boolean male = false;
	private int actualYear= -1;

	@SuppressWarnings("unused")
	private BirthNumber() {}

	public BirthNumber(String birthNumber)
	{
		if (Tools.isEmpty(birthNumber)) {
			return;
		}

		birthNumber = birthNumber.replaceAll("[^\\d.]", "");

		if (Tools.isEmpty(birthNumber) || birthNumber.length() < 9 || birthNumber.length() > 10) {
			return;
		}

		month = Tools.getIntValue(birthNumber.substring(2, 4), -1);
		year = Tools.getIntValue(birthNumber.substring(0, 2), -1) + 1900;
		day = Tools.getIntValue(birthNumber.substring(4, 6), -1);
		ext = Tools.getIntValue(birthNumber.substring(6, 9), -1);
		int c = Tools.getIntValue(birthNumber.substring(9), -1);
		actualYear = Calendar.getInstance().get(Calendar.YEAR);
		if (actualYear - year >= 100)
		{
			year += 100;
		}
		if (!((month >= 1 && month <= 12) || (month >= 51 && month <= 62)))
		{
			valid = false;
			return;
		}
		if (month >= 51)
		{
			month -= 50;
			male = false;
		}
		else
		{
			male = true;
		}
		if (year <= 1953)
		{
			if (birthNumber.length() != 9)
			{
				valid = false;
				return;
			}
		}
		else
		{
			if (birthNumber.length() != 10)
			{
				valid = false;
				return;
			}
			long celeCislo = Tools.getLongValue(birthNumber.substring(0, 9), -1);
			long mod = celeCislo % 11l;

			if (mod == 10) {
				mod = 0;
			}

			if (mod != c)
			{
				valid = false;
				return;
			}
		}
		valid = true;
		return;
	}

	public boolean isValid()
	{
		return valid;
	}

	public static boolean isValid(String birthNumber)
	{
		return new BirthNumber(birthNumber).isValid();
	}

	public int getYear()
	{
		return year;
	}

	public int getMonth()
	{
		return month;
	}

	public int getDay()
	{
		return day;
	}

	public boolean isMale()
	{
		return male;
	}

	public int getAge()
	{
		return actualYear-year;
	}

	public int getExt() {
		return ext;
	}

	public Date getBirthDate() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, getYear());
		c.set(Calendar.MONTH, getMonth());
		c.set(Calendar.DAY_OF_MONTH, getDay());

		return c.getTime();
	}
}
