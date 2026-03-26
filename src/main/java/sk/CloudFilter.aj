package sk;

import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PerexGroupBean;

/**
 *  CloudFilter.aj
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff Mari�n Hala� $
 *@version      $Revision: 1.3 $
 *@created      Date: 14.2.2013 15:19:06
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public privileged aspect CloudFilter {

	public pointcut groupsDetailsListMethod(): call(public List<GroupDetails> GroupsDB.*(..));
	public pointcut docDetailsListMethod(): call(List<DocDetails> DocDB.*(..)) || call(List<DocDetails> DocDB.*(..));
	public pointcut docDbPerexListMethod(): call(public List<PerexGroupBean> DocDB.*(..));

	List<PerexGroupBean> around(): docDbPerexListMethod()
	{
		List<PerexGroupBean> perexOriginal = proceed();
		if(InitServlet.isTypeCloud() )
		{
			//Logger.debug(DocDB.class,"AspectJ -> Filtering PerexGroupBean by domain");
			return filterPerexByDomain(perexOriginal);
		}
		else
			return perexOriginal;
	}

	List<DocDetails> around(): docDetailsListMethod(){
		List<DocDetails> docsL =  proceed();
		List<DocDetails> docs = new ArrayList<DocDetails>(docsL);
		if(InitServlet.isTypeCloud())
		{
			docs = filterByDomainDocs(docs);
		}
		return docs;
	}

	List<GroupDetails> around() : groupsDetailsListMethod(){
		List<GroupDetails> groups = proceed();
		if(InitServlet.isTypeCloud())
		{
			groups = filterByDomain(groups);
		}
		return groups;
	 }

	 List<GroupDetails> around() : groupsDetailsListMethod(){
			List<GroupDetails> groups = proceed();
			if(InitServlet.isTypeCloud())
			{
				groups = filterByDomain(groups);
			}
			return groups;
	}


	/**
	 * Vyfiltruje GroupDetails zo zoznamu podla domeny aktualne nastavenej v RequestBean
	 * @param groups zoznam adresarov
	 * @return vyfiltrovany zoznam, pouziva equalsIgnoreCase
	 */
   private List<GroupDetails> filterByDomain(List<GroupDetails> groups)
	{
		if (groups == null) return null;

   	List<GroupDetails> filtered = new ArrayList<GroupDetails>(groups.size());
		String domain = CloudToolsForCore.getDomainName();
		//Logger.debug(GroupsDB.class,"Filtering groups by domain : " + domain);
		for(GroupDetails group : groups)
		{
			if(group!=null && group.getDomainName()!=null && group.getDomainName().equalsIgnoreCase(domain))
				filtered.add(group);
		}
		//Logger.debug(GroupsDB.class,"Groups filtered. Before count: "+ groups.size() + " after: " + filtered.size());
		return filtered;
	}


   /**
	 * Vyfiltruje DocDetails zo zoznamu podla domeny aktualne nastavenej v RequestBean
	 * @param docs zoznam dokumentov
	 * @return vyfiltrovany zoznam, pouziva equalsIgnoreCase na meno domeny adresara v ktorom sa doc nachadza
	 */
   private List<DocDetails> filterByDomainDocs(List<DocDetails> docs)
  	{
   	List<DocDetails> filtered = new ArrayList<DocDetails>(docs.size());
   	String domain = CloudToolsForCore.getDomainName();
		//Logger.debug(DocDB.class,"Filtering docs by domain : " + domain);
   	GroupDetails g = null;
		for(DocDetails doc : docs)
   	{
   		if((g =doc.getGroup())!= null)
   			if(g.getDomainName()!=null && g.getDomainName().equalsIgnoreCase(domain))
   				filtered.add(doc);
   	}
   	//Logger.debug(GroupsDB.class,"Docs filtered. Before count: "+ docs.size() + " after: " + filtered.size());
   	return filtered;
  	}

   /**
    * Vyfiltuje perexOriginal podla aktualnej domeny do filteredPerex. Ak je v perexOriginal.getAvailableGroupsInt() aspon jedna
    * grupa z inej domeny, nevrati dany perex. Taktiez odfiltruje perexy ktore nemaju zadanu availabe-grupu.
    * @param perexOriginal - zdrojovy naplneny perex
    * @param filteredPerex
    * @return
    */
   private List<PerexGroupBean> filterPerexByDomain (List<PerexGroupBean> perexOriginal)
   {
   		List<PerexGroupBean> filteredPerex = (perexOriginal == null )? new ArrayList<PerexGroupBean>(0) : new ArrayList<PerexGroupBean>(perexOriginal.size());
		boolean passed = true;
		boolean notEmpty = false;
		GroupDetails gd = null;
		if(perexOriginal != null)
		for(PerexGroupBean perex : perexOriginal)
		{
			if(perex.getAvailableGroupsInt() != null && perex.getAvailableGroupsInt().length > 0)
			{
				passed = true;
				notEmpty = false;
				int[] perexy = perex.getAvailableGroupsInt();
				for (int j=0; j< perex.getAvailableGroupsInt().length; j++)
				{
					if((gd = GroupDetails.getById(perexy[j]) )!= null)
					{
						if (gd.getDomainName()==null || !gd.getDomainName().equalsIgnoreCase(CloudToolsForCore.getDomainName()))
							passed = false;
						else
							notEmpty = true;
					}
				}
				if(passed && notEmpty)
					filteredPerex.add(perex);
			}
		}
		//Logger.debug(DocDB.class,"AspectJ -> Filtering PerexGroupBean by domain ("+CloudToolsForCore.getDomainName()+"), count before: "+perexOriginal.size()+" count after: "+filteredPerex.size());
		return filteredPerex;
   }
}
