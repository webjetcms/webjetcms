package sk.iway.iwcm.tags;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.i18n.Prop;


/**
 *  Trieda pre vygenerovanie linky na /admin/scripts/combine.jsp kde je ako verzia uvedeny timestamp startu servera (kvoli efektivite) a aktualny jazyk
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@created      $Date: 2015/05/11 16:02:38 $
 */
@SuppressWarnings({"java:S1104", "java:S3008", "java:S1444"})
public final class CombineTag extends BodyTagSupport
{
	private static Date versionDateTime = new Date();

	private static final long serialVersionUID = -7352999434842740830L;

	public static String FILES_ADMIN_JQUERY_JS = "/admin/skins/webjet8/assets/global/plugins/jquery.min.js,"+
				"/admin/skins/webjet8/assets/global/plugins/jquery-migrate-3.3.0.min.js,"+
				"/admin/skins/webjet8/assets/global/plugins/jquery.browser.min.js,"+
				"/admin/skins/webjet8/assets/global/plugins/jquery-ui/jquery-ui.min.js,"+
				"/components/_common/javascript/ajax_support.js";

	public static String FILES_ADMIN_STANDARD_JS =
				"/admin/skins/webjet8/assets/global/plugins/bootstrap/js/popper.min.js,"+
				"/admin/skins/webjet8/assets/global/plugins/bootstrap/js/bootstrap.js,"+
				"/admin/skins/webjet8/assets/global/plugins/jquery-slimscroll/jquery.slimscroll.js,"+
				"/admin/skins/webjet8/assets/global/plugins/jquery.blockui.min.js,"+
				"/admin/skins/webjet8/assets/global/plugins/jquery.cokie.min.js,"+
				"/admin/skins/webjet8/assets/global/plugins/jstree/dist/jstree.js,"+
				"/admin/skins/webjet8/assets/global/plugins/datatables/datatables.js,"+
				"/admin/skins/webjet8/assets/global/plugins/jquery-multi-select/js/jquery.multi-select.js,"+
				"/admin/skins/webjet8/assets/global/plugins/bootstrap-select/bootstrap-select.js,"+
				"/admin/skins/webjet8/assets/global/plugins/bootstrap-datepicker/js/bootstrap-datepicker.js,"+
				"/admin/skins/webjet8/assets/global/plugins/bootstrap-datepicker/js/locales/bootstrap-datepicker.USERLANG.js,"+
				"/admin/skins/webjet8/assets/global/plugins/bootstrap-timepicker/js/bootstrap-timepicker.min.js,"+
				"/admin/skins/webjet8/assets/global/plugins/clockface/js/clockface.js,"+
				"/admin/skins/webjet8/assets/global/plugins/bootstrap-daterangepicker/moment.min.js,"+
				"/admin/skins/webjet8/assets/global/plugins/bootstrap-daterangepicker/daterangepicker.js,"+
				"/components/_common/minicolors/jquery.minicolors.min.js,"+
				"/admin/skins/webjet8/assets/global/plugins/bootstrap-datetimepicker/js/bootstrap-datetimepicker.min.js,"+
				"/admin/skins/webjet8/assets/global/scripts/metronic.js,"+
				"/admin/skins/webjet8/assets/admin/layout/scripts/layout.js,"+
				"/admin/skins/webjet8/assets/admin/pages/scripts/components-dropdowns.js";

	public static String FILES_ADMIN_STANDARD_CSS = "/admin/skins/webjet8/assets/global/plugins/tabler/css/tabler-icons.min.css,"+
				"/admin/skins/webjet8/assets/global/plugins/bootstrap/css/bootstrap.css,"+
				"/admin/skins/webjet8/assets/global/plugins/jstree/dist/themes/default/style.min.css,"+
				"/admin/skins/webjet8/assets/global/plugins/bootstrap-datepicker/css/datepicker3.css,"+
				"/components/_common/minicolors/jquery.minicolors.css,"+
				"/admin/skins/webjet8/assets/global/plugins/bootstrap-timepicker/css/bootstrap-timepicker.min.css,"+
				"/admin/skins/webjet8/assets/global/css/plugins.css,"+
				"/admin/skins/webjet8/assets/global/css/components.css,"+
				"/admin/skins/webjet8/assets/admin/pages/css/tasks.css,"+
				"/admin/skins/webjet8/assets/admin/layout/css/layout.css,"+
				"/admin/skins/webjet8/assets/admin/layout/css/themes/default.css,"+
				"/admin/skins/webjet8/assets/admin/layout/css/custom.css,"+
				"/admin/skins/webjet8/assets/global/plugins/jquery-multi-select/css/multi-select.css,"+
				"/admin/skins/webjet8/assets/global/plugins/datatables/datatables.css,"+
				"/admin/skins/webjet8/assets/global/css/custom.css,"+
				"/admin/skins/webjet8/assets/global/css/chosen-style.css,"+
				"/admin/skins/webjet8/assets/global/css/datatables-editor-style.css,"+
				"/admin/skins/webjet8/assets/global/css/new-webjet-style.css";

	public static String FILES_ADMIN_LOGON_JS =
				"/admin/skins/webjet8/assets/js/zxcvbn/core/zxcvbn-ts.js,"+
				"/admin/skins/webjet8/assets/js/zxcvbn/language-common/zxcvbn-ts.js,"+
				"/admin/skins/webjet8/assets/js/zxcvbn/language-en/zxcvbn-ts.js";


	public static String FILES_ADMIN_INLINE_JS = "/admin/skins/webjet8/ckeditor/dist/ckeditor.js," +
			"/admin/skins/webjet8/assets/global/plugins/jquery-ui/jquery-ui.js," +
			"/admin/webpages/page-builder/scripts/jquery.minicolors.min.js," +
			"/admin/webpages/page-builder/scripts/ninja-page-builder.js.jsp," +
			"/admin/webpages/page-builder/scripts/pagesupport.js";

	public static String FILES_ADMIN_INLINE_CSS = "/components/_common/admin/inline/inline.css," +
			"/admin/webpages/page-builder/style/style.css," +
			"/admin/webpages/page-builder/style/jquery.minicolors.css," +
			"/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/samples/contents.css," +
			"/admin/v9/dist/css/vendor-inline.style.css";

	//set oznacuje schovany zoznam suborov pre kombinovanie
	private String set = null;
	//typ kombinovaneho vystupu (js alebo css)
	private String type = null;

	private String combine = null;

	@Override
	public int doEndTag() throws JspException
	{
		HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

		TemplateDetails temp = (TemplateDetails)request.getAttribute("templateDetails");
		if (temp != null) {
			request.getSession().setAttribute("templateInstallName", temp.getTemplateInstallName());
		}

		String lng = getLng(pageContext, request);

		boolean combineEnabled = true;
		if ("false".equals(request.getParameter("combineEnabled")))
		{
			combineEnabled = false;
			request.getSession().setAttribute("combineEnabled", "false");
		}
		if ("true".equals(request.getParameter("combineEnabled")))
		{
			combineEnabled = true;
			request.getSession().removeAttribute("combineEnabled");
		}
		if (request.getSession().getAttribute("combineEnabled")!=null) combineEnabled = false;

		//moznost nastavit len pre jeden request
		if ("false".equals(request.getParameter("combineEnabledRequest"))) combineEnabled = false;
		if ("true".equals(request.getParameter("combineEnabledRequest"))) combineEnabled = true;
		if ("false".equals(getCombine())) combineEnabled = false;

		try
		{
			StringBuilder tag = new StringBuilder();
			long v = getVersion();

			String filesToCombine = getSet();

			if (Tools.isEmpty(filesToCombine))
			{
				BodyContent bc = getBodyContent();
				String body = bc.getString();
				if (Tools.isNotEmpty(body))
				{
					filesToCombine = body;
				}
			}

			filesToCombine = removeCrLf(filesToCombine);

			filesToCombine = Tools.replace(filesToCombine, "USERLANG", Tools.replace(lng, "cz", "cs"));

			String baseCss = (String)request.getAttribute("base_css_link_nocombine");
			String cssLink = (String)request.getAttribute("css_link_nocombine");

			if (Tools.isNotEmpty(baseCss) || Tools.isNotEmpty(cssLink))
			{
				filesToCombine = Tools.replace(filesToCombine, "base_css_link", removeCrLf(baseCss));
				filesToCombine = Tools.replace(filesToCombine, "css_link", removeCrLf(cssLink));
			}

			if (combineEnabled)
			{


				if (filesToCombine.startsWith("admin"))
				{
					//for admin always use lng from login session not page context
					lng = (String)Tools.sessionGetAttribute(request.getSession(), Prop.SESSION_I18N_PROP_LNG);
					//zobrazenie v admin casti
					if ("css".equals(getType()))
					{
						tag.append("<link href=\"/admin/scripts/combine.jsp?t=css&amp;set=").append(filesToCombine).append("&amp;v=").append(v).append("&amp;lng=").append(lng).append("\" rel=\"stylesheet\" type=\"text/css\"/>");
					}
					else
					{
						//MBO: ak vlozi jQuery tak to setne aj do req aby sa uz znova nedal vlozit cez Tool.insertJquery
						if (filesToCombine!=null && filesToCombine.startsWith("adminJqueryJs"))
						{
							Tools.insertJQuery(request);
						}
						tag.append("<script src=\"/admin/scripts/combine.jsp?t=js&amp;set=").append(filesToCombine).append("&amp;v=").append(v).append("&amp;lng=").append(lng).append("\" type=\"text/javascript\"></script>");
					}
				}
				else
				{
					//zobrazenie na beznej web stranke
					if ("css".equals(getType()))
					{
						if ("tempCss".equals(filesToCombine))
						{
							if (Tools.isNotEmpty(baseCss) || Tools.isNotEmpty(cssLink))
							{
								filesToCombine = baseCss;
								if (Tools.isEmpty(filesToCombine)) filesToCombine = cssLink;
								else if (Tools.isNotEmpty(cssLink)) filesToCombine = filesToCombine + ","+cssLink;
							}
						}

						tag.append("<link href=\"/components/_common/combine.jsp?t=css&amp;f=").append(filesToCombine).append("&amp;v=").append(v).append("&amp;lng=").append(lng).append("\" rel=\"stylesheet\" type=\"text/css\"/>");
					}
					else
					{
						tag.append("<script src=\"/components/_common/combine.jsp?t=js&amp;f=").append(filesToCombine).append("&amp;v=").append(v).append("&amp;lng=").append(lng).append("\" type=\"text/javascript\"></script>");
					}
				}
			}
			else
			{
				String[] files = Tools.getTokens( Tools.replace(getFiles(filesToCombine), "USERLANG", Tools.replace(lng, "cz", "cs")), ",\n");
				for (String file : files)
				{
					if ("css".equals(getType()))
					{
						tag.append("<link href=\"").append(file).append("?v=").append(v).append("&amp;lng=").append(lng).append("\" rel=\"stylesheet\" type=\"text/css\"/>\n");
					}
					else
					{
						tag.append("<script src=\"").append(file).append("?v=").append(v).append("&amp;lng=").append(lng).append("\" type=\"text/javascript\"></script>\n");
					}
				}
			}

			pageContext.getOut().write(tag.toString());
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return(EVAL_PAGE);
	}

	private String removeCrLf(String set)
	{
		String newSet = Tools.replace(set, "\n", ",");
		newSet = Tools.replace(newSet, "\r", "");
		newSet = Tools.replace(newSet, " ", "");
		newSet = Tools.replace(newSet, "\t", "");
		newSet = Tools.replace(newSet, ",,", ",");
		//ked nie je pagefunctions na konci a je to vlozene cez EL tak sa spojja dve dokopy (odstrani sa enter) a vtedy sa to posaha
		newSet = Tools.replace(newSet, "page_functions.js.jsp/", "page_functions.js.jsp,/");
		return newSet;
	}

	public static String getFiles(String set)
	{
		String files = set;

		if ("adminJqueryJs".equals(set))
		{
			files = CombineTag.FILES_ADMIN_JQUERY_JS;
		}
		else if ("adminJqueryJs2".equals(set))
		{
			files = CombineTag.FILES_ADMIN_JQUERY_JS;
		}
		else if ("adminStandardJs".equals(set))
		{
			files = CombineTag.FILES_ADMIN_STANDARD_JS;
		}
		else if ("adminLogonJs".equals(set))
		{
			files = CombineTag.FILES_ADMIN_LOGON_JS;
		}
		else if ("adminStandardCss".equals(set))
		{
			files = CombineTag.FILES_ADMIN_STANDARD_CSS;
		}
		else if ("adminStandardCssWj9".equals(set))
		{
			files = CombineTag.FILES_ADMIN_STANDARD_CSS;
			//files = Tools.replace(files, "/admin/skins/webjet8/assets/global/plugins/font-awesome/css/font-awesome.min.css,", "/admin/skins/webjet8/assets/global/plugins/font-awesome/css/fontawesome5.min.css,");
		}
		else if ("adminInlineJs".equals(set))
		{
			files = CombineTag.FILES_ADMIN_INLINE_JS;
		}
		else if ("adminInlineCss".equals(set))
		{
			files = CombineTag.FILES_ADMIN_INLINE_CSS;
		}
		else
		{
			String constValue = Constants.getString("combine-"+set);
			if (Tools.isNotEmpty(constValue)) files = constValue;
		}

		return files;
	}


	@Override
	public void release()
	{
		super.release();
		this.set = null;
		this.type = null;
		this.combine = null;
	}

	public String getSet()
	{
		return set;
	}

	public void setSet(String set)
	{
		this.set = set;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getCombine() {
		return combine;
	}

	public void setCombine(String combine) {
		this.combine = combine;
	}

	/**
	 * Vrati jazyk aktualne prihlaseneho usera, aby sa spravne nacitali cachovane subory (sucast parametra)
	 * @param pageContext
	 * @param request
	 * @return
	 */
	public static String getLng(PageContext pageContext, HttpServletRequest request)
	{
		String lng = Constants.getString("defaultLanguage");
		if (pageContext.getAttribute("lng")!=null)
		{
			lng = (String)pageContext.getAttribute("lng");
		}
		else if (request.getAttribute("PageLng")!=null)
		{
			lng = (String)request.getAttribute("PageLng");
		}
		else
		{
			lng = (String)request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG);
			if (lng == null)
			{
				lng = PageLng.getUserLng(request); // sk.iway.iwcm.Constants.getString("defaultLanguage");
			}
		}
		if (Tools.isEmpty(lng)) lng = "sk";
		return lng;
	}

	/**
	 * Vrati verziu aktualnych suborov, aby sa spravne cachovali subory
	 * @return
	 */
	public static long getVersion()
	{
		return versionDateTime.getTime();
	}

	public static void setVersion(long version)
	{
		versionDateTime = new Date(version);
	}

	public static void resetVersion()
	{
		versionDateTime = new Date();
	}
}
