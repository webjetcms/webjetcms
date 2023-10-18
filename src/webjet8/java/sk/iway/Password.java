package sk.iway;

import cryptix.provider.key.RawSecretKey;
import cryptix.util.core.Hex;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.users.PasswordsHistoryDB;
import xjava.security.IllegalBlockSizeException;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
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

   xjava.security.Cipher alg;

   /**
    *  Constructor for the Password object
    *
    *@exception  Exception  Description of the Exception
    */
   public Password() throws Exception
   {
      java.security.Security.addProvider(new cryptix.provider.Cryptix());
      //alg = xjava.security.Cipher.getInstance("Rijndael", "Cryptix");
      alg = xjava.security.Cipher.getInstance(new cryptix.provider.cipher.Rijndael(), null, null);
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
   	try {
   			String pw = password;
			//dlzka encryptovaneho stringu musi byt nasobok 16
			if (pw.length() % 16 > 0) {
				int len = ((pw.length() / 16) + 1) * 16;
				pw = pw + "                                 ";
				if (pw.length() > len) {
					pw = pw.substring(0, len);
				}
			}
			byte[] ect;
			//byte[] dct;
			//String a;
			//String b;

			String to_crypt = fromByteArray(pw.getBytes());

			RawSecretKey key = new RawSecretKey("Rijndael", Hex.fromString(getKey()));

			alg.initEncrypt(key);
			ect = alg.crypt(Hex.fromString(to_crypt));
			//a = Hex.toString(ect);

			return (fromByteArray(ect));
		} catch (IllegalBlockSizeException ex) { return ""; }
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
      if (password == null)
      {
         return ("");
      }
      if (password.length() < 10)
      {
         return (password);
      }

      byte[] pass = toByteArray(password);
      byte[] dct;
      //String a;
      String b;

      RawSecretKey key = new RawSecretKey("Rijndael", Hex.fromString(getKey()));

      alg.initDecrypt(key);
      dct = alg.crypt(pass);
      b = Hex.toString(dct);
      return (new String(toByteArray(b)).trim());
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
	public static boolean checkPassword(boolean isLogonForm, String password, boolean isAdmin, int userId, HttpSession session, ActionMessages errors)
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
					errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("passwordLengthFailed"));
				}
				jeChybneHeslo = true;
			}
			if(PasswordsHistoryDB.getInstance().existsPassword(password,userId))
            {
                Logger.error(Password.class,"heslo uz uzivatel v minulosti pouzil");
                if (errors != null)
                {
                    errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("passwordUsedInHistory"));
                }
                jeChybneHeslo = true;
            }
			if(countPocetCisel < pocetCisel)
			{
				//heslo ma maly pocet cisel
				Logger.error(Password.class,"heslo ma maly pocet cisel");
				if(errors != null)
				{
					errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("passwordCountOfDigitsFailed"));
				}
				jeChybneHeslo = true;
			}
			if(countPocetVelkychPismen < pocetVelkychPismen)
			{
				//heslo ma maly pocet velkych pismen
				Logger.error(Password.class,"heslo ma maly pocet velkych pismen");
				if(errors != null)
				{
					errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("passwordUpperCaseLettersFailed"));
				}
				jeChybneHeslo = true;
			}
			if(countPocetMalychPismen < pocetMalychPismen)
			{
				//heslo ma maly pocet malych pismen
				Logger.error(Password.class,"heslo ma maly pocet malych pismen");
				if(errors != null)
				{
					errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("passwordLowerCaseLettersFailed"));
				}
				jeChybneHeslo = true;
			}
			if(countPocetZnakov < pocetZnakov)
			{
				//heslo ma maly pocet znakov
				Logger.error(Password.class,"heslo ma maly pocet znakov");
				if(errors != null)
				{
					errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("passwordCountOfSpecialSignsFailed"));
				}
				jeChybneHeslo = true;
			}
		}

		if(isLogonForm || (!isLogonForm && password.trim().compareToIgnoreCase(UserTools.PASS_UNCHANGED) == 0))
		{
			//skontroluj este expiraciu terajsieho hesla
			if(!isLogonForm || (isLogonForm && jeChybneHeslo == false && session != null && session.getAttribute(Constants.USER_KEY+"_changepassword") == null))
			{
				if(vyprsanieHesla > 0)
				{
					long lastDate = Adminlog.getLastDate(Adminlog.TYPE_USER_CHANGE_PASSWORD, userId);
					Calendar calExpiryDate = Calendar.getInstance();
					calExpiryDate.setTimeInMillis(lastDate);
					calExpiryDate.add(Calendar.DATE, vyprsanieHesla);
					//expirovalo nam heslo
					if(Tools.getNow() > calExpiryDate.getTimeInMillis())
					{
						Logger.error(Password.class,"expirovalo heslo "+Tools.formatDateTime(calExpiryDate.getTimeInMillis()));
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
