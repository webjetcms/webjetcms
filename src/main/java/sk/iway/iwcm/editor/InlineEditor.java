package sk.iway.iwcm.editor;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.tags.WriteTag;
import sk.iway.iwcm.users.UsersDB;


/**
 *  InlineEditor.java - ukladanie JSON objektu z inline editacie
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2016
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 4.10.2016 16:00:14
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class InlineEditor
{
	private static final String NUMBER_ATTRIBUTE = "editor.inline.idSequenceNumber";

	private static final String[] EDITABLE_FIELDS = { "title", "htmlHead", "htmlData", "perexImage", "virtualPath", "externalLink", "priceWithVat" };

	private String json;
	private HttpServletRequest request;

	private StringBuilder errorMessage = new StringBuilder();

	public InlineEditor(String json, HttpServletRequest request)
	{
		this.json = json;
		this.request = request;
	}

	public void save()
	{
		Identity user = UsersDB.getCurrentUser(request);

		int formDocId = Tools.getIntValue(request.getParameter("docId"), -1);
		int groupId = Tools.getIntValue(request.getParameter("groupId"), -1);

		try
		{
			JSONObject obj = new JSONObject(getJson());
			JSONArray editable  = obj.getJSONArray("editable");
			for (int i = 0; i < editable.length(); i++)
			{
				JSONObject item = editable.getJSONObject(i);
				String data = item.getString("data");
				String wjApp = item.getString("wjApp");
				int wjAppKey = item.getInt("wjAppKey");
				String field = item.getString("wjAppField");

				if (EditorDB.isPageEditable(user, wjAppKey)==false) continue;

				if ("newsInline".equals(wjApp) || "pageBuilder".equals(wjApp))
				{
					EditorForm ef = EditorDB.getEditorForm(request, wjAppKey, -1, groupId);

					if ("perexPre".equals(field)) ef.setHtmlData(data);
					else if ("title".equals(field)) ef.setTitle(data);
					else
					{
						ef.setData(data);

						if (wjAppKey == formDocId)
						{
							saveOtherData(ef);
						}
					}

					ef.setPublish("1");
					ef.setAuthorId(user.getUserId());
					int historyId = EditorDB.saveEditorForm(ef, request);
					if(historyId > 0)
					{
						Logger.debug(InlineEditor.class, "ok");
					}
					else
					{
						errorMessage.append("Error saving doc ").append(wjAppKey).append("\n");
					}
					EditorDB.cleanSessionData(request);
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			errorMessage.append("Error parsing JSON object").append("\n");
		}

	}

	/**
	 * Ulozime parametre hlavneho formu
	 * @param ef
	 */
	private void saveOtherData(EditorForm ef)
	{
		for (String name : EDITABLE_FIELDS)
		{
			saveField(name, ef);
		}
		String[] fields = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"};
		for (String pismeno : fields)
		{
			saveField("field"+pismeno, ef);
		}

		if ("cloud".equals(Constants.getInstallName()) && InitServlet.isTypeCloud())
		{
			Logger.debug(InlineEditor.class, "som cloud, nastavuje pass protected, value="+request.getParameter("passwordProtected")+";");
			if ("1".equals(request.getParameter("passwordProtected")))
			{
				ef.setPasswordProtectedString("1");
			}
			else
			{
				ef.setPasswordProtectedString("");
			}
		}
	}

	private void saveField(String name, EditorForm ef)
	{
		String paramValue = request.getParameter(name);
		if (paramValue != null)
		{
			try
			{
				Logger.debug(InlineEditor.class, "saveOtherData, name="+name+" value="+paramValue);
				BeanUtils.setProperty(ef, name, paramValue);
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}

	public static String getEditAttrs(HttpServletRequest request, DocDetails doc)
	{
		return getEditAttributes(request, doc, getEditingMode(request).name(), null, false);
	}

	public static String getEditAttrs(HttpServletRequest request, DocDetails doc, String field, boolean generateId)
	{
		return getEditAttributes(request, doc, getEditingMode(request).name(), field, generateId);
	}

	private static String getEditAttributes(HttpServletRequest request, DocDetails doc, String appName, String field, boolean generateId)
	{
		StringBuilder atrs = new StringBuilder();

		Identity user = UsersDB.getCurrentUser(request);
		if (user != null && InlineEditor.isInlineEditingEnabled(request))
		{
			atrs.append(" data-wjapp='").append(appName).append("' data-wjappkey='").append(doc.getDocId()).append("'");
			if (Tools.isNotEmpty(field))
			{
				atrs.append(" data-wjappfield='").append(field).append("'");
			}
			if (generateId)
			{
				atrs.append(" id='wjInline").append(getUniqueIdAttribute(request)).append("'");
			}
		}

		return atrs.toString();
	}

	private static int getUniqueIdAttribute(HttpServletRequest request)
	{
		Object numberAttribute = request.getAttribute(NUMBER_ATTRIBUTE);
		int number = ((null == numberAttribute) ? 0 : ((Integer) numberAttribute).intValue()) + 1;
		request.setAttribute(NUMBER_ATTRIBUTE, Integer.valueOf(number));

		return number;
	}

	public String getJson()
	{
		return json;
	}

	public void setJson(String json)
	{
		this.json = json;
	}

	public String getErrorMessage()
	{
		return errorMessage.toString();
	}

	public static boolean isInlineEditingEnabled(HttpServletRequest request)
	{
		//ak su zapnute heatmapy nezobrazime InlineEditor
		boolean isHeatmapEnabled = request.getSession().getAttribute("heatMapEnabled") != null && (boolean) request.getSession().getAttribute("heatMapEnabled") == true;
		//ak sa jedan o preview - preview.do (PreviewAction) - nezobrazime InlineEditor
		boolean isPreview = request.getAttribute("isPreview") != null && (boolean) request.getAttribute("isPreview") == true;

		boolean inlineEditingEnabled = false; //57629 - not enabled anymore Constants.getBoolean("inlineEditingEnabled");

		if ("false".equals(request.getAttribute("isInlineEditing"))) return false;

		//DEPRECATED - allow temporary force editing
		if ("true".equals(request.getAttribute("isInlineEditing"))) inlineEditingEnabled = true;

		//ak sa jedna o priamu editaciu v editore stranok
		if ("true".equals(request.getParameter("inlineEditorAdmin"))) {
			Identity user = UsersDB.getCurrentUser(request);
			if (user != null && user.isAdmin()) {
				inlineEditingEnabled = true;
				isHeatmapEnabled = false;
			}
		}

		return (inlineEditingEnabled
					&& request.getHeader("dmail")==null
					&& request.getParameter(WriteTag.NO_WJTOOLBAR)==null
					&& request.getParameter("isDmail")==null
					&& !isHeatmapEnabled
					&& request.getAttribute("disableInlineEditing")==null
					&& !isPreview);
	}

	public enum EditingMode { newsInline, pageBuilder, gridEditor }; //NOSONAR
	private static final String EDITING_MODE_KEY = "sk.iway.iwcm.editor.editingMode";

    /**
     * Nastavi rezim inline editacie
     * @param mode
     * @param request
     */
	public static void setEditingMode(EditingMode mode, HttpServletRequest request)
    {
        request.setAttribute(EDITING_MODE_KEY, mode);
    }

    public static EditingMode getEditingMode(HttpServletRequest request)
    {
        EditingMode mode = (EditingMode)request.getAttribute(EDITING_MODE_KEY);
        if (mode == null)
        {
            mode = EditingMode.newsInline;

            TemplatesGroupBean tempGroup = (TemplatesGroupBean)request.getAttribute("templatesGroupDetails");
            if (tempGroup!=null)
            {
                if ("pageBuilder".equals(tempGroup.getInlineEditingMode())) mode = EditingMode.pageBuilder;
                else if ("gridEditor".equals(tempGroup.getInlineEditingMode())) mode = EditingMode.gridEditor;
            }

            TemplateDetails temp = (TemplateDetails)request.getAttribute("templateDetails");
            if (temp != null)
            {
                if (temp.getTempName().contains("PageBuilder") || temp.getAfterBodyData().contains("PageBuilder"))
                {
                    mode = EditingMode.pageBuilder;
                }
            }
        }
        return mode;
    }
}
