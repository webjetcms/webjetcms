package sk.iway.iwcm.doc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;


/**
 *  RelatedPagesDB.java - Informacie o pribuznych dokumentoch
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.20 $
 *@created      Date: 29.9.2004 20:48:03
 *@modified     $Date: 2009/12/14 10:21:36 $
 */
public class RelatedPagesDB
{
	protected RelatedPagesDB() {
		//utility class
	}

	public static List<DocDetails> getRelatedPagesByGroups(String perexGroupIds, String rootGroupIds, int actualDocId)
	{
		return getRelatedPagesByGroups(perexGroupIds, rootGroupIds, actualDocId, true);
	}

	/**
	 * Vrati List typu DocDetails s pribuznymi strankami pre zadanu skupinu (perexGroupId),
	 * ak je parentDocId > 0, tak sa prislusna stranka nebude zobrazovat na stranke (odstrani sa odkaz sam na seba)
	 * @param perexGroupIds  - ID perex skupiny
	 * @param rootGroupIds 	 - ID skupin, z ktorej sa zobrazia odkazy na stranke (napr. iba stranky v priecinku Novinky)
	 * @param actualDocId 	 - docid stranky na ktorej sa zobrazuje komponenta
	 * @param checkDuplicity - kontrola na duplicitu pri multigroup clankoch
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<DocDetails> getRelatedPagesByGroups(String perexGroupIds, String rootGroupIds, int actualDocId, boolean checkDuplicity)
	{
		DebugTimer dt = new DebugTimer("getRelatedPagesByGroups");
		dt.diff("   params: "+perexGroupIds+", "+rootGroupIds+", "+actualDocId);


		List<DocDetails> ret = null;
		Cache c = Cache.getInstance();
		String cacheKey = "RelatedPagesDB-getByGroup-"+perexGroupIds+"-"+rootGroupIds;
		int cacheInMinutes = Constants.getInt("RelatedPagesDBCacheMinutes");
		if (cacheInMinutes > 0)
		{
			ret = (List<DocDetails>)c.getObject(cacheKey);

			if (ret != null)
			{
				dt.diff("returning from cache");
				return(filterRelatetedPagesDoc(ret, actualDocId));
			}

		}

		boolean perexGroupUseJoin = Constants.getBoolean("perexGroupUseJoin");
		String[] rGroups = null;
		List<DocDetails> relatedPages = null;
		ret = new ArrayList<>();
		int rGroupId;
		boolean found;

		try
		{
			//Logger.println(this,"perexGroupName: "+perexGroupName+"\trootGroupIds: "+rootGroupIds+"\tparentDocId: "+parentDocId);
			if (Tools.isNotEmpty(perexGroupIds))
			{
				if(perexGroupUseJoin == false)
				{
					relatedPages = new ArrayList<>();
					String[] groupNames = getTokens(perexGroupIds, ",");
					int perexGroupId;
					for (int j=0; j<groupNames.length; j++)
					{
						perexGroupId = Tools.getIntValue(groupNames[j], -1);
						dt.diff(" getting related pages: "+j+"/"+groupNames.length);
						if(perexGroupId > 0) relatedPages.addAll(getRelatedPages(String.valueOf(perexGroupId), false, checkDuplicity));
					}
				}
				else
				{
					dt.diff(" getting related pages for perex groups: "+perexGroupIds);
					relatedPages = getRelatedPages(perexGroupIds, true, checkDuplicity);
				}

				if (Tools.isNotEmpty(rootGroupIds)) rGroups = getTokens(rootGroupIds, ",");
				for (DocDetails docDet : relatedPages)
				{

					//pre ziskane docId testujem, ci s nachadza v ziadanom priecinku
					if (docDet != null)
					{
						found = false;

						if (rGroups != null)
						{
							for (int k=0; k<rGroups.length; k++)
							{
								rGroupId = Tools.getIntValue(rGroups[k], -1);
								if (rGroupId == docDet.getGroupId())
								{
									found = true;
									//Logger.println(this,"--- preslo DocID: "+docDetails.getDocId()+"  RootGroup: "+rGroupId+" parentDocId: "+parentDocId);
								}
							}
						}
						else
						{
							found = true;
						}

						if (found)
						{
							ret.add(docDet);
							//Logger.println(this,"ret size: "+ret.size());
						}
					}
				}

				dt.diff("  size:"+ret.size());

				if (ret.isEmpty() == false)
				{
					//Logger.println(this,"---\nnot sorted - ret.size: "+ret.size());
					//usortuj to podla datumu - v poradi od najnovsieho
					Collections.sort(ret,
						new Comparator<DocDetails>()
						{
						@Override
							public int compare(DocDetails d1, DocDetails d2)
							{
								int result;
								if (d1.getPublishStart() > d2.getPublishStart())
									result = -1;
								else
									if (d1.getPublishStart() == d2.getPublishStart())
										result = 0;
									else
										result = 1;
								return (result);
							}
						});
					//Logger.println(this,"sorted - ret.size: "+ret.size());
				}

			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		if (cacheInMinutes > 0)
		{
			c.setObject(cacheKey, ret, cacheInMinutes);
		}

		ret = filterRelatetedPagesDoc(ret, actualDocId);
		dt.diff("done, size: "+ret.size());

		return(ret);
	}

	/**
	 * Z vysledkov relatedPages odfiltruje zadanu (aktualnu) stranku, robi sa to takto kvoli optimalizacii cache
	 * @param relatedPages
	 * @param docId
	 * @return
	 */
	private static List<DocDetails> filterRelatetedPagesDoc(List<DocDetails> relatedPages, int docId)
	{
		List<DocDetails> ret = new ArrayList<>();

		for (DocDetails doc : relatedPages)
		{
			if (doc.getDocId()!=docId) ret.add(doc);
		}

		return ret;
	}

	/**
	 * Usortuje suvosiace stranky podla najlepsej zhody - cim viac zhodnych skupin tym skor v zozname
	 * @param relatedPages
	 * @param perexGroupIdsString - zoznam perex skupin aktualnej stranky, napr ,1,4,22,
	 * @param minMatch - minimalny pocet zhodnych skupin (vratane)
	 * @return
	 */
	public static List<DocDetails> sortByBestMatch(List<DocDetails> relatedPages, String perexGroupIdsString, int minMatch)
	{
		int[] pagePerexGroups = Tools.getTokensInt(perexGroupIdsString, ",");

		List<DocDetails> filtered = new ArrayList<>();

		for (DocDetails relatedOrig : relatedPages)
		{
			DocDetails related = new DocDetails();
			related.setDocId(relatedOrig.getDocId());
			related.setVirtualPath(relatedOrig.getVirtualPath());
			related.setExternalLink(relatedOrig.getExternalLink());
			related.setTitle(relatedOrig.getTitle());
			related.setData(relatedOrig.getData());
			related.setPerexGroupString(relatedOrig.getPerexGroupIdsString());
			related.setHtmlData(relatedOrig.getHtmlData());

			int match = getBestMatch(related, pagePerexGroups);
			related.setSortPriority(match);

			Logger.debug(RelatedPagesDB.class, "sortByBestMatch: match="+match+" related="+related.getPerexGroupIdsString()+" page="+perexGroupIdsString);

			if (minMatch < 1 || match >= minMatch) filtered.add(related);
		}

		//usortuj
		Collections.sort(filtered, new Comparator<DocDetails>() {
			@Override
   		public int compare(DocDetails d1, DocDetails d2)
   		{
   		    if (d1.getSortPriority() < d2.getSortPriority())
   		      return 1;
   		    else if (d1.getSortPriority() > d2.getSortPriority())
   		      return -1;
   		    else
   		      return 0;
   		}

		});

		return filtered;
	}

	/**
	 * Vrati pocet zhodnych perex skupin medzi zadanou strankou a zadanymi perex skupinami
	 * @param relatedPage
	 * @param pagePerexGroups
	 * @return
	 */
	private static int getBestMatch(DocDetails relatedPage, int[] pagePerexGroups)
	{
		//porovnaj perex skupiny
		int pocetZhodnych = 0;

		for (int i=0; i<pagePerexGroups.length; i++)
		{
			Integer[] perexGroups = relatedPage.getPerexGroups();
			for (Integer pGroupId : perexGroups)
			{
				if (pGroupId != null && pGroupId.intValue() == pagePerexGroups[i]) pocetZhodnych++;
			}
		}

		return pocetZhodnych;
	}


	/**
	 * Vrati List s pribuznymi strankami pre zadanu skupinu
	 * @param perexGroupIds - perex skupiny
	 * @return
	 */
	private static List<DocDetails> getRelatedPages(String perexGroupIds, boolean perexGroupUseJoin, boolean checkDuplicity)
	{
		DebugTimer dt = new DebugTimer("getRelatedPages("+perexGroupIds+")");

		List<Integer> docIdList = checkDuplicity ? new ArrayList<>() : null;
		List<DocDetails> ret = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			if (Tools.isNotEmpty(perexGroupIds))
			{
				db_conn = DBPool.getConnection();

				if(perexGroupUseJoin == false)
				{
					String sql = "SELECT ";
					if (Constants.DB_TYPE == Constants.DB_MSSQL) sql += " TOP "+Constants.getInt("relatedPagesMaxSize")+" ";
					sql += DocDB.getDocumentFieldsNodata()+" FROM documents d WHERE d.available="+DB.getBooleanSql(true)+" AND d.perex_group LIKE ? ORDER BY d.publish_start DESC";
					if (Constants.DB_TYPE == Constants.DB_MYSQL || Constants.DB_TYPE==Constants.DB_PGSQL)
					{
						sql = Tools.replace(sql, "d.doc_id", "DISTINCT d.doc_id");
						sql += " LIMIT "+Constants.getInt("relatedPagesMaxSize");
					}

					ps = db_conn.prepareStatement(sql);
					ps.setString(1, "%," +perexGroupIds+ ",%");
					rs = ps.executeQuery();
				}
				else
				{
					String groupNamesIn = "";
					String[] groupNames = getTokens(perexGroupIds, ",");
					String gn;
					for (int j=0; j<groupNames.length; j++)
					{
						gn = groupNames[j];
						if(Tools.getIntValue(gn,0) > 0) groupNamesIn += gn+","; //NOSONAR
					}
					groupNamesIn = groupNamesIn.endsWith(",") ? groupNamesIn.substring(0, groupNamesIn.length()-1) : groupNamesIn;

					if (Tools.isNotEmpty(groupNamesIn))
					{
						String sql = "SELECT ";
						if (Constants.DB_TYPE == Constants.DB_MSSQL) sql += " TOP "+Constants.getInt("relatedPagesMaxSize")+" ";
						//#17157 - uprava getRelatedPages - zmena setovanie do IN podmienky BEZ PreparedStatement (robilo to haluze pri cislach skupin vacsich ako 10000 ktore boli na zaciatku)
						sql += DocDB.getDocumentFieldsNodata()+" FROM documents d LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id WHERE d.available="+DB.getBooleanSql(true)+" AND p.perex_group_id IN ("+groupNamesIn+") ORDER BY d.publish_start DESC";
						if (Constants.DB_TYPE == Constants.DB_MYSQL || Constants.DB_TYPE==Constants.DB_PGSQL) sql += " LIMIT "+Constants.getInt("relatedPagesMaxSize");
						ps = db_conn.prepareStatement(sql);
						rs = ps.executeQuery();
					}
				}

				dt.diff("   execute done");

				List<DocDetails> docsFromDB = new ArrayList<>();
				if (rs != null)
				{
					while(rs.next())
					{
						DocDetails docDet = new DocDetails();
						DocDB.getDocDetails(rs, docDet, true, true);

						//Logger.println(RelatedPagesDB.class,"related: " +docDet.getDocId());

						docsFromDB.add(docDet);
					}

					dt.diff("   rs done, size="+ret.size());

					rs.close();
				}
				if (ps != null) ps.close();
				db_conn.close();

				//jeeff: upravene na takyto dvojity cyklus pretoze pri checkDuplicity sa vola MultigroupMappingDB kde je potrebne dalsie DB spojenie a nastava nam deadlock
				for (DocDetails docDet : docsFromDB)
				{
					//docDet = DocDB.getDocDetails(rs, false);
					if(checkDuplicity && docIdList != null)
					{
						if(docIdList.contains(Integer.valueOf(docDet.getDocId()))) continue;

						int masterId = MultigroupMappingDB.getMasterDocId(docDet.getDocId());
						List<MultigroupMapping> slavesId = MultigroupMappingDB.getSlaveMappings(masterId > 0 ? masterId : docDet.getDocId());

						if(masterId > 0)
							docIdList.add(Integer.valueOf(masterId));
						if(slavesId != null && slavesId.isEmpty()==false)
							for(MultigroupMapping mm : slavesId)
								docIdList.add(Integer.valueOf(mm.getDocId()));
						if(masterId < 1 || docIdList.isEmpty())
							docIdList.add(Integer.valueOf(docDet.getDocId()));

						ret.add(docDet);
					}
					else
					{
						ret.add(docDet);
					}
				}
			}

			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		dt.diff("done");

		return (ret);

	}


	/**
	 * Vrati pole typu String, s jednotlivymi polozkami v retazci, ak sa retazec neda rozdelit, vrati prazdne pole
	 * @param groups 	- retazec, ktory sa ma rozparsovat
	 * @param delimiter
	 * @return
	 */
	public static String[] getTokens(String groups, String delimiter)
	{
		String[] ret = new String[0];
		StringTokenizer st;
		int i = 0;
		try
		{
			if (Tools.isNotEmpty(groups))
			{
				st = new StringTokenizer(groups, delimiter);
				ret = new String[st.countTokens()];
				while (st.hasMoreTokens())
				{
					ret[i++] = st.nextToken().trim();
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(ret);

	}




}

