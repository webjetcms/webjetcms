package sk.iway;

import sk.iway.iwcm.*;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.components.crypto.Rijndael;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.PasswordsHistoryDB;

import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Random;


/**
 *  Sifrovanie a desifrovanie hesiel pomocou Rijndael algoritmu (1024 bit kluc)
 *  maximalna dlzka hesla je 16 znakov !!!!
 *
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001
 *@author       jeeff
 *@version      1.0
 *@created      Štvrtok, 2001, december 20
 */
public class Password
{
   //kluc sifry
   private static final Random rand = new Random();

   /**
    *  Constructor for the Password object
    *
    *@exception  Exception  Description of the Exception
    */
   public Password() throws Exception
   {
		//originaly there was instance of RijndaelCipher
   }

   /**
    *  Zasifrovanie hesla
    *
    *@param  password       heslo, lubovolna dlzka, pricom dlzka vysledneho hashu = (nasobok 16, rovny alebo najblizsi vyssi od password.length())*2
    *@return                zasifrovane heslo, zahashovane ako string (mozne rovno strcit do DB)
    *@exception  Exception  Description of the Exception
    */
   public String encrypt(String password) throws Exception
   {
		String pw = password;
		//dlzka encryptovaneho stringu musi byt nasobok 16
		if (pw.length() % 16 > 0) {
			int len = ((pw.length() / 16) + 1) * 16;
			pw = pw + "                                 ";
			if (pw.length() > len) {
				pw = pw.substring(0, len);
			}
		}
		return Rijndael.encrypt(pw, getKey());
   }


   /**
    *  Desifrovanie hesla
    *
    *@param  password       zasifrovane heslo
    *@return                heslo
    *@exception  Exception  Description of the Exception
    */
   public String decrypt(String password) throws Exception
   {
      return Rijndael.decrypt(password, getKey());
   }

   /**
    *  konverzia stringu na pole bajtov (a sa prevedie na 65)
    *
    *@param  s  vstupny text
    *@return    pole bajtov
    */
   public static byte[] toByteArray(String s)
   {
      ByteArrayOutputStream b = new ByteArrayOutputStream(
            s.length() / 2);
      int hival = 0;
      boolean hinybble = false;
      for (int i = 0; i < s.length(); i++)
      {
         //char c = s.charAt(i);
         int hexval;
         try
         {
            hexval = Integer.parseInt(s.substring(i, i + 1), 16);
            if (hinybble)
            {
               b.write(hival + hexval);
               hinybble = false;
            }
            else
            {
               hival = hexval << 4;
               hinybble = true;
            }
         }
         catch (java.lang.NumberFormatException e)
         {
            //pokracuj nech sa deje co sa deje ;-)
         }
      }
      byte[] rv = b.toByteArray();
      try
      {
         b.close();
      }
      catch (IOException e)
      {
      }
      return rv;
   }

   /**
    *  konverzia pola bajtov na string reprezentaciu
    *  (65 sa prevedie na a)
    *
    *@param  a  pole bajtov
    *@return    skonvertovany string
    */
   public static String fromByteArray(byte[] a)
   {
      StringBuilder b = new StringBuilder(3 * a.length);
      for (int i = 0; i < a.length; i++)
      {
         b.append(Integer.toHexString(512 + a[i] & 0xffffffff).substring(1));
      }
      return new String(b);
   }

   /**
    * Vygeneruje heslo zlozene z size poctu cislic
    * @param size
    * @return
    */
   public static String generatePassword(int size)
   {
		//vygeneruj 5 miestny kod
		StringBuilder code = new StringBuilder();
		int i;
		for (i = 0; i < size; i++)
		{
			code.append(rand.nextInt(9));
		}
		return(code.toString());
   }

	/**
	 * generuje nahodny retazec
	 * @return
	 */
	public static String generateStringHash(int size)
	{
		int i;
		StringBuilder ret = new StringBuilder();
		char ch = ' ';
		int rnd;
		for (i = 0; i < size; i++)
		{
			rnd = rand.nextInt(20) + 66;
			ch = (char) rnd;
			ret.append(ch);
		}

		return (ret).toString();
	}

	/**
	 * Generate strong password in form of XXXXXX-XXXXXX-XXXXXX (3 parts, each of 6 chars)
	 * with random chars and digits and special chars with uppercase and lowercase letters
	 * @return
	 */
	public static String generateStrongPassword()
	{
		StringBuilder password = new StringBuilder();

		//skip o as it can be confused with 0 and l as it can be confused with 1
		String allowedChars = "abcdefghijkmnpqrstuvwxyz";
		String numbers = "123456789";
		String specialChars = "!.@%*()+=/:<>";

		int minNumbers = Integer.max(Constants.getInt("passwordAdminMinCountOfDigits"), 2);
		int minSpecialChars = Integer.max(Constants.getInt("passwordMinCountOfSpecialSigns"), 2);
		int minUpperCase = Integer.max(Constants.getInt("passwordAdminMinUpperCaseLetters"), 2);
		int minLowerCase = Integer.max(Constants.getInt("passwordMinLowerCaseLetters"), 2);

		//add required number of digits
		for (int i = 0; i < minNumbers; i++) {
			password.append(numbers.charAt(rand.nextInt(numbers.length())));
		}
		//add required number of special chars
		for (int i = 0; i < minSpecialChars; i++) {
			password.append(specialChars.charAt(rand.nextInt(specialChars.length())));
		}
		//add required number of uppercase letters
		for (int i = 0; i < minUpperCase; i++) {
			password.append((""+allowedChars.charAt(rand.nextInt(allowedChars.length()))).toUpperCase());
		}
		//add required number of lowercase letters
		for (int i = 0; i < minLowerCase; i++) {
			password.append(allowedChars.charAt(rand.nextInt(allowedChars.length())));
		}

		int minPasswordLength = 18;
		//add random chars until we reach min password length
		while (password.length() < minPasswordLength) {
			password.append(allowedChars.charAt(rand.nextInt(allowedChars.length())));
		}

		//shuffle the characters in the password - split password into array of chars, shuffle it and then join it back to string
		char[] passwordChars = password.toString().toCharArray();
		for (int i = 0; i < passwordChars.length; i++) {
			int randomIndex = rand.nextInt(passwordChars.length);
			//swap chars
			char temp = passwordChars[i];
			passwordChars[i] = passwordChars[randomIndex];
			passwordChars[randomIndex] = temp;
		}

		//add dash sign every 6 chars
		StringBuilder finalPassword = new StringBuilder();
		for (int i = 0; i < passwordChars.length; i++) {
			finalPassword.append(passwordChars[i]);
			if ((i + 1) % 6 == 0 && i != passwordChars.length - 1) {
				finalPassword.append("-");
			}
		}

		return finalPassword.toString();
	}

	private static String getKey() {
		StringBuilder sb = new StringBuilder();
		sb.append("5d16798e32165b9844c");
		sb.insert(0, "1465a651b5162c354");
		sb.append("765d65218a0354e82065f984f16b");
		return sb.toString();
	}

   /**
	 * skontroluje heslo podla nastavenych podmienok
	 * @param isLogonForm - true ak idem z logon formu
	 * @param password
	 * @param isAdmin
	 * @param userId
	 * @param session
	 * @param errors
	 * @return - vrati true ak je heslo v poriadku
	 */
	public static boolean checkPassword(boolean isLogonForm, String password, boolean isAdmin, int userId, HttpSession session, List<String> errors)
	{
		String constStr = "";
		if(isAdmin)
		{
			constStr = "Admin";
		}
		int dlzkaHesla = Constants.getInt("password"+constStr+"MinLength");
		int pocetZnakov = Constants.getInt("password"+constStr+"MinCountOfSpecialSigns");
		int pocetVelkychPismen = Constants.getInt("password"+constStr+"MinUpperCaseLetters");
		int pocetMalychPismen = Constants.getInt("password"+constStr+"MinLowerCaseLetters");
		int pocetCisel = Constants.getInt("password"+constStr+"MinCountOfDigits");
		int vyprsanieHesla = Constants.getInt("password"+constStr+"ExpiryDays");
		int countPocetZnakov = 0;
		int countPocetVelkychPismen = 0;
		int countPocetMalychPismen = 0;
		int countPocetCisel = 0;
		boolean jeChybneHeslo = false;

		Prop prop = Prop.getInstance(Prop.getLng(session));

		//testujem, len pri zmene hesla
		if(isLogonForm || (!isLogonForm && password.trim().compareToIgnoreCase(UserTools.PASS_UNCHANGED) != 0))
		{
			if(password.length() >= dlzkaHesla)
			{
				char[] passwordCharArray = password.toCharArray();
				for(int i=0; i < passwordCharArray.length; i++)
				{
					if(Character.isDigit(passwordCharArray[i]))
					{
						countPocetCisel++;
					}
					else if(Character.isLetter(passwordCharArray[i]))
					{
						if(Character.isUpperCase(passwordCharArray[i]))
						{
							countPocetVelkychPismen++;
						}
						else if(Character.isLowerCase(passwordCharArray[i]))
						{
							countPocetMalychPismen++;
						}
					}
					else
					{
						countPocetZnakov++;
					}
				}
			}
			else
			{
				//heslo je kratke
				Logger.error(Password.class,"heslo je kratke");
				if(errors != null)
				{
					errors.add(prop.getText("logon.change_password.min_length", ""+dlzkaHesla));
				}
				jeChybneHeslo = true;
			}
			if(PasswordsHistoryDB.getInstance().existsPassword(password,userId))
            {
                Logger.error(Password.class,"heslo uz uzivatel v minulosti pouzil");
                if (errors != null)
                {
                    errors.add(prop.getText("logon.change_password.used_in_history"));
                }
                jeChybneHeslo = true;
            }
			if(countPocetCisel < pocetCisel)
			{
				//heslo ma maly pocet cisel
				Logger.error(Password.class,"heslo ma maly pocet cisel");
				if(errors != null)
				{
					errors.add(prop.getText("logon.change_password.count_of_digits", ""+pocetCisel));
				}
				jeChybneHeslo = true;
			}
			if(countPocetVelkychPismen < pocetVelkychPismen)
			{
				//heslo ma maly pocet velkych pismen
				Logger.error(Password.class,"heslo ma maly pocet velkych pismen");
				if(errors != null)
				{
					errors.add(prop.getText("logon.change_password.count_of_upper_case", ""+pocetVelkychPismen));
				}
				jeChybneHeslo = true;
			}
			if(countPocetMalychPismen < pocetMalychPismen)
			{
				//heslo ma maly pocet malych pismen
				Logger.error(Password.class,"heslo ma maly pocet malych pismen");
				if(errors != null)
				{
					errors.add(prop.getText("logon.change_password.count_of_lower_case", ""+pocetMalychPismen));
				}
				jeChybneHeslo = true;
			}
			if(countPocetZnakov < pocetZnakov)
			{
				//heslo ma maly pocet znakov
				Logger.error(Password.class,"heslo ma maly pocet znakov");
				if(errors != null)
				{
					errors.add(prop.getText("logon.change_password.count_of_special_sign", ""+pocetZnakov));
				}
				jeChybneHeslo = true;
			}
		}

		if(isLogonForm || (!isLogonForm && password.trim().compareToIgnoreCase(UserTools.PASS_UNCHANGED) == 0))
		{
			//skontroluj este exspiraciu terajsieho hesla
			if(!isLogonForm || (isLogonForm && jeChybneHeslo == false && session != null && Tools.sessionGetAttribute(session, Constants.USER_KEY+"_changepassword") == null))
			{
				if(vyprsanieHesla > 0)
				{
					long lastDate = Adminlog.getLastDate(Adminlog.TYPE_USER_CHANGE_PASSWORD, userId);
					Calendar calExpiryDate = Calendar.getInstance();
					calExpiryDate.setTimeInMillis(lastDate);
					calExpiryDate.add(Calendar.DATE, vyprsanieHesla);
					//exspirovalo nam heslo
					if(Tools.getNow() > calExpiryDate.getTimeInMillis())
					{
						Logger.error(Password.class,"exspirovalo heslo "+Tools.formatDateTime(calExpiryDate.getTimeInMillis()));
						jeChybneHeslo = true;
					}
				}
			}
		}
		return (!jeChybneHeslo);
	}

	/** Ak su oba passwordy zadane, porovna ich a vrati vysledok. Ak nie su oba zadane vrati TRUE.
	 *
	 * @param password - zadany
	 * @param password2 - opakovane zadany
	 * @return
	 */
	public static boolean equalsPasswords(String password, String password2)
	{
		if(Tools.isNotEmpty(password2) && Tools.isNotEmpty(password))
			return password2.equals(password);
		return true;
	}
}
