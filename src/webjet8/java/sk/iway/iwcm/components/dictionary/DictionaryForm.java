package sk.iway.iwcm.components.dictionary;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.dictionary.model.DictionaryBean;
import sk.iway.iwcm.i18n.Prop;

/**
 *  DictionaryForm.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 10.7.2005 19:02:12
 *@modified     $Date: 2005/08/03 06:45:00 $
 */
public class DictionaryForm extends ActionForm
{
	private static final long serialVersionUID = 1L;

	//atributy
	private DictionaryBean dictionary;
	private int dictionaryId;

	public DictionaryForm()
	{
		//empty constructor
		dictionary = new DictionaryBean();
	}
	/**
	 * Konstruktor
	 * @param banner
	 */
	public DictionaryForm(DictionaryBean dictionary)
	{
		this.dictionary = dictionary;
		dictionaryId = dictionary.getDictionaryId();
	}

	/**
	 * Resetnutie formularu - pri update sa znova ziska BannerBean a potom sa mu zmenia hodnoty
	 */
	@Override
	public void reset(ActionMapping mapping, HttpServletRequest request)
	{
		//ziskanie objektu, toto tu musi byt ako prve!
		int id = Tools.getIntValue(request.getParameter("dictionaryId"), -1);
		if (id > 0)
		{
			dictionary = DictionaryDB.getDictionary(id);
		}
	}


	/**
	 * Validacia formularu
	 * @param prop - i18n properties
	 * @return
	 */
	public String _validate(Prop prop)
	{
		String ret = "";

		return (ret);
	}


	/**
	 * @return Returns the dictionary.
	 */
	public DictionaryBean getDictionary()
	{
		return dictionary;
	}
	/**
	 * @param dictionary The dictionary to set.
	 */
	public void setDictionary(DictionaryBean dictionary)
	{
		this.dictionary = dictionary;
	}
	/**
	 * @return Returns the dictionaryId.
	 */
	public int getDictionaryId()
	{
		return dictionaryId;
	}
	/**
	 * @param dictionaryId The dictionaryId to set.
	 */
	public void setDictionaryId(int dictionaryId)
	{
		this.dictionaryId = dictionaryId;
	}
}
