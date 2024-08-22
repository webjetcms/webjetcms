package sk.iway.iwcm.form;

import cvu.html.HTMLTokenizer;
import cvu.html.TagToken;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.validation.SimpleError;
import org.apache.struts.util.ResponseUtils;
import sk.iway.Password;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.PdfTools;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.components.upload.XhrFileUploadServlet;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.form.validators.PhoneValidator;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.captcha.Captcha;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.tags.WriteTag;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.upload.DiskMultiPartRequestHandler;
import sk.iway.upload.UploadedFile;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.sql.*;
import java.util.*;

/**
 *  univerzalne poslanie mailu
 *
 *@Title        iwcm
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.30 $
 *@created      ďż˝tvrtok, 2002, marec 28
 *@modified     $Date: 2004/03/23 19:23:02 $
 */
public class FormMailAction extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	/**
	 *  Description of the Field
	 */
	public static final String FORM_FILE_DIR = "/WEB-INF/formfiles/";
	/**
	 *  Description of the Field
	 */
	public static final String FORM_HTML_DIR = "/WEB-INF/formhtml/";

	/**
	 *  Zaslanie formularu z www stranky, nastavuje sa to cez:<br>
	 *  <br>
	 *  Kam sa to presmeruje po submite<br>
	 *  <INPUT type="hidden" name="forward" value="/showdoc.do?docid=1250"><br>
	 *  Dokumentu s predlohou formularu (fieldy su nahradene !meno_fieldu!)<br>
	 *  <INPUT type=hidden value="formular.html" name="source"><br>
	 *  Prijemcovia mailu oddeleny ciarkou<br>
	 *  <INPUT type="hidden" name="recipients" value="lubos.balat@interway.sk">
	 *  ak je nastavene toto, ulozi sa formular do databazy<br>
	 *  <INPUT type="hidden" name="savedb" value="nazov_formularu"><br>
	 *  <br>
	 *  Subject mailu<br>
	 *  <INPUT type="hidden" name="subject" value="Slovnaft WEB formular:
	 *  palivove karty (english)"><br>
	 *  Zoznam fieldov ktore sa poslu a ulozia do DB<br>
	 *  <INPUT type="hidden" name="fields"
	 *  value="Spolocnost,ICO,Meno,Adresa,Tel,Fax,Email,Info"><br>
	 *  Field ktory urcuje ktory field je email<br>
	 *  <INPUT type="hidden" name="femail" value="Email"><br>
	 *  Field ktory urcuje ktory field je meno<br>
	 *  <INPUT type="hidden" name="fname" value="Meno"><br>
	 *  Zaciatok tela mailu<br>
	 *  <INPUT type="hidden" name="body" value="Formular palivove karty z
	 *  www.slovnaft.sk"><br>
	 *
	 *  formmail_sendUserInfoDocId - docId stranky, ktorej text sa posle emailom odosielatelovi formularu (jeho email je v poli email)
	 *  formmail_overwriteOldForms - ak je nastavene na true a je prihlaseny pouzivatel tak sa predtym vyplneny formular prepise novym
	 *  formmail_allowOnlyOneSubmit - ak je nastavene na true a je prihlaseny pouzivatel a uz vyplnil formular, nezapise sa znova do DB a formfail sa nastavi na formIsAllreadySubmitted
	 *
	 *
	 *
	 *@param  request               Description of the Parameter
	 *@param  response              Description of the Parameter
	 *@return                       Description of the Return Value
	 *@exception  IOException       Description of the Exception
	 *@exception  ServletException  Description of the Exception */


	protected static void execute(
			HttpServletRequest request,
			HttpServletResponse response)
			throws IOException, ServletException
	{
		//sprav upload

		DiskMultiPartRequestHandler multipartHandler = null;
		List<UploadedFile> formFiles = new ArrayList<>();
		Map<String, List<UploadedFile>> formFilesTable = new Hashtable<>();
		String formName = "unknownForm";
		try
		{
			multipartHandler = new DiskMultiPartRequestHandler();
			request = multipartHandler.handleRequest(request);

			formName = DB.internationalToEnglish(request.getParameter("savedb"));
			FormFileRestriction restriction = null;
			if (FormDB.isThereFileRestrictionFor(formName))
				restriction = FormDB.getFileRestrictionFor(formName);

			Map<String, List<UploadedFile>> files = multipartHandler.getFileElementsMultiple();

			Set<Map.Entry<String, List<UploadedFile>>> set = files.entrySet();
			for(Map.Entry<String, List<UploadedFile>> me : set)
			{
				formFilesTable.put(me.getKey(), me.getValue());

				for (UploadedFile formFile : me.getValue())
				{
					if (formFile != null && formFile.getFileSize() > 0)
					{
						((IwcmRequest)request).setParameter(me.getKey(), "NOT_EMPTY");

						formFiles.add(formFile);
						if (restriction != null && restriction.isSentFileValid(formFile)==false)
						{
							request.setAttribute("invalidFiles", true);
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			multipartHandler = null;
		}

		//je to tu duplicitne ked padne multipart
		formName = DB.internationalToEnglish(request.getParameter("savedb"));

		request = fillRequestWithDatabaseOptions(formName, request, formFilesTable.get("fillRequestInterceptorFile"));

		String forward = saveForm(request, formFilesTable, formFiles, null);

		//zmaz temp subory z uploadu
		if (multipartHandler != null) multipartHandler.rollback();

		String forwardType=request.getParameter("forwardType");

		if (forward.indexOf("formfail=")!=-1)
		{
			//musime nastavit na redirect, inak sa nespracuje failstatus
			forwardType = null;
		}

		if ("forward".equals(forwardType))
		{
			//ziskaj si URL
			String url = forward;
			if (forward.indexOf("showdoc.do")==-1)
			{
				if (forward.indexOf('?')!=-1) url = forward.substring(0, forward.indexOf('?'));

				DocDB docDB = DocDB.getInstance();
				int docId = docDB.getVirtualPathDocId(url, DocDB.getDomain(request));
				if (docId > 0) url = Tools.addParametersToUrlNoAmp("/showdoc.do?docid="+docId, forward.substring(forward.indexOf('?')+1));
			}
			request.getRequestDispatcher(url).forward(request, response);
			return;
		}
		if ("addParams".equals(forwardType))
		{
			Enumeration<String> parameters = request.getParameterNames();
			while (parameters.hasMoreElements())
			{
				String name = parameters.nextElement();
				if ("docid".equals(name) || "doShowdocAction".equals(name)) continue;

				String[] values = request.getParameterValues(name);
				for (int i=0; i<values.length; i++)
				{
					forward = Tools.addParameterToUrlNoAmp(forward, name, values[i]);
				}
			}
		}

		response.sendRedirect(forward);
	}

	public static HttpServletRequest fillRequestWithDatabaseOptions(String formName, HttpServletRequest request, List<UploadedFile> excelFile)
	{
		Map<String, String> options = new FormAttributeDB().load(formName);
		IwcmRequest wrapped = new IwcmRequest(request);
		for(Map.Entry<String, String> entry : options.entrySet())
		{
			String paramName = entry.getKey();
			//nastaveny parameter useFormDocId ma prioritu z databazy (lebo WriteTag ho vzdy nastavi podla aktualnej stranky)
			if (wrapped.hasParameter(paramName)==false || "useFormDocId".equals(paramName))
			{
				if ("useFormDocId".equals(paramName))
				{
					wrapped.setParameter("useFormDocIdOriginal", request.getParameter("useFormDocId"));
					Logger.debug(FormMailAction.class, "fillRequestWithDatabaseOptions, setting: useFormDocIdOriginal with value from useFormDocId="+request.getParameter("useFormDocId"));
				}
				Logger.debug(FormMailAction.class, "fillRequestWithDatabaseOptions, setting:"+paramName+"="+entry.getValue());
				wrapped.setParameter(paramName, entry.getValue());
				wrapped.setAttribute("DB"+paramName, "true");
			}
		}
		if (Tools.isNotEmpty(request.getParameter("fillRequestInterceptorClass"))){
			// moznost naplnit data zo vstupneho subora do nasho requestu (wrapped), pouzite napr. v jasr pri importe studentov
			// treba dat do formulara hidden parameter fillRequestInterceptorClass nastavenym na triedu ktora to ma vykonat
			InputStream is = null;
			try
			{
				Class<?> c = Class.forName(request.getParameter("fillRequestInterceptorClass"));
				if (FillRequestInterceptor.class.isAssignableFrom(c))
				{
					if (excelFile!=null && excelFile.size()>0) is = excelFile.get(0).getInputStream();
					FillRequestInterceptor interceptor = (FillRequestInterceptor) c.getDeclaredConstructor().newInstance();
					if (interceptor!=null)
					{
						boolean successfullyIntercepted = interceptor.intercept(is, wrapped);
						if (!successfullyIntercepted) {
							Logger.error(FormMailAction.class, "Nedoslo k naplneniu dat do wrappera v triede: " + interceptor.getClass().getName());
						}
					}
				}
			} catch (Exception e)
			{
				Logger.error(FormMailAction.class, "Problem s incerceptorom: ", e);
			}
			finally
			{
			if (is!=null) try { is.close(); } catch (Exception ex) { /* */ }
			}
		}
		return wrapped;
	}

	public static String saveForm(HttpServletRequest request, Map<String, List<UploadedFile>> formFilesTable, List<UploadedFile> formFiles, ActionBeanContext context)
	{
		Prop prop = Prop.getInstance(PageLng.getUserLng(request));
		List<IwcmFile> attachs = null;
		boolean attachFiles = true;
		String failStatus=null;

		String formName = request.getParameter("savedb");

		//skontroluj multiupload subory
		String[] uploadedFilesParamNameList = request.getParameterValues("Multiupload.formElementName");
		if (uploadedFilesParamNameList != null && uploadedFilesParamNameList.length>0)
		{
			//mame to tu znova keby padlo mulipart parsovanie
			FormFileRestriction restriction = null;
			if (FormDB.isThereFileRestrictionFor(formName)) restriction = FormDB.getFileRestrictionFor(formName);

			//over subory uploadnute cez HTML5 upload
			for (String uploadedFilesParamName : uploadedFilesParamNameList)
			{
				if (Tools.isNotEmpty(request.getParameter(uploadedFilesParamName)))
				{
					StringBuilder fileNames = new StringBuilder();

					for (String keys : request.getParameterValues(uploadedFilesParamName))
					{

						for (String fileKey : Tools.getTokens(keys, ";"))
						{
							if (restriction!=null)
							{
								String filePath = XhrFileUploadServlet.getTempFilePath(fileKey);
								Logger.debug(FormMailAction.class, "Multiupload, fileKey=" + fileKey + " path=" + filePath);
								if (filePath != null)
								{
									IwcmFile file = new IwcmFile(filePath);
									if (file.exists())
									{
										if (restriction.isSentFileValid(file) == false)
										{
											request.setAttribute("invalidFiles", "true");
										}
									}
								}
							}
							String fileName = XhrFileUploadServlet.getTempFileName(fileKey);
							if (Tools.isNotEmpty(fileName)) {
								if (fileNames.length()>0) fileNames.append(", ");
								fileNames.append(fileName);
							}
						}
					}

					//nastav hodnotu input pola so zoznamom suborov, aby sa nam to pekne vyrenderovalo v maile a HTML verzii
					((IwcmRequest)request).setParameter(uploadedFilesParamName+"-fileNames", fileNames.toString());
				}
			}
		}

		if (formFiles==null) formFiles = new ArrayList<>();
		if (formFilesTable==null) formFilesTable = new Hashtable<>();

		//toto je thread safe
		String emailEncoding = SetCharacterEncodingFilter.getEncoding();
		String formMailEncoding = Constants.getString("formMailEncoding");
		if (Tools.isNotEmpty(formMailEncoding))
		{
			emailEncoding = formMailEncoding;
		}
		String formMailEncodingParam = request.getParameter("formMailEncoding");
		if (Tools.isNotEmpty(formMailEncodingParam))
		{
			emailEncoding = formMailEncodingParam;
		}

		String host = Constants.getString("smtpServer");
		String recipients = null;
		if (request.getParameter("recipients") != null)
		{
			recipients = WriteTag.decodeEmailAddress(request.getParameter("recipients"));
		}
		if (recipients != null && recipients.indexOf('@') == -1)
		{
			recipients = null;
		}

		String subject = "Formular z www stranky";
		if (request.getParameter("subject") != null)
		{
			subject = request.getParameter("subject");
		}

		String email = "";
		String meno = "";

		GroupsDB groupsDB = GroupsDB.getInstance();
		TemplatesDB tempDB = TemplatesDB.getInstance();
		TemplateDetails temp = null;
		GroupDetails group;

		//fmeno sa pouzivalo volakedy, uz je depreaced
		if (request.getParameter("fmeno") != null || request.getParameter("fname") != null)
		{
			String fmeno = request.getParameter("fmeno");
			if (fmeno == null)
			{
				fmeno = request.getParameter("fname");
			}
			if (fmeno.indexOf(',') == -1)
			{
				meno = DB.internationalToEnglish(request.getParameter(fmeno));
			}
			else
			{
				StringTokenizer st = new StringTokenizer(fmeno, ",");
				String token;
				meno = "";
				StringBuilder buf = new StringBuilder();
				while (st.hasMoreTokens())
				{
					token = st.nextToken();
					buf.append(' ').append(DB.internationalToEnglish(request.getParameter(token)));
				}
				meno = buf.toString();
			}
		}
		if (request.getParameter("femail") != null)
		{
			email = request.getParameter(request.getParameter("femail"));
		}
		else if (request.getParameter("e-mail") != null)
		{
			email = request.getParameter("e-mail");
		}
		else if (request.getParameter("email") != null)
		{
			email = request.getParameter("email");
		}

		String source = null;
		if (request.getParameter("source") != null)
		{
			source = request.getParameter("source");
		}

		String forwardDefault = "/";
		String forwardOk = null;
		String forwardFail = null;
		if (request.getParameter("forward") != null) forwardOk = request.getParameter("forward");
		if (request.getParameter("forwardFail") != null) forwardFail = request.getParameter("forwardFail");

		DocDB docDB = DocDB.getInstance();

		Integer iLastDocId = null;
		Integer iLastDocIdMail = null;
		try
		{

			try
			{
				String referer = request.getHeader("referer");
				if (referer!=null && referer.indexOf("docid=") != -1)
				{
					try
					{
						referer = referer.substring(referer.indexOf("docid=") + 6);
						StringTokenizer st = new StringTokenizer(referer, "&");
						if (st.hasMoreTokens())
						{
							referer = st.nextToken();
						}
						iLastDocId = Integer.valueOf(Integer.parseInt(referer));
					}
					catch (Exception ex)
					{
						//nerobime nic
					}
				}
				else if (referer!=null)
				{
					String url = referer;
					Logger.debug(FormMailAction.class, "referer="+referer);
					try
					{
						int i = url.indexOf('?');
						if (i!=-1) url = url.substring(0, i);
						//odstran http://xxx/
						i = url.indexOf('/', 10);
						if (i != -1)
						{
							url = url.substring(i);
						}

						Logger.debug(FormMailAction.class, "url="+url);
						i = DocDB.getDocIdFromURL(url, DocDB.getDomain(request));
						if (i > 0) iLastDocId = i;

						Logger.debug(FormMailAction.class, "iLastDocId="+iLastDocId);
					}
					catch (Exception e)
					{
						//nerobime nic
					}
				}
			}
			catch (NumberFormatException ex2)
			{
				//nerobime nic
			}
			if (iLastDocId==null)
			{
				iLastDocId = (Integer) request.getSession().getAttribute("last_doc_id");
			}

			//niekedy je formular napr v pravom menu, potom sa neparsuje docid podla requestu, ale
			//sa mu musi presne povedat, kde sa ten formular nachadza
			if (request.getParameter("useFormDocId")!=null)
			{
				if ("none".equals(request.getParameter("useFormDocId")))
				{
					iLastDocId = null;
				}
				else
				{
					iLastDocId = Integer.valueOf(Tools.getIntValue(request.getParameter("useFormDocId"), -1));
				}
			}
			iLastDocIdMail = iLastDocId;
			//aby sme pre mail mohli specifikovat inu stranku ako to co sa zobrazuje
			if (request.getParameter("useFormMailDocId")!=null)
			{
				if ("none".equals(request.getParameter("useFormMailDocId")))
				{
					iLastDocIdMail = null;
				}
				else
				{
					iLastDocIdMail = Integer.valueOf(Tools.getIntValue(request.getParameter("useFormMailDocId"), -1));
				}
			}
			Logger.debug(FormMailAction.class, "iLastDocId="+iLastDocId);
		}
		catch (Exception ex) { /* nerobime nic */ }


		//id stranky z ktorej je automaticky vyparsovany HTML kod formularu
		int docId = -1;

		//HTML kod stranky pre test emailu (aby sa nedal recipients nastavit iny ako je v stranke)
		String originalPageHtml = null;

		StringBuilder htmlData = new StringBuilder();
		boolean hasHtmlData = false;

		String cssData = null;
		String cssLink = null;

		//tu budu ulozene hodnoty atributu class pre jednotlive polia
		//da sa tak spravit server side kontrola required poli
		List<LabelValueDetails> classNames = new ArrayList<>();
		Map<String, String> labelsTable = new Hashtable<>();

		boolean forceTextPlain = "true".equals(request.getParameter("forceTextPlain"));

		//iteruj cez polozky formularu
		StringBuilder fields = null;
		if (request.getParameter("fields") != null) fields = new StringBuilder(request.getParameter("fields"));
		//ak je true, fields sa generuju automaticky
		boolean autoFields = false;
		if (fields == null || fields.toString().equals("*"))
		{
			try
			{
				if (iLastDocIdMail!=null && iLastDocId!=null)
				{
					Logger.debug(FormMailAction.class, "iLastDocId(int)="+iLastDocIdMail);
					DocDetails doc = docDB.getDoc(iLastDocIdMail, -1, false);
					docId = iLastDocId.intValue();

					if (doc!=null)
					{
						//toto potrebujeme pre technicke info
						request.setAttribute("doc_title", doc.getTitle());
						request.setAttribute("doc_title_seo", Tools.isEmpty(doc.getFieldQ()) ? doc.getTitle() : doc.getFieldQ());
						request.setAttribute("doc_full_url", DocDB.getInstance().getDocLink(doc.getDocId(), doc.getExternalLink(), true, request));

						originalPageHtml = originalPageHtml + doc.getData();

						temp = tempDB.getTemplate(doc.getTempId());
						group = groupsDB.getGroup(doc.getGroupId());

						if (doc.getData().contains("/components/formsimple/"))
						{
							doc.setData(EditorDB.renderIncludes(doc, false, request));
						}

						//UPDATNI FIELDS
						doc.setData(getCroppedHTML(ShowDoc.updateCodes(null, doc.getData(), iLastDocIdMail.intValue(), request, Constants.getServletContext())));

						if (request.getParameter("subject") == null)
						{
							subject = doc.getTitle();
						}

						if (request.getParameter("forward") == null)
						{
							//forward robime podla normalneho lastDocId
							forwardDefault = docDB.getDocLink(iLastDocId.intValue());
						}
						if (forwardFail==null) forwardFail = docDB.getDocLink(iLastDocId.intValue());

						if (request.getParameter("savedb") == null)
						{
							formName = DocTools.removeCharsDir(DB.internationalToEnglish(doc.getTitle())).toLowerCase();
						}

						fields = null;
						String field;

						//vytvor strom
						HTMLTokenizer htmlTokenizer = new HTMLTokenizer(Tools.replace(doc.getData(), "/>", ">").toCharArray());
						//HTMLTree htmlTree = new HTMLTree(htmlTokenizer);
						@SuppressWarnings("unchecked")
						Enumeration<Object> e = htmlTokenizer.getTokens();
						TagToken tagToken;
						Object o;
						String skipToTag = null;
						StringBuilder labelContent = null;
						String labelFor = null;
						while (e.hasMoreElements())
						{
							o = e.nextElement();
							if (o instanceof TagToken)
							{
								tagToken = (TagToken) o;
								if (hasHtmlData == false)
								{
									htmlData.append(tagToken.getLineForm(request));
								}
								field = tagToken.getFormField();
								if (field!=null)
								{
									if (fields==null) fields = new StringBuilder(field);
									else
									{
										//aby sa neopakovali, napr. radiobuttony
										if ((","+fields.toString()+",").indexOf(","+field+",")==-1)
										{
											fields.append(",").append(field);
										}
									}
								}
								skipToTag = tagToken.getSkipToTag(skipToTag);

								Logger.debug(FormMailAction.class, "TagToken name="+tagToken.getName()+" for="+tagToken.getAttribute("for"));
								if ("label".equalsIgnoreCase(tagToken.getName()))
								{
									if (tagToken.isEndTag() && labelFor != null)
									{
										if (labelContent != null) labelsTable.put(labelFor, labelContent.toString());
									}
									else
									{
										labelContent = null;
										if (Tools.isNotEmpty(tagToken.getAttribute("for"))) labelFor = tagToken.getAttribute("for");
										else labelFor = null;
									}
								}


								field = tagToken.getAttribute("name");
								if (Tools.isEmpty(field)) field = tagToken.getAttribute("id");

								if (field!=null)
								{
									String className = tagToken.getAttribute("class");
									if (className != null)
									{
										LabelValueDetails lvb = new LabelValueDetails(field, className);
										lvb.setValue2(tagToken.getAttribute("id"));
										classNames.add(lvb);
										Logger.debug(FormMailAction.class, "className: "+field+"="+className);
									}

									//skus najst pole nazvane email, to bude odosielatel emailu
									if (request.getParameter("femail") == null)
									{
										if ("email".equalsIgnoreCase(field) || "e-mail".equalsIgnoreCase(field))
										{
											//email = request.getParameter(field);
											//field = tagToken.getAttribute("value");
											field = request.getParameter(field);
											if (field!=null && field.length()>3)
											{
												email = field;
											}
										}
									}
									if (request.getParameter("fname") == null)
									{
										if ("name".equalsIgnoreCase(field) ||
												"firstname".equalsIgnoreCase(field) ||
												"lastname".equalsIgnoreCase(field) ||
												"meno".equalsIgnoreCase(field) ||
												"priezvisko".equalsIgnoreCase(field) ||
												"jmeno".equalsIgnoreCase(field) ||
												"prijmeni".equalsIgnoreCase(field)
										)
										{
											//field = tagToken.getAttribute("value");
											field = request.getParameter(field);
											if (field!=null && field.length()>3)
											{
												if (meno==null || meno.length()==0)
												{
													meno = DB.internationalToEnglish(field);
												}
												else
												{
													meno += " " + DB.internationalToEnglish(field); //NOSONAR
												}
											}
										}
									}
								}
							}
							else
							{
								if (skipToTag == null) htmlData.append(o.toString());
								if (labelFor != null)
								{
									if (labelContent == null) labelContent = new StringBuilder(o.toString());
									else labelContent.append(o.toString());
								}
							}
						}

						if (htmlData.length() > 10)
						{
							hasHtmlData = true;

							if (Constants.getBoolean("formMailSendPlainText")==false && forceTextPlain==false)
							{
								try
								{
									cssData = "<style type='text/css'>";
									cssLink = "";

									if (temp != null)
									{
										String domainAlias = MultiDomainFilter.getDomainAlias(group.getDomainName());

										String tempCssLink = null;
										if (temp.getCss() != null && temp.getCss().length() > 1)
										{
											tempCssLink = temp.getCss();
											if (group!=null && Constants.getBoolean("multiDomainEnabled")==true && Tools.isNotEmpty(group.getDomainName()))
											{
												//ak je cssko v /templates adresari uz domain alias nepridavame
												if (tempCssLink.contains(domainAlias)==false && tempCssLink.contains("/templates/")==false && tempCssLink.contains("/files/")==false)
												{
													tempCssLink = Tools.replace(tempCssLink, "/css/", "/css/" + domainAlias + "/");
												}
											}
										}
										String baseCssPath = temp.getBaseCssPath();
										if (group!=null && Constants.getBoolean("multiDomainEnabled")==true && Tools.isNotEmpty(group.getDomainName()))
										{
											//ak je cssko v /templates adresari uz domain alias nepridavame
											if (baseCssPath.contains(domainAlias)==false && baseCssPath.contains("/templates/")==false && baseCssPath.contains("/files/")==false)
											{

												baseCssPath = Tools.replace(baseCssPath, "/css/", "/css/" + MultiDomainFilter.getDomainAlias(group.getDomainName()) + "/"); //NOSONAR
											}
										}

										String editorEditorCss = Constants.getString("editorEditorCss");

										tempCssLink = checkEmailCssVersion(tempCssLink);
										baseCssPath = checkEmailCssVersion(baseCssPath);
										editorEditorCss = checkEmailCssVersion(editorEditorCss);

										Logger.debug(FormMailAction.class, "Reading baseCSS: "+baseCssPath+" tempCss: "+tempCssLink);

										//nacitaj css styl ako v editore stranok
										StringBuilder cssStyle = new StringBuilder(FileTools.readFileContent(baseCssPath)).append('\n');
										if (tempCssLink!=null) cssStyle.append(FileTools.readFileContent(tempCssLink)).append('\n');
										cssStyle.append(FileTools.readFileContent(editorEditorCss)).append('\n');

										cssData += cssStyle.toString();
										cssLink += "<link rel='stylesheet' href='"+baseCssPath+"' type='text/css'/>\n";
										if (tempCssLink!=null) cssLink += "<link rel='stylesheet' href='"+tempCssLink+"' type='text/css'/>\n";
										cssLink += "<link rel='stylesheet' href='"+editorEditorCss+"' type='text/css'/>\n";
									}
									else
									{
										//nacitaj css styl
										InputStream is = Constants.getServletContext().getResourceAsStream("/css/email.css");
										if (is==null)
										{
											is = Constants.getServletContext().getResourceAsStream(Constants.getString("editorPageCss"));
											cssLink += "<link rel='stylesheet' href='"+Constants.getString("editorPageCss")+"' type='text/css'/>\n";
										}
										else
										{
											cssLink += "<link rel='stylesheet' href='/css/email.css' type='text/css'/>\n";
										}
										if (is!=null)
										{
											BufferedReader br = new BufferedReader(new InputStreamReader(is, Constants.FILE_ENCODING));
											String line;
											StringBuilder startBuf = new StringBuilder(cssData);
											while ((line=br.readLine())!=null)
											{
												startBuf.append(line).append('\n');
											}
											cssData = startBuf.toString();
										}
									}
								}
								catch (Exception ex)
								{
									Logger.error(FormMailAction.class, ex);
								}

								cssData += "</style>";
							}
						}
					}
				}
			}
			catch (Exception ex)
			{
				Logger.error(FormMailAction.class, ex);
			}
		}
		if (fields == null || fields.toString().equals("*"))
		{
			//vytvor fields...
			fields = null;
			autoFields = true;
			Enumeration<String> params = request.getParameterNames();
			String param;
			while (params.hasMoreElements())
			{
				param = params.nextElement();
				param = Tools.replace(param, "amp;", "");
				if ("recipients".equals(param) || "savedb".equals(param) || "__lng".equals(param)) continue;

				if (fields == null)
				{
					fields = new StringBuilder(param);
				}
				else
				{
					fields.append(",").append(param);
				}
			}
		}

		Logger.debug(FormMailAction.class, "fields="+fields);

		boolean hasCaptchaInclude = htmlData.indexOf("captcha.jsp")!=-1;
		if (forceTextPlain)
		{
			hasHtmlData = false;
			htmlData.setLength(0);
		}

		CryptoFactory cryptoFactory = new CryptoFactory();
		String publicKey = request.getParameter("encryptKey");

		StringBuilder db_data = null;
		StringBuilder db_data_names = null;
		boolean addFieldToDatabase = true;
		if (fields != null)
		{
			request.setAttribute("fields", fields.toString());
			StringTokenizer st = new StringTokenizer(fields.toString(), ",");
			String field;
			String value;

			while (st.hasMoreTokens())
			{
				field = st.nextToken();
				if (field == null) continue;

				//tieto pre istotu preskakujeme
				if ("useFormDocId".equals(field) || "g-recaptcha-response".equals(field) || "bSubmit".equals(field) || "__token".equals(field) ||
						"addTechInfo".equals(field) || "forceTextPlain".equals(field) || "subject".equals(field) ||
						"pictureHeight".equals(field) || "maxSizeInKilobytes".equals(field) || "messageAsAttachFileName".equals(field) || "useFormMailDocId".equals(field) || "pictureWidth".equals(field) ||
						field.startsWith("formmail") || "useFormDocIdOriginal".equals(field)) continue;

				value = getValue(field, request, formFilesTable);
				String fieldText = field;
				if (forceTextPlain) fieldText = fieldToText(fieldText, labelsTable);

				value = value.replace('|', ' ');
				field = field.replace('|', ' ');

				RequestBean.addAllowedParameter(field);

				if (autoFields && htmlData != null && hasHtmlData)
				{
					//do databazy pridavame iba fieldy ktore sa najdu v HTML
					if (htmlData.indexOf("!" + field + "!") != -1)
					{
						addFieldToDatabase = true;
					}
					else
					{
						addFieldToDatabase = false;
					}
				}
				else
				{
					addFieldToDatabase = true;
				}

				if (addFieldToDatabase)
				{
					if (db_data == null)
					{
						db_data = new StringBuilder(field + "~" + cryptoFactory.encrypt(value, publicKey));
						db_data_names = new StringBuilder(field);
					}
					else
					{
						if (db_data_names == null) db_data_names = new StringBuilder();
						db_data.append("|").append(field).append("~").append(cryptoFactory.encrypt(value, publicKey));
						db_data_names.append("|~").append(field);
					}
				}
				try
				{
					if (htmlData != null && hasHtmlData)
					{
						//ak je to nieco ako Hodnota_checkboxu zmen to na Hodnota checkboxu
						if (value.indexOf(' ') == -1 && value.indexOf('@')==-1)
						{
							value = value.replace('_', ' ');
						}
						//nahradu spravim len v pripade, ked nie je pouzity specialny tvar HTML kodu
						if (value.length() < 1 && Tools.isEmpty(source))
						{
							value = "&nbsp;";
						}
						value = Tools.replace(value, "\n", "<br>");
						htmlData = Tools.replace(htmlData, "!" + field + "!", value);
					}
					else
					{
						//asi checkbox
						if ("true".equals(value) || "on".equals(value)) value = "[X]";
						fieldText = field;
						if (forceTextPlain) fieldText = fieldToText(fieldText, labelsTable);
						htmlData.append(fieldText).append(": ").append(value).append("\n");
					}
				}
				catch (Exception ex)
				{
					Logger.error(FormMailAction.class, ex);
				}
			}
		}

		if ("true".equals(request.getParameter("addTechInfo")))
		{
			String techInfoHtml = prop.getText("components.formsimple.techinfo");
			techInfoHtml = ShowDoc.updateCodes(null, techInfoHtml, docId, request, Constants.getServletContext());
			if (forceTextPlain)
			{
				techInfoHtml = SearchTools.htmlToPlain(techInfoHtml);
			}
			htmlData.append("\n\n").append(techInfoHtml);
		}

		//ak je nastavene beforePostMethod tak to sem dokaze nastavit return parametre
		String beforePostReturnParams = null;

		//ak aktualizujeme zaznam, toto potom nastavime na false, aby sa nic neposlalo
		boolean emailAllowed = true;

		//kontrola recipients voci povodnemu HTML kodu
		if (originalPageHtml!=null && Tools.isNotEmpty(originalPageHtml))
		{
			if (recipients!=null && Tools.isNotEmpty(recipients) && Constants.getBoolean("spamProtection") && !"true".equals(request.getAttribute("DBrecipients"))) //DBrecipients sa nastavi v fillRequestWithDatabaseOptions
			{
				int j;
				String[] recipientsArray = recipients.toLowerCase().split(",");

				String pageHtmlLC = originalPageHtml.toLowerCase();
				for (j=0; j<recipientsArray.length; j++)
				{
					if (pageHtmlLC.indexOf(recipientsArray[j].trim())==-1 && pageHtmlLC.indexOf(WriteTagToolsForCore.encodeEmailAddress(recipientsArray[j].trim()))==-1)
					{
						emailAllowed = false;
						break;
					}
				}
			}
		}
		else
		{
			emailAllowed = false;
		}

		String allowedRecipients = Constants.getString("formmailAllowedRecipients");
		if (Tools.isEmpty(allowedRecipients)) allowedRecipients = "@"+Tools.getServerName(request); //aby sa nahodou nestalo, ze je niekde zabudnute nastavenie email adresy
		if (emailAllowed == false && allowedRecipients!=null && Tools.isNotEmpty(allowedRecipients) && recipients!=null && Tools.isNotEmpty(recipients))
		{
			try
			{
				String[] arArray = allowedRecipients.split(",");
				int i;
				int j;
				String[] recipientsArray = recipients.split(",");

				for (j=0; j<recipientsArray.length; j++)
				{
					boolean emailFound = false;
					for (i=0; i<arArray.length; i++)
					{
						if (recipientsArray[j].toLowerCase().endsWith(arArray[i]))
						{
							emailFound = true;
						}
					}
					if (emailFound)
					{
						emailAllowed = true;
						break;
					}
				}
			}
			catch (Exception ex)
			{
				Logger.error(FormMailAction.class, ex);
			}
		}

		if ("cloud".equals(Constants.getInstallName()))
		{
			if (Tools.isEmpty(recipients))
			{
				UserDetails admin = CloudToolsForCore.getAdmin();
				if (admin != null && Tools.isEmail(admin.getEmail())) recipients = admin.getEmail();
			}
		}

		boolean spamProtectionEnabled = true;
		if (temp != null) spamProtectionEnabled = temp.isDisableSpamProtection()==false;

		//conf value overrides everything
		if (Constants.getBoolean("spamProtection")==false) spamProtectionEnabled = false;

		//DETEKCIA SPAMU
		boolean isCaptchOk = true;
		if (spamProtectionEnabled) {
			isCaptchOk = checkCaptcha(request, hasCaptchaInclude);
		}

		boolean isSpam = true;
		//spam kontrolujeme len ak je captcha OK, inak nam zarata odoslanie a musime cakat 30 sekund
		if (isCaptchOk) {
			if (spamProtectionEnabled)	isSpam = detectSpam(request, htmlData.toString(), toString(db_data_names));
			else isSpam = false;
		}

		boolean requiredFieldsOk = checkRequiredFields(request, classNames, labelsTable, context);
		boolean areFilesOk = request.getAttribute("invalidFiles") == null;
		boolean isFormNameOk = true;
		boolean isCsrfCorrect = true;
		if (spamProtectionEnabled) {
			isCsrfCorrect = checkCsrf(request);
		}

		if ("public".equals(Constants.getString("clusterMyNodeType")) && Constants.getBoolean("formAllowOnlyExistingFormsOnPublicNode")==true)
		{
			//na public node umoznime odoslat len definovane formulare
			int recordsCount = new SimpleQuery().forInt("select count(form_name) from form_attributes where form_name=?", formName);
			if (recordsCount < 1)
			{
				isFormNameOk = false;

				//RequestBean.setErrorText("send_mail_error.attributes");
				RequestBean.addError("Chyba overenia atributov formularu (na public node, formular nema definovane ziadne atributy - asi neexistuje)");
				RequestBean.addParameter("formName", formName);
				RequestBean.addParameter("DBDataNames", toString(db_data_names));

				Adminlog.add(Adminlog.TYPE_FORMMAIL, "", docId, -1);

			}
		}

		int formId = -1;
		boolean validationFailed = isSpam || requiredFieldsOk==false || emailAllowed==false || areFilesOk==false || isCaptchOk==false || isFormNameOk==false || isCsrfCorrect==false;
		if (validationFailed)
		{
			if (emailAllowed==false) failStatus = "probablySpamBotEmail";
			else if (requiredFieldsOk==false) failStatus = "requiredFields";
			else if (areFilesOk==false) failStatus = "bad_file";
			else if (isCaptchOk==false) failStatus = "captcha";
			else if (isCsrfCorrect==false) failStatus = "probablySpamBotCsrf";
			else failStatus = "probablySpamBot";

			emailAllowed = false;

			//RequestBean.setErrorText("send_mail_error." + failStatus);
			RequestBean.addError(prop.getText("send_mail_error." + failStatus), false);
			RequestBean.addParameter("formName", request.getParameter("savedb"));

			Adminlog.add(Adminlog.TYPE_FORMMAIL, "", docId, formId);

		}
		else
		{

			//DETEKCIA SPAMU KONIEC

			htmlData = new StringBuilder(replaceCaptchaInclude(request, htmlData.toString()));

			StringBuilder fileNames = null;
			StringBuilder fileNamesSendLater = null;	//ak nemame SMTP server sem ukladame odkazy pre SendMail.sendLater (ma ich inak formatovane)
			//oki doki, strc to do databazy

			Logger.debug(FormMailAction.class, "db_data_names="+toString(db_data_names)+" db_data="+toString(db_data)+" formName="+formName);



			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				if (formName != null && formName.length() > 0 && db_data_names!=null && db_data_names.length()>3)
				{
					db_conn = DBPool.getConnection(request);
					ps = db_conn.prepareStatement("SELECT min(id) AS id FROM forms WHERE form_name=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
					ps.setString(1, formName);
					rs = ps.executeQuery();
					if (rs.next())
					{
						formId = rs.getInt("id");
					}
					if (formId < 1)
					{
						formId = -1;
					}

					rs.close();
					ps.close();
					if (formId == -1)
					{
						//vytvor hlavicku s udajmi o datach formulara
						ps = db_conn.prepareStatement("INSERT INTO forms (form_name, data, user_id, doc_id, domain_id) VALUES (?, ?, -1, -1, ?)");
						ps.setString(1, formName);
						DB.setClob(ps, 2, toString(db_data_names));
						ps.setInt(3, CloudToolsForCore.getDomainId());
						ps.execute();
						ps.close();
					}
					else
					{
						if (db_data_names!=null)
						{
							//hlavicka uz existuje, iba ju updatneme
							ps = db_conn.prepareStatement("UPDATE forms SET data=? WHERE id=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
							DB.setClob(ps, 1, toString(db_data_names));
							ps.setInt(2, formId);
							ps.execute();
							ps.close();
						}
					}

					int userId = -1;
					try
					{
						Identity user = UsersDB.getCurrentUser(request);
						if (user!=null)
						{
							userId = user.getUserId();
						}
					}
					catch (Exception ex)
					{
						Logger.error(FormMailAction.class, ex);
					}

					if (userId > 0 && "true".equals(request.getParameter("formmail_overwriteOldForms")))
					{
						ps = db_conn.prepareStatement("DELETE FROM forms WHERE form_name=? AND user_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
						ps.setString(1, formName);
						ps.setInt(2, userId);
						ps.execute();
						ps.close();
					}

					boolean allowInsert = true;

					if (userId > 0 && "true".equals(request.getParameter("formmail_allowOnlyOneSubmit")))
					{
						ps = db_conn.prepareStatement("SELECT * FROM forms WHERE form_name=? AND user_id=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
						ps.setString(1, formName);
						ps.setInt(2, userId);
						rs = ps.executeQuery();
						if (rs.next())
						{
							emailAllowed = false;
							allowInsert = false;
							failStatus = "formIsAllreadySubmitted";

							//RequestBean.setServerClas("send_mail_error.formIsAllreadySubmitted");
							RequestBean.addError("Formular sa uz odosiela");
							RequestBean.addParameter("formName", request.getParameter("savedb"));
							RequestBean.addParameter("DBDataNames", toString(db_data_names));

							Adminlog.add(Adminlog.TYPE_FORMMAIL, "", docId, formId);

						}
						rs.close();
						ps.close();
					}

					if (allowInsert)
					{
						Identity user = UsersDB.getCurrentUser(request);
						int editFormId = Tools.getIntValue(request.getParameter("_editFormId"), -1);
						Logger.debug(FormMailAction.class, "editFormId="+editFormId);
						if (user!=null && user.isAdmin()==true && editFormId>0)
						{
							Logger.debug(FormMailAction.class, "Updating form: "+editFormId);

							ps = db_conn.prepareStatement("UPDATE forms SET data=?, html=? WHERE id=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
							DB.setClob(ps, 1, toString(db_data));
							DB.setClob(ps, 2, cryptoFactory.encrypt(appendStyle(htmlData.toString(), cssLink, null, forceTextPlain), publicKey));
							ps.setInt(3, editFormId);
							ps.execute();
							ps.close();

							formId = editFormId;

							emailAllowed = false;
						}
						else
						{
							long l_now = (new java.util.Date()).getTime();
							ps = db_conn.prepareStatement("INSERT INTO forms (form_name, data, create_date, html, user_id, doc_id, domain_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
							ps.setString(1, formName);
							DB.setClob(ps, 2, toString(db_data));
							ps.setTimestamp(3, new Timestamp(l_now));
							DB.setClob(ps, 4, cryptoFactory.encrypt(appendStyle(htmlData.toString(), cssLink, null, forceTextPlain), publicKey));
							ps.setInt(5, userId);
							ps.setInt(6, docId);
							ps.setInt(7, CloudToolsForCore.getDomainId());
							ps.execute();
							ps.close();

							//	ziskaj id zaznamu
							ps = db_conn.prepareStatement("SELECT max(id) AS id FROM forms WHERE form_name=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
							ps.setString(1, formName);
							rs = ps.executeQuery();
							if (rs.next())
							{
								formId = rs.getInt("id");
							}
							rs.close();
							ps.close();

							try
							{
								//v try je to preto, ze som si nie isty ci Oracle zvladne zapis !=
								//updatni doc_id, pretoze historicky moze byt zapisane zle
								ps = db_conn.prepareStatement("UPDATE forms SET doc_id=? WHERE form_name=? AND doc_id>0 AND doc_id!=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
								ps.setInt(1, docId);
								ps.setString(2, formName);
								ps.setInt(3, docId);
								ps.execute();
								ps.close();
							}
							catch (Exception ex2)
							{
								//nerobime nic
							}
						}

						request.setAttribute("formMailFormFormId", Integer.toString(formId));

						String pdfUrl = "";
						attachs = new ArrayList<>();

						if("true".equals(request.getParameter("isPdfVersion")))
						{
							pdfUrl = saveFormAsPdf(appendStyle(htmlData.toString(), cssData, null, forceTextPlain), formId, request);
							IwcmFile pdfFile =  new IwcmFile(pdfUrl);
							//fileNames = new StringBuilder(pdfFile.getVirtualPath() + ";" + pdfFile.getName());
							if (fileNames == null) fileNames = new StringBuilder();
							if (fileNames.length()>0) fileNames.append(";");
							fileNames.append(pdfFile.getName());

							//fileNamesSendLater = new StringBuilder();
							if (fileNamesSendLater == null) fileNamesSendLater = new StringBuilder();
							if (fileNamesSendLater.length()>0) fileNamesSendLater.append(";");
							fileNamesSendLater.append(FORM_FILE_DIR).append(pdfFile.getName()).append(";").append(pdfFile.getName());
							attachs.add(new IwcmFile(pdfUrl));
						}

						//ak su aj subory
						if (formFiles.size() > 0)
						{
							//skopiruj subory
							String newFileName;
							String path = Tools.getRealPath(FORM_FILE_DIR);
							if (path != null)
							{
								String fileName;
								for (UploadedFile formFile : formFiles)
								{
									fileName = fixFileNameDirPath(formFile.getFileName().trim());

									//Logger.println(this,"we have a file name="+fileName+" content type="+file.getContentType());

									if (fileName != null && fileName.length() > 1)
									{
										InputStream stream = formFile.getInputStream();

										newFileName = formId + "_" +
												DocTools.removeChars(
														DB.internationalToEnglish(fileName));

										if ("false".equals(Constants.getString("useSMTPServer")))
										{
											if (fileNamesSendLater == null)
											{
												fileNamesSendLater = new StringBuilder(FORM_FILE_DIR + newFileName+";"+fileName);
											}
											else
											{
												fileNamesSendLater.append(";").append(FORM_FILE_DIR).append(newFileName).append(";").append(fileName);
											}
										}

										if (fileNames == null)
										{
											fileNames = new StringBuilder(newFileName);
										}
										else
										{
											fileNames.append(",").append(newFileName);
										}

										if (!path.endsWith(Character.toString(File.separatorChar)))
										{
											path = path + File.separatorChar; //NOSONAR
										}
										IwcmFile fullPath = new IwcmFile(path + newFileName);
										fullPath.mkdirs();
										fullPath.delete();
										fullPath.createNewFile();
										attachs.add(fullPath);

										IwcmFsDB.writeFiletoDest(stream,new File(fullPath.getPath()),formFile.getFileSize());
									}
								}
							}
						}

						//MBO: kukni ci tam neni nejaky multiupload
						//je mozne ze boli uploadovane subory
						if (uploadedFilesParamNameList != null && uploadedFilesParamNameList.length>0)
						{
							for (String uploadedFilesParamName : uploadedFilesParamNameList)
							{
								String baseDirName = PathFilter.getRealPath(FORM_FILE_DIR + "/");
								IwcmFile dir = new IwcmFile(baseDirName);
								if (!dir.exists()) dir.mkdirs();
								if (Tools.isNotEmpty(request.getParameter(uploadedFilesParamName)))
								{
									for (String keys : request.getParameterValues(uploadedFilesParamName))
									{
										for (String param : Tools.getTokens(keys, ";"))
										{
											String fileName = XhrFileUploadServlet.moveFile(param, baseDirName + File.separator);
											IwcmFile file = new IwcmFile(dir, fileName);
											if (file.exists())
											{
												IwcmFile dest = new IwcmFile(dir, formId + "_" + fileName);
												file.renameTo(dest);
												if (dest.exists())
												{
													if (fileNames == null)
													{
														fileNames = new StringBuilder(dest.getName());
													} else
													{
														fileNames.append(",").append(dest.getName());
													}
													if ("false".equals(Constants.getString("useSMTPServer"))) {
														if (fileNamesSendLater==null) fileNamesSendLater = new StringBuilder();
														fileNamesSendLater.append(";").append(dest.getVirtualPath()).append(";").append(dest.getName());
													}


													attachs.add(dest);
												}
											}
										}
									}
								}
							}
						}

						if(attachs != null)
						{
							long size = 0;
							if (attachs != null)
							{
								//ak je velkost prilohy vacsia ako stanovena hranica, prilohy k mailu nepripojim
								for(IwcmFile file : attachs)
								{
									size += file.length();
									if(size > Constants.getLong("maxSizeOfAttachments")) attachFiles = false;
								}
							}

							//updatni DB
							ps = db_conn.prepareStatement("UPDATE forms SET files=? WHERE id=? "+CloudToolsForCore.getDomainIdSqlWhere(true));
							DB.setClob(ps, 1, toString(fileNames));
							ps.setInt(2, formId);
							ps.execute();
							ps.close();

							if(!attachFiles)
							{
								fileNamesSendLater = null;
								Logger.println(FormMailAction.class, "Nepripajam prilohy do e-mailu: prekorocena max. velkost priloh="+size);
							}
						}
					}
					db_conn.close();
					db_conn = null;
				}
			}
			catch (Exception ex)
			{
				Logger.error(FormMailAction.class, ex);


				//RequestBean.setServerClas("send_mail_error.savedb");
				RequestBean.addError("Nastala chyba pri ukladani formularu - " + ex.getMessage());
				RequestBean.addParameter("formName", request.getParameter("savedb"));
				RequestBean.addParameter("DBDataNames", toString(db_data_names));


				Adminlog.add(Adminlog.TYPE_FORMMAIL, "", docId, formId);

				failStatus = "savedb";
			}
			finally
			{
				try
				{
					if (rs != null) rs.close();
					if (ps != null) ps.close();
					if (db_conn != null) db_conn.close();
				}
				catch (Exception e)
				{
					//nerobime nic
				}
			}

			int sendUserInfoDocId = Tools.getIntValue(request.getParameter("formmail_sendUserInfoDocId"), -1);
			Logger.debug(FormMailAction.class,"sendUserInfoDocId="+sendUserInfoDocId+" email="+email);
			if (sendUserInfoDocId>0)
			{
				sendUserInfo(sendUserInfoDocId, formId, email, attachs, formFilesTable, request);
			}

			Logger.println(FormMailAction.class,"FormMailAction emailAllowed="+emailAllowed+" recipients="+recipients);

			//RequestBean.addAllowedParameter("recipients");
			RequestBean.addParameter("recipients", new String[]{recipients});
			RequestBean.addParameter("formName", request.getParameter("savedb"));
			RequestBean.addParameter("DBDataNames", toString(db_data_names));

			String beforePostMethod = request.getParameter("beforePostMethod");
			if (Tools.isNotEmpty(request.getParameter("afterSendInterceptor"))&&"true".equals(request.getAttribute("DBafterSendInterceptor"))) beforePostMethod = request.getParameter("afterSendInterceptor");
			if (Tools.isEmpty(beforePostMethod)) beforePostMethod = Constants.getString("formMailBeforePostMethod");

			if (Tools.isNotEmpty(beforePostMethod) && Tools.isEmpty(recipients))
			{
				//ked mame nastaveny interceptor je potrebne vojst nizsie aj ked nie je vyplneny email formularu (lebo sa to niekomu nezdalo vhodne)
				//je to tak preto, aby sa interceptor vobec zavolal (TB-specific)
				recipients = Constants.getString("emailProtectionSenderEmail");
			}

			if (emailAllowed && "nobody@nowhere.com".equals(recipients)==false && recipients!=null && recipients.contains("@"))
			{
				if (email == null || email.indexOf('@')==-1)
				{
					String emailProtectionSenderEmail = Constants.getString(SendMail.EMAIL_PROTECTION_SENDER_KEY);
					if (Tools.isEmail(emailProtectionSenderEmail))
					{
						email = emailProtectionSenderEmail;
					}
					else
					{
						//skus nastavit odosielatela ako prijemcu
						String[] emails = recipients.split(",");
						if (emails != null && emails.length > 0)
						{
							email = emails[0];
						} else
						{
							email = "webform@" + Tools.getServerName(request);
						}
					}
				}
				if (meno == null || meno.trim().length() < 1)
				{
					meno = email;
				}

				if (Constants.getBoolean("formMailSendPlainText") || forceTextPlain)
				{
					htmlData = new StringBuilder(SearchTools.htmlToPlain(htmlData.toString()));
					cssData = null;
				}
				else
				{
					htmlData = new StringBuilder(SearchTools.removeCommands(htmlData.toString()));	//odstranim z HTML riadiace bloky napr.: !INCLUDE(...)!, !PARAMETER(...)!
				}

				if (emailEncoding.indexOf("ASCII")!=-1) htmlData = new StringBuilder(DB.internationalToEnglish(htmlData.toString()));
				htmlData = new StringBuilder(createAbsolutePath(htmlData.toString(), request));
				cssData = createAbsolutePath(cssData, request);

				boolean sendMessageAsAttach = "true".equals(request.getParameter("messageAsAttach"));
				IwcmFile messageAsAttachFile = null;
				if(sendMessageAsAttach)
				{
					String messageAsAttachFileName = request.getParameter("messageAsAttachFileName");
					if(Tools.isEmpty(messageAsAttachFileName)) messageAsAttachFileName = "priloha.html";
					else messageAsAttachFileName = DocTools.removeChars(messageAsAttachFileName);
					messageAsAttachFile = new IwcmFile(Tools.getRealPath(FORM_FILE_DIR + File.separator + formId+"_"+messageAsAttachFileName));
					FileTools.saveFileContent(messageAsAttachFile.getVirtualPath(), "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset="+emailEncoding+"\">"+(cssData == null ? "" : cssData)+"</head><body id=\"WebJETEditorBody\" class=\"WebJETMailBody\"><div class=\"WebJETMailWrapper\">\n\n"+htmlData+"\n\n</div></body></html>", emailEncoding);
				}

				if ("false".equals(Constants.getString("useSMTPServer")))
				{
					if(sendMessageAsAttach && messageAsAttachFile!=null)
					{
						htmlData = new StringBuilder(prop.getText("form.formmailaction.pozrite_si_prilozeny_subor"));
						if (fileNamesSendLater == null) fileNamesSendLater = new StringBuilder();
						fileNamesSendLater.append(";").append(FORM_FILE_DIR).append(messageAsAttachFile.getName()).append(";").append(messageAsAttachFile.getName());
					}
					if(attachs != null && !attachFiles) htmlData.append(prop.getText("email.too_large_attachments"));

					String messageBody = htmlData.toString();
					if (sendMessageAsAttach==false && forceTextPlain==false)
					{
						messageBody = appendStyle(htmlData.toString(), cssData, emailEncoding, forceTextPlain);
					}

					// interceptor pre send later
					if(canCallInterceptor(beforePostMethod)) {
						MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
						Multipart multipart = new MimeMultipart();
						BodyPart mbp = new MimeBodyPart();

						try {
							mbp.setContent(messageBody, "text/html; " + emailEncoding);
							multipart.addBodyPart(mbp);
							message.setContent(multipart);
						} catch (MessagingException e) {
							sk.iway.iwcm.Logger.error(e);
						}

						beforePostReturnParams = callInterceptor(beforePostMethod, message, multipart, request);
						if(hasInterceptorFailed(beforePostReturnParams)) {
							failStatus = "beforePostMethod";
						}

						try {
							messageBody = (String) multipart.getBodyPart(0).getContent();
						} catch (Exception e) {
							sk.iway.iwcm.Logger.error(e);
						}
					}

					Adminlog.add(Adminlog.TYPE_FORMMAIL, "Formular "+formName+" uspesne ulozeny do databazy, odoslany bude neskor", docId, formId);

					//musime kvoli clustru a potencionalnemu zapisu suborov pozdrzat email
					long sendLaterTime = Tools.getNow();
					sendLaterTime += (5 * Constants.getInt("clusterRefreshTimeout"));

					SendMail.sendLater(meno, getFirstEmail(email), recipients, request.getParameter("replyTo"), request.getParameter("ccEmails"), request.getParameter("bccEmails"), subject, messageBody, Tools.getBaseHref(request), Tools.formatDate(sendLaterTime), Tools.formatTime(sendLaterTime), toString(fileNamesSendLater));
				}
				else
				{
					//vygeneruj mail a posli ho
					Properties props = System.getProperties();

					try
					{
						if (host != null && host.length() > 2)
						{
							props.put("mail.smtp.host", host);
						}
						else if (props.getProperty("mail.smtp.host") == null)
						{
							props.put("mail.smtp.host", InetAddress.getLocalHost().getHostName());
						}
					}
					catch (Exception ex)
					{
						Logger.error(FormMailAction.class, ex);
					}

					Session session = SendMail.getSession(props);

					MimeMessage msg = new MimeMessage(session);
					InternetAddress[] toAddrs = null;

					try
					{
						//este potrebujeme updatnut linky na subory

						FormDetails formDetails = new FormDetails();
						formDetails.setFiles(toString(fileNames));

						//updatni linky na attachementy
						String baseHref = Tools.getBaseHref(request);
						htmlData = Tools.replace(htmlData, "!ATTACHMENTS!", formDetails.getAttachements(baseHref));

						try
						{
							String proxyHost = request.getHeader("x-forwarded-for");
							if (proxyHost != null && proxyHost.length()>4)
							{
								msg.setHeader("X-sender-proxy", proxyHost);
							}

							msg.setHeader("X-sender-ip", Tools.getRemoteIP(request));
							msg.setHeader("X-sender-host", Tools.getRemoteHost(request));
							msg.setHeader("X-server-name", Tools.getServerName(request));
							Identity user = UsersDB.getCurrentUser(request);
							if (user!=null)
							{
								msg.setHeader("X-sender-userid", Integer.toString(user.getUserId()));
								msg.setHeader("X-sender-fullname", DB.internationalToEnglish(user.getFullName()));
							}
						}
						catch (Exception ex)
						{
							Logger.error(FormMailAction.class, ex);
						}

						toAddrs = InternetAddress.parse(recipients, false);
						msg.setRecipients(Message.RecipientType.TO, toAddrs);

						String ccEmails = request.getParameter("ccEmails");
						if (ccEmails != null && ccEmails.indexOf('@')!=-1)
						{
							InternetAddress[] ccAddrs = InternetAddress.parse(ccEmails, false);
							msg.setRecipients(Message.RecipientType.CC, ccAddrs);
						}
						String bccEmails = request.getParameter("bccEmails");
						if (bccEmails != null && bccEmails.indexOf('@')!=-1)
						{
							InternetAddress[] bccAddrs = InternetAddress.parse(bccEmails, false);
							msg.setRecipients(Message.RecipientType.BCC, bccAddrs);
						}

						String from = email;
						msg.setFrom(new InternetAddress(getFirstEmail(email), meno));

						//iteruj cez polozky formularu
						String fieldsEmailHeader = request.getParameter("fieldsEmailHeader");
						if (fieldsEmailHeader != null)
						{
							StringTokenizer st = new StringTokenizer(fieldsEmailHeader, ",");
							String field;
							String value;
							while (st.hasMoreTokens())
							{
								field = st.nextToken();
								value = DB.internationalToEnglish(recode(request.getParameter(field)));
								msg.setHeader(field, value);
							}
						}

						//subject = new String(subject.getBytes(), emailEncoding);
						//msg.setSubject(subject);
						msg.setSubject(MimeUtility.encodeText(subject, emailEncoding, null));

						msg.setSentDate(new java.util.Date());

						//msg.setText(body);

						//MimeBodyPart text = new MimeBodyPart();

						MimeBodyPart html = new MimeBodyPart();

						//htmlData = new String(htmlData.getBytes(EMAIL_ENCODING));

						if(sendMessageAsAttach) htmlData = new StringBuilder(prop.getText("form.formmailaction.pozrite_si_prilozeny_subor"));
						if(attachs != null && !attachFiles) htmlData.append(prop.getText("email.too_large_attachments"));

						//odstran diakritiku
						if (emailEncoding.indexOf("ASCII")!=-1)
						{
							htmlData = new StringBuilder(DB.internationalToEnglish(htmlData.toString()));
						}

						//test ci je to HTML content, alebo nie. <br> za HTML content nepovazujem
						String htmlDataNoBR = Tools.replace(htmlData.toString(), "<br>", "\n");
						htmlDataNoBR = Tools.replace(htmlDataNoBR, "<br/>", "\n");
						if (htmlDataNoBR.indexOf('<') != -1 && htmlDataNoBR.indexOf('>') != -1)
						{
							html.setContent(appendStyle(htmlData.toString(), cssData, emailEncoding, forceTextPlain), "text/html; charset="+emailEncoding);
						}
						else
						{
							html.setContent(htmlData.toString(), "text/plain; charset="+emailEncoding);
						}

						Multipart mp = new MimeMultipart("mixed");
						if ((forceTextPlain==false || (attachs != null && attachFiles)) && (Tools.isNotEmpty(beforePostMethod) || formFiles.size()>0 || sendMessageAsAttach || (htmlDataNoBR.indexOf('<') != -1 && htmlDataNoBR.indexOf('>') != -1)))
						{
							//html.setContent(htmlData, contentType);

							//mp.addBodyPart(text);

							//#15245 - ak je nastaveny forceTextPlain a mal som aj prilohy, text formularu necham ako text/plain a prilohy pridam standartnym sposobom
							if(forceTextPlain)
								html.setContent(SearchTools.htmlToPlain(htmlDataNoBR), "text/plain; charset="+emailEncoding);

							mp.addBodyPart(html);

							//Multipart mp = new MimeMultipart("mixed");

							if(attachs != null && attachFiles)
							{
								for(IwcmFile file : attachs)
									attFile(file, mp, emailEncoding);
							}

							if(sendMessageAsAttach) attFile(messageAsAttachFile, mp, emailEncoding);

							msg.setContent(mp);

							if (canCallInterceptor(beforePostMethod))
							{
								beforePostReturnParams = callInterceptor(beforePostMethod, msg, mp, request);
								if(hasInterceptorFailed(beforePostReturnParams)) {
									failStatus = "beforePostMethod";
								}
							}
						}
						else
						{
							//mame iba textovy obsah, posli zjednodusene
							htmlDataNoBR = SearchTools.htmlToPlain(htmlDataNoBR);
							msg.setContent(htmlDataNoBR, "text/plain; charset="+emailEncoding);
						}

						String formMailFixedSenderEmail = Constants.getString("formMailFixedSenderEmail");
						if (Tools.isEmail(formMailFixedSenderEmail))
						{
							msg.setFrom(new InternetAddress(formMailFixedSenderEmail, formMailFixedSenderEmail));
						}
						else
						{
							if (Tools.isEmail(Constants.getString(SendMail.EMAIL_PROTECTION_SENDER_KEY)))
							{
								from = Constants.getString(SendMail.EMAIL_PROTECTION_SENDER_KEY);
								Address[] oldSenders = msg.getFrom();
								if (oldSenders != null && oldSenders.length>0 && from.equals(oldSenders[0].toString()) == false) msg.setReplyTo(oldSenders);
								msg.setFrom(new InternetAddress(from, from));
							}
						}

						//ak mame parameter "replyTo", nastavme ho
						String replyTo = request.getParameter("replyTo");
						if(Tools.isNotEmpty(replyTo) && replyTo.indexOf("@") != -1)
						{
							InternetAddress[] replyToAddrs = InternetAddress.parse(replyTo, false);
							msg.setReplyTo(replyToAddrs);
						}

						if(beforePostReturnParams==null || beforePostReturnParams.indexOf("doNotSend") == -1)
						{
							Transport.send(msg);

							RequestBean.addParameter("formName", formName);
							RequestBean.addParameter("beforePostMethod", beforePostMethod);
							RequestBean.addParameter("from", from);
							RequestBean.addParameter("to", recipients);
							RequestBean.addParameter("subject", subject);

							Adminlog.add(Adminlog.TYPE_FORMMAIL, "Formular "+formName+" uspesne odoslany na email "+recipients, docId, formId);
						}
						else
						{
							RequestBean.addParameter("formName", formName);
							RequestBean.addParameter("beforePostMethod", beforePostMethod);
							RequestBean.addParameter("from", from);
							RequestBean.addParameter("subject", subject);

							Adminlog.add(Adminlog.TYPE_FORMMAIL, "Formular "+formName+" uspesne ulozeny", docId, formId);
						}
					}
					catch (Exception ex)
					{
						Logger.error(FormMailAction.class, ex);
						failStatus = "emailsend";

						//RequestBean.setErrorText("send_mail_error");
						RequestBean.addError("Chyba pri odosielani emailu");
						RequestBean.addError("Stack Trace: \n"+Logger.getStackTrace(ex));
						RequestBean.addParameter("formName", formName);

						Adminlog.add(Adminlog.TYPE_FORMMAIL, "", docId, formId);
					}
				}
			}
			else
			{
				Adminlog.add(Adminlog.TYPE_FORMMAIL, "Formular "+formName+" uspesne ulozeny do databazy", docId, formId);
			}
		} //else isSpam



		String param = "formsend=true";

		StringBuilder forward = new StringBuilder(forwardDefault);
		if (failStatus!=null)
		{
			param = "formfail="+failStatus;
			if (Tools.isNotEmpty(forwardFail)) forward = new StringBuilder(forwardFail);
		}
		else
		{
			if (Tools.isNotEmpty(forwardOk)) forward = new StringBuilder(forwardOk);
		}

		if (forward.indexOf("?")==-1)
		{
			forward = forward.append('?').append(param);
		}
		else
		{
			forward =  forward.append('&').append(param);
		}
		if (Tools.isNotEmpty(beforePostReturnParams))
		{
			forward =  forward.append('&').append(beforePostReturnParams);
		}

		return (forward.toString());
		//return(mapping.findForward("success"));
	}

	/**
	 * Zisti ci ma zavolat afterSendInterceptor
	 * @param beforePostMethod
	 * @return true ak je pre formular nastaveny afterSendInterceptor
	 */
	private static boolean canCallInterceptor(String beforePostMethod) {
		return Tools.isNotEmpty(beforePostMethod) && beforePostMethod.length()>10 && beforePostMethod.contains(".");
	}

	/**
	 * Overi ci interceptor padol
	 * @param beforePostReturnParams
	 * @return true ak interceptor obsahuje string fail
	 */
	public static boolean hasInterceptorFailed(String beforePostReturnParams) {
		if(beforePostReturnParams == null) beforePostReturnParams = "";
		return beforePostReturnParams.indexOf("fail") != -1;
	}

	/**
	 * Zavola afterSendInterceptor
	 * @param beforePostMethod
	 * @param message
	 * @param multipart
	 * @param request
	 * @return Map<String, String> obsahujuci failStatus a beforePostReturnParams
	 */
	private static String callInterceptor(String beforePostMethod, MimeMessage message, Multipart multipart, HttpServletRequest request) {
		String beforePostReturnParams = null;
		int i = beforePostMethod.lastIndexOf('.');
		String beforePostClass = beforePostMethod.substring(0, i);
		beforePostMethod = beforePostMethod.substring(i+1);
		if (Character.isUpperCase(beforePostMethod.charAt(0)))
		{
			//je to pravdepodobne meno triedy
			beforePostClass = beforePostClass+"."+beforePostMethod;
			try
			{
				Class<?> c = Class.forName(beforePostClass);
				if (AfterSendInterceptor.class.isAssignableFrom(c))
				{
					AfterSendInterceptor interceptor = (AfterSendInterceptor)c.getDeclaredConstructor().newInstance();
					if (interceptor!=null)
					{
						beforePostReturnParams = interceptor.intercept(message, multipart, request);
					}
				}
			}
			catch (Exception ex)
			{
				Logger.error(FormMailAction.class, ex);
			}
		}
		else
		{
			try
			{
				Class<?> c = Class.forName(beforePostClass);
				Object o = c.getDeclaredConstructor().newInstance();
				Method m;
				Class<?>[] parameterTypes = new Class[] {MimeMessage.class, Multipart.class, HttpServletRequest.class};
				Object[] arguments = new Object[] {message, multipart, request};
				m = c.getMethod(beforePostMethod, parameterTypes);
				beforePostReturnParams = (String)m.invoke(o, arguments);
			}
			catch (Exception ex)
			{
				Logger.error(FormMailAction.class, ex);
			}
		}

		return beforePostReturnParams;
	}

	/**
	 *  Description of the Method
	 *
	 *@param  input  Description of the Parameter
	 *@return        Description of the Return Value
	 */
	private static String recode(String input)
	{
		if (input == null)
		{
			return ("");
		}
		//Logger.println(this,"Recoding: "+input);
		return (input.trim());
	}

	/**
	 * Vrati parameter z request, ak ma request viac parametrov s rovnakym
	 * menom, spoji ich ciarkami
	 * @param name
	 * @param request
	 * @return
	 */
	private static String getValue(String name, HttpServletRequest request, Map<String, List<UploadedFile>> formFilesTable)
	{
		//recode(request.getParameter(field));

		StringBuilder ret = null;

		String[] params = request.getParameterValues(name);
		if (params != null) {
			String param = null;
			int size = 0;
			if (params!=null) size = params.length;
			int i;
			for (i=0; i<size; i++)
			{
				param = params[i];

				//takto mame v parametroch vrateny subor
				if ("NOT_EMPTY".equals(param)) param = null;

				if (param!=null)
				{
					if (ret == null)
					{
						ret = new StringBuilder(param);
					}
					else
					{
						ret.append(";;\n").append(param);
					}
				}
			}
		}

		if (ret == null)
		{
			//skus najst ako subor
			Set<Map.Entry<String, List<UploadedFile>>> set = formFilesTable.entrySet();
			for(Map.Entry<String, List<UploadedFile>> me : set)
			{
				if (me.getKey().equals(name))
				{
					for (UploadedFile f : me.getValue())
					{
						if (ret == null) ret = new StringBuilder();

						if (ret.length()>0) ret.append(";;\n");
						ret.append(fixFileNameDirPath(f.getFileName()));
					}
				}
			}
		}

		if (ret == null) return "";
		return(ret.toString().trim());
	}

	/**
	 *  Description of the Method
	 *
	 *@param  formFile  Description of the Parameter
	 *@param  mp        Description of the Parameter
	 */
	private static void attFile(IwcmFile formFile, Multipart mp, String emailEncoding)
	{
		try
		{
			if (formFile != null)
			{
				//retrieve the file name
				String fileName = formFile.getName().trim();
				//Logger.println(this,"we have the file name=" + fileName);
				if (fileName != null && fileName.length() > 1)
				{
					fileName = DB.internationalToEnglish(fileName);
					int ind = fileName.indexOf("_");
					if(ind != -1) fileName = fileName.substring(ind+1);

					MimeBodyPart mbp2 = new MimeBodyPart();

					FileDataSource fds = null;

					if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(formFile.getAbsolutePath())))
					{
						IwcmFsDB.writeFileToDisk(new File(formFile.getAbsolutePath()),new File(IwcmFsDB.getTempFilePath(formFile.getAbsolutePath())));
						fds=new FileDataSource(IwcmFsDB.getTempFilePath(formFile.getAbsolutePath()));
					}
					else
					{
						fds=new FileDataSource(formFile.getAbsolutePath());
					}
					//FileDataSource fds = new FileDataSource(formFile.getAbsolutePath());

					mbp2.setDataHandler(new DataHandler(fds));
					mbp2.setFileName(fileName);
					MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
					String mimeType = mimeTypesMap.getContentType(fileName);
					if(Tools.isNotEmpty(mimeType) && mimeType.startsWith("text"))
						mbp2.setHeader("Content-Type", mimeType+"; charset="+emailEncoding);
					mp.addBodyPart(mbp2);
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

	}

	public static final String CROP_START = "<!-- formmail crop form start -->";
	public static final String CROP_END = "<!-- formmail crop form end -->";
	/**
	 * Vrati orezany HTML kod (ak obsahuje CROP_START a CROP_END)
	 * @param data - html kod
	 * @return
	 */
	public static String getCroppedHTML(String data)
	{
		try
		{
			//odstran vsetko pred <body> a po </body>
			String data_lcase = data.toLowerCase();

			int start;
			int end;

			if (Constants.getBoolean("formMailCropForm"))
			{
				start = data_lcase.indexOf("<form");
				if (start != -1) start = data_lcase.indexOf('>', start);
				end = data_lcase.indexOf("</form");

				if (end > start)
				{
					data = data.substring(start+1, end);

				}
			}
			else
			{
				start = data_lcase.indexOf(CROP_START);
				end = data_lcase.indexOf(CROP_END);

				if (end > start)
				{
					data = data.substring(start + CROP_START.length(), end);

				}
			}


		}
		catch (RuntimeException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return(data);
	}

	/**
	 * Detekcia, ci sa nejedna o spamovanie
	 * @param request
	 * @param db_data_names
	 * @return
	 */
	private static boolean detectSpam(HttpServletRequest request, String htmlData, String db_data_names)
	{
		//aby sa to dalo vypnut
		if (Constants.getBoolean("spamProtection")==false) return false;

		//robot to posle ako amp;savedb
		if (Tools.isEmpty(request.getParameter("savedb")) || Tools.isEmpty(db_data_names))
		{
			//RequestBean.setErrorText("send_mail_error.span_detected");
			RequestBean.addError("Chyba meno formularu (savedb)");

			Logger.debug(FormMailAction.class, "detectSpam TRUE, saved="+request.getParameter("savedb")+" db_data_names="+db_data_names);
			//spam roboty toto nevedia dobre poslat, kedze je to kombinacia post a get
			return true;
		}

		//preiteruj parametre a najdi nepovolene hodnoty
		Enumeration<String> e = request.getParameterNames();
		while (e.hasMoreElements())
		{
			String name = e.nextElement();
			if (DocTools.testXss(name) || name.indexOf('"')!=-1 || name.indexOf('\'')!=-1) return true;
			String[] values = request.getParameterValues(name);
			for (int i=0; i<values.length; i++)
			{
				if (DocTools.testXss(values[i]))
				{
					RequestBean.addError("Detekovane XSS "+values[i]);
					Logger.debug(FormMailAction.class, "detectSpam TRUE, val="+values[i]);
					return true;
				}
			}
		}

		//test na cookies (spameri zvycajne nemaju nastavene)
		if (request.getCookies()==null || request.getCookies().length==0)
		{
			RequestBean.addError("Cookies su prazdne");

			Logger.debug(FormMailAction.class, "detectSpam TRUE, cookies are empty");
			return true;
		}

		if (!SpamProtection.canPost("form", htmlData, request))
		{
			Logger.debug(FormMailAction.class, "detectSpam TRUE, can't post");
			return true;
		}

		return false;
	}

	/**
	 * Vrati true, ak su zadane vsetky pozadovane povinne polia
	 * @param request
	 * @param classNames
	 * @return
	 */
	private static boolean checkRequiredFields(HttpServletRequest request, List<LabelValueDetails> classNames, Map<String, String> labelsTable, ActionBeanContext context)
	{
		request.getSession().removeAttribute("formMailValidationErrors");
		request.getSession().removeAttribute("formMailValidationErrorsElementNameTable");

		boolean allOK = true;

		Prop prop = Prop.getInstance(PageLng.getUserLng(request));

		StringBuilder formMailValidationErrors = new StringBuilder();
		Map<String, String> formMailValidationErrorsElementNameTable = new Hashtable<>();

		for (LabelValueDetails lvb : classNames )
		{
			String fieldName = lvb.getLabel();
			String className = lvb.getValue();

			if (Tools.isEmpty(className)) continue;

			if (fieldName != null) fieldName = fieldName.trim();
			String paramValue = request.getParameter(fieldName);

			if (className.indexOf("required")!=-1)
			{
				if (Tools.isEmpty(paramValue))
				{
					Logger.debug(FormMailAction.class, "checkRequiredFields: name="+fieldName+" value="+paramValue+" class="+className);
					if (Tools.isNotEmpty(lvb.getValue2()))
					{
						String labelValue = labelsTable.get(lvb.getValue2());
						if (Tools.isNotEmpty(labelValue)) fieldName = labelValue;
					}

					if (context!=null)
					{
						context.getValidationErrors().add(fieldName, new SimpleError(Tools.replace(prop.getText("validation.required.valueNotPresent"), "{0}", fieldName)));
					}
					formMailValidationErrors.append("<li>").append(Tools.replace(prop.getText("validation.required.valueNotPresent"), "{0}", fieldName)).append("</li>").append('\n');
					formMailValidationErrorsElementNameTable.put(fieldName, className);

					allOK = false;
				}
			}
			if (Tools.isNotEmpty(paramValue))
			{
				PhoneValidator phoneValidator = PhoneValidator.getInstance();
				List<String> phoneClasses = phoneValidator.getPhoneClasses();
				if (phoneClasses!=null && phoneClasses.size()>0)
				{
					List<String> classes = Arrays.asList(Tools.getTokens(className, " "));
					if (phoneValidator.hasBlacklistedPhoneClass(classes) && phoneValidator.isBlacklisted(paramValue)) {
						String text = "components.tatrabanka.blacklistedNumber".equalsIgnoreCase(prop.getText("components.tatrabanka.blacklistedNumber")) ? prop.getText("components.form.blacklistedNumber") : prop.getText("components.tatrabanka.blacklistedNumber");
						if (context != null)
						{
							context.getValidationErrors().add(fieldName, new SimpleError(text));
						}
						formMailValidationErrors.append("<li>").append(text).append("</li>").append('\n');
						Logger.debug(FormMailAction.class, "formMailValidationErrors: " + formMailValidationErrors);
						formMailValidationErrorsElementNameTable.put(fieldName, className);

						allOK = false;
					}
					else if (phoneValidator.hasPhoneClass(classes) && !phoneValidator.isValid(phoneClasses, paramValue))
					{
						if (context != null)
						{
							context.getValidationErrors().add(fieldName, new SimpleError(Tools.replace(Tools.replace(prop.getText("validation.expression.valueFailedExpression"), "{1}", paramValue), "{0}", fieldName)));
						}
						formMailValidationErrors.append("<li>").append(Tools.replace(Tools.replace(prop.getText("validation.expression.valueFailedExpression"), "{1}", paramValue), "{0}", fieldName)).append("</li>").append('\n');
						Logger.debug(FormMailAction.class, "formMailValidationErrors: " + formMailValidationErrors);
						formMailValidationErrorsElementNameTable.put(fieldName, className);

						allOK = false;
					}
				}

				FormDB myFormDB = FormDB.getInstance();
				List<String[]> regularExpr = myFormDB.getAllRegularExpression();

				List<String> classNameList = Arrays.asList(Tools.getTokens(className, " "));

				//case insensitive porovnanie
				paramValue = paramValue.toLowerCase();
				for(String[] regExp: regularExpr)
				{
					//paramValue je LC, musime dat aj regexp na LC
					String regex = Tools.replace(regExp[2], "\\\\", "\\").toLowerCase();
					if (classNameList.contains(regExp[1]) && paramValue.matches(regex)==false)
					{
						Logger.debug(FormMailAction.class, "checkRequiredFields regex: name="+fieldName+" value="+paramValue+" class="+className+" regExp name="+regExp[1]+" regExp="+regex);

						if (DocTools.testXss(paramValue)) paramValue = "";
						if (DocTools.testXss(fieldName)) fieldName = "";

						paramValue = ResponseUtils.filter(paramValue);
						fieldName = ResponseUtils.filter(fieldName);

						if (context!=null)
						{
							if(className.indexOf("email") != -1)
								context.getValidationErrors().add(fieldName, new SimpleError(Tools.replace(prop.getText("converter.email.invalidEmail"), "{1}", paramValue)));
							else if(className.indexOf("minLen") != -1)
								context.getValidationErrors().add(fieldName, new SimpleError(Tools.replace(Tools.replace(prop.getText("validation.minlength.valueTooShort"), "{2}", paramValue.replaceFirst("minLen", "")), "{0}", fieldName)));
							else if(className.indexOf("number") != -1)
								context.getValidationErrors().add(fieldName, new SimpleError(Tools.replace(Tools.replace(prop.getText("converter.number.invalidNumber"), "{1}", paramValue), "{0}", fieldName)));
							else
								context.getValidationErrors().add(fieldName, new SimpleError(Tools.replace(Tools.replace(prop.getText("validation.expression.valueFailedExpression"), "{1}", paramValue), "{0}", fieldName)));
						}
						if(className.indexOf("email") != -1)
							formMailValidationErrors.append("<li>").append(Tools.replace(prop.getText("converter.email.invalidEmail"), "{1}", paramValue)).append("</li>").append('\n');
						else if(className.indexOf("minLen") != -1)
							formMailValidationErrors.append("<li>").append(Tools.replace(Tools.replace(prop.getText("validation.minlength.valueTooShort"), "{2}", className.replaceFirst("minLen", "")), "{0}", fieldName)).append("</li>").append('\n');
						else if(className.indexOf("number") != -1)
							formMailValidationErrors.append("<li>").append(Tools.replace(Tools.replace(prop.getText("converter.number.invalidNumber"), "{1}", paramValue), "{0}", fieldName)).append("</li>").append('\n');
						else
							formMailValidationErrors.append("<li>").append(Tools.replace(Tools.replace(prop.getText("validation.expression.valueFailedExpression"), "{1}", paramValue), "{0}", fieldName)).append("</li>").append('\n');
						Logger.debug(FormMailAction.class, "formMailValidationErrors: " + formMailValidationErrors);
						formMailValidationErrorsElementNameTable.put(fieldName, className);

						RequestBean.addError("Chyba regexp validacie "+fieldName+": "+formMailValidationErrors.toString());

						allOK = false;
					}
				}
			}
		}

		if (formMailValidationErrors.length() > 4)
		{
			request.getSession().setAttribute("formMailValidationErrors", "<ol class='formMailValidationErrors'>"+formMailValidationErrors.toString()+"</ol>");
			request.getSession().setAttribute("formMailValidationErrorsElementNameTable", formMailValidationErrorsElementNameTable);
		}

		return allOK;
	}

	/**
	 * Ulozi formular ako pdfko
	 * @param htmlData
	 */
	private static String saveFormAsPdf(String htmlData, int formId, HttpServletRequest request)
	{
		Logger.println(FormMailAction.class, "Exportujem formular do pdf, form_id: "+formId+" path: "+
				Tools.getRealPath(FORM_FILE_DIR)+File.separator+formId+"_pdf.pdf");
		try
		{
			IwcmFile dir = new IwcmFile(Tools.getRealPath(FORM_FILE_DIR));
			if (dir.exists()==false) dir.mkdirs();

			FileOutputStream fos = new FileOutputStream(Tools.getRealPath(FORM_FILE_DIR)+File.separator+formId+"_pdf.pdf");

			PdfTools.renderHtmlCode(htmlData, fos, request);

			fos.close();

			return Tools.getRealPath(FORM_FILE_DIR)+File.separator+formId+"_pdf.pdf";
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return "";
	}

	/**
	 * Skontroluje captcha kod, ak v htmlData je retazes captcha.jsp
	 * @param request
	 * @param hasCaptchaInclude
	 * @return
	 */
	private static boolean checkCaptcha(HttpServletRequest request, boolean hasCaptchaInclude)
	{
		String captchaType = Constants.getString("captchaType");
		if (Tools.isEmpty(captchaType)) captchaType = "none";

		if ("internal".equals(captchaType)==false)
		{
			//nie su nastavene premenne, takze nemozeme validovat
			if(!Constants.getBoolean("reCaptchaEnabled") || Tools.isEmpty(Constants.getString("reCaptchaSiteKey")) || Tools.isEmpty(Constants.getString("reCaptchaSecret")))
			{
				captchaType = "none";
			}
		}

		String captchaResponse = getCaptchaRespons(request);
		if ("none".equals(captchaType)==false && (Tools.isNotEmpty(captchaResponse) || hasCaptchaInclude))
		{
			boolean captchaOK = Captcha.validateResponse(request, captchaResponse, null);
			return captchaOK;
		}

		return true;
	}

	private static String getCaptchaRespons(HttpServletRequest request)
	{
		String captchaResponse = Tools.getStringValue(request.getParameter("g-recaptcha-response"), "");
		if (Tools.isEmpty(captchaResponse)) {
			captchaResponse = Tools.getStringValue(request.getParameter("wjcaptcha"), "");
		}
		return captchaResponse;
	}

	/**
	 * Nahradi kod captcha include zadanou hodnotou pouzivatela
	 * @param request
	 * @param htmlData
	 * @return
	 */
	private static String replaceCaptchaInclude(HttpServletRequest request, String htmlData)
	{
		String value = getCaptchaRespons(request);
		if (Tools.isNotEmpty(value))
		{
			htmlData = Tools.replace(htmlData, "!INCLUDE(/components/form/captcha.jsp)!", value);
		}
		return htmlData;
	}

	private static String appendStyle(String htmlData, String styleHtml, String emailEncoding, boolean forceTextPlain)
	{
		if (forceTextPlain || Constants.getBoolean("formMailSendPlainText")) return htmlData;

		if (styleHtml == null) styleHtml = "";

		String metaEncoding = "";
		if (Tools.isNotEmpty(emailEncoding)) metaEncoding = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset="+emailEncoding+"\">";

		return ("<html><head>"+metaEncoding+styleHtml+"</head><body id='WebJETEditorBody' class=\"WebJETMailBody\"><div class=\"WebJETMailWrapper\">\n\n"+htmlData+"\n\n</div></body></html>");
	}

	/**
	 * Vytvori absolutne cesty v zadanom HTML kode
	 * @param htmlCode - HTML kod
	 * @return
	 */
	private static String createAbsolutePath(String htmlCode, HttpServletRequest request)
	{
		String basePath = Tools.getBaseHref(request);
		//ak je tam uz base path, tak ju zrus, inak tam bude 2x
		//zrusene, pretoze to odstranovalo domenu z technickeho info htmlCode = Tools.replace(htmlCode, basePath, "");
		htmlCode = Tools.replace(htmlCode, "../", "/");
		htmlCode = Tools.replace(htmlCode, "url(images/", "url("+basePath+"/images/");
		htmlCode = Tools.replace(htmlCode, "url(/images/", "url("+basePath+"/images/");
		htmlCode = Tools.replace(htmlCode, "\"/images/", "\""+basePath+"/images/");
		htmlCode = Tools.replace(htmlCode, "\"/files/", "\""+basePath+"/files/");
		htmlCode = Tools.replace(htmlCode, "\"/css/", "\""+basePath+"/css/");

		return(htmlCode);
	}

	private static String checkEmailCssVersion(String cssLink)
	{
		if (cssLink == null) return cssLink;

		//sprav custom verziu
		String emailCssLink = Tools.replace(cssLink, ".css", "-email.css");
		if (FileTools.isFile(emailCssLink)) return emailCssLink;

		return cssLink;
	}

	/**
	 * IE8 a podobne vratia ako meno suboru kompletnu cestu na disku cize c:\adresar\meno.doc, toto z toho spravi len meno.doc
	 * @param fileName
	 * @return
	 */
	private static String fixFileNameDirPath(String fileName)
	{
		try
		{
			int lomka = fileName.lastIndexOf("/");
			if (lomka > 0) fileName = fileName.substring(lomka+1);
			lomka = fileName.lastIndexOf("\\");
			if (lomka > 0) fileName = fileName.substring(lomka+1);
		}
		catch (Exception e)
		{
			Logger.error(FormMailAction.class, e);
		}

		return fileName;
	}

	/**
	 * Skontroluje CSRF token
	 * @param request
	 * @return
	 */
	private static boolean checkCsrf(HttpServletRequest request)
	{
		String spamProtectionJavascript = Constants.getString("spamProtectionJavascript");
		if (spamProtectionJavascript.contains("formmailCsrf")==false)
		{
			//csrf token sa nepouziva
			return true;
		}
		boolean isCorrect = CSRF.verifyTokenAndDeleteIt(request);
		return isCorrect;
	}

	private static String fieldToText(String fieldName, Map<String, String> labelsTable)
	{
		String label = labelsTable.get(fieldName);

		//ak je to radiobutton ma vlastny label, skus najst label bez _rb_ nazvu
		//Pozadavek-na-odber-el-energie_rb_Ne -> Pozadavek-na-odber-el-energie
		if (fieldName!=null && fieldName.toLowerCase().endsWith("_rb"))
		{
			int i = fieldName.indexOf("_rb");
			if (i>2)
			{
				String labelRadioButtons = labelsTable.get(fieldName.substring(0, i));
				if (Tools.isNotEmpty(labelRadioButtons))
				{
					label = labelRadioButtons;
				}
			}
		}

		if (Tools.isEmpty(label)) label = Tools.replace(fieldName, "-", " ");

		label = Tools.replace(label, " *", "");
		label = Tools.replace(label, "e mail", "e-mail");

		return label;
	}

	/**
	 * Vrati prvy email so zoznamu (email1@domena.sk,email2@domena.sk vrati email1@domena.sk)
	 * @param emails
	 * @return
	 */
	private static String getFirstEmail(String emails)
	{
		if (Tools.isEmpty(emails)) return "";

		String[] emailsArr = Tools.getTokens(emails, ",", true);
		if (emailsArr.length>0) return emailsArr[0];

		return "";
	}

	private static String getDoubleOptInParameters(HttpServletRequest request, int formId) {
		String hash = Password.generateStringHash(16);
		updateHash(request, formId, hash);
		return hash;
	}

	private static void updateHash(HttpServletRequest request, int formId, String hash) {
		Connection db_conn = null;
		PreparedStatement ps = null;

		try {
			db_conn = DBPool.getConnection(request);
			ps = db_conn.prepareStatement("UPDATE forms SET double_optin_hash=? WHERE id=? " + CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setString(1, hash);
			ps.setInt(2, formId);
			ps.execute();
			ps.close();
		} catch (SQLException e) {
			sk.iway.iwcm.Logger.error(e);
		} finally
		{
			try
			{
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}

	/**
	 * Odoslanie notifikacie na email navstevnika, ktory ho vyplnil, zadane v poli formmail_sendUserInfoDocId
	 * @param sendUserInfoDocId
	 * @param formId
	 * @param email
	 * @param attachs
	 * @param request
	 */
	private static void sendUserInfo(int sendUserInfoDocId, int formId, String email, List<IwcmFile> attachs, Map<String, List<UploadedFile>> formFilesTable, HttpServletRequest request)
	{
		DocDB docDB = DocDB.getInstance();

		DocDetails doc = docDB.getDoc(sendUserInfoDocId);
		Logger.debug(FormMailAction.class,"doubleOptIn="+request.getParameter("doubleOptIn")+", sendUserInfoDocId-doc="+(doc != null ? doc.getDocId() : null));
		if (doc != null)
		{
			String data = SearchTools.removeCommands(doc.getData()); //odstranim z HTML riadiace bloky napr.: !INCLUDE(...)!, !PARAMETER(...)!
			StringBuilder attachments = new StringBuilder();
			for (IwcmFile attach : attachs) {
				attachments.append(attach.getVirtualPath());
				attachments.append(";");
				attachments.append(attach.getName());
				attachments.append(";");
			}

			if ("true".equals(request.getParameter("doubleOptIn"))) {
				String hash = getDoubleOptInParameters(request, formId);
				data = Tools.replace(data, "!FORM_ID!", "" + formId);
				data = Tools.replace(data, "!OPTIN_HASH!", hash);
			}

			for (String parameterName: Collections.list(request.getParameterNames()))
			{
				String value = getValue(parameterName, request, formFilesTable);

				data = Tools.replace(data, "!" + parameterName.toUpperCase() + "!", value);
				data = Tools.replace(data, "!" + parameterName + "!", value);
			}

			String authorName = Constants.getString("formmailSendUserInfoSenderName");
			if(Tools.isEmpty(authorName)) authorName = doc.getAuthorName();
			String authorEmail = Constants.getString("formmailSendUserInfoSenderEmail");
			if(Tools.isEmail(authorEmail) == false) authorEmail = doc.getAuthorEmail();
			Logger.debug(FormMailAction.class,"sendUserInfoSenderName="+authorName+", sendUserInfoSenderEmail="+authorEmail);
			SendMail.send(authorName, authorEmail, email, null, null, null, doc.getTitle(), "<html><body>"+data+"</body></html>", Tools.getBaseHref(request), attachments.toString());
		}
	}

	private static String toString(StringBuilder sb) {
		if (sb == null) return null;
		return sb.toString();
	}
}
