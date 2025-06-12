package sk.iway.iwcm.tags;

import net.sourceforge.stripes.exception.SourcePageNotFoundException;
import org.apache.commons.lang.time.StopWatch;
import org.apache.struts.Globals;
import org.apache.struts.util.ResponseUtils;
import org.springframework.context.ApplicationContext;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.ninja.Amp;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.InlineEditor;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.system.WJResponseWrapper;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.monitoring.ExecutionTimeMonitor;
import sk.iway.iwcm.system.monitoring.MemoryMeasurement;
import sk.iway.iwcm.system.spring.components.SpringContext;
import sk.iway.iwcm.system.spring.webjet_component.WebjetComponentParser;
import sk.iway.iwcm.users.UsersDB;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  vypise string ulozeny v request objekte (vyhodne ked sa to setne v nejakej
 *  Action triede)
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.45 $
 *@created      $Date: 2010/01/20 10:13:50 $
 */
public class WriteTag extends BodyTagSupport
{
	private static final long serialVersionUID = -5549714303388477615L;

	public static final String SKIP_BODY = "writeTag.skipBody";

	// Name of request attribute - required
	private String name = null;

	private static final String INCLUDE_START = "!INCLUDE(";
	private static final String INCLUDE_END = ")!";

	public static final String PAGE_PARAMS = "includePageParams";

	private static final String AFTER_WRITE_INCLUDE_LIST = "sk.iway.iwcm.tags.afterWriteIncludeFile";

	private static final String INLINE_EDITING_BUTTONS_KEY = "inlineEditingButtons";
	private static final String INLINE_EDITING_BUTTONS_TOP_KEY = "inlineEditingButtonsTop";
	private static final String INLINE_EDITING_MODULE_PROPS_DISABLE_KEY = "inlineEditingModulePropsDisabled";
	private static final String INLINE_EDITING_MODULE_PROPS_TEXT_KEY = "inlineEditingModulePropsTextKey";
	private static final String INLINE_EDITING_MODULE_PROPS_ICON = "inlineEditingModulePropsIcon";
	public static final String INLINE_EDITING_PLACEHOLDER = "<div class='inlineEditingToolbarPlaceholder'></div>";

	private static final String INLINE_EDITING_DISABLE_DELETE_BUTTON = "inlineEditingDisableDeleteButton";


	@Override
	public void release()
	{
		super.release();
		name = null;
	}

	/**
	 *  Description of the Method
	 *
	 *@return                   Description of the Return Value
	 *@exception  JspException  Description of the Exception
	 */
	@Override
	public final int doEndTag() throws JspException
	{
		try
		{
			HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

			Object value = null;
			if (Tools.isNotEmpty(name))
			{
				value = request.getAttribute(name);
			}
			else if (getBodyContent()!=null)
			{
				value = getBodyContent().getString();
			}

			request.setAttribute("writeTagName", name);
			pageContext.setAttribute("writeTagName", name);

			String lng = PageLng.getUserLng(request);
			pageContext.setAttribute("lng", lng);

			int docId = Tools.getDocId(request);
			boolean inlineEditingToolbarAppended = false;
			if ("doc_data".equals(name))
			{
				//WebJET7 vyzaduje jQuery
				String tempName = (String)request.getAttribute("doc_temp_name");
				//ochrana pre RSS aby sa negenerovalo
				if (tempName == null || (tempName.toLowerCase().indexOf("rss")==-1 && tempName.toLowerCase().indexOf("nojquery")==-1))
				{
					String jQueryHtml = Tools.insertJQuery(request);
					if (Tools.isNotEmpty(jQueryHtml))
					{
						pageContext.getOut().write(jQueryHtml);
					}
				}

				if (InlineEditor.isInlineEditingEnabled(request))
				{
					Identity user = UsersDB.getCurrentUser(request);
					if (isInlinePageEditable(user, docId, request))
					{
						if ("true".equals(request.getParameter("inlineEditorAdmin"))) {
							request.setAttribute("writeTag.inlineEditingScriptInserted", "1");
							pageContext.include(WriteTag.getCustomPageAdminNotNull("/admin/inline/inline_page_toolbar_v9.jsp", request));
							inlineEditingToolbarAppended = true;
							if (request.getAttribute(SKIP_BODY)!=null)
							{
								value = null;
							}
						} else {
							appendInlineEditingScript(pageContext, request);
							pageContext.include(WriteTag.getCustomPageAdminNotNull("/admin/inline/inline_page_toolbar.jsp", request));
							inlineEditingToolbarAppended = true;
							if (request.getAttribute(SKIP_BODY)!=null)
							{
								value = null;
							}
						}
					}
				}
			}
			else if (InlineEditor.isInlineEditingEnabled(request))
			{
				String editableObjects = Constants.getString("inlineEditableObjects");
				if (Tools.isNotEmpty(name) && Tools.isNotEmpty(editableObjects) && editableObjects.indexOf(name)!=-1)
				{
					DocDetails doc = (DocDetails)request.getAttribute(name+"-docDetails");
					if (doc != null)
					{
						Identity user = UsersDB.getCurrentUser(request);
						if (isInlinePageEditable(user, docId, request) && isInlinePageEditable(user, doc.getDocId(), request))
						{
							String html = (String)request.getAttribute(name);
							value = "<div id='"+name+"Editor'"+InlineEditor.getEditAttrs(request, doc)+">"+html+"</div>";
						}
					}
				}
			}

			String afterWriteEndTag = null;
			if (inlineEditingToolbarAppended && "doc_data".equals(name))
			{
				DocDetails doc = (DocDetails)request.getAttribute("docDetails");
				if (doc != null)
				{
					pageContext.getOut().write("<div id='wjInline-docdata' "+InlineEditor.getEditAttrs(request, doc, "doc_data", false)+">");

					afterWriteEndTag = "</div>";
				}
			}

			if (value != null)
			{
				writeText(value.toString(), pageContext, name);
			}

			if (afterWriteEndTag != null)
			{
				pageContext.getOut().print(afterWriteEndTag);
			}

			if ("doc_data".equals(name) || "doc_header".equals(name))
			{
				try
				{
					boolean showToolbar = true;

					//skus ziskat zo sablony priznak, ze sa nema zobrazit
					String afterBody = (String)request.getAttribute("after_body");

					if (afterBody!=null && afterBody.indexOf("NO WJTOOLBAR")!=-1)
					{
						showToolbar = false;
					}

					if (request.getParameter("NO_WJTOOLBAR")!=null)
					{
						request.getSession().setAttribute("NO_WJTOOLBAR", "1");
					}

					if (request.getHeader("dmail")!=null || request.getAttribute("NO WJTOOLBAR")!=null || (request.getSession()!=null && request.getSession().getAttribute("NO_WJTOOLBAR")!=null) || request.getParameter("NO_WJTOOLBAR")!=null || request.getAttribute("isPreview")!=null)
					{
						showToolbar = false;
					}

					if (request.getSession().getAttribute("userWasInWebpages") == null)
					{
						//este nebol realne v admin casti v menu WebStranky (mozno sa prihlasil ako normalny user)
						showToolbar = false;
					}

					if (Constants.getBoolean("disableWebJETToolbar"))
					{
						showToolbar = false;
					}

					if ("false".equals(request.getParameter("NO_WJTOOLBAR")))
					{
						showToolbar = true;
						request.getSession().removeAttribute("NO_WJTOOLBAR");
					}

					if (inlineEditingToolbarAppended)
					{
						showToolbar = false;
					}

					if (showToolbar)
					{
						if ("doc_data".equals(name))
						{
							pageContext.include("/admin/page_toolbar.jsp");
						}
					}
				}
				catch (Exception e)
				{
					Logger.error(WriteTag.class, e);
				}

				if (request.getParameter("_editFormId")!=null)
				{
					pageContext.include("/components/form/admin_fill_form_data.jsp");
				}
			}

			//dt.diff("done: "+(String)value);
		}
		catch (JspException je)
		{
			Logger.error(WriteTag.class, "{"+Constants.getInstallName()+"} error: " + je.getMessage());
			Logger.error(WriteTag.class, je);
		}
		catch (Exception e)
		{
			Logger.error(WriteTag.class, "{"+Constants.getInstallName()+"} error: " + e.getMessage());
			Logger.error(WriteTag.class, e);
		}
		return EVAL_PAGE;
	}

	/**
	 *  Set the required tag attribute <b>name</b> .
	 *
	 *@param  newName  The new name value
	 */
	public final void setName(String newName)
	{
		name = newName;
	}

	/**
	 * @deprecated pouzi WriteTagToolsForCore.isFileExists((fileUrl));
	 */
	@Deprecated
	public static boolean isFileExists(String fileUrl)
	{
		return WriteTagToolsForCore.isFileExists((fileUrl));
	}

	/**
	 * Pre zadane URL JSP suboru najde jeho custom alternativu
	 * @param includeFileName
	 * @param request
	 * @return
	 *
	 * @deprecated pouzi WriteTagToolsForCore.getCustomPage(includeFileName,request);
	 */
	@Deprecated
	public static String getCustomPage(String includeFileName, HttpServletRequest request)
	{
		return WriteTagToolsForCore.getCustomPage(includeFileName,request);
	}

	/**
	 * Vrati custom cestu ku komponente, alebo null, ak nie je custom cesta
	 * @param includeFileName
	 * @param request
	 * @return
	 *
	 * @deprecated pouzi WriteTagToolsForCore.getCustomPageNull(includeFileName,request);
	 */
	@Deprecated
	public static String getCustomPageNull(String includeFileName, HttpServletRequest request)
	{
		return WriteTagToolsForCore.getCustomPageNull(includeFileName,request);

	}

	/**
	 * Vrati cestu k custom page pre Admin cast, alebo null, ak neexistuje ziadna
	 * @param pageURL
	 * @param request
	 * @return
	 *
	 * @deprecated pouzi WriteTagToolsForCore.getCustomPageAdmin(pageURL,request);
	 */
	@Deprecated
	public static String getCustomPageAdmin(String pageURL, HttpServletRequest request)
	{
		return WriteTagToolsForCore.getCustomPageAdmin(pageURL,request);
	}

	/**
	 * Vrati cestu k custom verzii stranky, ak neexistuje vrati povodnu pageURL
	 * @param pageURL
	 * @param request
	 * @return
	 */
	public static String getCustomPageAdminNotNull(String pageURL, HttpServletRequest request)
	{
		String customPath = WriteTagToolsForCore.getCustomPageAdmin(pageURL, request);
		if (customPath != null) return customPath;

		return pageURL;
	}

	public static void writeText(String text, PageContext pageContext, String name) throws Exception
	{
		DebugTimer dt = new DebugTimer("WriteTag");
		StringBuilder buff = new StringBuilder(text);

		if (Constants.getBoolean("editorEnableXHTML"))
		{
			pageContext.setAttribute(Globals.XHTML_KEY, "true", PageContext.PAGE_SCOPE);
		}

		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

		boolean writePerfStat = false;
		if ("true".equals(request.getParameter("_writePerfStat"))) writePerfStat = true;
		boolean disableCache = false;
		if ("true".equals(request.getParameter("_disableCache"))) disableCache = true;

		buff = WriteTagToolsForCore.fixXhtml(buff, request);
		buff = WriteTagToolsForCore.preventSpam(buff, request);
		buff = WriteTagToolsForCore.secureFormmail(buff, request);

		BrowserDetector browser = BrowserDetector.getInstance(request);
		if (browser.isAmp()) {
			Amp amp = new Amp(request);
			buff = new StringBuilder(amp.replaceInString(buff.toString()));
		}

		//Logger.println(this,"BUFF SIZE="+ response.getBufferSize());
		//Logger.println(this,"BUFF SIZE JSP="+ pageContext.getOut().getBufferSize());


		/*WJResponseWrapper respWrapper = new WJResponseWrapper(response);

      pageContext.getOut().write("ahoj");

      //pageContext.include("/components/sopk/meniny.jsp");
      request.getRequestDispatcher("/components/sopk/redir.jsp").include(request, respWrapper);

      if (respWrapper.redirectURL!=null)
      {
      	response.sendRedirect(respWrapper.redirectURL);
      	return;
      }

      pageContext.getOut().write(respWrapper.strWriter.getBuffer().toString());

      //response.sendRedirect("http://www.interway.sk");
      //response.sendRedirect("http://www.sme.sk");
      Logger.println(this,"redirected 5");
      if (1==1) return;*/

		HttpSession session = request.getSession();
		Identity user = null;
		try
		{
			//ziskaj meno lognuteho usera
			if (session.getAttribute(Constants.USER_KEY) != null)
			{
				user = (Identity) session.getAttribute(Constants.USER_KEY);
			}
		}
		catch (Exception ex)
		{
			Logger.error(WriteTag.class, ex);
		}

		StringBuilder content = new StringBuilder();
		Prop prop = Prop.getInstance(request);
		String lng = PageLng.getUserLng(request);

		try
		{
			//text = new String(text.getBytes(), "iso-8859-1");
			int docId = -1;
			try
			{
				if (Tools.isNotEmpty(request.getParameter("docid")))
				{
					docId = Integer.parseInt(Tools.getParameter(request, "docid"));
				}
				else
				{
					if (request.getAttribute("docid")!=null)
					{
						docId = Integer.parseInt((String)request.getAttribute("docid"));
					}
				}
			}
			catch (Exception ex)
			{
				Logger.error(WriteTag.class, ex);
			}
			buff = DocTools.updateCodes(user, buff, docId, request, pageContext.getServletContext());
		}
		catch (Exception ex2)
		{
			Logger.error(WriteTag.class, ex2);
		}

		//Logger.println(this,"----> QUERY WRITE TAG: " + request.getQueryString());

		Cache c = Cache.getInstance();

		//skus zistit ci tam je nejaky include
		if (buff.indexOf(INCLUDE_START)!=-1)
		{
			try
			{
				int startIndex = buff.indexOf(INCLUDE_START);
				int includeEndIndex;
				String includeFileName = "";
				String includeText = "";
				String pageParams;
				int commaIndex;
				int failsafe = 0;
				int cacheTime = 0;
				String cacheKey = null;
				while (startIndex != -1 && failsafe < 100)
				{
					failsafe++;
					try
					{
						//zapis zaciatocny text
						includeEndIndex = buff.indexOf(INCLUDE_END, startIndex);

						if (includeEndIndex < 0)
						{
							//nenasiel sa koniec
							content.append(buff.substring(0, startIndex+INCLUDE_START.length()));
							buff.delete(0,startIndex+INCLUDE_START.length());
							startIndex = buff.indexOf(INCLUDE_START);
							continue;
						}

						includeText = buff.substring(startIndex, includeEndIndex);
						cacheTime = 0;
						cacheKey = "writeTag_"+includeText;
						//aby sa dala spravit page dependent cache
						//lng - aby sa neprekryvali verzie v roznych jazykoch
						cacheKey = Tools.replace(cacheKey, "!DOC_ID!", request.getParameter("docid")) + " ;"+lng;


						StopWatch executionTimeStopWatch = new StopWatch();
						executionTimeStopWatch.start();
						MemoryMeasurement memoryConsumed = new MemoryMeasurement();

						try
						{
							if (disableCache==false)
							{
								int cacheTimePos = includeText.indexOf("cacheMinutes=");

								//news komponenta nemoze cachovat ak ma parameter page
								if (cacheTimePos!=-1 && includeText.indexOf("/news/news")!=-1 && (request.getParameter("page")!=null && "1".equals(request.getParameter("page"))==false))
								{
									cacheTimePos = -1;
								}

								if (cacheTimePos != -1)
								{
									StringTokenizer st = new StringTokenizer(includeText.substring(cacheTimePos+13), " ,)");
									if (st.hasMoreTokens())
									{
										cacheTime = Tools.getIntValue(Tools.replace(st.nextToken(), "&quot;", ""), 0);
									}

									//vybereme z cache, adminom ale zobrazujeme priamo (a dole spravime refresh cache)
									if (cacheTime > 0 && (user==null || user.isAdmin()==false || Constants.getBoolean("cacheStaticContentForAdmin")==true))
									{
										//skus ziskat z cache
										String htmlCode = (String)c.getObject(cacheKey);
										if (htmlCode == null)
										{
											//nastav do cache nieco, aby ostatne thready nespustili lavinu
											c.setObjectSeconds(cacheKey, "<!-- caching data, try reload -->", 120, false);
										}
										if (Tools.isNotEmpty(htmlCode))
										{
											Logger.debug(WriteTag.class, "Serving from cache: "+cacheKey);
											//vypiseme vysledok z cache
											if (startIndex > 0)
											{
												content.append(buff.substring(0, startIndex));
											}
											content.append(htmlCode);
											buff.delete(0,includeEndIndex + INCLUDE_END.length());
											startIndex = buff.indexOf(INCLUDE_START);

											if (writePerfStat)
											{
												long diff = dt.getDiff();
												long lastDiff = dt.getLastDiff();
												String logText = "PerfStat: " + diff + " ms (+"+lastDiff+") " + includeText+"\n";
												content.append('\n').append(logText);
												Logger.debug(WriteTag.class, logText);
											}

											executionTimeStopWatch.stop();
											ExecutionTimeMonitor.recordComponentExecutionFromCache(cacheKey, executionTimeStopWatch.getTime());
											continue;
										}

										request.setAttribute(BuffTag.IS_BUFF_TAG, "true");
									}
								}
							}
						}
						catch (Exception e)
						{
							Logger.error(WriteTag.class, e);
						}

						content.append(WriteTagToolsForCore.replaceWriteText(new StringBuilder(buff.substring(0, startIndex)), request));

						//ziskaj data

						includeFileName = buff.substring(startIndex + INCLUDE_START.length(), includeEndIndex);
						includeFileName = Tools.replace(includeFileName, "%20", " ");
						includeFileName = Tools.replace(includeFileName, "\n", "");
						includeFileName = Tools.replace(includeFileName, "\r", "");
						commaIndex = includeFileName.indexOf(',');
						if (commaIndex != -1)
						{
							pageParams = includeFileName.substring(commaIndex + 1);
							includeFileName =includeFileName.substring(0, commaIndex);
						}
						else
						{
							pageParams = "";
						}

						includeFileName = includeFileName.trim();
						pageParams = pageParams.trim();

						pageContext.getRequest().removeAttribute(PAGE_PARAMS);
						pageContext.getRequest().setAttribute(PAGE_PARAMS,  pageParams);
						//zisti ci ten include existuje
						includeFileName = WriteTagToolsForCore.getCustomPage(includeFileName, request);


						boolean canInclude = false;
						boolean isSpringComponent = false;
						ApplicationContext applicationContext = SpringContext.getApplicationContext();
						if (applicationContext.containsBean(includeFileName)) {
							canInclude = true;
							isSpringComponent = true;
						}
						else if(includeFileName.startsWith("/components") || includeFileName.startsWith("/apps") || includeFileName.startsWith("/templates"))
						{
							canInclude = true;
						}
						else if (includeFileName.startsWith("/maillist.do"))
						{
							canInclude = true;
						}
						else
						{
							Logger.debug(WriteTag.class, "attempt to include non component file: " + includeFileName + ", include denied");
						}

						boolean inPreviewMode = request.getAttribute("inPreviewMode")!=null;

						//Only if app can be included
						final String regexIncludeParam = "([^\";, ]*)";
						final String deviceParam = "device=";
						//In preview mode it does not matter what type of device we use (we want see all banners)

						/** Check if this app can be included in current device type **/
						if(canInclude && inPreviewMode==false && includeText.contains(deviceParam)) {

							Pattern pattern = Pattern.compile(deviceParam + regexIncludeParam);
							Matcher matcher = pattern.matcher(Tools.replace(includeText, "&quot;", ""));

							//Is deviceParam presented in include ? -> NO, then do nothing
							if(matcher.find()) {
								String devicesString = matcher.group(1);
								//Does deviceParam contain any value ? -> NO, then do nothing
								if(Tools.isNotEmpty(devicesString)) {
									//Devices values are separated by "+"
									StringTokenizer devices = new StringTokenizer(devicesString, "+");

									//So there is set device type limitation, set canInclude to false, until we verify that actual device is right type (supported device type)
									if(devices.hasMoreTokens()) canInclude = false;

									//Loop all suported device types and check if actual device has right type
									while(devices.hasMoreTokens()) {
										String device = Tools.getStringValue(devices.nextToken(), "");

										if("phone".equals(device) && browser.isPhone()) {
											canInclude = true;
											break;
										} else if("tablet".equals(device) && browser.isTablet()) {
											canInclude = true;
											break;
										} else if("pc".equals(device) && browser.isDesktop()) {
											canInclude = true;
											break;
										}
									}
								}
							}
						}

						if(canInclude && inPreviewMode==false && includeText.contains("showForLoggedUser=")) {
							Pattern pattern = Pattern.compile("showForLoggedUser=" + regexIncludeParam);
							Matcher matcher = pattern.matcher(Tools.replace(includeText, "&quot;", ""));
							if(matcher.find()) {
								String value = matcher.group(1);
								if ("onlyLogged".equals(value) && user == null)
								{
									canInclude = false;
								}
								else if ("onlyNotLogged".equals(value) && user != null)
								{
									canInclude = false;
								}
							}
						}

						buff.delete(0,includeEndIndex + INCLUDE_END.length());

						if(canInclude)
						{
							Logger.debug(WriteTag.class, "INCLUDING: "+includeFileName);
							//Logger.println(this,"includeFileName="+includeFileName);
							if (WriteTagToolsForCore.isFileExists(includeFileName) || includeFileName.endsWith(".do") || includeFileName.indexOf(".do?")!=-1 || includeFileName.endsWith(".action") || isSpringComponent)
							{
								preserveParametersSet(includeFileName, request);

								if (request.getAttribute("writeTagDisableCodeFix")==null && Constants.getBoolean("disableWJResponseWrapper")==false && (request.getAttribute(BuffTag.IS_BUFF_TAG)!=null || Constants.getBoolean("editorEnableXHTML")))
								{
									//respWrapper.setBufferSize(response.getBufferSize());
									//FakeHttpServletResponse respWrapper = new FakeHttpServletResponse(response);

									//inline toolbar append
									StringBuilder inlineEditingStart = null;
									boolean inlineEditingAppendEndDiv = false;
									request.removeAttribute(INLINE_EDITING_BUTTONS_KEY);
									//request.removeAttribute(INLINE_EDITING_BUTTONS_TOP_KEY); - toto je globalne pre cely request
									request.removeAttribute(INLINE_EDITING_MODULE_PROPS_DISABLE_KEY);
									request.removeAttribute(INLINE_EDITING_MODULE_PROPS_TEXT_KEY);
									request.removeAttribute(INLINE_EDITING_MODULE_PROPS_ICON);
									request.removeAttribute("isInlineEditing");
									int inlineDocId = -1;
									boolean isInlineEditing = false;
									if (Constants.getBoolean("inlineEditingEnabled"))
									{
										if (request.getHeader("dmail") == null && request.getParameter("NO_WJTOOLBAR") == null && request.getParameter("isDmail") == null && request.getAttribute("isPreview") == null)
										{
											if ("doc_data".equals(name) || request.getAttribute(name + "-docId=") != null)
											{
												if (user != null && user.isAdmin())
												{
													inlineDocId = DocTools.getRequestNameDocId(name, request);
													request.setAttribute("isInlineEditing", "true");
													isInlineEditing = true;
												}
											}
										}
									}

									StringBuilder htmlCode = null;
									if (applicationContext != null && applicationContext.containsBean(includeFileName)) {
										WebjetComponentParser webjetComponentParser = applicationContext.getBean("webjetComponentParser", WebjetComponentParser.class);
										if (webjetComponentParser != null) {
											htmlCode = new StringBuilder(webjetComponentParser.parse(request, response, text));
										}
									}

									if (htmlCode == null) {
										WJResponseWrapper respWrapper = new WJResponseWrapper(response,request);
										request.getRequestDispatcher(includeFileName).include(request, respWrapper);
										if (respWrapper.redirectURL != null) {
											response.sendRedirect(respWrapper.redirectURL);
											return;
										}

										htmlCode = new StringBuilder(respWrapper.strWriter.getBuffer().toString());
									}

									htmlCode = WriteTagToolsForCore.fixXhtml(htmlCode, request);
									htmlCode = WriteTagToolsForCore.preventSpam(htmlCode, request);
									htmlCode = WriteTagToolsForCore.secureFormmail(htmlCode, request);
									htmlCode = WriteTagToolsForCore.replaceWriteText(htmlCode, request);
									executionTimeStopWatch.stop();
									ExecutionTimeMonitor.recordComponentExecution(cacheKey, executionTimeStopWatch.getTime(), memoryConsumed.diff());

									if (isInlineEditing && isInlinePageEditable(user, inlineDocId, request))
									{
										inlineEditingStart = getInlineEditingStart(includeFileName, includeText, user, inlineDocId, request, prop);
									}

									if (inlineEditingStart!=null)
									{
										//ak nenastalo volanie hideInlineComponentEditButton
										if (isInlineComponentEditButton(request))
										{
											appendInlineEditingScript(pageContext, request);

											StringBuilder inlineEditingButtons = (StringBuilder)request.getAttribute(INLINE_EDITING_BUTTONS_KEY);
											if (inlineEditingButtons != null)
											{
												inlineEditingStart = Tools.replace(inlineEditingStart, "{inlineEditingButtons}", inlineEditingButtons.toString());
											}
											else
											{
												inlineEditingStart = Tools.replace(inlineEditingStart, "{inlineEditingButtons}", "");
											}

											if (htmlCode.indexOf(INLINE_EDITING_PLACEHOLDER)==-1)
											{
												content.append(inlineEditingStart);
												inlineEditingAppendEndDiv = true;
											}
											else
											{
												htmlCode = Tools.replace(htmlCode, INLINE_EDITING_PLACEHOLDER, inlineEditingStart.append("</div>").toString());
											}

										}
									}

									StringBuilder buttonsTop = (StringBuilder)request.getAttribute(INLINE_EDITING_BUTTONS_TOP_KEY);
									if (isInlineEditing && buttonsTop != null && buttonsTop.length()>0)
									{
										appendInlineEditingScript(pageContext, request);

										String buttonsTopString = buttonsTop.toString();
										if (buttonsTopString.indexOf("{inlineComponentEdit}")!=-1)
										{
											//ak sme toto nespravili nenaslo nam to nizsie getInlineEditorComponent
											request.removeAttribute(INLINE_EDITING_MODULE_PROPS_DISABLE_KEY);

											StringBuilder href = new StringBuilder();
											href.append("javascript:inlineComponentEdit('").append(includeFileName).append("', '").append(JSEscapeTag.jsEscape(pageParams)).append("', ");
											href.append(" '").append(JSEscapeTag.jsEscape(includeText)).append("', ");
											href.append(" '").append(JSEscapeTag.jsEscape(getInlineEditorComponent(includeFileName, request))).append("', ");
											href.append(inlineDocId);
											href.append(")");

											buttonsTopString = Tools.replace(buttonsTopString, "{inlineComponentEdit}", href.toString());
										}

										content.append("<div class=\"inlineComponentEditButtonsTop\">").append(buttonsTopString).append("</div>");

										request.removeAttribute(INLINE_EDITING_BUTTONS_TOP_KEY);
									}

									content.append(htmlCode);

									if (inlineEditingAppendEndDiv) content.append("</div>");

									if (cacheTime > 0)
									{
										Logger.debug(WriteTag.class, "Setting to cache: "+cacheKey);
										c.setObjectSeconds(cacheKey, htmlCode.toString(), cacheTime*60, true);
									}

									preserveParametersRemove(includeFileName, request);
								}
								else
								{
									pageContext.include(includeFileName);
								}
							}
							else
							{
								//subor neexistuje
								content.append(getErrorMessage(prop, "writetag.error_not_exists", includeFileName));
							}
						}
					}
					catch (SourcePageNotFoundException ex1) {
						//toto nas nezaujima, pravdepodobne to je len search bot
						Logger.error(WriteTag.class, "WRITE TAG INCLUDE ERROR: " + ex1.getMessage());
						content.append(getErrorMessage(prop, "writetag.error", includeFileName));
					}
					catch (Exception ex1)
					{
						Logger.error(WriteTag.class,"WRITE TAG INCLUDE ERROR: " + ex1.getMessage());

						StringWriter sw = new StringWriter();
						ex1.printStackTrace(new PrintWriter(sw));

						String stack = sw.toString();

						if (stack != null && stack.contains("Unabled to prepare ActionBean for JSP Usage")==false && stack.contains("_404_jsp")==false)
						{
							Logger.error(WriteTag.class, ex1);
							content.append(getErrorMessage(prop, "writetag.error", includeFileName));

							if (user != null && user.isAdmin() && user.isEnabledItem("cmp_adminlog") && request.getAttribute("writeTagDontShowError") == null)
							{
								content.append("<div style='border:2px solid red; background-color: white; color: black; margin: 5px; white-space: pre;'>" + ResponseUtils.filter(ex1.getMessage()) + "<br>");
								String stackTrace = ResponseUtils.filter(stack);
								content.append(stackTrace + "</div>");
							}

							Adminlog.add(Adminlog.TYPE_JSPERROR, "ERROR: " + includeFileName + "\n\n" + ex1.getMessage() + "\n\n" + sw.toString(), -1, -1);
							//break;
						}
					}

					if (request.getAttribute(SKIP_BODY)!=null)
					{
						//uz neincludujeme vysledok dalej
						buff = new StringBuilder();
						request.removeAttribute(SKIP_BODY);
					}

					startIndex = buff.indexOf(INCLUDE_START);

					//dt.diff(includeText);
					if (writePerfStat)
					{
						long diff = dt.getDiff();
						long lastDiff = dt.getLastDiff();
						String logText = "PerfStat: " + diff + " ms (+"+lastDiff+") " + includeText+"\n";
						content.append('\n').append(logText);
						Logger.debug(WriteTag.class, logText);
					}
				}

				content.append(WriteTagToolsForCore.replaceWriteText(buff, request));
			}
			catch (Exception ex)
			{
				Logger.error(WriteTag.class, ex);
				//nieco sa nam potototo...
				content.append(buff.toString());
			}
		}
		else
		{
			content.append(WriteTagToolsForCore.replaceWriteText(buff, request));
		}

		try
		{
			@SuppressWarnings("unchecked")
			List<String> afterWriteIncludeFile = (List<String>)request.getAttribute(AFTER_WRITE_INCLUDE_LIST);
			if (afterWriteIncludeFile!=null)
			{
				for (String includeFileName : afterWriteIncludeFile)
				{
					pageContext.include(includeFileName);
				}
				request.removeAttribute(AFTER_WRITE_INCLUDE_LIST);
			}
		}
		catch (RuntimeException ex1)
		{
			sk.iway.iwcm.Logger.error(ex1);
		}

		String html = content.toString();

		//replace cloud keys with texts in corresponding language, ticket 15053
		if(Constants.getString("installName").equals("cloud") && InitServlet.isTypeCloud()==false && (DocDB.getDomain(request).indexOf("template")!=-1 || request.getParameter("forceLng")!=null))
		{
			//preklad pri zobrazeni sa vykonava len pre manager node
			Logger.debug(WriteTag.class, "cloud manager, translating texts.., lng="+lng);
			html = CloudToolsForCore.translate(lng, html);
		}

		pageContext.getOut().write(html);
	}

	/**
	 * Prida do zoznamu subor, ktory sa includne po vykonani tohto write
	 * (ak nemoze byt includnuty kdekolvek do textu toho write)
	 * @param file - url suboru pre include (napr. /component/nieco/subor.jsp)
	 */
	public static void addAfterWriteInclude(String file, HttpServletRequest request)
	{
		@SuppressWarnings("unchecked")
		List<String> afterWriteIncludeFile = (List<String>)request.getAttribute(AFTER_WRITE_INCLUDE_LIST);
		if (afterWriteIncludeFile==null)
		{
			afterWriteIncludeFile = new ArrayList<>();
		}
		for (String includeFileName : afterWriteIncludeFile)
		{
			if (file.equals(includeFileName))
			{
				//uz je to tam pridane, preskakujem
				return;
			}
		}
		afterWriteIncludeFile.add(file);
		request.setAttribute(AFTER_WRITE_INCLUDE_LIST, afterWriteIncludeFile);
	}

	/**
	 * Vykona nahradenie znaciek !WRITE a !CALL v texte ktory sa vypisuje (napr. az po vykonani nejakeho include)
	 * @param text
	 * @param request
	 * @return
	 *
	 * @deprecated pouzi WriteTagToolsForCore.replaceWriteText(text,request);
	 */
	@Deprecated
	public static StringBuilder replaceWriteText(StringBuilder text, HttpServletRequest request)
	{
		return WriteTagToolsForCore.replaceWriteText(text,request);
	}

	public static String getErrorMessage(Prop prop, String key, String fullPath)
	{
		String fileName = fullPath;
		try
		{
			int i = fullPath.lastIndexOf('/');
			if (i>0) fileName = fullPath.substring(i+1);
		}
		catch (Exception e)
		{
			//
		}
		String fileNameUser = Tools.replace(fileName, ".jsp", "");
		fileNameUser = Tools.replace(fileNameUser, "_", " ");
		fileNameUser = Tools.replace(fileNameUser, "-", " ");

		return prop.getText(key, fullPath, fileName, fileNameUser);
	}

	/**
	 * @deprecated pouzi WriteTagToolsForCore.fixXhtml(text,request);
	 */
	@Deprecated
	public static StringBuilder fixXhtml(StringBuilder text, HttpServletRequest request)
	{
		return WriteTagToolsForCore.fixXhtml(text,request);
	}

	/**
	 * @deprecated pouzi WriteTagToolsForCore.preventSpam(text,request);
	 */
	@Deprecated
	public static StringBuilder preventSpam(StringBuilder text, HttpServletRequest request)
	{
		return WriteTagToolsForCore.preventSpam(text,request);
	}

	/**
	 * @deprecated pouzi WriteTagToolsForCore.encodeEmailAddress(email);
	 */
	@Deprecated
	public static String encodeEmailAddress(String email)
	{
		return WriteTagToolsForCore.encodeEmailAddress(email);
	}

	public static String decodeEmailAddress(String email)
	{
		if (email.indexOf('~')==-1 && email.indexOf('!')==-1)
		{
			//nie je zakodovane
			return(email);
		}

		String returnEmail = email;
		returnEmail = returnEmail.replace('~', '@');
		returnEmail = returnEmail.replace('!', '.');

		//cele to pre istotu otoc
		char[] newEmail = new char[returnEmail.length()];
		for (int i=0; i<returnEmail.length(); i++)
		{
			newEmail[i] = returnEmail.charAt(returnEmail.length()-i-1);
		}
		returnEmail = new String(newEmail);

		return(returnEmail);
	}

	/**
	 * Vlozi do stranky subor pre podporu inline editacia (primarne JavaScript kod)
	 * @param context
	 * @param request
	 * @throws Exception
	 */
	public static void appendInlineEditingScript(PageContext context, HttpServletRequest request) throws Exception
	{
		String KEY = "writeTag.inlineEditingScriptInserted";
		if (request.getAttribute(KEY)!=null) return;

		context.include( WriteTag.getCustomPageAdminNotNull("/admin/inline/inline_script.jsp", request));

		request.setAttribute(KEY, "1");
	}

	/**
	 * Pre zadanu cestu k JSP komponente najde jej verziu editor_component.jsp, alebo NULL ak neexistuje
	 * @param includeFileName
	 * @return
	 */
	@SuppressWarnings("java:S1075")
	private static String getInlineEditorComponent(String includeFileName, HttpServletRequest request)
	{
		if (isInlineComponentEditButton(request)==false) return null;

		String path = includeFileName.substring(0, includeFileName.lastIndexOf("/"))+"/editor_component.jsp";
		if (FileTools.isFile(path))
		{
			if (ContextFilter.isRunning(request)) path = ContextFilter.addContextPath(request.getContextPath(), path);
			return path;
		}

		//skus removnut installName
		if (path.indexOf("/"+Constants.getInstallName()+"/")!=-1)
		{
			path = Tools.replace(path, "/"+Constants.getInstallName()+"/", "/");
			if (FileTools.isFile(path))
			{
				if (ContextFilter.isRunning(request)) path = ContextFilter.addContextPath(request.getContextPath(), path);
				return path;
			}
		}

		//skus pridat installName
		if (path.indexOf("/"+Constants.getInstallName()+"/")==-1)
		{
			path = Tools.replace(path, "/components/", "/components/"+Constants.getInstallName()+"/");
			if (FileTools.isFile(path))
			{
				if (ContextFilter.isRunning(request)) path = ContextFilter.addContextPath(request.getContextPath(), path);
				return path;
			}
		}

		return null;
	}

	/**
	 * Pomocna metoda pre cachovanie hodnoty EditorDB.isEditable
	 * @param user
	 * @param docId
	 * @param request
	 * @return
	 */
	public static boolean isInlinePageEditable(Identity user, int docId, HttpServletRequest request)
	{
		if (user == null || user.isAdmin()==false) return false;

		String KEY = "writeTag.isInlinePageEditable-"+docId;
		Boolean isEditable = (Boolean)request.getAttribute(KEY);
		if (isEditable == null)
		{
			String path = PathFilter.getOrigPath(request);

			if (DocTools.isXssStrictUrlException(path, "inlineEditingDisabledUrls")) isEditable = Boolean.FALSE;

			if (isEditable==null) isEditable = Boolean.valueOf(EditorDB.isPageEditable(user, docId));

			//Logger.debug(WriteTag.class, "isInlinePageEditable, docId="+docId+" path="+path+" exception="+ShowDocAction.isXssStrictUrlException(path, "inlineEditingDisabledUrls")+" isEditable="+isEditable);
			request.setAttribute(KEY, isEditable);
		}
		return isEditable.booleanValue();
	}

	/**
	 * Vrati HTML kod pre inline editaciu, alebo NULL ak pre danu komponentu inline editacia nie je podporovana
	 * @param includeFileName
	 * @param includeText
	 * @param user
	 * @param docId
	 * @param request
	 * @return
	 */
	public static StringBuilder getInlineEditingStart(String includeFileName, String includeText, Identity user, int docId, HttpServletRequest request, Prop prop)
	{
		if (Constants.getBoolean("inlineEditingEnabled")==false || user == null || user.isAdmin()==false) return null;

		//test na isPageEditable je az tu, aby sa zbytocne netestoval pre ine komponenty na zaciatku metody
		String includeFileNameNoInstallName = includeFileName;

		String returnIncludeFileName = includeFileName;
		if (includeFileNameNoInstallName.startsWith("/components/"+Constants.getInstallName())) returnIncludeFileName = "/components"+returnIncludeFileName.substring(("/components/"+Constants.getInstallName()).length());

		if (DocTools.isXssStrictUrlException(returnIncludeFileName, "inlineEditingComponents")  && isInlinePageEditable(user, docId, request) && Tools.isNotEmpty(getInlineComponentEditTextKey(request)))
		{
			StringBuilder html = new StringBuilder();
			html.append("<div class=\"inlineComponentEdit\"><div class='inlineComponentButtonsWrapper'><div class='inlineComponentButtons'>");

			String editorComponent = getInlineEditorComponent(returnIncludeFileName, request);

			String pageParams = "";
			if (editorComponent != null && Tools.isNotEmpty(getInlineComponentEditTextKey(request)))
			{
				int ciarka = includeText.indexOf(',');
				if (ciarka > 0)
				{
					pageParams = includeText.substring(ciarka+1);
				}

				html.append("<a href=\"javascript:inlineComponentEdit('").append(returnIncludeFileName).append("', '").append(JSEscapeTag.jsEscape(pageParams)).append("', ");
				html.append(" '").append(JSEscapeTag.jsEscape(includeText)).append("', ");
				html.append(" '").append(JSEscapeTag.jsEscape(editorComponent)).append("', ");
				html.append(docId);
				html.append(")\" class=\"inlineComponentButton inlineComponentButtonEdit cke_button\" style=\"background-image:url('");
				html.append(getInlineComponentEditIcon(request));
				html.append("');\"><span>");
				html.append(prop.getText(getInlineComponentEditTextKey(request)));
				html.append("</span></a>");
			}

			html.append("{inlineEditingButtons}");

			if (editorComponent != null && Constants.getBoolean("inlineEditingAllowDelete") && request.getAttribute(INLINE_EDITING_DISABLE_DELETE_BUTTON)==null)
			{
				html.append("<a href=\"javascript:inlineComponentDelete('").append(returnIncludeFileName).append("', '").append(JSEscapeTag.jsEscape(pageParams)).append("', ");
				html.append(" '").append(JSEscapeTag.jsEscape(includeText)).append("', ");
				html.append(" '").append(JSEscapeTag.jsEscape(editorComponent)).append("', ");
				html.append(docId);
				html.append(")\" class=\"inlineComponentButton inlineComponentButtonDelete cke_button\" style=\"background-image:url('");
				html.append("/components/_common/admin/inline/icon-delete.png");
				html.append("');\"><span>");
				html.append(prop.getText("button.delete"));
				html.append("</span></a>");
			}
			request.removeAttribute(INLINE_EDITING_DISABLE_DELETE_BUTTON);

			html.append("</div></div>");

			return html;
		}

		return null;
	}

	/**
	 * Vrati true ak je pre aktualne vkladanu komponentu do stranky povolena inline editacia, vola sa priamo z danej JSP
	 * @param request
	 * @return
	 */
	public static boolean isInlineEditing(HttpServletRequest request)
	{
		return "true".equals(request.getAttribute("isInlineEditing"));
	}

	public static void addInlineButton(String textKey, String iconLink, String href, String textParam1, String textParam2, HttpServletRequest request)
	{
		String lng = PageLng.getUserLng(request);
		Prop prop = Prop.getInstance(lng);

		StringBuilder buttons = (StringBuilder)request.getAttribute(INLINE_EDITING_BUTTONS_KEY);
		if (buttons == null)
		{
			buttons = new StringBuilder();
			request.setAttribute(INLINE_EDITING_BUTTONS_KEY, buttons);
		}

		buttons.append("<a class=\"inlineComponentButton\" href=\"");
		buttons.append(href);
		buttons.append("\" style=\"background-image:url('");
		buttons.append(iconLink);
		buttons.append("');\"><span>");
		buttons.append(prop.getText(textKey, textParam1, textParam2));
		buttons.append("</span></a>");
	}

	public static void addInlineButtonTop(String textKey, String iconLink, String href, String textParam1, String textParam2, HttpServletRequest request)
	{
		String lng = PageLng.getUserLng(request);
		Prop prop = Prop.getInstance(lng);

		StringBuilder buttons = (StringBuilder)request.getAttribute(INLINE_EDITING_BUTTONS_TOP_KEY);
		if (buttons == null)
		{
			buttons = new StringBuilder();
			request.setAttribute(INLINE_EDITING_BUTTONS_TOP_KEY, buttons);
		}

		buttons.append("<a class=\"inlineComponentButton\" href=\"");
		buttons.append(href);
		buttons.append("\" style=\"background-image:url('");
		buttons.append(iconLink);
		buttons.append("');\">");
		buttons.append(prop.getText(textKey, textParam1, textParam2));
		buttons.append("</a>");
	}

	public static void hideInlineComponentEditButton(HttpServletRequest request)
	{
		request.setAttribute(INLINE_EDITING_MODULE_PROPS_DISABLE_KEY, Boolean.FALSE);
	}

	private static boolean isInlineComponentEditButton(HttpServletRequest request)
	{
		if (request.getAttribute(INLINE_EDITING_MODULE_PROPS_DISABLE_KEY)!=null)
		{
			return false;
		}
		return true;
	}

	public static void setInlineComponentEditTextKey(String key, HttpServletRequest request)
	{
		request.setAttribute(INLINE_EDITING_MODULE_PROPS_TEXT_KEY, key);
	}

	public static void disableDeleteButton(HttpServletRequest request)
	{
		request.setAttribute(INLINE_EDITING_DISABLE_DELETE_BUTTON, "1");
	}

	private static String getInlineComponentEditTextKey(HttpServletRequest request)
	{
		String key = (String)request.getAttribute(INLINE_EDITING_MODULE_PROPS_TEXT_KEY);
		if (key == null) key = "editor.inline.editComponent";

		return key;
	}

	public static void setInlineComponentEditIcon(String url, HttpServletRequest request)
	{
		request.setAttribute(INLINE_EDITING_MODULE_PROPS_ICON, url);
	}

	private static String getInlineComponentEditIcon(HttpServletRequest request)
	{
		String icon = (String)request.getAttribute(INLINE_EDITING_MODULE_PROPS_ICON);
		if (icon == null) icon = "/components/_common/admin/inline/icon-properties.png";

		return icon;
	}

	private static void preserveParametersSet(String includeFileName, HttpServletRequest request)
	{
		//toto robi search komponente problem, koliduje to s jej vyrazmi, kvoli problemom s custom JSP to davam radsej sem
		if (includeFileName.indexOf("/search")!=-1)
		{
			for (String pname : SearchTools.getCheckInputParams())
			{
				String value = (String)request.getAttribute(pname);
				if (Tools.isNotEmpty(value))
				{
					request.setAttribute("preserve_"+pname, value);
					request.removeAttribute(pname);
				}
			}
		}
	}

	private static void preserveParametersRemove(String includeFileName, HttpServletRequest request)
	{
		//toto robi search komponente problem, koliduje to s jej vyrazmi, kvoli problemom s custom JSP to davam radsej sem
		if (includeFileName.indexOf("/search")!=-1)
		{
			for (String pname : SearchTools.getCheckInputParams())
			{
				String value = (String)request.getAttribute("preserve_"+pname);
				if (Tools.isNotEmpty(value))
				{
					request.setAttribute(pname, value);
					request.removeAttribute("preserve_"+pname);
				}
			}
		}
	}
}
