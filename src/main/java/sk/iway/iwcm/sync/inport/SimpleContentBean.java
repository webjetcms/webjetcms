package sk.iway.iwcm.sync.inport;

/**
 * Zobrazenie importu beanu.
 * Spolocna funkcionalita: cislo beanu, vzdialeny bean, lokalny bean.
 * V odvodenej triede doplnime funkcie specificke pre konkretny bean,
 * s moznostou zmenit strategiu pre "isSelected".
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 28.6.2012 12:09:24
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public abstract class SimpleContentBean<Item>
{

	protected final int number;
	protected final Item remoteItem;
	protected final Item localItem;

	public SimpleContentBean(int number, Item remoteItem, Item localItem)
	{
		this.number = number;
		this.remoteItem = remoteItem;
		this.localItem = localItem;
	}

	/**
	 * Cislo do HTML stranky, podla ktoreho rozozname, ktore beany chce pouzivatel importovat.
	 * 
	 * @return
	 */
	public int getNumber()
	{
		return this.number;
	}

	/**
	 * Ci existuje dany bean na lokalnom webjete.
	 * 
	 * @return
	 */
	public boolean isLocal()
	{
		return null != localItem;
	}

	/**
	 * Ci pouzivatelovi odporucame importovat tento bean.
	 * Default riesenie je importovat prave vtedy, ak lokalne neexistuje.
	 * Odvodene triedy mozu toto spravanie nahradit niecim specifickym pre dany typ beanu.
	 * 
	 * @return
	 */
	public boolean isSelected()
	{
		return !isLocal();
	}

}
