package sk.iway.iwcm.doc;


import static sk.iway.iwcm.doc.SearchAction.getInputParamMode;
import static sk.iway.iwcm.doc.SearchAction.getParamAttribute;
import static sk.iway.iwcm.doc.SearchAction.getParamAttributeUnsafe;
import static sk.iway.iwcm.doc.SearchAction.getTextToFind;
import static sk.iway.iwcm.doc.SearchAction.hasInputParams;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.util.Version;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.fulltext.FulltextSearch;
import sk.iway.iwcm.system.fulltext.LuceneQuery;
import sk.iway.iwcm.system.fulltext.indexed.Documents;
import sk.iway.iwcm.system.fulltext.lucene.AnalyzerFactory;
import sk.iway.iwcm.system.fulltext.lucene.Lemmas;
import sk.iway.iwcm.system.fulltext.lucene.LuceneUtils;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UsersDB;

/**
 * LuceneSearchAction.java
 *
 *@Title webjet7
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2011
 *@author $Author: jeeff thaber $
 *@version $Revision: 1.3 $
 *@created Date: 30.3.2011 16:33:22
 *@modified $Date: 2004/08/16 06:26:11 $
 */
public class LuceneSearchAction
{
	protected LuceneSearchAction() {
		//utility class
	}

	/**
	 *
	 * @param request
	 * @return
	 */
	public static String search(HttpServletRequest request)
	{
		DebugTimer dt = new DebugTimer("LuceneSearchAction");

		request.removeAttribute("aList");
		request.removeAttribute("totalResults");

		String language =  PageLng.getUserLng(request);
		Identity user = UsersDB.getCurrentUser(request);
		DocDB docDB = DocDB.getInstance();

		String forward = "success";
		String error = "error";

		boolean english = false;
		if (request.getParameter("lng") != null)
		{
			english = true;
			forward = "english";
		}
		String pForward = request.getParameter("forward");
		if (pForward != null && pForward.endsWith(".jsp"))
		{
			forward = "/templates/" + pForward;
		}
		String searchGroupsParam = getParamAttribute("rootGroup", request,Integer.toString(Constants.getInt("rootGroupId")));
		searchGroupsParam = searchGroupsParam.replace('+', ',');
		String[] groupIdParams = request.getParameterValues("groupId");
		if (groupIdParams != null && groupIdParams.length>0)
		{
			searchGroupsParam = StringUtils.join(groupIdParams,',');
		}

		PageParams pageParams = new PageParams(request);
		boolean searchAlsoProtectedPages = pageParams.getBooleanValue("searchAlsoProtectedPages", false);

		StringBuilder searchFileNameQuery = null;

		StringTokenizer st = new StringTokenizer(searchGroupsParam, ",+; ");
		GroupsDB groupsDB = GroupsDB.getInstance();
		GroupDetails group;
		//tu si poznacim zadane root groups a tie nebudem neskor povazovat za internal folders
		Map<Integer, Integer> rootGroupIdsTable = new Hashtable<>();

		List<DocDetails> docsInGroups = null; //vsetky stranky v zadanych adresaroch
		List<GroupDetails> searchGroupsArray = null;
		List<String> searchGroupNamePaths = null;
		DebugTimer dtt = new DebugTimer("LuceneSearchAction DUPLICITY");
		//ak je TRUE, tak chcem kontrolovat duplicitu pre multikategorie
		boolean checkDuplicity = pageParams.getBooleanValue("checkDuplicity", false);
		while (st.hasMoreTokens())
		{
			try
			{
				//schvalne je tu Integer.parseInt aby to pripadne padlo na exception
				int searchRootGroupId = Integer.parseInt(st.nextToken());

				rootGroupIdsTable.put(Integer.valueOf(searchRootGroupId), Integer.valueOf(1));

				//v stlpci file_name mame ulozenu cestu k adresaru, nieco ako /Slovensky/InterWay/Produkty/Nejaky Produkt/
				//nad tym limitujeme adresare (interne adresare to maju v databaze prazdne)

				GroupDetails fileNameGrp = groupsDB.getGroup(searchRootGroupId);
				if (fileNameGrp != null)
				{
					if (searchFileNameQuery == null) searchFileNameQuery = new StringBuilder("file_name:\"").append(DB.removeSlashes(groupsDB.getGroupNamePath(fileNameGrp.getGroupId()))).append("\"");
					else searchFileNameQuery.append(" OR file_name:\""+DB.removeSlashes(groupsDB.getGroupNamePath(fileNameGrp.getGroupId()))).append("\"");

					if(searchGroupNamePaths == null) searchGroupNamePaths = new ArrayList<>();
					searchGroupNamePaths.add(DB.removeSlashes(groupsDB.getGroupNamePath(fileNameGrp.getGroupId())));
				}

				if(checkDuplicity)
				{
					searchGroupsArray = groupsDB.getGroupsTree(searchRootGroupId, true, false);
					for (GroupDetails searchGroup : searchGroupsArray)
					{
						if (searchGroup != null)
						{
							if(docsInGroups == null) docsInGroups = new ArrayList<>();
							if(docDB == null) docDB = DocDB.getInstance();
							docsInGroups.addAll(docDB.getBasicDocDetailsByGroup(Tools.getIntValue(searchGroup.getGroupId(),0), 0));
						}
					}
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		dtt.diff("after get all docs");
		List<Integer> docIdsNotIn = null;
		if(checkDuplicity)
		{
			docIdsNotIn = new ArrayList<>();
			List<Integer> slavesMasterForDoc = null;

			if(docDB.getSlavesMasterMappings() != null && docsInGroups!=null)
			{
				if(docsInGroups.size() > 0)
				{
					HashMap<Integer, Boolean> docsInGroupsHm = new HashMap<>();
					for(DocDetails dd : docsInGroups) docsInGroupsHm.put(Integer.valueOf(dd.getDocId()), Boolean.TRUE);

					for (Entry<Integer, Boolean> entry : docsInGroupsHm.entrySet())
					{
						if(entry.getValue() != Boolean.FALSE)
						{
							Integer docId = entry.getKey();
							//ziskam zoznam docId, ktore su rovnake k danej stranke
							slavesMasterForDoc = null;

							if(slavesMasterForDoc == null) slavesMasterForDoc = new ArrayList<>();

							Integer masterId = docDB.getSlavesMasterMappings().get(docId);
							if(masterId != null)
							{
								//ak sa master nachadza v zozname stranok, tak ponecham master miesto slave a vymazem vsetky slave stranky danej master stranky
								//v opacnom pripade vymazem vsetky ostatne slaves
								boolean containsMaster = docsInGroupsHm.get(masterId) != null && docsInGroupsHm.get(masterId) == Boolean.TRUE;
								Integer[] slaves = docDB.getMasterMappings().get(masterId);
								if(slaves != null)
								{
									//aby preskocilo pri iteracii master
									if(containsMaster) docsInGroupsHm.put(masterId, Boolean.FALSE);
									for(Integer slave : slaves)
										if(containsMaster || (!containsMaster && slave.intValue() != docId.intValue())) slavesMasterForDoc.add(slave);
								}
							}
							else
							{
								//ak docId je master, tak pridam do duplicitnych vsetky jeho slaves
								Integer[] slaves = docDB.getMasterMappings().get(docId);
								if(slaves != null) slavesMasterForDoc.addAll(Arrays.asList(slaves));
							}
							//odstranim ich zo zonamu stranok
							if(slavesMasterForDoc != null && slavesMasterForDoc.size() > 0)
							{
								for(Integer sm: slavesMasterForDoc)
								{
									if(docsInGroupsHm.get(sm) != null)
									{
										docsInGroupsHm.put(sm, Boolean.FALSE);
										docIdsNotIn.add(sm);
									}
								}
							}
						}
					}
				}
			}
		}
		dtt.diff("after docIdsNotIn");

		int perPage = 10;
		try
		{
			if (getParamAttribute("perpage", request, "10") != null)
			{
				perPage = Integer.parseInt(getParamAttribute("perpage", request, "10"));
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		String rq = request.getParameter("index");
		int index;
		if (rq == null)
		{
			index = 0;
		}
		else
		{
			try
			{
				index = Integer.parseInt(rq);
			}
			catch (Exception e)
			{
				index = 0;
				sk.iway.iwcm.Logger.error(e);
			}
		}

		String words = request.getParameter("words");
		if (Tools.isEmpty(words))
		{
			words = (String)request.getAttribute("words");
		   if (words == null) words = "";
		}

		request.removeAttribute("words");

		String text = request.getParameter("text");
		if (Tools.isNotEmpty(text))
		{
			words = text;
		}

		//aby sme vedeli vnutit slovo pre vyhladavanie
		if (Tools.isNotEmpty((String)request.getAttribute("forceWords")))
		{
			words = (String)request.getAttribute("forceWords");
		}

		String pom;
		words = words.replaceAll("[\"',:();.]"," ").trim();


		//	otestuj, ci su zadane fields parametre
		boolean hasInputParams = hasInputParams(request);

		if ("***".equals((String)request.getAttribute("forceWords"))) //NOSONAR
		{
			words = "";
			hasInputParams = true;
		}

		request.removeAttribute("forceWords");

		// odstranenie jedno - dvoj znakovych slov
		StringTokenizer sTok = new StringTokenizer(words);
		words = "";
		StringBuilder wordsBuilder = new StringBuilder();
		while (sTok.hasMoreElements())
		{
			pom = sTok.nextToken();
			if (pom.length() > 2)
			{
				pom = pom.trim();
				pom = Lemmas.get(language, pom);
				wordsBuilder.append(" +").append(pom);
			}
		}
		words = wordsBuilder.toString().trim();

		//ochrana proti DOS utoku &&
		//prazdne vyhladavanie

		if (isDOS(request) || isEmptySearch(request,words,hasInputParams))
		{
			return forward;
		}

		String sesWord;
		sesWord = (String) request.getAttribute("words");
		if (sesWord == null)
		{
			sesWord = "";
		}
		List<SearchDetails> searchResults;
		int aListCount = 0;
		int totalResults = 0;
		StringBuilder luceneQuery = new StringBuilder();
		//kvoli zvyrazneniu slov potrebujeme len hodnotu vo words (inak nam zvyraznuje aj hodnoty z file_name, password_protected atd)
		String queryHighlight = null;
		try
		{
			luceneQuery.append("(((title:($) OR title:($*)) OR (headings:($) OR headings:($*)) OR (data:($) OR data:($*)))");
			queryHighlight = "(((title:($) OR title:($*)) OR (headings:($) OR headings:($*)) OR (data:($) OR data:($*))))";
			luceneQuery.append(") ");

			if (words.length()==0)
			{
				luceneQuery.delete(0, luceneQuery.length());
				luceneQuery.append("NOT title:").append(LuceneUtils.EMPTY);
			}

			if (Tools.isNotEmpty(searchFileNameQuery))
			{
				//v stlpce file_name mame ulozenu cestu k adresaru, nieco ako /Slovensky/InterWay/Produkty/Nejaky Produkt/
				//nad tym limitujeme adresare
				luceneQuery.append(" AND (").append(searchFileNameQuery).append(") ");
			}

			if (user == null && searchAlsoProtectedPages==false)
			{
				luceneQuery.append(" AND (password_protected:").append(LuceneUtils.EMPTY).append(" OR password_protected:0) ");
			}
			else if (user != null && searchAlsoProtectedPages==false)
			{
				//skupiny pre web stranky
				StringBuilder sqlProtected = new StringBuilder(" AND (password_protected:").append(LuceneUtils.EMPTY).append(" OR password_protected:0");
				StringTokenizer stu = new StringTokenizer(user.getUserGroupsIds(), ",");
				while (stu.hasMoreTokens())
				{
					int groupId = Tools.getIntValue(stu.nextToken(), -1);
					if (groupId > 0)
					{
						sqlProtected.append(" OR password_protected:").append(groupId);
					}
				}
				sqlProtected.append(") ");
				luceneQuery.append(sqlProtected.toString());
			}

			//multidomain
			String domainName = DocDB.getDomain(request);
			/* this will require to uncomment indexing in Documents and also full reindexing
			if (Constants.getBoolean("multiDomainEnabled")) {
				//we must filter by domain using root_group_l1
				String domainName = DocDB.getDomain(request);
				if (Tools.isNotEmpty(domainName)) {
					List<Integer> rootGroupIds = new ArrayList<>();

					List<GroupDetails> rootGroups = GroupsDB.getRootGroups();
					for (GroupDetails rootGroup : rootGroups) {
						if (domainName.equals(rootGroup.getDomainName())) {
							//add ID to list
							rootGroupIds.add(rootGroup.getGroupId());
						}
					}

					//add ids into Lucene query as AND root_group_l1:(1 OR 2 OR 3)
					if (rootGroupIds.size() > 0) {
						for (int i = 0; i < rootGroupIds.size(); i++) {
							if (i == 0) {
								luceneQuery.append(" AND root_group_l1:(").append(rootGroupIds.get(i));
							} else {
								luceneQuery.append(" OR ").append(rootGroupIds.get(i));
							}
						}
						luceneQuery.append(") ");
					}
				}
			}*/

			//dodatocne parametre
			luceneQuery.append(addInputParamsLucene(request));

			if(luceneQuery.toString().indexOf("publish_start") == -1 && luceneQuery.toString().indexOf("publish_end") == -1 && Constants.getBoolean("showOnlyActualPublishedDoc"))
			{
				Date now = new Date();
				luceneQuery.append(" AND (publish_start:").append(LuceneUtils.EMPTY).append(" OR publish_start:[").append(LuceneUtils.DATE_MIN).append(" TO ").append(DateTools.dateToString(now, Resolution.MINUTE)).append("]) ")
				           .append(" AND (publish_end:").append(LuceneUtils.EMPTY).append(" OR publish_end:[").append(DateTools.dateToString(now, Resolution.MINUTE)).append(" TO ").append(LuceneUtils.DATE_MAX).append("]) ");
			}

			// text to find
			Logger.debug(LuceneSearchAction.class,"words: " + words);

			String query = luceneQuery.toString();

			query = setParameters(request, words, query);
			queryHighlight = setParameters(request, words, queryHighlight);

			Logger.println(LuceneSearchAction.class,"search Lucene Query ="+query);
			dt.diff("mam query");

			searchResults = new ArrayList<>();

			Documents indexed = new Documents(language);

			dt.diff("mam indexed");

			String[] places = pageParams.getValue("places","documents").split("\\+");


			/* update the query to include other places */

			boolean searchInTickets = ArrayUtils.contains(places,"tickets");
			boolean searchInForums = ArrayUtils.contains(places,"forums");

			if (searchInTickets || searchInForums && Tools.isNotEmpty(words)){
				query = "(" + query + " AND type:documents ) ";
				if (searchInTickets)
				{
					query += String.format(" OR (type:tickets AND (data:%s) ) ",words);
				}
				if (searchInForums)
				{
					query += String.format(" OR (type:forums AND (data:%s) ) ",words);
				}
				Logger.debug(LuceneSearchAction.class,"Updated search Lucene Query ="+query);
			}

			dt.diff("before luceneSearchQuery");
			LuceneQuery luceneSearchQuery = new LuceneQuery(language);
			dt.diff("after luceneSearchQuery");

			luceneSearchQuery.setSort(buildSortExpression(request));

			dt.diff("after set sort");

			List<Document> documents = luceneSearchQuery.documents(query);

			dt.diff("after documents");

			totalResults = documents.size();

			dt.diff("after size");

			String textToFind = getTextToFind(request);

			dt.diff("after ttf");

			if (totalResults == 0 )
			{
				/* proximity search */
				query = setParameters(request, (words+"~0.8").replace(" ","~0.8 "), query);
				luceneSearchQuery = new LuceneQuery(indexed);
				luceneSearchQuery.setSort(buildSortExpression(request));

				documents = luceneSearchQuery.documents(query);
				totalResults = documents.size();

				String[] suggestions = FulltextSearch.suggestSimilar(textToFind,language);
				if (suggestions != null && suggestions.length >0 )
				{
					request.setAttribute("suggestion",suggestions[0]);
				}

				dt.diff("after totalResults == 0");
			}

			//ak mame nejake duplicity, dame ich prec
			if(docIdsNotIn != null && documents.size() > 0)
			{
				List<Document> documentsTmp = new ArrayList<>();
				Integer doc_id;
				for(Document luceneDocument : documents)
				{
					doc_id = Integer.valueOf(luceneDocument.getFieldable("doc_id").stringValue());
					if(!docIdsNotIn.contains(doc_id))
						documentsTmp.add(luceneDocument);
				}
				documents = new ArrayList<>(documentsTmp);
				totalResults = documents.size();

				dt.diff("after remove duplicity");
			}

			//odstranim zo stranok tie ktore maju available, alebo serachable na false
			if(documents.size() > 0)
			{
				List<Document> documentsTmp = new ArrayList<>();
				String type = null;
				DocDetails actualDoc = null;
				Set<String> forumAlreadyHasUrl = new HashSet<>();

				String docGroupNamePath = null;
				boolean namePathMatch = false;

				for(Document luceneDocument : documents)
				{
					//#15422 - lucene nevie vyhladavat ako startsWith, preto sa to musi riesit filtrovanie na zaklade groupId takto
					docGroupNamePath = DB.removeSlashes(groupsDB.getGroupNamePath(docDB.getBasicDocDetails(Tools.getIntValue(luceneDocument.getFieldable("doc_id").stringValue(),0),true).getGroupId()));
					if(searchGroupNamePaths != null)
					{
						namePathMatch = false;
						for(String searchNamePath : searchGroupNamePaths)
							if(docGroupNamePath.startsWith(searchNamePath)) namePathMatch = true;

						//nesedi nam virtual path - preskakujem
						if(namePathMatch == false) continue;
					}

					type = luceneDocument.getFieldable("type").stringValue();
					if("documents".equals(type) || "forums".equals(type))
					{
						actualDoc = docDB.getBasicDocDetails(Tools.getIntValue(luceneDocument.getFieldable("doc_id").stringValue(), 0), false);
						if(actualDoc != null && actualDoc.isAvailable() && actualDoc.isSearchable())
						{
							if ("forums".equals(type))
							{
								//aby sa nam vo vysledku neobjavili rovnake prispevky viac krat (duplicita)
								String url = luceneDocument.getFieldable("url").stringValue();
								if (forumAlreadyHasUrl.contains(url)) continue;
								forumAlreadyHasUrl.add(url);
							}
							documentsTmp.add(luceneDocument);
						}
					}
					else
					{
						documentsTmp.add(luceneDocument);
					}
				}
				documents = new ArrayList<>(documentsTmp);
				totalResults = documents.size();
			}

			dt.diff("after execute");

			Set<String> unfilteredFileExtensions = null;
			String extensions = Tools.isEmpty(request.getParameter("unfilteredFileExtensions")) ? request
						.getAttribute("unfilteredFileExtensions")!=null ? request
									.getAttribute("unfilteredFileExtensions").toString():null: request.getParameter("unfilteredFileExtensions");

			if (extensions!=null && Tools.isNotEmpty(extensions))
			{
				unfilteredFileExtensions = new HashSet<>(Arrays.asList(extensions.split(";")));
			}


			if (Tools.isNotEmpty(request.getParameter("page"))){
				int page =Integer.parseInt(request.getParameter("page"));
				index = (page-1) * perPage;
			}

			int i = index;

			SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter("<strong>","</strong>");
			LuceneQuery luceneSearchQueryHighlight = null;
			if(Tools.isNotEmpty(queryHighlight))
			{
				luceneSearchQueryHighlight = new LuceneQuery(language);
				luceneSearchQueryHighlight.documents(queryHighlight);
			}

			Logger.println(LuceneSearchAction.class, "queryHighlight(getParsedQuery): "+(luceneSearchQueryHighlight != null ? luceneSearchQueryHighlight.getParsedQuery() : luceneSearchQuery.getParsedQuery()));
			Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(luceneSearchQueryHighlight != null ? luceneSearchQueryHighlight.getParsedQuery() : luceneSearchQuery.getParsedQuery()));
			highlighter.setTextFragmenter(new SimpleFragmenter(300));
			String ttf = SearchAction.getTextToFind(request);

			while (searchResults.size()<perPage && i<documents.size())
			{
				String link;
				dt.diff("NEXT "+aListCount);
				aListCount++;
				if (aListCount == (perPage + 1))
				{
					Logger.debug(LuceneSearchAction.class,"aListCount="+aListCount+" perPage="+perPage);
					continue;
				}

            	Document luceneDocument = documents.get(i);
				i++;
				String externalLink = "";
				String type = luceneDocument.getFieldable("type").stringValue();

				SearchDetails qDet= new SearchDetails();

				if ("documents".equals(type) || "forums".equals(type))
				{
					qDet.setDocId(Tools.getIntValue(luceneDocument.getFieldable("doc_id").stringValue(), 4));

					DocDetails actualDoc = docDB.getBasicDocDetails(qDet.getDocId(), false);
					if (actualDoc == null)
					{
						totalResults--;
						continue;
					}

					if (Constants.getBoolean("multiDomainEnabled"))
					{
						//verify domain name
						GroupDetails docGroupDetails = groupsDB.getGroup(actualDoc.getGroupId());
						if (Tools.isNotEmpty(domainName) && domainName.equals(docGroupDetails.getDomainName())==false)
						{
							totalResults--;
							continue;
						}
					}

					qDet.setTitle(actualDoc.getTitle());
					qDet.setNavbar(actualDoc.getNavbar());
					qDet.setExternalLink(actualDoc.getExternalLink());
					externalLink = qDet.getExternalLink();
					qDet.setGroupId(actualDoc.getGroupId());
					qDet.setVirtualPath(actualDoc.getVirtualPath());
					qDet.setAvailable(true);
					qDet.setSearchable(true);
					qDet.setShowInMenu(actualDoc.isShowInMenu());
					qDet.setSortPriority(actualDoc.getSortPriority());
					qDet.setPasswordProtected(actualDoc.getPasswordProtected());
					qDet.setTempId(actualDoc.getTempId());
					qDet.setDateCreated(actualDoc.getDateCreated());
					qDet.setFieldA(actualDoc.getFieldA());
					qDet.setFieldB(actualDoc.getFieldB());
					qDet.setFieldC(actualDoc.getFieldC());
				}

				if ("forums".equals(type))
				{
					qDet.setTitle(luceneDocument.getFieldable("title").stringValue());
					qDet.setDateCreated(LuceneUtils.luceneDateToDate(luceneDocument.getFieldable("question_date").stringValue()).getTime());
					//qDet.setPublishStart(LuceneUtils.luceneDateToDate(luceneDocument.getFieldable("question_date").stringValue()).getTime());
					//qDet.setPublishEnd(LuceneUtils.luceneDateToDate(luceneDocument.getFieldable("question_date").stringValue()).getTime());
				}

				if (Tools.isEmpty(qDet.getExternalLink()))
				{
					externalLink = qDet.getExternalLink();
					qDet.setDoc_id(4);
				}

				if (unfilteredFileExtensions != null && !unfilteredFileExtensions.isEmpty())
				{

					if (Tools.isEmpty(externalLink) && !"html".equals(extensions) )
					{
						totalResults--;
						aListCount--;
						continue;
					}

					if (!Tools.isEmpty(externalLink))
					{
						int extensionIndex = externalLink.lastIndexOf('.');
						if (extensionIndex > 0)
						{
							if (!unfilteredFileExtensions.contains(externalLink.substring(extensionIndex+1)))
							{
								totalResults--;
								aListCount--;
								continue;
							}
						}
					}
				}

				if ("tickets".equals(type) || "forums".equals(type))
				{
					externalLink = luceneDocument.getFieldable("url").stringValue();
				}

				qDet.setExternalLink(externalLink);
				//toto nepotrebujeme, nizsie to prepiseme snippetom qDet.setData(luceneDocument.getFieldable("data").stringValue());

				Fieldable docId = luceneDocument.getFieldable("doc_id");
				if (docId!=null)
				{
					qDet.setDoc_id(Integer.parseInt(docId.stringValue()));
				}

				qDet.setDataOriginal(luceneDocument.getFieldable("data").stringValue());
				String snippet = null;

				if (SearchAction.shouldDoQuickSnippet(qDet, request)==false)
				{
					Analyzer analyzer = AnalyzerFactory.getAnalyzer(Version.LUCENE_31, "sk");
					TokenStream tokenStream = TokenSources.getTokenStream(luceneDocument, "data", analyzer);
					String[] snippets = highlighter.getBestFragments(tokenStream, qDet.getDataOriginal(), 4);
					if (snippets != null && snippets.length>0)
					{
						for (String s : snippets)
						{
							if (Tools.isEmpty(snippet)) snippet = s;
							else snippet += "... ..." + s; //NOSONAR
						}
					}
					tokenStream.close();
					analyzer.close();
				}

				SearchSnippet searchSnippet = new SearchSnippet(qDet, ttf, request);
				if (snippet == null) snippet = searchSnippet.getSnippet();
				if (snippet != null)	qDet.setData("..."+snippet+"...");
				else qDet.setData("");


				dt.diff("after getDocDetails: docId=" + qDet.getDocId() + " title=" + qDet.getTitle() + " path=" + qDet.getVirtualPath());
				//vymazeme mu data, ak by sa nieco pototo aby sa nezobrazila cela stranka

				group = groupsDB.getGroup(qDet.getGroupId());
				//nastav prava stranke (aby sa dalo detekovat vo vysledkoch a zobrazit napr. obrazok zamku)
				if (Tools.isEmpty(qDet.getPasswordProtected()) && group!=null && Tools.isNotEmpty(group.getPasswordProtected()))
				{
					qDet.setPasswordProtected(group.getPasswordProtected());
				}

				//dokumenty v internal foldroch neprehladavame (okrem root groups)
				if (group != null)
				{
					if (rootGroupIdsTable.get(Integer.valueOf(group.getGroupId()))==null && group.isInternal())
					{
						Logger.debug(LuceneSearchAction.class, "preskakujem interny adresar: "+group.getGroupIdName());
						aListCount--;
						continue;
					}
					link = groupsDB.getNavbar(group.getGroupId());
				}
				else
				{
					//group uz neexistuje...
					link = null;
				}
				try
				{
					if (qDet.getExternalLink().startsWith("/files/"))
					{
						//skip protected folder
						if (qDet.getExternalLink()!=null && qDet.getExternalLink().startsWith("/files/protected")) continue;

						//navbar pre subory generujem bez moznosti kliknutia na odkaz
						String[] fileLink = MultiDomainFilter.fixDomainPaths(qDet.getExternalLink(), request).split("\\/");
						link = "";
						for (int p=0; p<fileLink.length-1; p++)
						{
							String part = fileLink[p];
							if (Tools.isEmpty(link)) link = part;
							else link += " "+Constants.getString("navbarSeparator")+" "+part; //NOSONAR
						}
						//skontroluj ci subor skutocne existuje
						if (request.getAttribute("searchDontCheckFile")==null)
						{
							IwcmFile iwf = new IwcmFile(Tools.getRealPath(qDet.getExternalLink()));
							if (iwf.exists()==false)
							{
								Logger.debug(LuceneSearchAction.class, "Subor "+qDet.getExternalLink()+" neexistuje, preskakujem");
								totalResults--;
								continue;
							}
						}
					}
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
				}

				if (link == null)
				{
					qDet.setLink("");
				}
				else
				{
					qDet.setLink(link);
				}

				if (luceneDocument.getFieldable("publish_start") != null)
				{
					Date date = LuceneUtils.luceneDateToDate(luceneDocument.getFieldable("publish_start").stringValue());
					if (date != null) qDet.setPublishStart(date.getTime());
				}
				if (luceneDocument.getFieldable("publish_end") != null)
				{
					Date date = LuceneUtils.luceneDateToDate(luceneDocument.getFieldable("publish_end").stringValue());
					if (date != null) qDet.setPublishEnd(date.getTime());
				}
				if(luceneDocument.getFieldable("question_date")!= null)
				{
					Date date = LuceneUtils.luceneDateToDate(luceneDocument.getFieldable("question_date").stringValue());
					if (date != null) qDet.setPublishStart(date.getTime());
				}
				if (luceneDocument.getFieldable("author_id") != null)	qDet.setAuthorId(Tools.getIntValue(luceneDocument.getFieldable("author_id").stringValue(), -1));
				//nemame v indexe hodnotu if (luceneDocument.getFieldable("html_head") != null)	qDet.setHtmlHead(luceneDocument.getFieldable("html_head").stringValue());
				if (luceneDocument.getFieldable("html_data") != null)	qDet.setHtmlHead(luceneDocument.getFieldable("html_data").stringValue());
				if (luceneDocument.getFieldable("perex_place") != null)	qDet.setPerexPlace(luceneDocument.getFieldable("perex_place").stringValue());
				if (luceneDocument.getFieldable("perex_image") != null)	qDet.setPerexImage(luceneDocument.getFieldable("perex_image").stringValue());
				if (luceneDocument.getFieldable("event_date") != null)
				{
					Date date = LuceneUtils.luceneDateToDate(luceneDocument.getFieldable("event_date").stringValue());
					if (date != null) qDet.setEventDate(date.getTime());
				}

				if (luceneDocument.getFieldable("field_d") != null)	qDet.setFieldD(luceneDocument.getFieldable("field_d").stringValue());
				if (luceneDocument.getFieldable("field_e") != null)	qDet.setFieldE(luceneDocument.getFieldable("field_e").stringValue());
				if (luceneDocument.getFieldable("field_f") != null)	qDet.setFieldF(luceneDocument.getFieldable("field_f").stringValue());
				if (luceneDocument.getFieldable("field_g") != null)	qDet.setFieldG(luceneDocument.getFieldable("field_g").stringValue());
				if (luceneDocument.getFieldable("field_h") != null)	qDet.setFieldH(luceneDocument.getFieldable("field_h").stringValue());
				if (luceneDocument.getFieldable("field_i") != null)	qDet.setFieldI(luceneDocument.getFieldable("field_i").stringValue());
				if (luceneDocument.getFieldable("field_j") != null)	qDet.setFieldJ(luceneDocument.getFieldable("field_j").stringValue());
				if (luceneDocument.getFieldable("field_k") != null)	qDet.setFieldK(luceneDocument.getFieldable("field_k").stringValue());
				if (luceneDocument.getFieldable("field_l") != null)	qDet.setFieldL(luceneDocument.getFieldable("field_l").stringValue());

				dt.diff("after snippet");

				searchResults.add(qDet);
			}
			dt.diff("done");
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			request.setAttribute("err_msg", "Chyba pri práci s databázou..." + e.getMessage());
			return (error);
		}

		request.setAttribute("words", words);
		request.setAttribute("aList", searchResults);
		if (searchResults.isEmpty())
		{
			request.setAttribute("wrong", "true");
			request.setAttribute("notfound", "true");
			return (forward);
		}
		String paramsLink = "";
		StringBuilder paramsLinkBuilder = new StringBuilder();
		Enumeration<String> e = request.getParameterNames();
		String name;
		while (e.hasMoreElements())
		{
			name = e.nextElement();
			if ("index".equals(name) || "docid".equals(name)) continue;

			String[] values = request.getParameterValues(name);
		   for (int v=0; v<values.length; v++)
		   {
		   	paramsLinkBuilder.append("&amp;").append(name).append('=').append(Tools.URLEncode(values[v]));
		   }
		}
		paramsLink = paramsLinkBuilder.toString();
		request.setAttribute("paramsLink", paramsLink);

		//vypocitaj celkove pocty
		request.setAttribute("fromIndex", Integer.toString(index + 1));
		int toIndex = index + searchResults.size();
		request.setAttribute("toIndex", Integer.toString(toIndex));
		Logger.debug(LuceneSearchAction.class, "totalResults="+totalResults);
		request.setAttribute("totalResults", Integer.toString(totalResults));

		// vytvor pages
		SearchAction.preparePages(request, perPage, index, totalResults, paramsLink,"lucene_search.do");

		if (totalResults > toIndex)
		{
			if (!english)
			{
				request.setAttribute("next", "<a href=\"lucene_search.do?index=" + (index + perPage) + paramsLink + "\"> ďalej &gt;&gt;&gt; </a>");
			}
			else
			{
				request.setAttribute("next", "<a href=\"lucene_search.do?index=" + (index + perPage) + paramsLink + "\"> Next &gt;&gt;&gt; </a>");
			}

			request.setAttribute("nextHref", "lucene_search.do?index=" + (index + perPage) + paramsLink);
		}
		else
		{
			//request.setAttribute("next", NEXT);
		}
		if (index != 0)
		{
			if (!english)
			{
				request.setAttribute("prev", "<a href=\"lucene_search.do?index=" + (index - perPage) + paramsLink + "\"> &lt;&lt;&lt; Späť </a>");
			}
			else
			{
				request.setAttribute("prev", "<a href=\"lucene_search.do?index=" + (index - perPage) + paramsLink + "\"> &lt;&lt;&lt; Back </a>");
			}
			request.setAttribute("prevHref", "lucene_search.do?index=" + (index - perPage)	+ paramsLink);
		}
		else
		{
			//request.setAttribute("prev", PREV);
		}


		request.setAttribute("numberOfPages",(int)Math.ceil((double)totalResults/(double)perPage) );

		String searchTerm = request.getParameter("words");
		if (Tools.isEmpty(searchTerm)) searchTerm = request.getParameter("text");
		if (Tools.isNotEmpty(searchTerm))
		{
			StatDB.addStatSearchLocal(searchTerm, Tools.getIntValue(request.getParameter("docid"), -1), request);
		}return (forward);
	}

	/**
	 * @param request
	 * @param searchDetaultInTitle
	 * @param words
	 * @param query
	 * @return
	 */
	private static String setParameters(HttpServletRequest request, String words, String query)
	{
		if (words.length()>0)
		{
			/*replacement for match against title AND data */

			String parsedWords = words;
			//ak mame viac slov je potrebne pridat * za kazde slovo
			String withAsterisk = parsedWords.replace(" ","* ");

			query = setNextParameter(query,parsedWords);
			query = setNextParameter(query,withAsterisk);
			query = setNextParameter(query,parsedWords);
			query = setNextParameter(query,withAsterisk);
			query = setNextParameter(query,parsedWords);
			query = setNextParameter(query,withAsterisk);
		}

		query = addInputParamsLucene(request, query);
		return query;
	}

	/**
	 * @return
	 */
	private static boolean isEmptySearch(HttpServletRequest request, String words,boolean hasInputParams)
	{
		if (words.length() == 0 && !hasInputParams)
		{
			//nemame co vyhladavat
			request.setAttribute("aList", new ArrayList<SearchDetails>());
			request.setAttribute("wrong", "true");
			request.setAttribute("emptyrequest", "true");
			return (true);
		}
		return false;
	}

	/**
	 * @param request
	 * @return
	 */
	private static boolean isDOS(HttpServletRequest request)
	{
		if (request.getAttribute("disableSearchSpamProtection")==null && request.getAttribute("forceWords")==null && request.getParameter("index")==null) //len ak ide o samotne vyhladavanie, nie o zobrazenie dalsej stranky vyhladavania
		{
			long wait=0;
			if ((wait=SpamProtection.getWaitTimeout("search", request)) != 0)
			{
				//prekrocil sa hodinovy limit
				request.setAttribute("wrong", "true");
				request.setAttribute("crossHourlyLimit", "true");
				request.setAttribute("wait", (wait/60/1000));
				return true;
			}
			if (!SpamProtection.canPost("search", "", request))
			{
				//prekrocil sa cas medzi dvoma prehladavaniami
				request.setAttribute("wrong", "true");
				request.setAttribute("crossTimeout", "true");
				return true;
			}
		}
		return false;
	}

	/**
	 * @param request
	 */
	private static Sort buildSortExpression(HttpServletRequest request)
	{
		String order_var = "ASC";
		String order = getParamAttribute("order", request, "asc");
		String orderType = getParamAttribute("orderType", request, "sort_priority");
		List<String> sort = new ArrayList<>();

		if ("desc".equalsIgnoreCase(order))
		{
			order_var = "DESC";
		}
		if ("asc".equalsIgnoreCase(order))
		{
			order_var = "ASC";
		}
		if ("lastUpdate".equalsIgnoreCase(orderType))
		{
			orderType = "date_created";
		}
		else if ("sortPriority".equalsIgnoreCase(orderType))
		{
			orderType = "sort_priority";
		}
		else if ("title".equalsIgnoreCase(orderType))
		{
			orderType = "title";
		}
		else if ("publishStart".equalsIgnoreCase(orderType))
		{
			orderType = "publish_start";
		}
		else if ("saveDate".equalsIgnoreCase(orderType))
		{
			orderType = "date_created";
		}
		else if ("score".equalsIgnoreCase(orderType))
		{
			orderType = "score";
		}
		sort.add(orderType);

		//dalsie order by
		for (int i=2; i<=5; i++)
		{
			orderType = getParamAttribute("orderType"+i, request, null);
			if (Tools.isNotEmpty(orderType))
			{
				order_var = "ASC";
				order = getParamAttribute("order"+i, request, "asc");
				if ("desc".equalsIgnoreCase(order))
				{
					order_var = "DESC";
				}

				if ("lastUpdate".equalsIgnoreCase(orderType))
				{
					orderType = "date_created";
				}
				else if ("sortPriority".equalsIgnoreCase(orderType))
				{
					orderType = "sort_priority";
				}
				else if ("title".equalsIgnoreCase(orderType))
				{
					orderType = "title";
				}
				else if ("publishStart".equalsIgnoreCase(orderType))
				{
					orderType = "publish_start";
				}
				else if ("score".equalsIgnoreCase(orderType))
				{
					orderType = "score";
				}

				sort.add(orderType);
			}
		}

		final boolean reverse = order_var.equals("DESC");
		Collection<SortField> sortFields = CollectionUtils.collect(sort, obj -> {
			String field = (String) obj;
			if ("sort_priority".equals(field))
			{
				return new SortField(field, SortField.INT, reverse);
			}
			else if ("score".equals(field))
			{
				return SortField.FIELD_SCORE;
			}
			return new SortField(field, SortField.STRING, reverse);
		});
		sortFields.add(new SortField(null, SortField.SCORE, false));

		return new Sort(sortFields.toArray(new SortField[sortFields.size()]));
	}

	public static String addInputParamsLucene(HttpServletRequest request)
	{
		String[] checkInputParams = SearchTools.getCheckInputParams();
		StringBuilder ret = new StringBuilder();
		int len = checkInputParams.length;
		int i;
		String param;

		//	pridanie publish_start a publish_end parametrov nerobime ak je nejaky rozumny dateText
		String dateText = getParamAttributeUnsafe("dateText", request, null);
		boolean checkPublishDates = false;
		if (dateText == null || "custom".equals(dateText))
		{
			checkPublishDates = true;
		}

		for (i=0; i<len; i++)
		{
			param = getParamAttributeUnsafe(checkInputParams[i], request, null);
			if (Tools.isNotEmpty(param))
			{
				if (checkInputParams[i].equals("publish_start"))
				{
					if (checkPublishDates)
					{
						ret.append(" AND ( ( NOT publish_start:").append(LuceneUtils.EMPTY).append(" AND publish_start:[$ TO ").append(LuceneUtils.DATE_MAX).append("] ) OR (publish_start:").append(LuceneUtils.EMPTY).append(" AND date_created:[$ TO ").append(LuceneUtils.DATE_MAX).append("]) ) ");
					}
				}
				else if (checkInputParams[i].equals("publish_start_lt"))
				{
					if (checkPublishDates)
					{
						ret.append(" AND ( ( NOT publish_start:").append(LuceneUtils.EMPTY).append(" AND publish_start:[").append(LuceneUtils.DATE_MIN).append(" TO $]) OR (publish_start:").append(LuceneUtils.EMPTY).append(" AND date_created:[").append(LuceneUtils.DATE_MIN).append(" TO $]) ) ");
					}
				}
				else if (checkInputParams[i].equals("publish_start_gt"))
				{
					if (checkPublishDates)
					{
						ret.append(" AND ( ( NOT publish_start:").append(LuceneUtils.EMPTY).append(" AND publish_start:[$ TO ").append(LuceneUtils.DATE_MAX).append("]) OR (publish_start:").append(LuceneUtils.EMPTY).append(" AND date_created:[$ TO ").append(LuceneUtils.DATE_MAX).append("]) ) ");
					}
				}
				else if (checkInputParams[i].equals("publish_end"))
				{
					if (checkPublishDates)
					{
						ret.append(" AND ( ( NOT publish_end:").append(LuceneUtils.EMPTY).append(" AND publish_end[").append(LuceneUtils.DATE_MIN).append(" TO $ ]) OR (publish_end:").append(LuceneUtils.EMPTY).append(" AND date_created:[").append(LuceneUtils.DATE_MIN).append(" TO $]) ) ");
					}
				}
				else if (checkInputParams[i].equals("publish_end_gt"))
				{
					if (checkPublishDates)
					{
						ret.append(" AND ( (publish_end:").append(LuceneUtils.EMPTY).append(" AND publish_end:[$ TO ").append(LuceneUtils.DATE_MAX).append("]) OR (publish_end:").append(LuceneUtils.EMPTY).append(" AND date_created:[$ TO ").append(LuceneUtils.DATE_MAX).append("]) ) ");
					}
				}
				else if (checkInputParams[i].equals("publish_end_lt"))
				{
					if (checkPublishDates)
					{
						ret.append(" AND ( (publish_end:").append(LuceneUtils.EMPTY).append(" AND publish_end:[").append(LuceneUtils.DATE_MIN).append(" TO $").append("]) OR (publish_end:").append(LuceneUtils.EMPTY).append(" AND date_created:[").append(LuceneUtils.DATE_MIN).append(" TO $").append("]) ) ");
					}
				}
				else if (checkInputParams[i].equals("temp_id"))
				{
					ret.append(" AND " + checkInputParams[i] + ":$ ");
				}
				else if (checkInputParams[i].equals("keyword"))
				{
					DocDB docDB = DocDB.getInstance();
					PerexGroupBean pgb = docDB.getPerexGroup(-1, param);
					if (pgb != null) ret.append(" AND ( perex_group:").append(pgb.getPerexGroupName()).append(" ) ");
				}
				else
				{
					//ak mame viac slov rozdel po slovach
					StringTokenizer st = new StringTokenizer(param);
					if (st.countTokens()<2 || param.startsWith("\""))
					{
						ret.append(" AND " + checkInputParams[i] + ":$");
					}
					else
					{
						String mode = getInputParamMode(checkInputParams[i], request);
						ret.append(" AND (");
						while (st.hasMoreTokens())
						{
							st.nextToken();
							ret.append(checkInputParams[i]).append(":$ ");
							if (st.hasMoreTokens()) ret.append(mode).append(' ');
						}
						ret.append(") ");
					}
				}
			}
		}
		//	textove vyjadrenie dateText = "today,lastWeek,lastMonth";
		if (checkPublishDates==false && Tools.isNotEmpty(dateText))
		{
			ret.append(" AND ( (NOT publish_start:").append(LuceneUtils.EMPTY).append(" AND publish_start:[$ TO ").append(LuceneUtils.DATE_MAX).append("]) OR (publish_start:").append(LuceneUtils.EMPTY).append(" AND date_created:[$ TO ").append(LuceneUtils.DATE_MAX).append("]) ) AND ( (NOT publish_end:").append(LuceneUtils.EMPTY).append(" AND publish_end;[").append(LuceneUtils.DATE_MIN).append(" TO $]) OR (publish_end:").append(LuceneUtils.EMPTY).append(" AND date_created:[$ TO ").append(LuceneUtils.DATE_MAX).append("]) ) ");
		}
		return(ret).toString();
	}

	/**
	 * Prida parametre do PreparedStatementu
	 * @param request
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	public static String addInputParamsLucene(HttpServletRequest request, String query)
	{
		String[] checkInputParams = SearchTools.getCheckInputParams();
		int len = SearchTools.getCheckInputParams().length;
		int i;
		String param;

		//pridanie publish_start a publish_end parametrov nerobime ak je nejaky rozumny dateText
		String dateText = getParamAttributeUnsafe("dateText", request, null);
		boolean checkPublishDates = false;
		if (dateText == null || "custom".equals(dateText))
		{
			checkPublishDates = true;
		}
		//SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat parser =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (i=0; i<len; i++)
		{
			param = getParamAttributeUnsafe(checkInputParams[i], request, null);
			if (Tools.isNotEmpty(param))
			{
				Logger.debug(LuceneSearchAction.class, "addPram "+checkInputParams[i]+"="+param);
				if (checkInputParams[i].equals("publish_start"))
				{
					if (checkPublishDates)
					{
						long d = LuceneUtils.getTimestamp(param,parser);
						query = setNextParameter(query,new Date(d));
						query = setNextParameter(query,new Date(d));
					}
				}
				else if (checkInputParams[i].equals("publish_start_lt"))
				{
					if (checkPublishDates)
					{
						long d = LuceneUtils.getTimestamp(param,parser);
						query = setNextParameter(query, new Date(d));
						query = setNextParameter(query, new Date(d));
					}
				}
				else if (checkInputParams[i].equals("publish_start_gt"))
				{
					if (checkPublishDates)
					{
						long d = LuceneUtils.getTimestamp(param,parser);
						query = setNextParameter(query, new Date(d));
						query = setNextParameter(query, new Date(d));
					}
				}
				else if (checkInputParams[i].equals("publish_end"))
				{
					if (checkPublishDates)
					{
						long d = LuceneUtils.getTimestamp(param,parser);
						query = setNextParameter(query, new Date(d));
						query = setNextParameter(query, new Date(d));
					}
				}
				else if (checkInputParams[i].equals("publish_end_gt"))
				{
					if (checkPublishDates)
					{
						long d = LuceneUtils.getTimestamp(param,parser);
						query = setNextParameter(query, new Date(d));
						query = setNextParameter(query, new Date(d));
					}
				}
				else if (checkInputParams[i].equals("publish_end_lt"))
				{
					if (checkPublishDates)
					{
						long d = LuceneUtils.getTimestamp(param,parser);
						query = setNextParameter(query, new Date(d));
						query = setNextParameter(query, new Date(d));
					}
				}
				else if (checkInputParams[i].equals("temp_id"))
				{
					query = setNextParameter(query,Tools.getIntValue(param, 0));
				}
				else
				{
					StringTokenizer st = new StringTokenizer(param);
					if (st.countTokens()<2 || param.startsWith("\""))
					{
						query = setNextParameter(query,Tools.replace(param, "\"", ""));
					}
					else
					{
						while (st.hasMoreTokens())
						{
							query = setNextParameter(query, st.nextToken());
						}
					}
				}
			}
		}
		//	textove vyjadrenie dateText = "today,lastWeek,lastMonth";
		if (checkPublishDates==false && Tools.isNotEmpty(dateText))
		{
			Calendar cal = Calendar.getInstance();
			long dEnd = DB.getTimestamp(Tools.formatDate(cal.getTimeInMillis()), "23:59");
			if ("lastWeek".equals(dateText))
			{
				cal.add(Calendar.WEEK_OF_YEAR, -1);
			}
			else if ("lastMonth".equals(dateText))
			{
				cal.add(Calendar.MONTH, -1);
			}
			long dStart = DB.getTimestamp(Tools.formatDate(cal.getTimeInMillis()), "0:00");
			setNextParameter(query,new Timestamp(DB.getTimestampNotBeforeAfterYear(dStart, 1970, 3000)));
			setNextParameter(query, new Timestamp(DB.getTimestampNotBeforeAfterYear(dStart, 1970, 3000)));
			setNextParameter(query, new Timestamp(DB.getTimestampNotBeforeAfterYear(dEnd, 1970, 3000)));
			setNextParameter(query, new Timestamp(DB.getTimestampNotBeforeAfterYear(dEnd, 1970, 3000)));
		}
		return query;
	}

	public static String setNextParameter(String query,Object value)
	{
		if (value instanceof Timestamp)
		{
			return query.replaceFirst("\\$",LuceneUtils.timestampToLucene(((Timestamp)value).getTime()));
		}
		if (value instanceof java.util.Date)
		{
			return query.replaceFirst("\\$",LuceneUtils.timestampToLucene(((java.util.Date)value).getTime()));
		}
		return query.replaceFirst("\\$", value.toString());
	}

	public static  String setNextParameter(String query,int value)
	{
		return query.replaceFirst("\\$", Integer.toString(value));
	}
}
