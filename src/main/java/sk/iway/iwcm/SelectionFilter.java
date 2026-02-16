package sk.iway.iwcm;

/**
 *  SelectionFilter.java
 *  
 *  Command pattern interface, ktory riesi situacie, ked sa objavi mnoho
 *  metod typu: 
 *  vrat mi DocDetails, ktore maju zadane tempId
 *  vrat mi DocDetails, ktore su vytvorene danym uzivatelom
 *  vrat mi DocDetails, ktore nie su defaultnymi strankami svojho adresara
 *  ...
 *  Vytvorime si vlastnu triedu, ktora zadefinuje, ake podmienky musi splnat objekt, aby 
 *  bol vybrany. A cielova trieda musi definovat tento filter ako jeden zo svojich parametrov
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 11.8.2008 11:09:45
 *@modified     $Date: 2008/08/11 11:41:00 $
 */
public interface SelectionFilter<T>
{	
	public boolean fullfilsConditions(T candidate);
	
}
