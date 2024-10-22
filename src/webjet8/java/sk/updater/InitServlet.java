package sk.updater;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 *  InitServlet.java - inicializacia Update Managera
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.11 $
 *@created      $Date: 2009/11/20 12:41:40 $
 *@modified     $Date: 2009/11/20 12:41:40 $
 */
@SuppressWarnings({"java:S106", "java:S899"})
public class InitServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	public static String INSTALL_NAME = "unknown"; //NOSONAR

	@Override
	public void init() throws ServletException
	{
		ServletContext sc = getServletContext();
		String version = getInitParameter("version");
		INSTALL_NAME = getInitParameter("installName"); //NOSONAR

		Logger.println(InitServlet.class,"Initializing UpdateManager, version 4: " + sc.getRealPath("/"));

		Collection<File> notDeleted = new LinkedList<>();

		try
		{
			if (version != null)
			{
				//dokopiruj custom kniznice
				addCustomLib(version, sc);

				//zmaz nepotrebne subory z lib adresara
				deleteUnwantedFiles("/WEB-INF/lib", version, notDeleted, sc);

				try
				{
					//vymaz subory stareho editora
					File f = new File(sc.getRealPath("/admin/FCKeditor/editor/js/fckeditorcode_gecko_1.js"));
					if (f.exists())
					{
						String[] files = new String[27];
						int i=0;
						files[i++]="fckconfig.js";
						files[i++]="FCKeditor.Packager.exe";
						files[i++]="media_edit.jsp";
						files[i++]="media_list.jsp";
						files[i++]="editor/fckdialog.html.jsp";
						files[i++]="editor/fckeditorarea.html.jsp";
						files[i++]="editor/css/editor.css";
						files[i++]="editor/css/fck_borders_moz.css";
						files[i++]="editor/css/stylecombo.css";
						files[i++]="editor/dialog/fck_about.html.jsp";
						files[i++]="editor/dialog/fck_paste.html.jsp";
						files[i++]="editor/dialog/fck_smiley.html.jsp";
						files[i++]="editor/dialog/fck_specialchar.html.jsp";
						files[i++]="editor/dialog/fck_table.html.jsp";
						files[i++]="editor/dialog/fck_tablecell.html.jsp";
						files[i++]="editor/dialog/css/common.css";
						files[i++]="editor/dialog/js/fck_link.js";
						files[i++]="editor/js/fck_startup.js.jsp";
						files[i++]="editor/js/fckeditorcode_gecko_1.js";
						files[i++]="editor/js/fckeditorcode_gecko_11.js";
						files[i++]="editor/js/fckeditorcode_gecko_12.js";
						files[i++]="editor/js/fckeditorcode_gecko_2.js";
						files[i++]="editor/js/fckeditorcode_ie_1.js";
						files[i++]="editor/js/fckeditorcode_ie_2.js";
						files[i++]="editor/js/fck_startup.js";
						files[i++]="editor/js/ns.js";
						files[i++]="editor/lang/fcklanguagemanager.js";

						for (i=0; i<files.length; i++)
						{
							f = new File(sc.getRealPath("/admin/FCKeditor/"+files[i]));
							if (f.exists() && f.canWrite())
							{
								f.delete();
							}
						}
					}
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}

				//skopiruj vsetky subory
				moveDir("/", version, sc);
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		//zmaz stare class subory
		List<String> files = new ArrayList<>();
		files.add("/WEB-INF/classes/org/apache/commons/fileupload/MultipartStream$IllegalBoundaryException.class");
		files.add("/WEB-INF/classes/org/apache/commons/fileupload/MultipartStream$MalformedStreamException.class");
		files.add("/WEB-INF/classes/org/apache/commons/fileupload/MultipartStream.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/SmsAddressBookBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/SmsTemplateBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/MultilangBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/uschovna/XhrFileUploadServlet.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/uschovna/XhrFileUploadServlet$PartialUploadHolder.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/uschovna/XhrFileUploadServlet$PathHolder.class");
		//nahradene za sk.iway.iwcm.common.ImageGallery
		files.add("/WEB-INF/classes/sk/iway/iwcm/gallery/ImageTools.class");
		//zmena logovania, zmazanie starych, prechod na log4j
		files.add("/WEB-INF/classes/simplelog.properties");
		files.add("/WEB-INF/classes/commons-logging.properties");
		//ak nezmazeme zostane stara verzia ktora potrebuje collections ktore uz nemame
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/spring/HelloRestServiceController.class");

		//2023
		files.add("/WEB-INF/classes/com/");
		files.add("/WEB-INF/classes/it/");
		files.add("/WEB-INF/classes/mp4/");
		files.add("/WEB-INF/classes/net/");
		files.add("/WEB-INF/classes/za/");
		files.add("/WEB-INF/classes/org/apache/axis/");
		files.add("/WEB-INF/classes/org/apache/commons/httpclient/");
		files.add("/WEB-INF/classes/org/apache/struts/");
		files.add("/WEB-INF/classes/org/arabidopsis/");
		files.add("/WEB-INF/classes/org/displaytag/");
		files.add("/WEB-INF/classes/org/eclipse/");
		files.add("/WEB-INF/classes/org/infohazard/");
		files.add("/WEB-INF/classes/org/jcrontab/");
		files.add("/WEB-INF/classes/org/objectstyle/");
		files.add("/WEB-INF/classes/org/outerj/");
		files.add("/WEB-INF/classes/org/tempuri/");
		files.add("/WEB-INF/classes/org/w3c/");
		files.add("/WEB-INF/classes/org/xml/");

		files.add("/WEB-INF/classes/sk/iway/apollo/");
		files.add("/WEB-INF/classes/sk/iway/asterisk/");
		files.add("/WEB-INF/classes/sk/iway/cals/");
		files.add("/WEB-INF/classes/sk/iway/city_university/");
		files.add("/WEB-INF/classes/sk/iway/ckm/");
		files.add("/WEB-INF/classes/sk/iway/columbex/");
		files.add("/WEB-INF/classes/sk/iway/cps/");
		files.add("/WEB-INF/classes/sk/iway/cykloportal/");
		files.add("/WEB-INF/classes/sk/iway/dekampo/");
		files.add("/WEB-INF/classes/sk/iway/detivpohybe/");
		files.add("/WEB-INF/classes/sk/iway/dexia/");
		files.add("/WEB-INF/classes/sk/iway/dovera/");
		files.add("/WEB-INF/classes/sk/iway/druchema/");
		files.add("/WEB-INF/classes/sk/iway/dsoft/");
		files.add("/WEB-INF/classes/sk/iway/electroworld/");
		files.add("/WEB-INF/classes/sk/iway/enviro_intra/");
		files.add("/WEB-INF/classes/sk/iway/essox/");
		files.add("/WEB-INF/classes/sk/iway/express/");
		files.add("/WEB-INF/classes/sk/iway/ferona/");
		files.add("/WEB-INF/classes/sk/iway/fincop/");
		files.add("/WEB-INF/classes/sk/iway/generali/");
		files.add("/WEB-INF/classes/sk/iway/glanc/");
		files.add("/WEB-INF/classes/sk/iway/gtsint/");
		files.add("/WEB-INF/classes/sk/iway/helpdesk/");
		files.add("/WEB-INF/classes/sk/iway/i6shop/");
		files.add("/WEB-INF/classes/sk/iway/infolib/");
		files.add("/WEB-INF/classes/sk/iway/ing/");
		files.add("/WEB-INF/classes/sk/iway/jasr/");
		files.add("/WEB-INF/classes/sk/iway/keep/");
		files.add("/WEB-INF/classes/sk/iway/lesy/");
		files.add("/WEB-INF/classes/sk/iway/livage/");
		files.add("/WEB-INF/classes/sk/iway/magma/");
		files.add("/WEB-INF/classes/sk/iway/mamina/");
		files.add("/WEB-INF/classes/sk/iway/measureshop/");
		files.add("/WEB-INF/classes/sk/iway/mediamonitoring/");
		files.add("/WEB-INF/classes/sk/iway/metroprojekt/");
		files.add("/WEB-INF/classes/sk/iway/mobileapp/");
		files.add("/WEB-INF/classes/sk/iway/pamas/");
		files.add("/WEB-INF/classes/sk/iway/pfizer/");
		files.add("/WEB-INF/classes/sk/iway/pfizer_cz/");
		files.add("/WEB-INF/classes/sk/iway/playstation/");
		files.add("/WEB-INF/classes/sk/iway/plus7/");
		files.add("/WEB-INF/classes/sk/iway/pluska/");
		files.add("/WEB-INF/classes/sk/iway/pss/");
		files.add("/WEB-INF/classes/sk/iway/sba/");
		files.add("/WEB-INF/classes/sk/iway/slovanet/");
		files.add("/WEB-INF/classes/sk/iway/slovnaft/");
		files.add("/WEB-INF/classes/sk/iway/sony/");
		files.add("/WEB-INF/classes/sk/iway/sopk/");
		files.add("/WEB-INF/classes/sk/iway/strategie/");
		files.add("/WEB-INF/classes/sk/iway/syts/");
		files.add("/WEB-INF/classes/sk/iway/tento/");
		files.add("/WEB-INF/classes/sk/iway/ugkk/");
		files.add("/WEB-INF/classes/sk/iway/ulib/");
		files.add("/WEB-INF/classes/sk/iway/uninova/");
		files.add("/WEB-INF/classes/sk/iway/victory/");
		files.add("/WEB-INF/classes/sk/iway/vszp/");
		files.add("/WEB-INF/classes/sk/iway/vszpi/");
		files.add("/WEB-INF/classes/sk/iway/vubintra/");
		files.add("/WEB-INF/classes/sk/iway/vubweb/");
		files.add("/WEB-INF/classes/sk/iway/vuc_nitra/");
		files.add("/WEB-INF/classes/sk/iway/vuc_presov/");
		files.add("/WEB-INF/classes/sk/iway/vuc_zilina/");
		files.add("/WEB-INF/classes/sk/iway/vucza/");
		files.add("/WEB-INF/classes/sk/iway/wtc/");
		files.add("/WEB-INF/classes/sk/iway/zscargo/");

		//problemy po aktualizacii WJ
		files.add("/WEB-INF/classes/sk/iway/iwcm/database/ComplexQueryJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/database/ComplexQueryJUnit$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/xls/DefaultEntityImporter$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/xls/ImportCS.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/xls/XlsToHtmlCacheBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/xls/XlsChartAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/xls/ExcelImportJXLTest.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/xls/XlsToHtmlTable.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/form/FormResultsParserJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/form/FormSaveNoteAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/form/FormDelAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/form/FormToPdfAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/form/FormListAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/form/FormEditActionBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/form/FormHtmlAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/form/FormEditNoteAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/form/FormNoteForm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/calendar/CalendarAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/calendar/ListEventsAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/calendar/DeleteEventAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/calendar/SaveEventAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/calendar/EditEventAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/inquiry/InquiryCreateForm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/inquiry/InquiryCreateAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/inquiry/InquiryDeleteAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/inquiry/InquiryAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/ebanking/epayments/class_structure.uxf");
		files.add("/WEB-INF/classes/sk/iway/iwcm/ebanking/epayments/CsobTlacitkoInformation.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/ebanking/epayments/ebanking_workflow.uxf");
		files.add("/WEB-INF/classes/sk/iway/iwcm/ebanking/MoneyJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/ebanking/MoneyJUnit$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/AdminlogJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/test/BaseWebjetTest.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/test/AssertionsJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/test/TestRequest.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/test/Equal.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/test/BaseWebjetTest$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/test/Assertions$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/test/Assertions.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/test/Assertions$2.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/test/AssertionsJUnit$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/stripes/doc/UserSettingsActionBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/XmlSaxImporter.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/gallery/UploadAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/gallery/MultipleOperationsAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/gallery/PhotoGalleryAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/orgtree/UserVertexShapeTransformer.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/orgtree/UserLabelTransformer.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/orgtree/OrgTreeBuilder.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/orgtree/UserDetailsNameComparator.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/pdf/AbstractExample.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/pdf/AcroFormTools.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/utils/MapUtilsJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/utils/PairJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/utils/MapUtilsJUnit$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/tags/CalendarTag.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/tags/AddStatTag.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/tags/MultilangTextareaTag.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/tags/MultiUploadTag.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/tags/SendEmailTag.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/tags/ToJSTag.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/tags/DocMenuTag.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/tags/MultilangTextTag.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/tags/NetscapeTag.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/tags/TopTenTag.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/tags/NotNetscapeTag.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/alfresco/AlfrescoRestServiceController.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/alfresco/AlfrescoRecentFiles$RecentFile.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/alfresco/ExcelTemplate.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/alfresco/AlfrescoTask.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/alfresco/AlfrescoNode.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/alfresco/AlfrescoRecentFiles.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/alfresco/AlfrescoFile.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/ResponseHeaderFilter.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/ssl/DummySSLSocketFactory.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/ssl/DummyTrustManager.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/UrlRedirectActionBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/msg/AdminMessage.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/msg/_AdminMessage.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/stripes/MultipartWrapper$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/ntlm/AuthenticatedRequest.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/ntlm/NtlmLogonAction$RedirectedException.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/ntlm/Negotiate.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/ntlm/NtlmHttpFilter.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/MultilangDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/adminlog/AdminlogNotifyAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/adminlog/AdminlogNotifyBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/cayenne/postgre/PostgreSQLAdapter.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/cayenne/oracle/OracleAdapter.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/cayenne/mysql/MySQLAdapter.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/cayenne/PkGenerator.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/cayenne/mssql/SQLServerAdapter.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/FilePartNoTransferEncoding.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/jpa/WebJETInitializationHelper.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/fulltext/lucene/SynonymExpansionFilter.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/fulltext/FulltextSearchTest$2.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/fulltext/Indexed$Callback.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/fulltext/indexed/Tickets$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/fulltext/indexed/Documents$3$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/fulltext/indexed/Documents$3.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/fulltext/indexed/Tickets.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/fulltext/FulltextSearchTest$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/fulltext/Indexed.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/fulltext/FulltextSearchTest.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/fulltext/jobs/TicketsIndexing.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/captcha/CaptchaDictionaryAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/Modules.java.bak");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/CayenneServletConfiguration.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/StringPartNoTransferEncoding.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/model/_UrlRedirect.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/packager/Packager.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/webdav/IwSecurityManager$AccessRight.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/webdav/FsDirectoryResource.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/webdav/MiltonWrapper.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/webdav/FsResource.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/webdav/FileSystemResourceFactory.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/webdav/FsFileResource.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/webdav/MiltonWrapper$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/webdav/IwSecurityManager.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/webdav/FsMemoryLockManager$CurrentLock.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/webdav/FsMemoryLockManager.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/ReqTestAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/CayenneDataSourceFactory.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/QueryString$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/Portal$PortletRegistrationListener.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/util/PortletUtils.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/driver/DriverConfigurationFactory.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/driver/IwayPortalDriverServlet.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/driver/IwayPortalUrlParser.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/Portal.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/QueryString.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/services/impl/IwayUserInfoService.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/services/impl/IwayPortletPreferencesService$2.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/services/impl/IwayPortletPreferencesService.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/services/impl/IwayPortletPreferencesService$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/services/impl/IwayPortletPreferencesService$3.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/services/IwayOptionalContainerServices.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/portlet/services/IwayRequiredContainerServices.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/monitoring/MemoryMeasurementJUnit$1TestMemoryMeasurement.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/monitoring/MemoryMeasurementJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/monitoring/MonitoringClusterJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/monitoring/ExecutionTimeMonitoringJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/UrlRedirect.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/sass/SassFileMonitor.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/sass/FileMonitor.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/help/SearchResultBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/help/SearchHelpAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/help/SearchManager.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/cron/JCrontabSQLSource.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/cron/CronJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/cron/ClusteredCrontabEntryBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/cron/CrontabAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/spring/SpringUrlMapping$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/system/spring/SpringSecurityConf$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/LogoffAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/basket/ElectronicPayments$PostBankMethodInformation.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/basket/ElectronicPayments$TatraPayMethodInformation.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/basket/ElectronicPayments$SporoPayMethodInformation.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/basket/_BasketItem.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/basket/_BasketInvoice.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/basket/BasketItem.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/basket/ElectronicPayments.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/basket/BasketInvoice.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/basket/ElectronicPayments$PaymentMethodInformation.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/basket/StavyObjednavok$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/basket/ElectronicPayments$VubMethodInformation.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/domainRedirects/DomainRedirectAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/KurzyDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/uschovna/UploadFileAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/uschovna/DeleteOldFilesCronJob.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/SearchFactory.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/SearchFactory$SearchType.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/GroupSubSearch$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/SpotlightSearch$2.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/FileSubSearch.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/ConfigSubSearch$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/SubSearch.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/Icon.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/GroupSubSearch.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/UserSubSearch.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/SpotlightSearch$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/SearchResult.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/FileIndexer.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/FileSubSearch$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/DocumentSubSearch$2.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/FileIndexer$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/UserSubSearch$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/DocumentSubSearch.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/Ranker.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/DocumentSubSearch$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/ConfigSubSearch.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/SpotlightSearch.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/FileIndexer$2.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/SearchFactory$NullSubSearch.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/spotlight/TemplateSubSearch.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/proxy/ProxyByHttpClient.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/proxy/ProxyBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/proxy/ProxyAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/Entity.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/ChatDB$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/Session.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/ChatRoomAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/SystemMessage.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/Message.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/Room.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/User.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/ChatController.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/HtmlChatRoom.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/ChatDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/ChatRoomForm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/chat/ModeratedRoom.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/dataDeleting/DataDeletingAjaxAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/dataDeleting/DataDeletingBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/pageUpdateInfo/PageUpdateInfoForm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/pageUpdateInfo/PageUpdateInfoDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/pageUpdateInfo/PageUpdateInfoAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/sita/NewsItem.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/sita/SitaParser.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/bazar/BazarDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/bazar/BazarGroupAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/bazar/BazarAdvertisementAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/bazar/BazarGroupBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/bazar/BazarAdvertisementBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/graph/GraphAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/graph/XlsParser.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/graph/GraphTools.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/webserices/GetDoc.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/importZipFile/ImportZipFileAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/importZipFile/ImportZipFileForm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/importZipFile/ImportZipFileAction$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/reservation/ReservationObjectPriceAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/wiki/WikiAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/welcome/WallAjaxAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/todo/ToDoAjaxAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/stat/StatViewChartAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/stat/StatViewsDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/stat/StatComparator.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/page/EditPage.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/page/ViewPage.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/page/ListPage.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/page/CommonPage.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/page/DetailPage.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/InventoryDetailEditAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/Message.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/Type.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/InventoryException.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/Type$TypeBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/InventoryEditAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryDetailDB$2.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryLogBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryLogDB$2.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryDetailBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryDetailDB$4.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryLogDB$4.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryDetailDB$3.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryLogDB$3.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryLogDB$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryDetailDB$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryLogDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/CommonBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryFilter.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryDetailDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/inventory/db/InventoryBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/SmsDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/tips/TipsAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/tips/TipsDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/tips/TipsExcelImport.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/tips/TipBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/SmsTemplateBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/zmluvy/ZmluvyDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/zmluvy/PrilohaBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/zmluvy/ZmluvyAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/zmluvy/ZmluvyDB$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/zmluvy/ZmluvaBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/zmluvy/ImportZmluv.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/docman/DocmanFilter.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/docman/WebDavOpenAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/docman/DocmanDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/docman/UploadAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/docman/DocmanAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/export/ExportDatAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/insertScript/InsertScriptAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/convert2pdf/Convert2pdf.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/KurzBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/appCache/AppCacheBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/appCache/AppCacheFileDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/appCache/AppCachePageBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/appCache/AppCachePageDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/appCache/AppCacheActionBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/appCache/AppCacheDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/appCache/AppCacheFileBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/banner/BannerAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/banner/model/_BannerStatViews.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/banner/model/_BannerBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/banner/model/_BannerStatClicks.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/banner/model/BannerForm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/banner/model/_BannerGroupBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/InitServlet$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/stat/StatNewDBTest.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/stat/BrowserDetectorJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/stat/ChartAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/stat/heat_map/HeatMapJUnit$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/stat/heat_map/HeatMapJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/stat/ChartNewAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/mobile/Forms.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/mobile/GroupsAjaxAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/mobile/EditorAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/mobile/FileEditorAjaxAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/mobile/EditorAjaxAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/SocialAuthenticationAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/XLSServlet.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/RegAlarm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/ListUsersAction$5.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/EditUserAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/ListUsersAction$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/AlarmDetails.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/EditUserGroupAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/ListUsersAction$3.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/AuthForm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/OutlookImport.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/ListUsersAction$4.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/RegUpdateAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/SaveUserAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/ListUsersAction$6.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/ImportAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/ListUsersAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/UserFormPfizer.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/ListUsersAction$2.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/PasswordSecurityJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/SaveUserGroupAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/RegUserAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/MultipleOperationsAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/SocialAuthSuccessAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/ChangeUserAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/STokenizer.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/RegUserPfizerAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/users/UserForm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/filebrowser/EditAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/filebrowser/UnzipAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/filebrowser/DeleteAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/filebrowser/SaveAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/Script.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/profesia/Offer.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/profesia/OfferDetails.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/profesia/OfferList.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/DocDB$AjcClosure89.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/ShowDocAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/LinkCheckAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/DocDB$AjcClosure91.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/GroupsListAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/DocDB$AjcClosure103.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/ListDocAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/DocDB$AjcClosure101.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/DocDB$AjcClosure129.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/DeleteAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/DocDB$AjcClosure99.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/TemplatesForm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/GroupsListAction.java.bak");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/ListTempsAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/DocDetails$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/DocDB$AjcClosure83.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/DocDB$AjcClosure131.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/FileSyncAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/NewGroupAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/NewGroupForm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/GroupsListAction$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/SpeedTestAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/SkkEuroLoader.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/XmlSaxImport.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/sync/SyncAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/sync/inport/NumberedTest.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/SendMail$1Authenticator.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/helpers/BeanDiffJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/UsrLogonSMSAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/intranet/reminder/ICalActionBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/intranet/reminder/ReminderDocumentDB.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/intranet/reminder/ReminderDocumentDB$ReminderItem.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/intranet/reminder/ReminderDocumentDB$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/intranet/reminder/ReminderDocumentDB$ReminderMapper.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/intranet/reminder/DocDetailsConverter.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/SendSMS.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/PreviewAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/SaveTemplateAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/EditorAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/DirAction$2.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/templates/TemplateWordpress$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/templates/TemplateHtml$1.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/templates/TemplateExternal.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/templates/TemplateHtml.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/templates/TemplateWordpress.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/templates/ImportTemplatesAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/HTMLCleanAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/editor/FckStylesAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/PKeyGeneratorJUnit.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/dmail/DomainLimitsActionBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/dmail/EmailCsvImport.class");

		files.add("/WEB-INF/classes/sk/iway/spirit/ProjectForm.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/BranchAction.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/ServiceComparator.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/CustomerForm.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/CaseStudyForm.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/CaseStudyAction.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/RefDB.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/JobAction.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/JobForm.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/RefBranch.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/_RefCustomerBranch.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/RefJob.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/RefService.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/_RefProject.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/_RefCustomer.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/_RefJob.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/RefCustomerBranch.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/RefCustomer.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/RefProject.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/RefCaseStudy.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/_RefCaseStudy.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/RefCustomer$1.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/RefBranch$1.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/_Media.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/_RefBranch.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/RefCustomer$2.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/RefProject$1.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/model/_RefService.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/BranchForm.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/ProjectAction.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/CustomerAction.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/ServiceAction.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/ServiceForm.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/MediaAction.class");
		files.add("/WEB-INF/classes/sk/iway/spirit/MediaForm.class");
		files.add("/WEB-INF/classes/sk/iway/FixedSizeList.class");
		files.add("/WEB-INF/classes/sk/iway/Downloader.class");
		files.add("/WEB-INF/classes/sk/iway/Downloader$DownloadResult.class");
		files.add("/WEB-INF/classes/sk/iway/tags/RndTag.class");
		files.add("/WEB-INF/classes/sk/iway/tags/CssTag.class");
		files.add("/WEB-INF/classes/sk/iway/ImageInfo.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/payments/UserPaymentBean.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/payments/UserPaymentDB.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/app/AppManager.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/app/AppManager$1.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/app/AppBean.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/CloudTools.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/sport/CloudSportMatchDB.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/sport/CloudSportMatchBean.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/sport/CloudSportComparator.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/styles/SaveLogoAjaxAction.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/styles/CustomHeaderFooterAction.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/styles/StyleTools.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/cleanup/CleanupCronJob$1.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/cleanup/CleanupCronJob.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/registration_domain/RestJsonReaderWebSupport.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/registration_domain/CompareByLength.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/registration_domain/RestDomainPrices.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/registration_domain/DomainPrices.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/registration_domain/CloudChangeDomainDB.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/registration_domain/RegisterDomainApiTools.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/registration_domain/Domain.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/registration_domain/RestHostingBean.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/registration_domain/CloudChangeDomainBean.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/registration_domain/RestApiWebSupport.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/registration_domain/JsonReaderWebSupport.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/FilePathTools.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/users/LogonAction.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/extended/ExtendedServicesAction.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/services/UserServiceAction.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/services/UserServiceDB.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/services/UserServiceBean.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/mailing/CloudSendedWarningBean.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/mailing/CloudSendedWarningDB.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/mailing/CloudWarningMailingBean.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/mailing/CloudWarningMailingDB.class");
		files.add("/WEB-INF/classes/sk/iway/cloud/mailing/CloudWarningMailing.class");
		files.add("/WEB-INF/classes/sk/iway/FtpDownloader$1.class");
		files.add("/WEB-INF/classes/sk/iway/FtpDownloader.class");
		files.add("/WEB-INF/classes/sk/iway/upload/MultipartIterator.class");
		files.add("/WEB-INF/classes/sk/iway/Downloader$1.class");
		files.add("/WEB-INF/classes/sk/iway/displaytag/RowCountDaysDecorator.class");
		files.add("/WEB-INF/classes/WTCMap.map.xml");
		files.add("/WEB-INF/classes/VUCZilinaMap.map.xml");
		files.add("/WEB-INF/classes/WebJETMap.map.xml");
		files.add("/WEB-INF/classes/SpiritRefMap.map.xml");
		files.add("/WEB-INF/classes/text_de-sopk.properties");
		files.add("/WEB-INF/classes/cayenne.xml");
		files.add("/WEB-INF/classes/cvu/html/TagToken.jbx");
		files.add("/WEB-INF/classes/text-sopk.properties");
		files.add("/WEB-INF/classes/text_en-sopk.properties");
		files.add("/WEB-INF/classes/EekonomiaMap.map.xml");
		files.add("/WEB-INF/classes/ApplicationResources.properties");

		files.add("/WEB-INF/classes/sk/iway/iwcm/sync/GetObjectAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/LogonForm.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/SetUsrLogonAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/UsrLogonAction.class");

		files.add("/WEB-INF/classes/sk/iway/iwcm/components/qa/QuestionsAnswersEditorFields.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/qa/QuestionsAnswersEntity.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/qa/QuestionsAnswersRepository.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/qa/QuestionsAnswersRestController.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/qa/QuestionsAnswersService.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/qa/AddAction.class");

		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/ApproveAction.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/doc/ApproveDelAction.class");

		files.add("/WEB-INF/classes/sk/iway/iwcm/components/monitoring/MonitoringActualBean.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/monitoring/MonitoringAggregator.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/monitoring/MonitoringEntity.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/monitoring/MonitoringManager.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/monitoring/MonitoringRepository.class");
		files.add("/WEB-INF/classes/sk/iway/iwcm/components/monitoring/MonitoringRestController.class");

		files.add("/WEB-INF/classes/sk/iway/iwcm/system/StrutsRedirectServlet.class");

		files.add("/admin/elFinder/js/i18n/elfinder.cs.js.jsp");
		files.add("/admin/elFinder/js/i18n/elfinder.sk.js.jsp");

		for (String file : files)
		{
			File f = new File(sc.getRealPath(file));
			if (f.exists() && f.canWrite())
			{
				if (INSTALL_NAME==null || file.contains("/"+INSTALL_NAME+"/")==false) {
					if (f.isDirectory()) {
						deleteDirTree(file, notDeleted, version, sc);
					} else {
						deleteFile(f, file, notDeleted, version, sc);
					}
				}
			}
		}

		//zmaz subory z WJ7 ktore nefunguju vo WJ8
		//deleteDirTree("/WEB-INF/classes/sk/iway/iwcm/components/banner/model/", sc);
		deleteDirTree("/WEB-INF/classes/sk/iway/magzilla/", notDeleted, version, sc);
        deleteDirTree("/WEB-INF/classes/sk/iway/kanban/", notDeleted, version, sc);
		deleteDirTree("/WEB-INF/classes/sk/iway/iwcm/catalog/", notDeleted, version, sc);
		deleteDirTree("/WEB-INF/classes/sk/iway/iwcm/components/cestovka/", notDeleted, version, sc);
		deleteDirTree("/WEB-INF/classes/sk/iway/iwcm/components/db_browser/", notDeleted, version, sc);
		//deleteDirTree("/WEB-INF/classes/sk/iway/iwcm/components/dictionary/model/", notDeleted, sc);
		deleteDirTree("/components/bazar/", notDeleted, version, sc);
		deleteDirTree("/components/chat/", notDeleted, version, sc);
		deleteDirTree("/components/page_update_info/", notDeleted, version, sc);
		deleteDirTree("/components/magzilla/", notDeleted, version, sc);
		deleteDirTree("/components/tips/", notDeleted, version, sc);
		//fix verzie org.json - presun na JAR archiv
		deleteDirTree("/WEB-INF/classes/org/json/", notDeleted, version, sc);

		if(!notDeleted.isEmpty())
		{
			//list not deleted files
			BufferedWriter writer = null;
	        try {
	            //create a temporary file
	            File logFile = new File(sc.getRealPath("/WEB-INF/update/error-log.txt"));
	            // This will output the full path where the file will be written to...
	            writer = new BufferedWriter(new FileWriter(logFile));
	            for(File f : notDeleted)
	            {
	            	writer.write(f.getAbsolutePath()); writer.newLine();
	            }
	        } catch (Exception e) {
	            sk.iway.iwcm.Logger.error(e);
	        } finally {
	            try {
	                // Close the writer regardless of what happens...
	                if (writer!=null) writer.close();
	            } catch (Exception e) {
	            }
	        }
		}

		//zachova web-update.xml
		copyFile("/WEB-INF/web.xml", "/WEB-INF/web-update.xml", sc);

		//skopiruj naspat web.xml

		//kontrola noveho web.xml suboru
		File webXML2023 = new File(sc.getRealPath("/WEB-INF/web-v2023.xml"));
		File beforeWebXML = new File(sc.getRealPath("/WEB-INF/web-before_update.xml"));
		if (webXML2023.exists() && beforeWebXML.exists())
		{
			//nacitaj si obsah aktualneho web-before_update.xml
			String actualWebXML = readFileContent(beforeWebXML, "utf-8");
			if (actualWebXML.contains("stripes")==false || actualWebXML.contains("web-app_2_4.xsd"))
			{
				//2.4 web.xml je potrebne aktualizovat na 3.0
				moveFile("/WEB-INF/web-v2023.xml", "/WEB-INF/web.xml", sc);
			}
			else
			{
				moveFile("/WEB-INF/web-before_update.xml", "/WEB-INF/web.xml", sc);
			}
		}
		else
		{
			moveFile("/WEB-INF/web-before_update.xml", "/WEB-INF/web.xml", sc);
		}

		//zmen mu datum
		File webXmlFile = new File(sc.getRealPath("/WEB-INF/web.xml"));
		webXmlFile.setLastModified((new java.util.Date()).getTime());

		//restartni sa
		restart(sc);

		Logger.println(InitServlet.class,"Update init done, version="+version);
	}

	private static void restart(ServletContext sc)
	{
		touch(sc, "/WEB-INF/classes/sk/updater/InitServlet.class");
		touch(sc, "/WEB-INF/web.xml");
		touch(sc, "/WEB-INF/classes/sk/iway/iwcm/InitServlet.class");
		touch(sc, "/WEB-INF/classes/sk/updater/ResponseServlet.class");
	}

	private static boolean touch(ServletContext sc, String url) {
		boolean ret = false;
		File f = new File(sc.getRealPath(url));
		if (f.exists())
		{
			System.out.println("RESTART request update.InitServlet " + f.getAbsolutePath());
			f.setLastModified(System.currentTimeMillis());
			ret = true;
		}
		return ret;
	}

	/**
	 * Presunie subor
	 * @param fromPath
	 * @param toPath
	 * @param sc
	 * @return
	 */
	private static boolean moveFile(String fromPath, String toPath, ServletContext sc)
	{
		//if (toPath.indexOf("web.xml")==-1) return(true);

		if (toPath.indexOf("/sk/updater/")!=-1)
		{
			//sk.updater.* sa nesmie aktualizovat
			return(true);
		}

		boolean renamed = false;

		try
		{
			renamed = copyFile(fromPath, toPath, sc);

			try
			{
				//skus ho zmazat
				//fromFile.delete();
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return renamed;
	}

	private static boolean copyFile(String fromPath, String toPath, ServletContext sc)
	{
		boolean copyOK = false;

		Logger.println(InitServlet.class,"   Copy file: " + fromPath + " -> " + toPath);

		if (toPath.indexOf("/sk/updater/")!=-1)
		{
			//sk.updater.* sa nesmie aktualizovat
			return(true);
		}

		try
		{
			File fromFile = new File(sc.getRealPath(fromPath));
			File toFile = new File(sc.getRealPath(toPath));

			if (toFile.exists())
			{
				//toFile.delete();
				//toto nemozeme kvoli clustru a symlinkom na web.xml
			}
			File toFileParent = toFile.getParentFile();
			if(toFileParent.exists()==false)
			{
				if(toFileParent.mkdirs() == false)
				{
					Logger.println(InitServlet.class," [FAIL] - can't create parent dir");
					return false;
				}
			}
			if(toFile.exists()==false && toFile.createNewFile() == false)
			{
				Logger.println(InitServlet.class," [FAIL] - can't create new file");
				return false;
			}

			FileInputStream inStream = new FileInputStream(fromFile);
			FileOutputStream out = new FileOutputStream(toFile);

			int c;
			byte[] buff = new byte[150000];
			while ((c = inStream.read(buff)) != -1)
			{
				out.write(buff, 0, c);
			}
			out.close();
			inStream.close();

			copyOK = true;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		if (copyOK)
		{
		   Logger.println(InitServlet.class," [OK]");
		}
		else
		{
			Logger.println(InitServlet.class," [FAIL]");
		}

		return copyOK;
	}

	/**
	 * Rekurzivna funkcia, ktora presuva obsah adresara
	 * @param path
	 */
	private static void moveDir(String path, String version, ServletContext sc)
	{
		Logger.println(InitServlet.class,"Move dir: " + path);

		String srcDir = "/WEB-INF/update/"+version+path;
		File srcDirFile = new File(sc.getRealPath(srcDir));

		File[] files = srcDirFile.listFiles();
		int size = files.length;
		int i;

		for (i=0; i<size; i++)
		{
			if (files[i].isDirectory())
			{
				//rekurzia
				moveDir(path + files[i].getName() + "/", version, sc);
			}
			else
			{
				//presun subor
				moveFile(srcDir+files[i].getName(), path+files[i].getName(), sc);
			}
		}

	}

	/**
	 * Vymaze subory z normalneho adresara, ktore sa nenachadzaju v aktualizacii
	 * @param path
	 * @param version
	 * @param sc
	 */
	private static void deleteUnwantedFiles(String path, String version, Collection<File> notDeleted, ServletContext sc)
	{
		Logger.println(InitServlet.class,"deleteUnwantedFiles dir: " + path);

		String updateDir = "/WEB-INF/update/"+version+path;
		File updateDirFile = new File(sc.getRealPath(updateDir));

		if (updateDirFile.exists()==false || updateDirFile.isDirectory()==false)
		{
			return;
		}

		File srcDirFile = new File(sc.getRealPath(path));
		File[] srcFiles = srcDirFile.listFiles();


		File[] updateFiles = updateDirFile.listFiles();

		int size = updateFiles.length;
		Hashtable<String, String> updateTable = new Hashtable<>();
		int i;
		for (i=0; i<size; i++)
		{
			if (updateFiles[i].isFile())
			{
				updateTable.put(updateFiles[i].getName(), updateFiles[i].getAbsolutePath());
			}
		}

		//pridaj tam este kniznice z custom lib adresara
		size = srcFiles.length;
		for (i=0; i<size; i++)
		{
			if (srcFiles[i].isDirectory())
			{
				//rekurzia
				//moveDir(path + files[i].getName() + "/", version, sc);
			}
			else
			{
				//vymaz subor
				if (updateTable.get(srcFiles[i].getName())==null)
				{
					boolean delOK = srcFiles[i].delete();
					Logger.println(InitServlet.class,"MAZEM: " + srcFiles[i].getAbsolutePath()+" ok="+delOK);
					if(!delOK) notDeleted.add(srcFiles[i]);

				}
			}
		}
	}

	/**
	 * Nakopiruje do noveho LIB adreasra to co je v /WEB-INF/lib-custom
	 * @param version
	 * @param sc
	 */
	private static void addCustomLib(String version, ServletContext sc)
	{
		String customDir = "/WEB-INF/lib-custom/";

		String updateDir = "/WEB-INF/update/"+version+"/WEB-INF/lib/";

		File libCustom = new File(sc.getRealPath(customDir));
		if (libCustom.exists()==false || libCustom.canRead()==false) return;

		//skopiruj subory
		File[] files = libCustom.listFiles();
		for (File f : files)
		{
			if (f.isFile() && f.canRead())
			{
				copyFile(customDir+f.getName(), updateDir+f.getName(), sc);
			}
		}
	}

	private static String readFileContent(File f, String encoding)
	{
		StringBuilder contextFile = new StringBuilder();

		if (encoding == null) encoding = "windows-1250";

		try
		{
			if (f.exists() && f.canRead())
			{

				InputStreamReader isr = new InputStreamReader(new FileInputStream(f), encoding);
				char[] buff = new char[64000];
				int len;
				while ((len = isr.read(buff))!=-1)
				{
					contextFile.append(buff, 0, len);
				}
				isr.close();
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		//System.out.println("!!!! in fileContent "+url);
   	return(contextFile.toString());
	}

	private static boolean deleteDirTree(String url, Collection<File> notDeleted, String version, ServletContext sc)
	{
		File file = new File(sc.getRealPath(url));

		boolean result = true;

		if (file.exists() && file.isDirectory())
		{
			System.out.println("deleting: " + url);

			int size;
			int i;

			try
			{
				if (file.isDirectory())
				{
					File f;
					File[] files = file.listFiles();
					if (files != null)
					{
						size = files.length;
						for (i = 0; i < size; i++)
						{
							f = files[i];
							if (f.isDirectory())
							{
								result = deleteDirTree(url+f.getName()+"/", notDeleted, version, sc);
							} else
							{
								result = deleteFile(f, url+f.getName(), notDeleted, version, sc);
								if (result==false) notDeleted.add(f);
							}
						}
					}
					result = file.delete();
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		return (result);
	}

	private static boolean deleteFile(File f, String url, Collection<File> notDeleted, String version, ServletContext sc)
    {
        if (f.exists())
        {
			//create backup
			copyFile(url, "/WEB-INF/update/"+version+"_backup"+url, sc);

            System.out.println("Deleting " + url);
            boolean ok = f.delete();
			if (ok==false) notDeleted.add(f);
			return ok;
        }

        return false;
    }
}
