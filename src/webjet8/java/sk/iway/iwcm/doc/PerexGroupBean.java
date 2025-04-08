package sk.iway.iwcm.doc;

import java.util.StringTokenizer;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
;

/**
 *  Vlastnosti perex skupiny
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      $Date: 2007/09/07 13:39:29 $
 *@modified     $Date: 2007/09/07 13:39:29 $
 */
public class PerexGroupBean
{
   private int perexGroupId;
   private String perexGroupName;
   private String[] relatedPages;

   /**
    * V akych skupinach je mozne perex skupinu pouzit
    */
   private String availableGroups;

   /**
     * Get perex group name by language of current webpage
     * @return
     */
    public String getPerexGroupName() {
        String lng = null;

        //Get value by actual language
        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        if(rb != null) lng = rb.getLng();

        return getPerexGroupName(lng);
    }

    /**
     * Get perex group name by language
     * @param lng
     * @return
     */
    public String getPerexGroupName(String lng) {
        String name = getPerexGroupNameInternal(lng);
        //Get value by REQUIRED perexGroupName
        if(Tools.isEmpty(name)) name = perexGroupName;

        return name;
    }

    private String getPerexGroupNameInternal(String lng) {
        if (Tools.isEmpty(lng)) return "";
        switch(lng) {
            case "sk": return perexGroupNameSk;
            case "cz": return perexGroupNameCz;
            case "en": return perexGroupNameEn;
            case "de": return perexGroupNameDe;
            case "pl": return perexGroupNamePl;
            case "ru": return perexGroupNameRu;
            case "hu": return perexGroupNameHu;
            case "cho": return perexGroupNameCho;
            case "esp": return perexGroupNameEsp;
            default: return "";
        }
    }

	public String getPerexGroupNameId()
	{
		if (Constants.getBoolean("perexGroupIncludeId")==false) return getPerexGroupName();

		return perexGroupName+" (id:"+perexGroupId+")";
	}

	/**
	 * @param perexGroupName The perexGroupName to set.
	 */
	public void setPerexGroupName(String perexGroupName)
	{
		this.perexGroupName = perexGroupName;
	}

	/**
	 * @return Returns the relatedPages.
	 */
	public String[] getRelatedPages()
	{
		return relatedPages;
	}

	/**
	 * @param pages The relatedPages to set.
	 */
	public void setRelatedPages(String pages)
	{
		int size;
		StringTokenizer st;
		int index = 0;
		String[] relatedPages = new String[0];

		try
		{

			if (Tools.isNotEmpty(pages))
			{
				st = new StringTokenizer(pages, ",");
				size = st.countTokens();
				if (size > 0)
				{
					//Logger.println(this,"-------\nRelatedPages: "+pages);
					relatedPages = new String[size];
					while (st.hasMoreTokens())
					{
						if ( index < size)
						{
							relatedPages[index] = st.nextToken().trim();
							//Logger.println(this,relatedPages[index]);
							index++;
						}
					}
				}
			}

			//relatedPages = RelatedPagesDB.getTokens(pages, ",");
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		this.relatedPages = relatedPages;
	}

	/**
	 * @return Returns the perexGroupId.
	 */
	public int getPerexGroupId()
	{
		return perexGroupId;
	}
	/**
	 * @param perexGroupId The perexGroupId to set.
	 */
	public void setPerexGroupId(int perexGroupId)
	{
		this.perexGroupId = perexGroupId;
	}

	public String getAvailableGroups()
	{
		return availableGroups;
	}

	public int[] getAvailableGroupsInt()
	{
		return Tools.getTokensInt(availableGroups, ",");
	}

	public void setAvailableGroups(String availableGroups)
	{
		this.availableGroups = availableGroups;
	}

	private String perexGroupNameSk;
	private String perexGroupNameCz;
	private String perexGroupNameEn;
	private String perexGroupNameDe;
	private String perexGroupNamePl;
	private String perexGroupNameRu;
	private String perexGroupNameHu;
	private String perexGroupNameCho;
	private String perexGroupNameEsp;

	public String getPerexGroupNameSk() {
		return perexGroupNameSk;
	}

	public void setPerexGroupNameSk(String perexGroupNameSk) {
		this.perexGroupNameSk = perexGroupNameSk;
	}

	public String getPerexGroupNameCz() {
		return perexGroupNameCz;
	}

	public void setPerexGroupNameCz(String perexGroupNameCz) {
		this.perexGroupNameCz = perexGroupNameCz;
	}

	public String getPerexGroupNameEn() {
		return perexGroupNameEn;
	}

	public void setPerexGroupNameEn(String perexGroupNameEn) {
		this.perexGroupNameEn = perexGroupNameEn;
	}

	public String getPerexGroupNameDe() {
		return perexGroupNameDe;
	}

	public void setPerexGroupNameDe(String perexGroupNameDe) {
		this.perexGroupNameDe = perexGroupNameDe;
	}

	public String getPerexGroupNamePl() {
		return perexGroupNamePl;
	}

	public void setPerexGroupNamePl(String perexGroupNamePl) {
		this.perexGroupNamePl = perexGroupNamePl;
	}

	public String getPerexGroupNameRu() {
		return perexGroupNameRu;
	}

	public void setPerexGroupNameRu(String perexGroupNameRu) {
		this.perexGroupNameRu = perexGroupNameRu;
	}

	public String getPerexGroupNameHu() {
		return perexGroupNameHu;
	}

	public void setPerexGroupNameHu(String perexGroupNameHu) {
		this.perexGroupNameHu = perexGroupNameHu;
	}

	public String getPerexGroupNameCho() {
		return perexGroupNameCho;
	}

	public void setPerexGroupNameCho(String perexGroupNameCho) {
		this.perexGroupNameCho = perexGroupNameCho;
	}

	public String getPerexGroupNameEsp() {
		return perexGroupNameEsp;
	}

	public void setPerexGroupNameEsp(String perexGroupNameEsp) {
		this.perexGroupNameEsp = perexGroupNameEsp;
	}

	private String fieldA;
	private String fieldB;
	private String fieldC;
	private String fieldD;
	private String fieldE;
	private String fieldF;

	private String notNull(String field) {
		if (field == null) return "";
		return field;
	}

	public String getFieldA() {
		return notNull(fieldA);
	}

	public void setFieldA(String fieldA) {
		this.fieldA = fieldA;
	}

	public String getFieldB() {
		return notNull(fieldB);
	}

	public void setFieldB(String fieldB) {
		this.fieldB = fieldB;
	}

	public String getFieldC() {
		return notNull(fieldC);
	}

	public void setFieldC(String fieldC) {
		this.fieldC = fieldC;
	}

	public String getFieldD() {
		return notNull(fieldD);
	}

	public void setFieldD(String fieldD) {
		this.fieldD = fieldD;
	}

	public String getFieldE() {
		return notNull(fieldE);
	}

	public void setFieldE(String fieldE) {
		this.fieldE = fieldE;
	}

	public String getFieldF() {
		return notNull(fieldF);
	}

	public void setFieldF(String fieldF) {
		this.fieldF = fieldF;
	}

}
