package sk.iway.iwcm.components.seo.rest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintViolationException;

import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.seo.SeoKeyword;
import sk.iway.iwcm.components.seo.SeoManager;
import sk.iway.iwcm.components.seo.jpa.BotsDTO;
import sk.iway.iwcm.components.seo.jpa.BotsDetailsDTO;
import sk.iway.iwcm.components.seo.jpa.NumberKeywordsDTO;
import sk.iway.iwcm.components.seo.jpa.StatKeywordsDTO;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.stat.jpa.SearchEnginesDTO;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;


public class SeoService {

	private static final int MAX_ROWS = 100;

	/********************************** SEO - BOTS  SECTIONS **************************************************/
	public static List<BotsDTO> getBotsTableData(Date from, Date to, int groupId) {
		List<BotsDTO> listItems = new ArrayList<>();
		Map<Integer, BotsDTO> mapItems = getSeoBots(from, to, groupId);

        float sum = 0.0f;
        for(Entry<Integer, BotsDTO> entry: mapItems.entrySet()) {
            listItems.add(entry.getValue());
            sum += entry.getValue().getVisits();
        }

		if (sum > 0) {
			for(BotsDTO item : listItems) {
				Float percentual = Float.valueOf( (item.getVisits() / sum) * 100 );
				item.setPercentual( percentual );
			}
		}

		return listItems;
	}

	public static List<BotsDTO> getBotsPieChartData(Date from, Date to, int groupId) {
		Map<Integer, BotsDTO> mapItems = getSeoBots(from, to, groupId);
		List<BotsDTO> pieChartData = new ArrayList<>(mapItems.values());
		//Neded soft (for better overal look)
		Collections.sort(pieChartData, (BotsDTO botA, BotsDTO botB) -> botB.getVisits() - botA.getVisits());
		return pieChartData;
	}

	private static Map<Integer, BotsDTO> getSeoBots(Date from, Date to, int groupId) {
		Map<Integer, BotsDTO> bots = new HashMap<>();
		for (String suffix : StatNewDB.getTableSuffix(from.getTime(), to.getTime())) {
			String sql = getBotSqlString(suffix, groupId, from, to);

			new ComplexQuery().setSql(sql).setParams(from, to).setMaxSize(MAX_ROWS).list(new Mapper<BotsDTO>() {
				@Override
				public BotsDTO map(ResultSet rs) throws SQLException {
					BotsDTO botDTO =
						new BotsDTO(
							rs.getInt("botId"),
							rs.getString("name"),
							rs.getInt("visits"),
							rs.getDate("dayDate")
						);

					if (!bots.containsKey(botDTO.getBotId()))
						bots.put(botDTO.getBotId(), botDTO);
					else {
						BotsDTO tempBot = bots.get(botDTO.getBotId());
						tempBot.setVisits(tempBot.getVisits() + botDTO.getVisits()); // zvys pocet navstev
						if (botDTO.getDayDate().after(tempBot.getDayDate()))		// ak je navsteva vyssieho datumu, prepis ju
							tempBot.setDayDate(botDTO.getDayDate());
					}

					return null;
				}
			});
		}
		return bots;
	}

	private static String getBotSqlString(String suffix, int groupId, Date from, Date to) {
		StringBuilder sql = new StringBuilder("SELECT DISTINCT s.browser_id AS botId, seo_bots.name AS name,COUNT(DISTINCT s.session_id) AS visits, ");
		sql.append("MAX(s.view_time) AS dayDate");
		sql.append(" FROM stat_views");
		sql.append(suffix);
		sql.append(" s JOIN seo_bots ON s.browser_id = seo_bots.seo_bots_id");
		sql.append(" WHERE s.browser_id < ");
		sql.append(Constants.getInt("loggedUserBrowserId"));
		sql.append(" AND s.view_time >= ? AND ");
		sql.append("s.view_time <= ? ");
		sql.append(StatDB.getRootGroupWhere("s.group_id", groupId));
		sql.append(" GROUP BY s.browser_id, seo_bots.name");
		return sql.toString();
	}

	public static Map<String, List<BotsDTO>> getLineChartData(Date from, Date to, int groupId) {
		return getSeoBotsTime(
			getBotsPieChartData(from, to, groupId),
			from,
			to,
			groupId
		);
	}

	private static Map<String, List<BotsDTO>> getSeoBotsTime(List<BotsDTO> bots, Date from, Date to, int groupId) {
		Map<String, List<BotsDTO>> collection = new Hashtable<>();

		for (BotsDTO bot : bots)  {
			Map<Date, Integer> visitDays = new Hashtable<>();
			for (String suffix : StatNewDB.getTableSuffix(from.getTime(), to.getTime())) {
				String sql = getBotTimeSqlString(suffix, groupId);

				new ComplexQuery().setSql(sql).setParams(bot.getBotId(), from, to).list(new Mapper<BotsDTO>() {
					@Override
					public BotsDTO map(ResultSet rs) throws SQLException {
						Date day = getEditedDate( rs.getDate("dayDate") );

						if (!visitDays.containsKey(day))
							visitDays.put(day, 1);
						else
							visitDays.put(day, (visitDays.get(day)+1));

						return null;
					}
				});
			}
			collection.put( bot.getName(), convertMapBotsToList(visitDays) );
		}
		return collection;
	}

	private static String getBotTimeSqlString(String suffix, int groupId) {
		StringBuilder sql = new StringBuilder("SELECT MAX(s.view_time) AS dayDate FROM stat_views");
		sql.append(suffix).append(" s ");
		sql.append("WHERE s.browser_id = ? AND s.view_time >= ? AND s.view_time <= ? ");
		sql.append(StatDB.getRootGroupWhere("s.group_id", groupId));
		sql.append(" GROUP BY s.session_id");
		return sql.toString();
	}

	private static List<BotsDTO> convertMapBotsToList(Map<Date, Integer> mapBots) {
		List<BotsDTO> listBots = new ArrayList<>();
		for(Map.Entry<Date, Integer> entry : mapBots.entrySet())
			listBots.add( new BotsDTO(entry.getKey(), entry.getValue()) );
		return listBots;
	}

	private static Date getEditedDate(Date timestamp) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(timestamp);
		calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
		calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
		calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
		calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
		return calendar.getTime();
	}

	/********************************** SEO - NUMBER KEYWORDS  SECTIONS **************************************************/
	public static List<NumberKeywordsDTO> getNumberKeywordsTableData(FilterHeaderDto filter) {
		return getNumberSeoKeywordsOnPage(filter);
	}

	public static List<NumberKeywordsDTO> getNumberKeywordsBarChartData(FilterHeaderDto filter) {
		List<NumberKeywordsDTO> barChartData = getNumberKeywordsTableData(filter);
		//Neded soft (for better overal look)
		Collections.sort(barChartData, new Comparator<NumberKeywordsDTO>() {
			public int compare(NumberKeywordsDTO nkA, NumberKeywordsDTO nkB) {
				return nkB.getNumberOfKeywords() - nkA.getNumberOfKeywords();
			}
		});
		return barChartData;
	}

	private static String getNumberSeoKeywordsOnPageSql(FilterHeaderDto filter) {
		StringBuilder sql = new StringBuilder("SELECT data_asc, title FROM documents WHERE (data_asc LIKE ? OR title LIKE ?) ");

		if(filter.getWebPageId() > 0)
			sql.append(" AND doc_id = ").append(filter.getWebPageId()).append(" ");
		else if (Tools.isNotEmpty(filter.getRootGroupIdQuery()))
			sql.append(" ").append(filter.getRootGroupIdQuery());

		return sql.toString();
	}

	private static List<NumberKeywordsDTO> getNumberSeoKeywordsOnPage(FilterHeaderDto filter) {
		List<NumberKeywordsDTO> filterSeoKeywords = new ArrayList<>();

		int order = 1;
		for (String seoKeyword : getDistinctSeoKeywords()) {

			String sql = getNumberSeoKeywordsOnPageSql(filter);
			Object[] params = {
				"%" + DB.internationalToEnglish(seoKeyword.toLowerCase()) + "%",
				"%" + seoKeyword + "%"
			};

			List<Integer> counter = Arrays.asList(0);
			List<Integer> tempCount = Arrays.asList(0);
			List<Integer> counterB = Arrays.asList(0);
			new ComplexQuery().setSql(sql).setParams(params).list(new Mapper<NumberKeywordsDTO>() {
				@Override
				public NumberKeywordsDTO map(ResultSet rs) throws SQLException {
					int counterValue = Tools.getNumberSubstring((DB.internationalToEnglish(rs.getString("title")) + " " + DB.getDbString(rs, "data_asc")), DB.internationalToEnglish(seoKeyword.toLowerCase()));
					counter.set(0, counter.get(0) + counterValue);

					int counterValueB = getNumberSubstringNoBoundary((DB.internationalToEnglish(rs.getString("title")) + " " + DB.getDbString(rs, "data_asc")), DB.internationalToEnglish(seoKeyword.toLowerCase()));
					counterB.set(0, counterB.get(0) + counterValueB);

					tempCount.set(0, tempCount.get(0) + 1);
					return null;
				}
			});

			filterSeoKeywords.add(
				new NumberKeywordsDTO(
					order++,
					seoKeyword.toLowerCase(),
					tempCount.get(0),
					counter.get(0),
					counterB.get(0)
				)
			);
		}
		return filterSeoKeywords;
	}

	private static int getNumberSubstringNoBoundary(String src, String subString) {
		if (src == null)
			return (-1);

		if (src.indexOf(subString) == -1 || src.isEmpty() || subString.isEmpty())
			return (0);

	   	int counter = 0;
	   	Pattern p = Pattern.compile(subString);
	   	Matcher m = p.matcher(src); // v com sa to ma matchovat

	   	while(m.find())	counter++;

		return (counter);
	}

	/********************************** SEO - STAT KEYWORDS  SECTIONS **************************************************/
	public static List<StatKeywordsDTO> getStatKeywordsTableData(FilterHeaderDto filter) {
		Map<String, StatKeywordsDTO> mapData = getFilterSeoKeywords(filter);

		//Convert map to list
		List<StatKeywordsDTO> listData = new ArrayList<>(mapData.values());

		//Sort list by accessCount
		Collections.sort(listData, new Comparator<StatKeywordsDTO>() {
			public int compare(StatKeywordsDTO o1, StatKeywordsDTO o2) {
				return o2.getQueryCount().compareTo(o1.getQueryCount());
			}
		});

		//Count all access counts and set order
		int order = 1;
        int allQueryCounts = 0;
		for(StatKeywordsDTO entity : listData) {
			entity.setOrder(order++);
			allQueryCounts += entity.getQueryCount();
		}

		//Count percentage
		if (allQueryCounts > 0) {
			for(StatKeywordsDTO entity : listData) {
				entity.setPercentage( (double) entity.getQueryCount() * 100 / allQueryCounts );
			}
		}

		return listData;
	}

	private static String getFilterSeoKeywordsSql(FilterHeaderDto filter, String suffix) {
		StringBuilder sql = new StringBuilder("SELECT query, COUNT(query) AS total FROM stat_searchengine");
		sql.append(suffix);
		sql.append(" WHERE doc_id >= 0 AND "+DB.fixAiCiCol("query")+" = ?");
		sql.append(" AND search_date >= ? AND search_date <= ? ");

		if(Tools.isNotEmpty(filter.getSearchEngineName()))
			sql.append(" AND server = '").append(filter.getSearchEngineName()).append("' ");

		if (filter.getWebPageId() > 0)
			sql.append(" AND doc_id = ").append(filter.getWebPageId()).append(" ");

		sql.append(filter.getRootGroupIdQuery());

		sql.append(" GROUP BY query");
		sql.append(" ORDER BY total DESC");
		return sql.toString();
	}

	private static Map<String, StatKeywordsDTO> getFilterSeoKeywords(FilterHeaderDto filter) {
		Map<String, StatKeywordsDTO> mapData = new HashMap<>();
		List<String> seoKeywords = getDistinctSeoKeywords();
		Object[] params = {"", filter.getDateFrom(), filter.getDateTo()};

		for (String suffix : StatNewDB.getTableSuffix("stat_searchengine", filter.getDateFrom().getTime(), filter.getDateTo().getTime())) {
			for (String seoKeyword : seoKeywords) {

				String sql = getFilterSeoKeywordsSql(filter, suffix);
				params[0] = DB.fixAiCiValue(seoKeyword);
				String key = seoKeyword.toLowerCase();

				new ComplexQuery().setSql(sql).setParams(params).list(new Mapper<StatKeywordsDTO>() {
					@Override
					public StatKeywordsDTO map(ResultSet rs) throws SQLException {
						if(mapData.get(key) == null) mapData.put(key, new StatKeywordsDTO(key, rs.getInt("total")));
						else {
							StatKeywordsDTO tmp = mapData.get(key);
							tmp.setQueryCount( tmp.getQueryCount() + rs.getInt("total") );
							mapData.put(key, tmp);
						}

						return null;
					}
				});

				//If there was no data for this seoKeyword
				if(mapData.get(key) == null)
					mapData.put(key, new StatKeywordsDTO(key, 0));
			}
		}
		return mapData;
	}

	/********************************** SEO - STAT KEYWORDS DETAILS SECTIONS **************************************************/
	public static List<SearchEnginesDTO> getStatKeywordsDetailsTableData(FilterHeaderDto filter) {
        Map<String, SearchEnginesDTO> mapData = getStatKeywordsDetails(filter);

        //Convert map to list
		List<SearchEnginesDTO> listData = new ArrayList<>(mapData.values());

		//Sort list by accessCount
		Collections.sort(listData, new Comparator<SearchEnginesDTO>() {
			public int compare(SearchEnginesDTO o1, SearchEnginesDTO o2) {
				return o2.getAccesCount().compareTo(o1.getAccesCount());
			}
		});

		//Count all access counts and set order
		int order = 1;
        int allAccessCounts = 0;
		for(SearchEnginesDTO entity : listData) {
			entity.setOrder(order++);
			allAccessCounts += entity.getAccesCount();
		}

		//Count percentage
		if (allAccessCounts > 0) {
			for(SearchEnginesDTO entity : listData) {
				entity.setPercentage( (double) entity.getAccesCount() * 100 / allAccessCounts );
			}
		}

		return listData;
	}


	private static String getSearchEnginesCountSql(FilterHeaderDto filter, String suffix) {
		StringBuilder sql = new StringBuilder("SELECT server, COUNT(server) AS total FROM stat_searchengine");
		sql.append(suffix);
		sql.append(" WHERE doc_id >= 0 ").append(filter.getRootGroupIdQuery());
		sql.append(" AND search_date >= ? AND search_date <= ? ");
		sql.append(" AND "+DB.fixAiCiCol("query")+" = ? ");
		sql.append(" GROUP BY server");
		return sql.toString();
	}

	private static Map<String, SearchEnginesDTO> getStatKeywordsDetails(FilterHeaderDto filter) {
		Map<String, SearchEnginesDTO> mapData = new HashMap<>();

		for (String suffix : StatNewDB.getTableSuffix("stat_searchengine", filter.getDateFrom().getTime(), filter.getDateTo().getTime())) {
			String sql = getSearchEnginesCountSql(filter, suffix);

			new ComplexQuery().setSql(sql).setParams(filter.getDateFrom(), filter.getDateTo(), DB.fixAiCiValue(filter.getUrl())).setMaxSize(MAX_ROWS).list(new Mapper<SearchEnginesDTO>() {
				@Override
				public SearchEnginesDTO map(ResultSet rs) throws SQLException {
					String serverName =  DB.prepareString(DB.getDbString(rs, "server"), 25);

					if(mapData.get(serverName) == null) mapData.put(serverName, new SearchEnginesDTO(serverName, rs.getInt("total")));
					else {
						SearchEnginesDTO tmp = mapData.get(serverName);
						tmp.setAccesCount( tmp.getAccesCount() +  rs.getInt("total"));
						mapData.put(serverName, tmp);
					}

					return null;
				}
			});
		}
		return mapData;
	}

	/********************************** SEO - BOTS  SECTIONS **************************************************/
	public static List<BotsDetailsDTO> getUserStatViews(FilterHeaderDto filter, Integer botId) {
		botId = Tools.getIntValue(botId, -1);
		if(botId == -1) throw new ConstraintViolationException("Invalid parameter botId, cant be empty.", null);

		List<Column> columns = StatNewDB.getUserStatViews(botId, filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupId());

		return convert(columns);
	}

	private static List<BotsDetailsDTO> convert(List<Column> columns) {
        List<BotsDetailsDTO> data = new ArrayList<>();
        for(Column column : columns) {
            data.add(
                new BotsDetailsDTO(
                    Tools.getIntValue(column.getColumn4(), -1),
                    column.getDateColumn1(),
                    column.getColumn1(),
                    column.getColumn2(),
                    column.getColumn3()
                )
            );
        }
        return data;
    }

	public static Map<String, List<BotsDetailsDTO>> getBotsDetailsLineChartData(Date from, Date to, int botId, int rootGroupId) {
        Map<Date, Integer> viewsCount = new HashMap<>();
		Map<String, List<BotsDetailsDTO>> result = new HashMap<>();
        result.put("", new ArrayList<>());

        for(Column column : StatNewDB.getUserStatViews(botId, from, to, rootGroupId)) {
            Date dayDate = formatDate( column.getDateColumn1() );

            if(viewsCount.get(dayDate) == null)
                viewsCount.put(dayDate, 1);
            else
                viewsCount.put(dayDate, viewsCount.get(dayDate) + 1);
        }

        for (Map.Entry<Date, Integer> entry : viewsCount.entrySet()) {
            result.get("").add(
                new BotsDetailsDTO(
                    entry.getKey(),
                    entry.getValue()
                )
            );
        }
        return result;
	}

	private static Date formatDate(Date dateToFormat) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateToFormat);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	/********************************** SUPPORT METHODS **************************************************/
	private static String getSearchEnginesSelectSql(String suffix, Integer webPageId, String rootGroupIdQuery) {
		StringBuilder sql = new StringBuilder("SELECT DISTINCT server FROM stat_searchengine").append(suffix);
		sql.append(" WHERE ");
		//Now we must speify date range
		sql.append(" search_date >= ? AND search_date <= ? ");

		//For specific webpage
		if(webPageId > 0) {
			sql.append(" AND ").append("doc_id=").append(webPageId);
		} else if(!Tools.isEmpty(rootGroupIdQuery)){
			//Make sure that not doc_id > 0 (in case when docId is not specified)
			sql.append(" AND doc_id > 0 ");
		}

		//Append rootGroupIdQuery
		sql.append(rootGroupIdQuery);

		return sql.toString();
	}

	public static List<String> getSearchEnginesSelectValues(String dayDate, Integer rootDir, Integer webPageId) {
		Set<String> finalList = new LinkedHashSet<>();
		Date[] dateRangeArr = StatService.processDateRangeString(dayDate);
		String rootGroupIdQuery = FilterHeaderDto.groupIdToQuery(rootDir);
		webPageId = Tools.getIntValue(webPageId, -1);

		String[] suffixis = StatNewDB.getTableSuffix("stat_searchengine", dateRangeArr[0].getTime(), dateRangeArr[1].getTime());
		for(String suffix : suffixis) {
			List<String> searchEngines = new SimpleQuery().forListString(getSearchEnginesSelectSql(suffix, webPageId, rootGroupIdQuery), dateRangeArr[0], dateRangeArr[1]);

			finalList.addAll(searchEngines);
		}

		return new ArrayList<>(finalList);
	}

	public static Map<Integer, String> getWebPageSelectValues(int rootGroupId, DocDetailsRepository repo) {
		Map<Integer, String> webPagesSelectData = new HashMap<>();
        rootGroupId = Tools.getIntValue(rootGroupId, -1);
        if(rootGroupId == -1) return webPagesSelectData;

        List<DocDetails> webPages = repo.findAllByGroupId(rootGroupId);
        for(DocDetails webpage : webPages)
            webPagesSelectData.put(webpage.getDocId(), webpage.getTitle());

        return webPagesSelectData;
	}

	public static List<UserDetails> getUsersFromIds(List<Integer> userIds) {
		List<UserDetails> users = new ArrayList<>();
        for(Integer userId : userIds) {
            UserDetails user = UsersDB.getUserCached(userId);
            if(user != null) users.add(user);
        }
		return users;
	}

	private static List<String> getDistinctSeoKeywords() {
		return new SimpleQuery().forListString("SELECT DISTINCT name FROM seo_keywords");
	}


	public static void saveKeywordsPositions() {
		if (Tools.isEmpty(Constants.getString("seo.serpApiKey"))) return;

		for(SeoKeyword keyword : SeoManager.getSeoKeywords(-1)) {
			Integer position = getSeoKeywordActualPosition(keyword);
			//Add new seo_google_position record
			(new SimpleQuery()).execute("INSERT INTO seo_google_position (keyword_id, position, search_datetime) VALUES (?, ?, ?)", keyword.getSeoKeywordId(), position, new Date());
			//Update seo_keywords, set new actual position
			(new SimpleQuery()).execute("UPDATE seo_keywords SET actual_position=? WHERE seo_keyword_id=?", position, keyword.getSeoKeywordId());
		}
	}

	/********************************** LOGIC TO HANDLE KEYWORDS POSITION UPDATING **************************************************/
	private enum EngineType {
		GOOGLE,
		BING,
		YAHOO,
		NOT_SUPPORTED
	}

	private static EngineType getEngineType(String searchBot) {
		if(searchBot == null || searchBot.isEmpty()) return EngineType.NOT_SUPPORTED;

		if(searchBot.indexOf("google") != -1)
			return EngineType.GOOGLE;
		else if(searchBot.indexOf("yahoo") != -1)
			return EngineType.YAHOO;
		else if(searchBot.indexOf("bing") != -1)
			return EngineType.BING;
		return EngineType.NOT_SUPPORTED;
	}

	private static String prepareUrlForRequest(SeoKeyword keyword) {
		String searchBot = keyword.getSearchBot();
		EngineType engineType = getEngineType(searchBot);

		//For example is we have seynam.cz (SerpAPI does NOT support seznam.cz)
		if(engineType == EngineType.NOT_SUPPORTED) return null;

		String query = keyword.getName();
		StringBuilder url = new StringBuilder("https://serpapi.com/search.json?");

		if(engineType == EngineType.GOOGLE) {
			url.append("q=").append(query);
			url.append("&engine=google");

			String postfix = null;
			String[] parseByDot = searchBot.split("\\.");
			if(parseByDot.length > 1) postfix = parseByDot[parseByDot.length - 1];

			if(postfix != null) {
				//Set google domain
				url.append("&google_domain=google.").append(postfix);

				//postfix .com represent USA - when we are speaking about "location"
				url.append("&gl=").append(postfix.equals("com") ? "us" : postfix);

				//postfix .com represent English - when we are speaking about "language"
				String language = postfix.equals("com") ? "en" : postfix;
				//CZ language is represented by CS
				language = language.equals("cz") ? "cs" : language;
				url.append("&hl=").append(language);
			}
			//Max number of results
			url.append("&num=").append(Constants.getInt("seo.serpApiGoogleMaxResult"));
		} else if(engineType == EngineType.YAHOO) {
			url.append("p=").append(query);
			url.append("&engine=yahoo");

			String prefix = null;
			String[] parseByDot = searchBot.split("\\.");
			//Test if domain is default one, without country prefix
			boolean isDefault = false;
			if(parseByDot[0].equals("yahoo")) isDefault = true;
			if(parseByDot[0].equals("search") && parseByDot[1].equals("yahoo")) isDefault = true;

			//If start is not "yahoo" nor "search.yahoo", prefix is country code (pl, fr ..)
			if(!isDefault) {
				prefix = parseByDot[0];

				//Set Yahoo domain
				url.append("&yahoo_domain=").append(prefix);

				//Yahoo engines does NOT support many country like sk. / cz. / hu.
				url.append("&vc=").append(prefix); //Position

				//Set language
				url.append("&vl=").append(prefix);
			} else {
				//Domain HAS NO prefix

				//Set default country as US
				url.append("&vc=").append("us");

				//Set default language
				url.append("&vl=").append("en");
			}
			//Result number filter not supported
		} else if(engineType == EngineType.BING) {
			url.append("q=").append(query);
			url.append("&engine=bing");

			//BING does NOT support country prefix or postfix (we cant figure out country by domain)

			//Use it as country
			url.append("&cc=").append( getBingCountry( Constants.getString("defaultLanguage") ) );
		}
		//Set API key
		url.append("&api_key=").append(Constants.getString("seo.serpApiKey"));
		return url.toString();
	}

	private static String getBingCountry(String code) {
		String[] supportedCountries =
			{"AR", "AU", "AT", "BE", "BR", "CA", "CL", "DK", "FI", "FR", "DE", "HK", "IN", "ID", "IT", "JP", "KR", "MY", "MX", "NL", "NZ", "NO", "PL", "PT", "PH", "SA", "ZA", "ES", "SE", "CH", "TW", "TR", "GB", "US"};

		for(String country : supportedCountries)
			if(country.equalsIgnoreCase(code)) return code;

		//If not suspported, return US as default
		return "us";
	}

	private static Integer getSeoKeywordActualPosition(SeoKeyword keyword) {
		RestTemplate restTemplate = new RestTemplate();
		ObjectMapper objectMapper = new ObjectMapper();
		Integer position = -1;

		String url = prepareUrlForRequest(keyword);
		if(url == null) return position;

		try {
			String response = restTemplate.getForObject(url, String.class);
			String resultsStringArr = JsonTools.getValue(response, "organic_results");

			//JsonTools.getValue can return null is parsing JSON gone wrong
			if(resultsStringArr == null) return position;

			JsonNode rootNode = objectMapper.readTree(resultsStringArr);
			for(JsonNode node : rootNode) {
				//If node has NOT "link" or "position" param, it's useless for us, skip this node
				if(!node.has("link") || !node.has("position")) continue;

				String link = node.get("link").toString();
				if(link != null && (link.indexOf(keyword.getDomain()) != -1)) {
					position = node.get("position").intValue();
					break;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

		return position;
	}
}
